/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool3.DestroyMode;
import org.apache.commons.pool3.KeyedPooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.Waiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AbandonedConfig}.
 */
public class TestAbandonedKeyedObjectPool {

    private final class ConcurrentBorrower extends Thread {
        private final ArrayList<PooledTestObject> borrowed;

        private ConcurrentBorrower(final ArrayList<PooledTestObject> borrowed) {
            this.borrowed = borrowed;
        }

        @Override
        public void run() {
            try {
                borrowed.add(pool.borrowObject(0));
            } catch (final Exception e) {
                // expected in most cases
            }
        }
    }

    private final class ConcurrentReturner extends Thread {
        private final PooledTestObject returned;

        private ConcurrentReturner(final PooledTestObject obj) {
            returned = obj;
        }

        @Override
        public void run() {
            try {
                sleep(20);
                pool.returnObject(0, returned);
            } catch (final Exception e) {
                // ignore
            }
        }
    }

    private static final class SimpleFactory implements KeyedPooledObjectFactory<Integer, PooledTestObject, InterruptedException> {

        private final long destroyLatencyMillis;
        private final long validateLatencyMillis;

        private SimpleFactory() {
            destroyLatencyMillis = 0;
            validateLatencyMillis = 0;
        }

        private SimpleFactory(final long destroyLatencyMillis, final long validateLatencyMillis) {
            this.destroyLatencyMillis = destroyLatencyMillis;
            this.validateLatencyMillis = validateLatencyMillis;
        }

        @Override
        public void activateObject(final Integer key, final PooledObject<PooledTestObject> obj) {
            obj.getObject().setActive(true);
        }

        @Override
        public void destroyObject(final Integer key, final PooledObject<PooledTestObject> obj) throws InterruptedException {
            destroyObject(key, obj, DestroyMode.NORMAL);
        }

        @Override
        public void destroyObject(final Integer key, final PooledObject<PooledTestObject> obj, final DestroyMode destroyMode) throws InterruptedException {
            obj.getObject().setActive(false);
            // while destroying instances, yield control to other threads
            // helps simulate threading errors
            Thread.yield();
            if (destroyLatencyMillis != 0) {
                Thread.sleep(destroyLatencyMillis);
            }
            obj.getObject().destroy(destroyMode);
        }

        @Override
        public PooledObject<PooledTestObject> makeObject(final Integer key) {
            return new DefaultPooledObject<>(new PooledTestObject());
        }

        @Override
        public void passivateObject(final Integer key, final PooledObject<PooledTestObject> obj) {
            obj.getObject().setActive(false);
        }

        @Override
        public boolean validateObject(final Integer key, final PooledObject<PooledTestObject> obj) {
            Waiter.sleepQuietly(validateLatencyMillis);
            return true;
        }
    }

    private GenericKeyedObjectPool<Integer, PooledTestObject, InterruptedException> pool;

    private AbandonedConfig abandonedConfig;

    @SuppressWarnings("deprecation")
    @BeforeEach
    public void setUp() {
        abandonedConfig = new AbandonedConfig();

        // Uncomment the following line to enable logging:
        // abandonedConfig.setLogAbandoned(true);

        // One second Duration.
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        assertEquals(TestConstants.ONE_SECOND_DURATION, abandonedConfig.getRemoveAbandonedTimeoutDuration());

        pool = new GenericKeyedObjectPool<>(
               new SimpleFactory(),
               new GenericKeyedObjectPoolConfig<>(),
               abandonedConfig);
    }

    @AfterEach
    public void tearDown() throws Exception {
        final ObjectName jmxName = pool.getJmxName();
        final String poolName = Objects.toString(jmxName, null);
        pool.clear();
        pool.close();
        pool = null;

        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final Set<ObjectName> result = mbs.queryNames(new ObjectName("org.apache.commoms.pool3:type=GenericKeyedObjectPool,*"), null);
        // There should be no registered pools at this point
        final int registeredPoolCount = result.size();
        final StringBuilder msg = new StringBuilder("Current pool is: ");
        msg.append(poolName);
        msg.append("  Still open pools are: ");
        for (final ObjectName name : result) {
            // Clean these up ready for the next test
            msg.append(name.toString());
            msg.append(" created via\n");
            msg.append(mbs.getAttribute(name, "CreationStackTrace"));
            msg.append('\n');
            mbs.unregisterMBean(name);
        }
        assertEquals(0, registeredPoolCount, msg.toString());
    }

    /**
     * Verify that an object that gets flagged as abandoned and is subsequently
     * invalidated is only destroyed (and pool counter decremented) once.
     *
     * @throws InterruptedException May occur in some failure modes
     */
    @Test
    public void testAbandonedInvalidate() throws InterruptedException {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(Duration.ofMillis(2000));
        pool.close();  // Unregister pool created by setup
        pool = new GenericKeyedObjectPool<>(
                // destroys take 100 millis
                new SimpleFactory(100, 0),
                new GenericKeyedObjectPoolConfig<>(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        pool.setDurationBetweenEvictionRuns(Duration.ofMillis(250));
        PooledTestObject pooledObj = null;
        final Integer key = 0;
        for (int i = 0; i < 5; i++) {
            pooledObj = pool.borrowObject(key);
        }
        Thread.sleep(1000); // abandon checked out instances and let evictor start
        if (!pool.getKeys().contains(key)) {
            Thread.sleep(1000); // Wait a little more.
        }
        if (!pool.getKeys().contains(key)) {
            Thread.sleep(1000); // Wait a little more.
        }
        pool.invalidateObject(key, pooledObj); // Should not trigger another destroy / decrement
        Thread.sleep(2000); // give evictor time to finish destroys
        assertEquals(0, pool.getNumActive());
        assertEquals(5, pool.getDestroyedCount());
    }

    /**
     * Verify that an object that gets flagged as abandoned and is subsequently returned
     * is destroyed instead of being returned to the pool (and possibly later destroyed
     * inappropriately).
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testAbandonedReturn() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        pool.close();  // Unregister pool created by setup
        pool = new GenericKeyedObjectPool<>(
                new SimpleFactory(200, 0),
                new GenericKeyedObjectPoolConfig<>(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        PooledTestObject obj = null;
        for (int i = 0; i < n - 2; i++) {
            obj = pool.borrowObject(0);
        }
        Objects.requireNonNull(obj, "Unable to borrow object from pool");
        final int deadMansHash = obj.hashCode();
        final ConcurrentReturner returner = new ConcurrentReturner(obj);
        Thread.sleep(2000);  // abandon checked out instances
        // Now start a race - returner waits until borrowObject has kicked
        // off removeAbandoned and then returns an instance that borrowObject
        // will deem abandoned.  Make sure it is not returned to the borrower.
        returner.start();    // short delay, then return instance
        assertTrue(pool.borrowObject(0).hashCode() != deadMansHash);
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumActive());
    }

    /**
     * Tests fix for Bug 28579, a bug in AbandonedObjectPool that causes numActive to go negative
     * in GenericKeyedObjectPool
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testConcurrentInvalidation() throws Exception {
        final int POOL_SIZE = 30;
        pool.setMaxTotalPerKey(POOL_SIZE);
        pool.setMaxIdlePerKey(POOL_SIZE);
        pool.setBlockWhenExhausted(false);

        // Exhaust the connection pool
        final ArrayList<PooledTestObject> vec = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            vec.add(pool.borrowObject(0));
        }

        // Abandon all borrowed objects
        for (final PooledTestObject element : vec) {
            element.setAbandoned(true);
        }

        // Try launching a bunch of borrows concurrently.  Abandoned sweep will be triggered for each.
        final int CONCURRENT_BORROWS = 5;
        final Thread[] threads = new Thread[CONCURRENT_BORROWS];
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i] = new ConcurrentBorrower(vec);
            threads[i].start();
        }

        // Wait for all the threads to finish
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i].join();
        }

        // Return all objects that have not been destroyed
        for (final PooledTestObject pto : vec) {
            if (pto.isActive()) {
                pool.returnObject(0, pto);
            }
        }

        // Now, the number of active instances should be 0
        assertEquals(0, pool.getNumActive(), "numActive should have been 0, was " + pool.getNumActive());
    }

    public void testDestroyModeAbandoned() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        pool.close();  // Unregister pool created by setup
        pool = new GenericKeyedObjectPool<>(
             // validate takes 1 second
             new SimpleFactory(0, 0),
             new GenericKeyedObjectPoolConfig<>(), abandonedConfig);
        pool.setDurationBetweenEvictionRuns(Duration.ofMillis(50));
        // Borrow an object, wait long enough for it to be abandoned
        final PooledTestObject obj = pool.borrowObject(0);
        Thread.sleep(100);
        assertTrue(obj.isDetached());
    }

    public void testDestroyModeNormal() throws Exception {
        abandonedConfig = new AbandonedConfig();
        pool.close();  // Unregister pool created by setup
        pool = new GenericKeyedObjectPool<>(new SimpleFactory(0, 0));
        pool.setMaxIdlePerKey(0);
        final PooledTestObject obj = pool.borrowObject(0);
        pool.returnObject(0, obj);
        assertTrue(obj.isDestroyed());
        assertFalse(obj.isDetached());
    }

    /**
     * Verify that an object that the evictor identifies as abandoned while it
     * is in process of being returned to the pool is not destroyed.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testRemoveAbandonedWhileReturning() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        pool.close();  // Unregister pool created by setup
        pool = new GenericKeyedObjectPool<>(
             // validate takes 1 second
             new SimpleFactory(0, 1000),
             new GenericKeyedObjectPoolConfig<>(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        pool.setDurationBetweenEvictionRuns(Duration.ofMillis(500));
        pool.setTestOnReturn(true);
        // Borrow an object, wait long enough for it to be abandoned
        // then arrange for evictor to run while it is being returned
        // validation takes a second, evictor runs every 500 ms
        final PooledTestObject obj = pool.borrowObject(0);
        Thread.sleep(50);       // abandon obj
        pool.returnObject(0, obj); // evictor will run during validation
        final PooledTestObject obj2 = pool.borrowObject(0);
        assertEquals(obj, obj2);          // should get original back
        assertFalse(obj2.isDestroyed());  // and not destroyed
    }

    /**
     * JIRA: POOL-300
     */
    @Test
    public void testStackTrace() throws Exception {
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final BufferedOutputStream bos = new BufferedOutputStream(baos);
        final PrintWriter pw = new PrintWriter(bos);
        abandonedConfig.setLogWriter(pw);
        pool.setAbandonedConfig(abandonedConfig);
        pool.setDurationBetweenEvictionRuns(Duration.ofMillis(100));
        final PooledTestObject o1 = pool.borrowObject(0);
        Thread.sleep(2000);
        assertTrue(o1.isDestroyed());
        bos.flush();
        assertTrue(baos.toString().indexOf("Pooled object") >= 0);
    }

    /**
     * Test case for https://issues.apache.org/jira/browse/DBCP-260.
     * Borrow and abandon all the available objects then attempt to borrow one
     * further object which should block until the abandoned objects are
     * removed. We don't want the test to block indefinitely when it fails so
     * use maxWait be check we don't actually have to wait that long.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testWhenExhaustedBlock() throws Exception {
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        pool.setAbandonedConfig(abandonedConfig);
        pool.setDurationBetweenEvictionRuns(Duration.ofMillis(500));

        pool.setMaxTotal(1);

        @SuppressWarnings("unused") // This is going to be abandoned
        final PooledTestObject o1 = pool.borrowObject(0);

        final long startMillis = System.currentTimeMillis();
        final PooledTestObject o2 = pool.borrowObject(0, 5000);
        final long endMillis = System.currentTimeMillis();

        pool.returnObject(0, o2);

        assertTrue(endMillis - startMillis < 5000);
    }
}

