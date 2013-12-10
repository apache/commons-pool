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

package org.apache.commons.pool2.impl;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.TrackedUse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TestCase for AbandonedObjectPool
 *
 * @version $Revision: 1158659 $ $Date: 2011-08-17 05:37:26 -0700 (Wed, 17 Aug 2011) $
 */
public class TestAbandonedObjectPool {
    private GenericObjectPool<PooledTestObject> pool = null;
    private AbandonedConfig abandonedConfig = null;

    @Before
    public void setUp() throws Exception {
        abandonedConfig = new AbandonedConfig();

        // -- Uncomment the following line to enable logging --
        // abandonedConfig.setLogAbandoned(true);

        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setRemoveAbandonedTimeout(1);
        pool = new GenericObjectPool<PooledTestObject>(
               new SimpleFactory(),
               new GenericObjectPoolConfig(),
               abandonedConfig);
    }

    @After
    public void tearDown() throws Exception {
        String poolName = pool.getJmxName().toString();
        pool.clear();
        pool.close();
        pool = null;

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> result = mbs.queryNames(new ObjectName(
                "org.apache.commoms.pool2:type=GenericObjectPool,*"), null);
        // There should be no registered pools at this point
        int registeredPoolCount = result.size();
        StringBuilder msg = new StringBuilder("Current pool is: ");
        msg.append(poolName);
        msg.append("  Still open pools are: ");
        for (ObjectName name : result) {
            // Clean these up ready for the next test
            msg.append(name.toString());
            msg.append(" created via\n");
            msg.append(mbs.getAttribute(name, "CreationStackTrace"));
            msg.append('\n');
            mbs.unregisterMBean(name);
        }
        Assert.assertEquals(msg.toString(), 0, registeredPoolCount);
    }

    /**
    * Tests fix for Bug 28579, a bug in AbandonedObjectPool that causes numActive to go negative
    * in GenericObjectPool
    *
    */
    @Test
    public void testConcurrentInvalidation() throws Exception {
        final int POOL_SIZE = 30;
        pool.setMaxTotal(POOL_SIZE);
        pool.setMaxIdle(POOL_SIZE);
        pool.setBlockWhenExhausted(false);

        // Exhaust the connection pool
        ArrayList<PooledTestObject> vec = new ArrayList<PooledTestObject>();
        for (int i = 0; i < POOL_SIZE; i++) {
            vec.add(pool.borrowObject());
        }

        // Abandon all borrowed objects
        for (int i = 0; i < vec.size(); i++) {
            vec.get(i).setAbandoned(true);
        }

        // Try launching a bunch of borrows concurrently.  Abandoned sweep will be triggered for each.
        final int CONCURRENT_BORROWS = 5;
        Thread[] threads = new Thread[CONCURRENT_BORROWS];
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i] = new ConcurrentBorrower(vec);
            threads[i].start();
        }

        // Wait for all the threads to finish
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i].join();
        }

        // Return all objects that have not been destroyed
        for (int i = 0; i < vec.size(); i++) {
            PooledTestObject pto = vec.get(i);
            if (pto.isActive()) {
                pool.returnObject(pto);
            }
        }

        // Now, the number of active instances should be 0
        Assert.assertTrue("numActive should have been 0, was " + pool.getNumActive(), pool.getNumActive() == 0);
    }

    /**
     * Verify that an object that gets flagged as abandoned and is subsequently returned
     * is destroyed instead of being returned to the pool (and possibly later destroyed
     * inappropriately).
     */
    @Test
    public void testAbandonedReturn() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setRemoveAbandonedTimeout(1);
        pool.close();  // Unregister pool created by setup
        pool = new GenericObjectPool<PooledTestObject>(
                new SimpleFactory(200, 0),
                new GenericObjectPoolConfig(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        PooledTestObject obj = null;
        for (int i = 0; i < n - 2; i++) {
            obj = pool.borrowObject();
        }
        if (obj == null) {
            throw new NullPointerException("Unable to borrow object from pool");
        }
        final int deadMansHash = obj.hashCode();
        ConcurrentReturner returner = new ConcurrentReturner(obj);
        Thread.sleep(2000);  // abandon checked out instances
        // Now start a race - returner waits until borrowObject has kicked
        // off removeAbandoned and then returns an instance that borrowObject
        // will deem abandoned.  Make sure it is not returned to the borrower.
        returner.start();    // short delay, then return instance
        Assert.assertTrue(pool.borrowObject().hashCode() != deadMansHash);
        Assert.assertEquals(0, pool.getNumIdle());
        Assert.assertEquals(1, pool.getNumActive());
    }

    /**
     * Verify that an object that gets flagged as abandoned and is subsequently
     * invalidated is only destroyed (and pool counter decremented) once.
     */
    @Test
    public void testAbandonedInvalidate() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(1);
        pool.close();  // Unregister pool created by setup
        pool = new GenericObjectPool<PooledTestObject>(
                // destroys take 200 ms
                new SimpleFactory(200, 0),
                new GenericObjectPoolConfig(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        pool.setTimeBetweenEvictionRunsMillis(500);
        PooledTestObject obj = null;
        for (int i = 0; i < 5; i++) {
            obj = pool.borrowObject();
        }
        Thread.sleep(1000);          // abandon checked out instances and let evictor start
        pool.invalidateObject(obj);  // Should not trigger another destroy / decrement
        Thread.sleep(2000);          // give evictor time to finish destroys
        Assert.assertEquals(0, pool.getNumActive());
        Assert.assertEquals(5, pool.getDestroyedCount());
    }

    /**
     * Verify that an object that the evictor identifies as abandoned while it
     * is in process of being returned to the pool is not destroyed.
     */
    @Test
    public void testRemoveAbandonedWhileReturning() throws Exception {
        abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        abandonedConfig.setRemoveAbandonedTimeout(1);
        pool.close();  // Unregister pool created by setup
        pool = new GenericObjectPool<PooledTestObject>(
             // validate takes 1 second
                new SimpleFactory(0, 1000),
                new GenericObjectPoolConfig(), abandonedConfig);
        final int n = 10;
        pool.setMaxTotal(n);
        pool.setBlockWhenExhausted(false);
        pool.setTimeBetweenEvictionRunsMillis(500);
        pool.setTestOnReturn(true);
        // Borrow an object, wait long enough for it to be abandoned
        // then arrange for evictor to run while it is being returned
        // validation takes a second, evictor runs every 500 ms
        PooledTestObject obj = pool.borrowObject();
        Thread.sleep(50);       // abandon obj
        pool.returnObject(obj); // evictor will run during validation
        PooledTestObject obj2 = pool.borrowObject();
        Assert.assertEquals(obj, obj2);          // should get original back
        Assert.assertTrue(!obj2.isDestroyed());  // and not destroyed
    }

    /**
     * Test case for https://issues.apache.org/jira/browse/DBCP-260.
     * Borrow and abandon all the available objects then attempt to borrow one
     * further object which should block until the abandoned objects are
     * removed. We don't want the test to block indefinitely when it fails so
     * use maxWait be check we don't actually have to wait that long.
     */
    @Test
    public void testWhenExhaustedBlock() throws Exception {
        abandonedConfig.setRemoveAbandonedOnMaintenance(true);
        pool.setAbandonedConfig(abandonedConfig);
        pool.setTimeBetweenEvictionRunsMillis(500);

        pool.setMaxTotal(1);

        @SuppressWarnings("unused") // This is going to be abandoned
        PooledTestObject o1 = pool.borrowObject();

        long start = System.currentTimeMillis();
        PooledTestObject o2 = pool.borrowObject(5000);
        long end = System.currentTimeMillis();

        pool.returnObject(o2);

        Assert.assertTrue (end - start < 5000);
    }

    class ConcurrentBorrower extends Thread {
        private ArrayList<PooledTestObject> _borrowed;

        public ConcurrentBorrower(ArrayList<PooledTestObject> borrowed) {
            _borrowed = borrowed;
        }

        @Override
        public void run() {
            try {
                _borrowed.add(pool.borrowObject());
            } catch (Exception e) {
                // expected in most cases
            }
        }
    }

    class ConcurrentReturner extends Thread {
        private PooledTestObject returned;
        public ConcurrentReturner(PooledTestObject obj) {
            returned = obj;
        }
        @Override
        public void run() {
            try {
                sleep(20);
                pool.returnObject(returned);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static class SimpleFactory implements PooledObjectFactory<PooledTestObject> {

        private final long destroyLatency;
        private final long validateLatency;

        public SimpleFactory() {
            destroyLatency = 0;
            validateLatency = 0;
        }

        public SimpleFactory(long destroyLatency, long validateLatency) {
            this.destroyLatency = destroyLatency;
            this.validateLatency = validateLatency;
        }

        @Override
        public PooledObject<PooledTestObject> makeObject() {
            return new DefaultPooledObject<PooledTestObject>(new PooledTestObject());
        }

        @Override
        public boolean validateObject(PooledObject<PooledTestObject> obj) {
            try {
                Thread.sleep(validateLatency);
            } catch (Exception ex) {
                // ignore
            }
            return true;
        }

        @Override
        public void activateObject(PooledObject<PooledTestObject> obj) {
            obj.getObject().setActive(true);
        }

        @Override
        public void passivateObject(PooledObject<PooledTestObject> obj) {
            obj.getObject().setActive(false);
        }

        @Override
        public void destroyObject(PooledObject<PooledTestObject> obj) throws Exception {
            obj.getObject().setActive(false);
            // while destroying instances, yield control to other threads
            // helps simulate threading errors
            Thread.yield();
            if (destroyLatency != 0) {
                Thread.sleep(destroyLatency);
            }
            obj.getObject().destroy();
        }
    }
}

class PooledTestObject implements TrackedUse {
    private boolean active = false;
    private boolean destroyed = false;
    private int _hash = 0;
    private boolean _abandoned = false;
    private static AtomicInteger hash = new AtomicInteger();

    public PooledTestObject() {
        _hash = hash.incrementAndGet();
    }

    public synchronized void setActive(boolean b) {
        active = b;
    }

    public synchronized boolean isActive() {
        return active;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public int hashCode() {
        return _hash;
    }

    public void setAbandoned(boolean b) {
        _abandoned = b;
    }

    @Override
    public long getLastUsed() {
        if (_abandoned) {
            // Abandoned object sweep will occur no matter what the value of removeAbandonedTimeout,
            // because this indicates that this object was last used decades ago
            return 1;
        } else {
            // Abandoned object sweep won't clean up this object
            return 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PooledTestObject)) return false;
        return obj.hashCode() == hashCode();
    }
}

