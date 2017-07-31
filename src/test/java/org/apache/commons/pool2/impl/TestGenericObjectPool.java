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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.TestBaseObjectPool;
import org.apache.commons.pool2.VisitTracker;
import org.apache.commons.pool2.VisitTrackerFactory;
import org.apache.commons.pool2.Waiter;
import org.apache.commons.pool2.WaiterFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @version $Revision$
 */
public class TestGenericObjectPool extends TestBaseObjectPool {

    @Override
    protected ObjectPool<String> makeEmptyPool(final int mincap) {
       final GenericObjectPool<String> mtPool =
               new GenericObjectPool<String>(new SimpleFactory());
       mtPool.setMaxTotal(mincap);
       mtPool.setMaxIdle(mincap);
       return mtPool;
    }

    @Override
    protected ObjectPool<Object> makeEmptyPool(
            final PooledObjectFactory<Object> fac) {
        return new GenericObjectPool<Object>(fac);
    }

    @Override
    protected Object getNthObject(final int n) {
        return String.valueOf(n);
    }

    @Before
    public void setUp() throws Exception {
        factory = new SimpleFactory();
        pool = new GenericObjectPool<String>(factory);
    }

    @After
    public void tearDown() throws Exception {
        final String poolName = pool.getJmxName().toString();
        pool.clear();
        pool.close();
        pool = null;
        factory = null;

        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final Set<ObjectName> result = mbs.queryNames(new ObjectName(
                "org.apache.commoms.pool2:type=GenericObjectPool,*"), null);
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
        Assert.assertEquals(msg.toString(), 0, registeredPoolCount);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorNullFactory() {
        // add dummy assert (won't be invoked because of IAE) to avoid "unused" warning
        assertNotNull(new GenericObjectPool<String>(null));
        // TODO this currently causes tearDown to report an error
        // Looks like GOP needs to call close() or jmxUnregister() before throwing IAE
    }

    @Test(timeout=60000)
    public void testConstructors() throws Exception {

        // Make constructor arguments all different from defaults
        final int minIdle = 2;
        final long maxWait = 3;
        final int maxIdle = 4;
        final int maxTotal = 5;
        final long minEvictableIdleTimeMillis = 6;
        final int numTestsPerEvictionRun = 7;
        final boolean testOnBorrow = true;
        final boolean testOnReturn = true;
        final boolean testWhileIdle = true;
        final long timeBetweenEvictionRunsMillis = 8;
        final boolean blockWhenExhausted = false;
        final boolean lifo = false;
        final PooledObjectFactory<Object> dummyFactory = new DummyFactory();
        GenericObjectPool<Object> dummyPool =
                new GenericObjectPool<Object>(dummyFactory);
        assertEquals(GenericObjectPoolConfig.DEFAULT_MAX_IDLE, dummyPool.getMaxIdle());
        assertEquals(BaseObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS, dummyPool.getMaxWaitMillis());
        assertEquals(GenericObjectPoolConfig.DEFAULT_MIN_IDLE, dummyPool.getMinIdle());
        assertEquals(GenericObjectPoolConfig.DEFAULT_MAX_TOTAL, dummyPool.getMaxTotal());
        assertEquals(BaseObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                dummyPool.getMinEvictableIdleTimeMillis());
        assertEquals(BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                dummyPool.getNumTestsPerEvictionRun());
        assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_TEST_ON_BORROW),
                Boolean.valueOf(dummyPool.getTestOnBorrow()));
        assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_TEST_ON_RETURN),
                Boolean.valueOf(dummyPool.getTestOnReturn()));
        assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE),
                Boolean.valueOf(dummyPool.getTestWhileIdle()));
        assertEquals(BaseObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                dummyPool.getTimeBetweenEvictionRunsMillis());
        assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED),
                Boolean.valueOf(dummyPool.getBlockWhenExhausted()));
        assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_LIFO),
                Boolean.valueOf(dummyPool.getLifo()));
        dummyPool.close();

        final GenericObjectPoolConfig config =
                new GenericObjectPoolConfig();
        config.setLifo(lifo);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);
        config.setMaxTotal(maxTotal);
        config.setMaxWaitMillis(maxWait);
        config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);
        config.setTestWhileIdle(testWhileIdle);
        config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        config.setBlockWhenExhausted(blockWhenExhausted);
        dummyPool = new GenericObjectPool<Object>(dummyFactory, config);
        assertEquals(maxIdle, dummyPool.getMaxIdle());
        assertEquals(maxWait, dummyPool.getMaxWaitMillis());
        assertEquals(minIdle, dummyPool.getMinIdle());
        assertEquals(maxTotal, dummyPool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                dummyPool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun, dummyPool.getNumTestsPerEvictionRun());
        assertEquals(Boolean.valueOf(testOnBorrow),
                Boolean.valueOf(dummyPool.getTestOnBorrow()));
        assertEquals(Boolean.valueOf(testOnReturn),
                Boolean.valueOf(dummyPool.getTestOnReturn()));
        assertEquals(Boolean.valueOf(testWhileIdle),
                Boolean.valueOf(dummyPool.getTestWhileIdle()));
        assertEquals(timeBetweenEvictionRunsMillis,
                dummyPool.getTimeBetweenEvictionRunsMillis());
        assertEquals(Boolean.valueOf(blockWhenExhausted),
                Boolean.valueOf(dummyPool.getBlockWhenExhausted()));
        assertEquals(Boolean.valueOf(lifo), Boolean.valueOf(dummyPool.getLifo()));
        dummyPool.close();
    }

    @Test(timeout=60000)
    public void testWhenExhaustedFail() throws Exception {
        pool.setMaxTotal(1);
        pool.setBlockWhenExhausted(false);
        final String obj1 = pool.borrowObject();
        assertNotNull(obj1);
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
        pool.returnObject(obj1);
        assertEquals(1, pool.getNumIdle());
        pool.close();
    }

    @Test(timeout=60000)
    public void testWhenExhaustedBlock() throws Exception {
        pool.setMaxTotal(1);
        pool.setBlockWhenExhausted(true);
        pool.setMaxWaitMillis(10L);
        final String obj1 = pool.borrowObject();
        assertNotNull(obj1);
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
        pool.returnObject(obj1);
        pool.close();
    }

    @Test(timeout=60000)
    public void testWhenExhaustedBlockInterupt() throws Exception {
        pool.setMaxTotal(1);
        pool.setBlockWhenExhausted(true);
        pool.setMaxWaitMillis(-1);
        final String obj1 = pool.borrowObject();

        // Make sure on object was obtained
        assertNotNull(obj1);

        // Create a separate thread to try and borrow another object
        final WaitingTestThread wtt = new WaitingTestThread(pool, 200000);
        wtt.start();
        // Give wtt time to start
        Thread.sleep(200);
        wtt.interrupt();

        // Give interrupt time to take effect
        Thread.sleep(200);

        // Check thread was interrupted
        assertTrue(wtt._thrown instanceof InterruptedException);

        // Return object to the pool
        pool.returnObject(obj1);

        // Bug POOL-162 - check there is now an object in the pool
        pool.setMaxWaitMillis(10L);
        String obj2 = null;
        try {
             obj2 = pool.borrowObject();
            assertNotNull(obj2);
        } catch(final NoSuchElementException e) {
            // Not expected
            fail("NoSuchElementException not expected");
        }
        pool.returnObject(obj2);
        pool.close();

    }

    @Test(timeout=60000)
    public void testEvictWhileEmpty() throws Exception {
        pool.evict();
        pool.evict();
        pool.close();
    }

    /**
     * Tests addObject contention between ensureMinIdle triggered by
     * the Evictor with minIdle &gt; 0 and borrowObject.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test(timeout=60000)
    public void testEvictAddObjects() throws Exception {
        factory.setMakeLatency(300);
        factory.setMaxTotal(2);
        pool.setMaxTotal(2);
        pool.setMinIdle(1);
        pool.borrowObject(); // numActive = 1, numIdle = 0
        // Create a test thread that will run once and try a borrow after
        // 150ms fixed delay
        final TestThread<String> borrower = new TestThread<String>(pool, 1, 150, false);
        final Thread borrowerThread = new Thread(borrower);
        // Set evictor to run in 100 ms - will create idle instance
        pool.setTimeBetweenEvictionRunsMillis(100);
        borrowerThread.start();  // Off to the races
        borrowerThread.join();
        assertTrue(!borrower.failed());
    }

    @Test(timeout=60000)
    public void testEvictLIFO() throws Exception {
        checkEvict(true);
    }

    @Test(timeout=60000)
    public void testEvictFIFO() throws Exception {
        checkEvict(false);
    }

    private void checkEvict(final boolean lifo) throws Exception {
        // yea this is hairy but it tests all the code paths in GOP.evict()
        pool.setSoftMinEvictableIdleTimeMillis(10);
        pool.setMinIdle(2);
        pool.setTestWhileIdle(true);
        pool.setLifo(lifo);
        PoolUtils.prefill(pool, 5);
        pool.evict();
        factory.setEvenValid(false);
        factory.setOddValid(false);
        factory.setThrowExceptionOnActivate(true);
        pool.evict();
        PoolUtils.prefill(pool, 5);
        factory.setThrowExceptionOnActivate(false);
        factory.setThrowExceptionOnPassivate(true);
        pool.evict();
        factory.setThrowExceptionOnPassivate(false);
        factory.setEvenValid(true);
        factory.setOddValid(true);
        Thread.sleep(125);
        pool.evict();
        assertEquals(2, pool.getNumIdle());
    }

    /**
     * Test to make sure evictor visits least recently used objects first,
     * regardless of FIFO/LIFO.
     *
     * JIRA: POOL-86
     *
     * @throws Exception May occur in some failure modes
     */
    @Test(timeout=60000)
    public void testEvictionOrder() throws Exception {
        checkEvictionOrder(false);
        tearDown();
        setUp();
        checkEvictionOrder(true);
    }

    private void checkEvictionOrder(final boolean lifo) throws Exception {
        checkEvictionOrderPart1(lifo);
        tearDown();
        setUp();
        checkEvictionOrderPart2(lifo);
    }

    private void checkEvictionOrderPart1(final boolean lifo) throws Exception {
        pool.setNumTestsPerEvictionRun(2);
        pool.setMinEvictableIdleTimeMillis(100);
        pool.setLifo(lifo);
        for (int i = 0; i < 5; i++) {
            pool.addObject();
            Thread.sleep(100);
        }
        // Order, oldest to youngest, is "0", "1", ...,"4"
        pool.evict(); // Should evict "0" and "1"
        final Object obj = pool.borrowObject();
        assertTrue("oldest not evicted", !obj.equals("0"));
        assertTrue("second oldest not evicted", !obj.equals("1"));
        // 2 should be next out for FIFO, 4 for LIFO
        assertEquals("Wrong instance returned", lifo ? "4" : "2" , obj);
    }

    private void checkEvictionOrderPart2(final boolean lifo) throws Exception {
        // Two eviction runs in sequence
        pool.setNumTestsPerEvictionRun(2);
        pool.setMinEvictableIdleTimeMillis(100);
        pool.setLifo(lifo);
        for (int i = 0; i < 5; i++) {
            pool.addObject();
            Thread.sleep(100);
        }
        pool.evict(); // Should evict "0" and "1"
        pool.evict(); // Should evict "2" and "3"
        final Object obj = pool.borrowObject();
        assertEquals("Wrong instance remaining in pool", "4", obj);
    }

    /**
     * Verifies that the evictor visits objects in expected order
     * and frequency.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testEvictorVisiting() throws Exception {
        checkEvictorVisiting(true);
        checkEvictorVisiting(false);
    }

    private void checkEvictorVisiting(final boolean lifo) throws Exception {
        VisitTrackerFactory<Object> trackerFactory = new VisitTrackerFactory<Object>();
        GenericObjectPool<VisitTracker<Object>> trackerPool =
                new GenericObjectPool<VisitTracker<Object>>(trackerFactory);
        trackerPool.setNumTestsPerEvictionRun(2);
        trackerPool.setMinEvictableIdleTimeMillis(-1);
        trackerPool.setTestWhileIdle(true);
        trackerPool.setLifo(lifo);
        trackerPool.setTestOnReturn(false);
        trackerPool.setTestOnBorrow(false);
        for (int i = 0; i < 8; i++) {
            trackerPool.addObject();
        }
        trackerPool.evict(); // Visit oldest 2 - 0 and 1
        VisitTracker<Object> obj = trackerPool.borrowObject();
        trackerPool.returnObject(obj);
        obj = trackerPool.borrowObject();
        trackerPool.returnObject(obj);
        //  borrow, return, borrow, return
        //  FIFO will move 0 and 1 to end
        //  LIFO, 7 out, then in, then out, then in
        trackerPool.evict();  // Should visit 2 and 3 in either case
        for (int i = 0; i < 8; i++) {
            final VisitTracker<Object> tracker = trackerPool.borrowObject();
            if (tracker.getId() >= 4) {
                assertEquals("Unexpected instance visited " + tracker.getId(),
                        0, tracker.getValidateCount());
            } else {
                assertEquals("Instance " +  tracker.getId() +
                        " visited wrong number of times.",
                        1, tracker.getValidateCount());
            }
        }
        trackerPool.close();

        trackerFactory = new VisitTrackerFactory<Object>();
        trackerPool = new GenericObjectPool<VisitTracker<Object>>(trackerFactory);
        trackerPool.setNumTestsPerEvictionRun(3);
        trackerPool.setMinEvictableIdleTimeMillis(-1);
        trackerPool.setTestWhileIdle(true);
        trackerPool.setLifo(lifo);
        trackerPool.setTestOnReturn(false);
        trackerPool.setTestOnBorrow(false);
        for (int i = 0; i < 8; i++) {
            trackerPool.addObject();
        }
        trackerPool.evict(); // 0, 1, 2
        trackerPool.evict(); // 3, 4, 5
        obj = trackerPool.borrowObject();
        trackerPool.returnObject(obj);
        obj = trackerPool.borrowObject();
        trackerPool.returnObject(obj);
        obj = trackerPool.borrowObject();
        trackerPool.returnObject(obj);
        // borrow, return, borrow, return
        //  FIFO 3,4,5,6,7,0,1,2
        //  LIFO 7,6,5,4,3,2,1,0
        // In either case, pointer should be at 6
        trackerPool.evict();
        // Should hit 6,7,0 - 0 for second time
        for (int i = 0; i < 8; i++) {
            final VisitTracker<Object> tracker = trackerPool.borrowObject();
            if (tracker.getId() != 0) {
                assertEquals("Instance " +  tracker.getId() +
                        " visited wrong number of times.",
                        1, tracker.getValidateCount());
            } else {
                assertEquals("Instance " +  tracker.getId() +
                        " visited wrong number of times.",
                        2, tracker.getValidateCount());
            }
        }
        trackerPool.close();

        // Randomly generate a pools with random numTests
        // and make sure evictor cycles through elements appropriately
        final int[] smallPrimes = {2, 3, 5, 7};
        final Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                trackerPool = new GenericObjectPool<VisitTracker<Object>>(trackerFactory);
                trackerPool.setNumTestsPerEvictionRun(smallPrimes[i]);
                trackerPool.setMinEvictableIdleTimeMillis(-1);
                trackerPool.setTestWhileIdle(true);
                trackerPool.setLifo(lifo);
                trackerPool.setTestOnReturn(false);
                trackerPool.setTestOnBorrow(false);
                trackerPool.setMaxIdle(-1);
                final int instanceCount = 10 + random.nextInt(20);
                trackerPool.setMaxTotal(instanceCount);
                for (int k = 0; k < instanceCount; k++) {
                    trackerPool.addObject();
                }

                // Execute a random number of evictor runs
                final int runs = 10 + random.nextInt(50);
                for (int k = 0; k < runs; k++) {
                    trackerPool.evict();
                }

                // Number of times evictor should have cycled through the pool
                final int cycleCount = (runs * trackerPool.getNumTestsPerEvictionRun()) /
                        instanceCount;

                // Look at elements and make sure they are visited cycleCount
                // or cycleCount + 1 times
                VisitTracker<Object> tracker = null;
                int visitCount = 0;
                for (int k = 0; k < instanceCount; k++) {
                    tracker = trackerPool.borrowObject();
                    assertTrue(trackerPool.getNumActive() <= trackerPool.getMaxTotal());
                    visitCount = tracker.getValidateCount();
                    assertTrue(visitCount >= cycleCount &&
                            visitCount <= cycleCount + 1);
                }
                trackerPool.close();
            }
        }
    }

    @Test(timeout=60000)
    public void testExceptionOnPassivateDuringReturn() throws Exception {
        final String obj = pool.borrowObject();
        factory.setThrowExceptionOnPassivate(true);
        pool.returnObject(obj);
        assertEquals(0,pool.getNumIdle());
    }

    @Test(timeout=60000)
    public void testExceptionOnDestroyDuringBorrow() throws Exception {
        factory.setThrowExceptionOnDestroy(true);
        pool.setTestOnBorrow(true);
        pool.borrowObject();
        factory.setValid(false); // Make validation fail on next borrow attempt
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }

    @Test(timeout=60000)
    public void testExceptionOnDestroyDuringReturn() throws Exception {
        factory.setThrowExceptionOnDestroy(true);
        pool.setTestOnReturn(true);
        final String obj1 = pool.borrowObject();
        pool.borrowObject();
        factory.setValid(false); // Make validation fail
        pool.returnObject(obj1);
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }

    @Test(timeout=60000)
    public void testExceptionOnActivateDuringBorrow() throws Exception {
        final String obj1 = pool.borrowObject();
        final String obj2 = pool.borrowObject();
        pool.returnObject(obj1);
        pool.returnObject(obj2);
        factory.setThrowExceptionOnActivate(true);
        factory.setEvenValid(false);
        // Activation will now throw every other time
        // First attempt throws, but loop continues and second succeeds
        final String obj = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        pool.returnObject(obj);
        factory.setValid(false);
        // Validation will now fail on activation when borrowObject returns
        // an idle instance, and then when attempting to create a new instance
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }

    @Test(timeout=60000)
    public void testNegativeMaxTotal() throws Exception {
        pool.setMaxTotal(-1);
        pool.setBlockWhenExhausted(false);
        final String obj = pool.borrowObject();
        assertEquals(getNthObject(0),obj);
        pool.returnObject(obj);
    }

    @Test(timeout=60000)
    public void testMaxIdle() throws Exception {
        pool.setMaxTotal(100);
        pool.setMaxIdle(8);
        final String[] active = new String[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject();
        }
        assertEquals(100,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        for(int i=0;i<100;i++) {
            pool.returnObject(active[i]);
            assertEquals(99 - i,pool.getNumActive());
            assertEquals((i < 8 ? i+1 : 8),pool.getNumIdle());
        }
    }

    @Test(timeout=60000)
    public void testMaxIdleZero() throws Exception {
        pool.setMaxTotal(100);
        pool.setMaxIdle(0);
        final String[] active = new String[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject();
        }
        assertEquals(100,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        for(int i=0;i<100;i++) {
            pool.returnObject(active[i]);
            assertEquals(99 - i,pool.getNumActive());
            assertEquals(0, pool.getNumIdle());
        }
    }

    @Test(timeout=60000)
    public void testMaxTotal() throws Exception {
        pool.setMaxTotal(3);
        pool.setBlockWhenExhausted(false);

        pool.borrowObject();
        pool.borrowObject();
        pool.borrowObject();
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
    }

    @Test(timeout=60000)
    public void testTimeoutNoLeak() throws Exception {
        pool.setMaxTotal(2);
        pool.setMaxWaitMillis(10);
        pool.setBlockWhenExhausted(true);
        final String obj = pool.borrowObject();
        final String obj2 = pool.borrowObject();
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        pool.returnObject(obj2);
        pool.returnObject(obj);

        pool.borrowObject();
        pool.borrowObject();
    }

    @Test(timeout=60000)
    public void testMaxTotalZero() throws Exception {
        pool.setMaxTotal(0);
        pool.setBlockWhenExhausted(false);

        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
    }

    @Test(timeout=60000)
    @SuppressWarnings("rawtypes")
    public void testMaxTotalUnderLoad() {
        // Config
        final int numThreads = 199; // And main thread makes a round 200.
        final int numIter = 20;
        final int delay = 25;
        final int maxTotal = 10;

        factory.setMaxTotal(maxTotal);
        pool.setMaxTotal(maxTotal);
        pool.setBlockWhenExhausted(true);
        pool.setTimeBetweenEvictionRunsMillis(-1);

        // Start threads to borrow objects
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            // Factor of 2 on iterations so main thread does work whilst other
            // threads are running. Factor of 2 on delay so average delay for
            // other threads == actual delay for main thread
            threads[i] = new TestThread<String>(pool, numIter * 2, delay * 2);
            final Thread t = new Thread(threads[i]);
            t.start();
        }
        // Give the threads a chance to start doing some work
        try {
            Thread.sleep(5000);
        } catch(final InterruptedException e) {
            // ignored
        }

        for (int i = 0; i < numIter; i++) {
            String obj = null;
            try {
                try {
                    Thread.sleep(delay);
                } catch(final InterruptedException e) {
                    // ignored
                }
                obj = pool.borrowObject();
                // Under load, observed _numActive > _maxTotal
                if (pool.getNumActive() > pool.getMaxTotal()) {
                    throw new IllegalStateException("Too many active objects");
                }
                try {
                    Thread.sleep(delay);
                } catch(final InterruptedException e) {
                    // ignored
                }
            } catch (final Exception e) {
                // Shouldn't happen
                e.printStackTrace();
                fail("Exception on borrow");
            } finally {
                if (obj != null) {
                    try {
                        pool.returnObject(obj);
                    } catch (final Exception e) {
                        // Ignore
                    }
                }
            }
        }

        for(int i=0;i<numThreads;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(final InterruptedException e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail("Thread "+i+" failed: "+threads[i]._error.toString());
            }
        }
    }

    /**
     * This is the test case for POOL-263. It is disabled since it will always
     * pass without artificial delay being injected into GOP.returnObject() and
     * a way to this hasn't currently been found that doesn't involve
     * polluting the GOP implementation. The artificial delay needs to be
     * inserted just before the final call to isLifo() in the returnObject()
     * method.
     */
    //@Test(timeout=60000)
    public void testReturnObject() throws Exception {

        pool.setMaxTotal(1);
        pool.setMaxIdle(-1);
        final String active = pool.borrowObject();

        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        final Thread t = new Thread() {

            @Override
            public void run() {
                pool.close();
            }

        };
        t.start();

        pool.returnObject(active);

        // Wait for the close() thread to complete
        while (t.isAlive()) {
            Thread.sleep(50);
        }

        assertEquals(0, pool.getNumIdle());
    }


    @Test(timeout=60000)
    public void testSettersAndGetters() throws Exception {
        {
            // The object receives an Exception during its creation to prevent
            // memory leaks. See BaseGenericObjectPool constructor for more details.
            assertTrue(false == "".equals(pool.getCreationStackTrace()));
        }
        {
            assertEquals(0, pool.getBorrowedCount());
        }
        {
            assertEquals(0, pool.getReturnedCount());
        }
        {
            assertEquals(0, pool.getCreatedCount());
        }
        {
            assertEquals(0, pool.getDestroyedCount());
        }
        {
            assertEquals(0, pool.getDestroyedByEvictorCount());
        }
        {
            assertEquals(0, pool.getDestroyedByBorrowValidationCount());
        }
        {
            assertEquals(0, pool.getMeanActiveTimeMillis());
        }
        {
            assertEquals(0, pool.getMeanIdleTimeMillis());
        }
        {
            assertEquals(0, pool.getMeanBorrowWaitTimeMillis());
        }
        {
            assertEquals(0, pool.getMaxBorrowWaitTimeMillis());
        }
        {
            assertEquals(0, pool.getNumIdle());
        }
        {
            pool.setMaxTotal(123);
            assertEquals(123,pool.getMaxTotal());
        }
        {
            pool.setMaxIdle(12);
            assertEquals(12,pool.getMaxIdle());
        }
        {
            pool.setMaxWaitMillis(1234L);
            assertEquals(1234L,pool.getMaxWaitMillis());
        }
        {
            pool.setMinEvictableIdleTimeMillis(12345L);
            assertEquals(12345L,pool.getMinEvictableIdleTimeMillis());
        }
        {
            pool.setNumTestsPerEvictionRun(11);
            assertEquals(11,pool.getNumTestsPerEvictionRun());
        }
        {
            pool.setTestOnBorrow(true);
            assertTrue(pool.getTestOnBorrow());
            pool.setTestOnBorrow(false);
            assertTrue(!pool.getTestOnBorrow());
        }
        {
            pool.setTestOnReturn(true);
            assertTrue(pool.getTestOnReturn());
            pool.setTestOnReturn(false);
            assertTrue(!pool.getTestOnReturn());
        }
        {
            pool.setTestWhileIdle(true);
            assertTrue(pool.getTestWhileIdle());
            pool.setTestWhileIdle(false);
            assertTrue(!pool.getTestWhileIdle());
        }
        {
            pool.setTimeBetweenEvictionRunsMillis(11235L);
            assertEquals(11235L,pool.getTimeBetweenEvictionRunsMillis());
        }
        {
            pool.setSoftMinEvictableIdleTimeMillis(12135L);
            assertEquals(12135L,pool.getSoftMinEvictableIdleTimeMillis());
        }
        {
            pool.setBlockWhenExhausted(true);
            assertTrue(pool.getBlockWhenExhausted());
            pool.setBlockWhenExhausted(false);
            assertFalse(pool.getBlockWhenExhausted());
        }
    }

    @Test(timeout=60000)
    public void testDefaultConfiguration() throws Exception {
        assertConfiguration(new GenericObjectPoolConfig(),pool);
    }

    @Test(timeout=60000)
    public void testSetConfig() throws Exception {
        final GenericObjectPoolConfig expected = new GenericObjectPoolConfig();
        assertConfiguration(expected,pool);
        expected.setMaxTotal(2);
        expected.setMaxIdle(3);
        expected.setMaxWaitMillis(5L);
        expected.setMinEvictableIdleTimeMillis(7L);
        expected.setNumTestsPerEvictionRun(9);
        expected.setTestOnCreate(true);
        expected.setTestOnBorrow(true);
        expected.setTestOnReturn(true);
        expected.setTestWhileIdle(true);
        expected.setTimeBetweenEvictionRunsMillis(11L);
        expected.setBlockWhenExhausted(false);
        pool.setConfig(expected);
        assertConfiguration(expected,pool);
    }

    @Test(timeout=60000)
    public void testStartAndStopEvictor() throws Exception {
        // set up pool without evictor
        pool.setMaxIdle(6);
        pool.setMaxTotal(6);
        pool.setNumTestsPerEvictionRun(6);
        pool.setMinEvictableIdleTimeMillis(100L);

        for(int j=0;j<2;j++) {
            // populate the pool
            {
                final String[] active = new String[6];
                for(int i=0;i<6;i++) {
                    active[i] = pool.borrowObject();
                }
                for(int i=0;i<6;i++) {
                    pool.returnObject(active[i]);
                }
            }

            // note that it stays populated
            assertEquals("Should have 6 idle",6,pool.getNumIdle());

            // start the evictor
            pool.setTimeBetweenEvictionRunsMillis(50L);

            // wait a second (well, .2 seconds)
            try { Thread.sleep(200L); } catch(final InterruptedException e) { }

            // assert that the evictor has cleared out the pool
            assertEquals("Should have 0 idle",0,pool.getNumIdle());

            // stop the evictor
            pool.startEvictor(0L);
        }
    }

    @Test(timeout=60000)
    public void testEvictionWithNegativeNumTests() throws Exception {
        // when numTestsPerEvictionRun is negative, it represents a fraction of the idle objects to test
        pool.setMaxIdle(6);
        pool.setMaxTotal(6);
        pool.setNumTestsPerEvictionRun(-2);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);

        final String[] active = new String[6];
        for(int i=0;i<6;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<6;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue("Should at most 6 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 6);
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue("Should at most 3 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 3);
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue("Should be at most 2 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 2);
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());
    }

    @Test(timeout=60000)
    public void testEviction() throws Exception {
        pool.setMaxIdle(500);
        pool.setMaxTotal(500);
        pool.setNumTestsPerEvictionRun(100);
        pool.setMinEvictableIdleTimeMillis(250L);
        pool.setTimeBetweenEvictionRunsMillis(500L);
        pool.setTestWhileIdle(true);

        final String[] active = new String[500];
        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<500;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 500);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 400);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 300);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 200);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 100);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());

        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<500;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 500);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 400);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 300);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 200);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 100);
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());
    }

    public static class TestEvictionPolicy<T> implements EvictionPolicy<T> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public boolean evict(final EvictionConfig config, final PooledObject<T> underTest,
                final int idleCount) {
            if (callCount.incrementAndGet() > 1500) {
                return true;
            }
            return false;
        }
    }

    @Test(timeout=60000)
    public void testEvictionPolicy() throws Exception {
        pool.setMaxIdle(500);
        pool.setMaxTotal(500);
        pool.setNumTestsPerEvictionRun(500);
        pool.setMinEvictableIdleTimeMillis(250L);
        pool.setTimeBetweenEvictionRunsMillis(500L);
        pool.setTestWhileIdle(true);

        // ClassNotFoundException
        try {
            pool.setEvictionPolicyClassName(Long.toString(System.currentTimeMillis()));
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // InstantiationException
        try {
            pool.setEvictionPolicyClassName(java.io.Serializable.class.getName());
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // IllegalAccessException
        try {
            pool.setEvictionPolicyClassName(java.util.Collections.class.getName());
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            pool.setEvictionPolicyClassName(java.lang.String.class.getName());
            fail("setEvictionPolicyClassName must throw an error if a class that does not "
                    + "implement EvictionPolicy is specified.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        pool.setEvictionPolicyClassName(TestEvictionPolicy.class.getName());
        assertEquals(TestEvictionPolicy.class.getName(), pool.getEvictionPolicyClassName());

        final String[] active = new String[500];
        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<500;i++) {
            pool.returnObject(active[i]);
        }

        // Eviction policy ignores first 1500 attempts to evict and then always
        // evicts. After 1s, there should have been two runs of 500 tests so no
        // evictions
        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertEquals("Should be 500 idle", 500, pool.getNumIdle());
        // A further 1s wasn't enough so allow 2s for the evictor to clear out
        // all of the idle objects.
        try { Thread.sleep(2000L); } catch(final InterruptedException e) { }
        assertEquals("Should be 0 idle", 0, pool.getNumIdle());
    }


    @Test(timeout=60000)
    public void testEvictionSoftMinIdle() throws Exception {
        class TimeTest extends BasePooledObjectFactory<TimeTest> {
            private final long createTime;
            public TimeTest() {
                createTime = System.currentTimeMillis();
            }
            @Override
            public TimeTest create() throws Exception {
                return new TimeTest();
            }
            @Override
            public PooledObject<TimeTest> wrap(final TimeTest value) {
                return new DefaultPooledObject<TimeTest>(value);
            }
            public long getCreateTime() {
                return createTime;
            }
        }

        final GenericObjectPool<TimeTest> timePool =
            new GenericObjectPool<TimeTest>(new TimeTest());

        timePool.setMaxIdle(5);
        timePool.setMaxTotal(5);
        timePool.setNumTestsPerEvictionRun(5);
        timePool.setMinEvictableIdleTimeMillis(3000L);
        timePool.setSoftMinEvictableIdleTimeMillis(1000L);
        timePool.setMinIdle(2);

        final TimeTest[] active = new TimeTest[5];
        final Long[] creationTime = new Long[5] ;
        for(int i=0;i<5;i++) {
            active[i] = timePool.borrowObject();
            creationTime[i] = Long.valueOf((active[i]).getCreateTime());
        }

        for(int i=0;i<5;i++) {
            timePool.returnObject(active[i]);
        }

        // Soft evict all but minIdle(2)
        Thread.sleep(1500L);
        timePool.evict();
        assertEquals("Idle count different than expected.", 2, timePool.getNumIdle());

        // Hard evict the rest.
        Thread.sleep(2000L);
        timePool.evict();
        assertEquals("Idle count different than expected.", 0, timePool.getNumIdle());
        timePool.close();
    }

    @Test(timeout=60000)
    public void testEvictionInvalid() throws Exception {

        final GenericObjectPool<Object> invalidFactoryPool =
                new GenericObjectPool<Object>(new InvalidFactory());

        invalidFactoryPool.setMaxIdle(1);
        invalidFactoryPool.setMaxTotal(1);
        invalidFactoryPool.setTestOnBorrow(false);
        invalidFactoryPool.setTestOnReturn(false);
        invalidFactoryPool.setTestWhileIdle(true);
        invalidFactoryPool.setMinEvictableIdleTimeMillis(100000);
        invalidFactoryPool.setNumTestsPerEvictionRun(1);

        final Object p = invalidFactoryPool.borrowObject();
        invalidFactoryPool.returnObject(p);

        // Run eviction in a separate thread
        final Thread t = new EvictionThread<Object>(invalidFactoryPool);
        t.start();

        // Sleep to make sure evictor has started
        Thread.sleep(300);

        try {
            invalidFactoryPool.borrowObject(1);
        } catch (final NoSuchElementException nsee) {
            // Ignore
        }

        // Make sure evictor has finished
        Thread.sleep(1000);

        // Should have an empty pool
        assertEquals("Idle count different than expected.", 0, invalidFactoryPool.getNumIdle());
        assertEquals("Total count different than expected.", 0, invalidFactoryPool.getNumActive());
        invalidFactoryPool.close();
    }

    /**
     * POOL-231 - verify that concurrent invalidates of the same object do not
     * corrupt pool destroyCount.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testConcurrentInvalidate() throws Exception {
        // Get allObjects and idleObjects loaded with some instances
        final int nObjects = 1000;
        pool.setMaxTotal(nObjects);
        pool.setMaxIdle(nObjects);
        final String[] obj = new String[nObjects];
        for (int i = 0; i < nObjects; i++) {
            obj[i] = pool.borrowObject();
        }
        for (int i = 0; i < nObjects; i++) {
            if (i % 2 == 0) {
                pool.returnObject(obj[i]);
            }
        }
        final int nThreads = 20;
        final int nIterations = 60;
        final InvalidateThread[] threads = new InvalidateThread[nThreads];
        // Randomly generated list of distinct invalidation targets
        final ArrayList<Integer> targets = new ArrayList<Integer>();
        final Random random = new Random();
        for (int j = 0; j < nIterations; j++) {
            // Get a random invalidation target
            Integer targ = Integer.valueOf(random.nextInt(nObjects));
            while (targets.contains(targ)) {
                targ = Integer.valueOf(random.nextInt(nObjects));
            }
            targets.add(targ);
            // Launch nThreads threads all trying to invalidate the target
            for (int i = 0; i < nThreads; i++) {
                threads[i] = new InvalidateThread(pool, obj[targ.intValue()]);
            }
            for (int i = 0; i < nThreads; i++) {
                new Thread(threads[i]).start();
            }
            boolean done = false;
            while (!done) {
                done = true;
                for (int i = 0; i < nThreads; i++) {
                    done = done && threads[i].complete();
                }
                Thread.sleep(100);
            }
        }
        Assert.assertEquals(nIterations, pool.getDestroyedCount());
    }

    /**
     * Attempts to invalidate an object, swallowing IllegalStateException.
     */
    static class InvalidateThread implements Runnable {
        private final String obj;
        private final ObjectPool<String> pool;
        private boolean done = false;
        public InvalidateThread(final ObjectPool<String> pool, final String obj) {
            this.obj = obj;
            this.pool = pool;
        }
        @Override
        public void run() {
            try {
                pool.invalidateObject(obj);
            } catch (final IllegalStateException ex) {
                // Ignore
            } catch (final Exception ex) {
                Assert.fail("Unexpected exception " + ex.toString());
            } finally {
                done = true;
            }
        }
        public boolean complete() {
            return done;
        }
    }

    @Test(timeout=60000)
    public void testMinIdle() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxTotal(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        final String[] active = new String[5];
        active[0] = pool.borrowObject();

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=1 ; i<5 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);
    }

    @Test(timeout=60000)
    public void testMinIdleMaxTotal() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxTotal(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        final String[] active = new String[10];

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);

        for(int i=0 ; i<10 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        for(int i=0 ; i<10 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);
    }

    /**
     * Kicks off <numThreads> test threads, each of which will go through
     * <iterations> borrow-return cycles with random delay times <= delay
     * in between.
     */
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void runTestThreads(final int numThreads, final int iterations, final int delay, final GenericObjectPool testPool) {
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            threads[i] = new TestThread<String>(testPool,iterations,delay);
            final Thread t = new Thread(threads[i]);
            t.start();
        }
        for(int i=0;i<numThreads;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(final InterruptedException e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail("Thread "+i+" failed: "+threads[i]._error.toString());
            }
        }
    }

    @Test(timeout=60000)
    public void testThreaded1() throws Exception {
        pool.setMaxTotal(15);
        pool.setMaxIdle(15);
        pool.setMaxWaitMillis(1000L);
        runTestThreads(20, 100, 50, pool);
    }

    /**
     * Verifies that maxTotal is not exceeded when factory destroyObject
     * has high latency, testOnReturn is set and there is high incidence of
     * validation failures.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test(timeout=60000)
    public void testMaxTotalInvariant() throws Exception {
        final int maxTotal = 15;
        factory.setEvenValid(false);     // Every other validation fails
        factory.setDestroyLatency(100);  // Destroy takes 100 ms
        factory.setMaxTotal(maxTotal); // (makes - destroys) bound
        factory.setValidationEnabled(true);
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(-1);
        pool.setTestOnReturn(true);
        pool.setMaxWaitMillis(1000L);
        runTestThreads(5, 10, 50, pool);
    }

    @Test(timeout=60000)
    public void testConcurrentBorrowAndEvict() throws Exception {

        pool.setMaxTotal(1);
        pool.addObject();

        for( int i=0; i<5000; i++) {
            final ConcurrentBorrowAndEvictThread one =
                    new ConcurrentBorrowAndEvictThread(true);
            final ConcurrentBorrowAndEvictThread two =
                    new ConcurrentBorrowAndEvictThread(false);

            one.start();
            two.start();
            one.join();
            two.join();

            pool.returnObject(one.obj);

            /* Uncomment this for a progress indication
            if (i % 10 == 0) {
                System.out.println(i/10);
            }
            */
        }
    }

    /**
     * Verifies that concurrent threads never "share" instances
     */
    @Test
    public void testNoInstanceOverlap() {
        final int maxTotal = 5;
        final int numThreads = 100;
        final int delay = 1;
        final int iterations = 1000;
        final AtomicIntegerFactory factory = new AtomicIntegerFactory();
        final GenericObjectPool<AtomicInteger> pool = new GenericObjectPool<AtomicInteger>(factory);
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(maxTotal);
        pool.setTestOnBorrow(true);
        pool.setBlockWhenExhausted(true);
        pool.setMaxWaitMillis(-1);
        runTestThreads(numThreads, iterations, delay, pool);
        Assert.assertEquals(0, pool.getDestroyedByBorrowValidationCount());
    }

    /**
     * POOL-189
     *
     * @throws Exception May occur in some failure modes
     */
    @Test(timeout=60000)
    public void testWhenExhaustedBlockClosePool() throws Exception {
        pool.setMaxTotal(1);
        pool.setBlockWhenExhausted(true);
        pool.setMaxWaitMillis(-1);
        final Object obj1 = pool.borrowObject();

        // Make sure an object was obtained
        assertNotNull(obj1);

        // Create a separate thread to try and borrow another object
        final WaitingTestThread wtt = new WaitingTestThread(pool, 200);
        wtt.start();
        // Give wtt time to start
        Thread.sleep(200);

        // close the pool (Bug POOL-189)
        pool.close();

        // Give interrupt time to take effect
        Thread.sleep(200);

        // Check thread was interrupted
        assertTrue(wtt._thrown instanceof InterruptedException);
    }

    private class ConcurrentBorrowAndEvictThread extends Thread {
        private final boolean borrow;
        public String obj;

        public ConcurrentBorrowAndEvictThread(final boolean borrow) {
            this.borrow = borrow;
        }

        @Override
        public void run() {
            try {
                if (borrow) {
                    obj = pool.borrowObject();
                } else {
                    pool.evict();
                }
            } catch (final Exception e) { /* Ignore */}
        }
    }

    static class TestThread<T> implements Runnable {

        /** source of random delay times */
        private final java.util.Random _random;

        /** pool to borrow from */
        private final ObjectPool<T> _pool;

        /** number of borrow attempts */
        private final int _iter;

        /** delay before each borrow attempt */
        private final int _startDelay;

        /** time to hold each borrowed object before returning it */
        private final int _holdTime;

        /** whether or not start and hold time are randomly generated */
        private final boolean _randomDelay;

        /** object expected to be borrowed (fail otherwise) */
        private final Object _expectedObject;

        private volatile boolean _complete = false;
        private volatile boolean _failed = false;
        private volatile Throwable _error;

        public TestThread(final ObjectPool<T> pool) {
            this(pool, 100, 50, true, null);
        }

        public TestThread(final ObjectPool<T> pool, final int iter) {
            this(pool, iter, 50, true, null);
        }

        public TestThread(final ObjectPool<T> pool, final int iter, final int delay) {
            this(pool, iter, delay, true, null);
        }

        public TestThread(final ObjectPool<T> pool, final int iter, final int delay,
                final boolean randomDelay) {
            this(pool, iter, delay, randomDelay, null);
        }

        public TestThread(final ObjectPool<T> pool, final int iter, final int delay,
                final boolean randomDelay, final Object obj) {
            this(pool, iter, delay, delay, randomDelay, obj);
        }

        public TestThread(final ObjectPool<T> pool, final int iter, final int startDelay,
            final int holdTime, final boolean randomDelay, final Object obj) {
        _pool = pool;
        _iter = iter;
        _startDelay = startDelay;
        _holdTime = holdTime;
        _randomDelay = randomDelay;
        _random = _randomDelay ? new Random() : null;
        _expectedObject = obj;
    }

        public boolean complete() {
            return _complete;
        }

        public boolean failed() {
            return _failed;
        }

        @Override
        public void run() {
            for(int i=0;i<_iter;i++) {
                final long startDelay =
                    _randomDelay ? (long)_random.nextInt(_startDelay) : _startDelay;
                final long holdTime =
                    _randomDelay ? (long)_random.nextInt(_holdTime) : _holdTime;
                try {
                    Thread.sleep(startDelay);
                } catch(final InterruptedException e) {
                    // ignored
                }
                T obj = null;
                try {
                    obj = _pool.borrowObject();
                } catch(final Exception e) {
                    _error = e;
                    _failed = true;
                    _complete = true;
                    break;
                }

                if (_expectedObject != null && !_expectedObject.equals(obj)) {
                    _error = new Throwable("Expected: "+_expectedObject+ " found: "+obj);
                    _failed = true;
                    _complete = true;
                    break;
                }

                try {
                    Thread.sleep(holdTime);
                } catch(final InterruptedException e) {
                    // ignored
                }
                try {
                    _pool.returnObject(obj);
                } catch(final Exception e) {
                    _error = e;
                    _failed = true;
                    _complete = true;
                    break;
                }
            }
            _complete = true;
        }
    }

    @Test(timeout=60000)
    public void testFIFO() throws Exception {
        String o = null;
        pool.setLifo(false);
        pool.addObject(); // "0"
        pool.addObject(); // "1"
        pool.addObject(); // "2"
        assertEquals("Oldest", "0", pool.borrowObject());
        assertEquals("Middle", "1", pool.borrowObject());
        assertEquals("Youngest", "2", pool.borrowObject());
        o = pool.borrowObject();
        assertEquals("new-3", "3", o);
        pool.returnObject(o);
        assertEquals("returned-3", o, pool.borrowObject());
        assertEquals("new-4", "4", pool.borrowObject());
    }

    @Test(timeout=60000)
    public void testLIFO() throws Exception {
        String o = null;
        pool.setLifo(true);
        pool.addObject(); // "0"
        pool.addObject(); // "1"
        pool.addObject(); // "2"
        assertEquals("Youngest", "2", pool.borrowObject());
        assertEquals("Middle", "1", pool.borrowObject());
        assertEquals("Oldest", "0", pool.borrowObject());
        o = pool.borrowObject();
        assertEquals("new-3", "3", o);
        pool.returnObject(o);
        assertEquals("returned-3", o, pool.borrowObject());
        assertEquals("new-4", "4", pool.borrowObject());
    }

    @Test(timeout=60000)
    public void testAddObject() throws Exception {
        assertEquals("should be zero idle", 0, pool.getNumIdle());
        pool.addObject();
        assertEquals("should be one idle", 1, pool.getNumIdle());
        assertEquals("should be zero active", 0, pool.getNumActive());
        final String obj = pool.borrowObject();
        assertEquals("should be zero idle", 0, pool.getNumIdle());
        assertEquals("should be one active", 1, pool.getNumActive());
        pool.returnObject(obj);
        assertEquals("should be one idle", 1, pool.getNumIdle());
        assertEquals("should be zero active", 0, pool.getNumActive());
    }

    protected GenericObjectPool<String> pool = null;

    private SimpleFactory factory = null;

    private void assertConfiguration(final GenericObjectPoolConfig expected, final GenericObjectPool<?> actual) throws Exception {
        assertEquals("testOnCreate",Boolean.valueOf(expected.getTestOnCreate()),
                Boolean.valueOf(actual.getTestOnCreate()));
        assertEquals("testOnBorrow",Boolean.valueOf(expected.getTestOnBorrow()),
                Boolean.valueOf(actual.getTestOnBorrow()));
        assertEquals("testOnReturn",Boolean.valueOf(expected.getTestOnReturn()),
                Boolean.valueOf(actual.getTestOnReturn()));
        assertEquals("testWhileIdle",Boolean.valueOf(expected.getTestWhileIdle()),
                Boolean.valueOf(actual.getTestWhileIdle()));
        assertEquals("whenExhaustedAction",
                Boolean.valueOf(expected.getBlockWhenExhausted()),
                Boolean.valueOf(actual.getBlockWhenExhausted()));
        assertEquals("maxTotal",expected.getMaxTotal(),actual.getMaxTotal());
        assertEquals("maxIdle",expected.getMaxIdle(),actual.getMaxIdle());
        assertEquals("maxWait",expected.getMaxWaitMillis(),actual.getMaxWaitMillis());
        assertEquals("minEvictableIdleTimeMillis",expected.getMinEvictableIdleTimeMillis(),actual.getMinEvictableIdleTimeMillis());
        assertEquals("numTestsPerEvictionRun",expected.getNumTestsPerEvictionRun(),actual.getNumTestsPerEvictionRun());
        assertEquals("evictorShutdownTimeoutMillis",expected.getEvictorShutdownTimeoutMillis(),actual.getEvictorShutdownTimeoutMillis());
        assertEquals("timeBetweenEvictionRunsMillis",expected.getTimeBetweenEvictionRunsMillis(),actual.getTimeBetweenEvictionRunsMillis());
    }

    public static class SimpleFactory implements PooledObjectFactory<String> {
        public SimpleFactory() {
            this(true);
        }

        public SimpleFactory(final boolean valid) {
            this(valid,valid);
        }

        public SimpleFactory(final boolean evalid, final boolean ovalid) {
            evenValid = evalid;
            oddValid = ovalid;
        }

        public synchronized void setValid(final boolean valid) {
            setEvenValid(valid);
            setOddValid(valid);
        }

        public synchronized void setEvenValid(final boolean valid) {
            evenValid = valid;
        }

        public synchronized void setOddValid(final boolean valid) {
            oddValid = valid;
        }

        public synchronized void setThrowExceptionOnPassivate(final boolean bool) {
            exceptionOnPassivate = bool;
        }

        public synchronized void setMaxTotal(final int maxTotal) {
            this.maxTotal = maxTotal;
        }

        public synchronized void setDestroyLatency(final long destroyLatency) {
            this.destroyLatency = destroyLatency;
        }

        public synchronized void setMakeLatency(final long makeLatency) {
            this.makeLatency = makeLatency;
        }

        public synchronized void setValidateLatency(final long validateLatency) {
            this.validateLatency = validateLatency;
        }

        @Override
        public PooledObject<String> makeObject() {
            final long waitLatency;
            synchronized(this) {
                activeCount++;
                if (activeCount > maxTotal) {
                    throw new IllegalStateException(
                        "Too many active instances: " + activeCount);
                }
                waitLatency = makeLatency;
            }
            if (waitLatency > 0) {
                doWait(waitLatency);
            }
            final int counter;
            synchronized(this) {
                counter = makeCounter++;
            }
            return new DefaultPooledObject<String>(String.valueOf(counter));
        }

        @Override
        public void destroyObject(final PooledObject<String> obj) throws Exception {
            final long waitLatency;
            final boolean hurl;
            synchronized(this) {
                waitLatency = destroyLatency;
                hurl = exceptionOnDestroy;
            }
            if (waitLatency > 0) {
                doWait(waitLatency);
            }
            synchronized(this) {
                activeCount--;
            }
            if (hurl) {
                throw new Exception();
            }
        }

        @Override
        public boolean validateObject(final PooledObject<String> obj) {
            final boolean validate;
            final boolean evenTest;
            final boolean oddTest;
            final long waitLatency;
            final int counter;
            synchronized(this) {
                validate = enableValidation;
                evenTest = evenValid;
                oddTest = oddValid;
                counter = validateCounter++;
                waitLatency = validateLatency;
            }
            if (waitLatency > 0) {
                doWait(waitLatency);
            }
            if (validate) {
                return counter%2 == 0 ? evenTest : oddTest;
            }
            return true;
        }

        @Override
        public void activateObject(final PooledObject<String> obj) throws Exception {
            final boolean hurl;
            final boolean evenTest;
            final boolean oddTest;
            final int counter;
            synchronized(this) {
                hurl = exceptionOnActivate;
                evenTest = evenValid;
                oddTest = oddValid;
                counter = activationCounter++;
            }
            if (hurl) {
                if (!(counter%2 == 0 ? evenTest : oddTest)) {
                    throw new Exception();
                }
            }
        }

        @Override
        public void passivateObject(final PooledObject<String> obj) throws Exception {
            final boolean hurl;
            synchronized(this) {
                hurl = exceptionOnPassivate;
            }
            if (hurl) {
                throw new Exception();
            }
        }
        int makeCounter = 0;
        int activationCounter = 0;
        int validateCounter = 0;
        int activeCount = 0;
        boolean evenValid = true;
        boolean oddValid = true;
        boolean exceptionOnPassivate = false;
        boolean exceptionOnActivate = false;
        boolean exceptionOnDestroy = false;
        boolean enableValidation = true;
        long destroyLatency = 0;
        long makeLatency = 0;
        long validateLatency = 0;
        int maxTotal = Integer.MAX_VALUE;

        public synchronized boolean isThrowExceptionOnActivate() {
            return exceptionOnActivate;
        }

        public synchronized void setThrowExceptionOnActivate(final boolean b) {
            exceptionOnActivate = b;
        }

        public synchronized void setThrowExceptionOnDestroy(final boolean b) {
            exceptionOnDestroy = b;
        }

        public synchronized boolean isValidationEnabled() {
            return enableValidation;
        }

        public synchronized void setValidationEnabled(final boolean b) {
            enableValidation = b;
        }

        public synchronized int getMakeCounter() {
            return makeCounter;
        }

        private void doWait(final long latency) {
            try {
                Thread.sleep(latency);
            } catch (final InterruptedException ex) {
                // ignore
            }
        }
    }

    protected static class AtomicIntegerFactory
    extends BasePooledObjectFactory<AtomicInteger> {

    private long activateLatency = 0;
    private long passivateLatency = 0;
    private long createLatency = 0;
    private long destroyLatency = 0;
    private long validateLatency = 0;

    @Override
    public AtomicInteger create() {
        try {
            Thread.sleep(createLatency);
        } catch (final InterruptedException ex) {}
        return new AtomicInteger(0);
    }

    @Override
    public PooledObject<AtomicInteger> wrap(final AtomicInteger integer) {
        return new DefaultPooledObject<AtomicInteger>(integer);
    }

    @Override
    public void activateObject(final PooledObject<AtomicInteger> p) {
        p.getObject().incrementAndGet();
        try {
            Thread.sleep(activateLatency);
        } catch (final InterruptedException ex) {}
    }

    @Override
    public void passivateObject(final PooledObject<AtomicInteger> p) {
        p.getObject().decrementAndGet();
        try {
            Thread.sleep(passivateLatency);
        } catch (final InterruptedException ex) {}
    }

    @Override
    public boolean validateObject(final PooledObject<AtomicInteger> instance) {
        try {
            Thread.sleep(validateLatency);
        } catch (final InterruptedException ex) {}
        return instance.getObject().intValue() == 1;
    }

    @Override
    public void destroyObject(final PooledObject<AtomicInteger> p) {
        try {
            Thread.sleep(destroyLatency);
        } catch (final InterruptedException ex) {}
    }


    /**
     * @param activateLatency the activateLatency to set
     */
    public void setActivateLatency(final long activateLatency) {
        this.activateLatency = activateLatency;
    }


    /**
     * @param passivateLatency the passivateLatency to set
     */
    public void setPassivateLatency(final long passivateLatency) {
        this.passivateLatency = passivateLatency;
    }


    /**
     * @param createLatency the createLatency to set
     */
    public void setCreateLatency(final long createLatency) {
        this.createLatency = createLatency;
    }


    /**
     * @param destroyLatency the destroyLatency to set
     */
    public void setDestroyLatency(final long destroyLatency) {
        this.destroyLatency = destroyLatency;
    }


    /**
     * @param validateLatency the validateLatency to set
     */
    public void setValidateLatency(final long validateLatency) {
        this.validateLatency = validateLatency;
    }
}

    @Override
    protected boolean isLifo() {
        return true;
    }

    @Override
    protected boolean isFifo() {
        return false;
    }

    /*
     * Note: This test relies on timing for correct execution. There *should* be
     * enough margin for this to work correctly on most (all?) systems but be
     * aware of this if you see a failure of this test.
     */
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test(timeout=60000)
    public void testBorrowObjectFairness() throws Exception {

        final int numThreads = 40;
        final int maxTotal = 40;

        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxTotal);
        config.setFairness(true);
        config.setLifo(false);

        pool = new GenericObjectPool(factory, config);

        // Exhaust the pool
        final String[] objects = new String[maxTotal];
        for (int i = 0; i < maxTotal; i++) {
            objects[i] = pool.borrowObject();
        }

        // Start and park threads waiting to borrow objects
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            threads[i] = new TestThread(pool, 1, 0, 2000, false, String.valueOf(i % maxTotal));
            final Thread t = new Thread(threads[i]);
            t.start();
            // Short delay to ensure threads start in correct order
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                fail(e.toString());
            }
        }

        // Return objects, other threads should get served in order
        for (int i = 0; i < maxTotal; i++) {
            pool.returnObject(objects[i]);
        }

        // Wait for threads to finish
        for(int i=0;i<numThreads;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(final InterruptedException e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail("Thread "+i+" failed: "+threads[i]._error.toString());
            }
        }
    }

    /**
     * On first borrow, first object fails validation, second object is OK.
     * Subsequent borrows are OK. This was POOL-152.
     */
    @Test(timeout=60000)
    public void testBrokenFactoryShouldNotBlockPool() {
        final int maxTotal = 1;

        factory.setMaxTotal(maxTotal);
        pool.setMaxTotal(maxTotal);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnBorrow(true);

        // First borrow object will need to create a new object which will fail
        // validation.
        String obj = null;
        Exception ex = null;
        factory.setValid(false);
        try {
            obj = pool.borrowObject();
        } catch (final Exception e) {
            ex = e;
        }
        // Failure expected
        assertNotNull(ex);
        assertTrue(ex instanceof NoSuchElementException);
        assertNull(obj);

        // Configure factory to create valid objects so subsequent borrows work
        factory.setValid(true);

        // Subsequent borrows should be OK
        try {
            obj = pool.borrowObject();
        } catch (final Exception e1) {
            fail();
        }
        assertNotNull(obj);
        try {
            pool.returnObject(obj);
        } catch (final Exception e) {
            fail();
        }
    }

    /*
     * Very simple test thread that just tries to borrow an object from
     * the provided pool returns it after a wait
     */
    static class WaitingTestThread extends Thread {
        private final GenericObjectPool<String> _pool;
        private final long _pause;
        private Throwable _thrown;

        private long preborrow; // just before borrow
        private long postborrow; //  borrow returned
        private long postreturn; // after object was returned
        private long ended;
        private String objectId;

        public WaitingTestThread(final GenericObjectPool<String> pool, final long pause) {
            _pool = pool;
            _pause = pause;
            _thrown = null;
        }

        @Override
        public void run() {
            try {
                preborrow = System.currentTimeMillis();
                final String obj = _pool.borrowObject();
                objectId = obj;
                postborrow = System.currentTimeMillis();
                Thread.sleep(_pause);
                _pool.returnObject(obj);
                postreturn = System.currentTimeMillis();
            } catch (final Exception e) {
                _thrown = e;
            } finally{
                ended = System.currentTimeMillis();
            }
        }
    }

    private static final boolean DISPLAY_THREAD_DETAILS=
        Boolean.valueOf(System.getProperty("TestGenericObjectPool.display.thread.details", "false")).booleanValue();
    // To pass this to a Maven test, use:
    // mvn test -DargLine="-DTestGenericObjectPool.display.thread.details=true"
    // @see http://jira.codehaus.org/browse/SUREFIRE-121

    /*
     * Test multi-threaded pool access.
     * Multiple threads, but maxTotal only allows half the threads to succeed.
     *
     * This test was prompted by Continuum build failures in the Commons DBCP test case:
     * TestPerUserPoolDataSource.testMultipleThreads2()
     * Let's see if the this fails on Continuum too!
     */
    @Test(timeout=60000)
    public void testMaxWaitMultiThreaded() throws Exception {
        final long maxWait = 500; // wait for connection
        final long holdTime = 2 * maxWait; // how long to hold connection
        final int threads = 10; // number of threads to grab the object initially
        pool.setBlockWhenExhausted(true);
        pool.setMaxWaitMillis(maxWait);
        pool.setMaxTotal(threads);
        // Create enough threads so half the threads will have to wait
        final WaitingTestThread wtt[] = new WaitingTestThread[threads * 2];
        for(int i=0; i < wtt.length; i++){
            wtt[i] = new WaitingTestThread(pool,holdTime);
        }
        final long origin = System.currentTimeMillis()-1000;
        for (final WaitingTestThread element : wtt) {
            element.start();
        }
        int failed = 0;
        for (final WaitingTestThread element : wtt) {
            element.join();
            if (element._thrown != null){
                failed++;
            }
        }
        if (DISPLAY_THREAD_DETAILS || wtt.length/2 != failed){
            System.out.println(
                    "MaxWait: " + maxWait +
                    " HoldTime: " + holdTime +
                     " MaxTotal: " + threads +
                    " Threads: " + wtt.length +
                    " Failed: " + failed
                    );
            for (final WaitingTestThread wt : wtt) {
                System.out.println(
                        "Preborrow: " + (wt.preborrow-origin) +
                        " Postborrow: " + (wt.postborrow != 0 ? wt.postborrow-origin : -1) +
                        " BorrowTime: " + (wt.postborrow != 0 ? wt.postborrow-wt.preborrow : -1) +
                        " PostReturn: " + (wt.postreturn != 0 ? wt.postreturn-origin : -1) +
                        " Ended: " + (wt.ended-origin) +
                        " ObjId: " + wt.objectId
                        );
            }
        }
        assertEquals("Expected half the threads to fail",wtt.length/2,failed);
    }

    /**
     * Test the following scenario:
     *   Thread 1 borrows an instance
     *   Thread 2 starts to borrow another instance before thread 1 returns its instance
     *   Thread 1 returns its instance while thread 2 is validating its newly created instance
     * The test verifies that the instance created by Thread 2 is not leaked.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test(timeout=60000)
    public void testMakeConcurrentWithReturn() throws Exception {
        pool.setTestOnBorrow(true);
        factory.setValid(true);
        // Borrow and return an instance, with a short wait
        final WaitingTestThread thread1 = new WaitingTestThread(pool, 200);
        thread1.start();
        Thread.sleep(50); // wait for validation to succeed
        // Slow down validation and borrow an instance
        factory.setValidateLatency(400);
        final String instance = pool.borrowObject();
        // Now make sure that we have not leaked an instance
        assertEquals(factory.getMakeCounter(), pool.getNumIdle() + 1);
        pool.returnObject(instance);
        assertEquals(factory.getMakeCounter(), pool.getNumIdle());
    }

    /**
     * Ensure the pool is registered.
     */
    @Test(timeout=60000)
    public void testJmxRegistration() {
        final ObjectName oname = pool.getJmxName();
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final Set<ObjectName> result = mbs.queryNames(oname, null);
        Assert.assertEquals(1, result.size());
        pool.jmxUnregister();

        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setJmxEnabled(false);
        final GenericObjectPool<String> poolWithoutJmx = new GenericObjectPool<String>(factory, config);
        assertNull(poolWithoutJmx.getJmxName());
        config.setJmxEnabled(true);
        poolWithoutJmx.jmxUnregister();

        config.setJmxNameBase(null);
        final GenericObjectPool<String> poolWithDefaultJmxNameBase = new GenericObjectPool<String>(factory, config);
        assertNotNull(poolWithDefaultJmxNameBase.getJmxName());
    }

    /**
     * Verify that threads waiting on a depleted pool get served when a checked out object is
     * invalidated.
     *
     * JIRA: POOL-240
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testInvalidateFreesCapacity()
        throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        final GenericObjectPool<String> pool = new GenericObjectPool<String>(factory);
        pool.setMaxTotal(2);
        pool.setMaxWaitMillis(500);
        // Borrow an instance and hold if for 5 seconds
        final WaitingTestThread thread1 = new WaitingTestThread(pool, 5000);
        thread1.start();
        // Borrow another instance
        final String obj = pool.borrowObject();
        // Launch another thread - will block, but fail in 500 ms
        final WaitingTestThread thread2 = new WaitingTestThread(pool, 100);
        thread2.start();
        // Invalidate the object borrowed by this thread - should allow thread2 to create
        Thread.sleep(20);
        pool.invalidateObject(obj);
        Thread.sleep(600); // Wait for thread2 to timeout
        if (thread2._thrown != null) {
            fail(thread2._thrown.toString());
        }
    }

    /**
     * Verify that threads waiting on a depleted pool get served when a returning object fails
     * validation.
     *
     * JIRA: POOL-240
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testValidationFailureOnReturnFreesCapacity()
        throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        factory.setValid(false); // Validate will always fail
        factory.setValidationEnabled(true);
        final GenericObjectPool<String> pool = new GenericObjectPool<String>(factory);
        pool.setMaxTotal(2);
        pool.setMaxWaitMillis(1500);
        pool.setTestOnReturn(true);
        pool.setTestOnBorrow(false);
        // Borrow an instance and hold if for 5 seconds
        final WaitingTestThread thread1 = new WaitingTestThread(pool, 5000);
        thread1.start();
        // Borrow another instance and return it after 500 ms (validation will fail)
        final WaitingTestThread thread2 = new WaitingTestThread(pool, 500);
        thread2.start();
        Thread.sleep(50);
        // Try to borrow an object
        final String obj = pool.borrowObject();
        pool.returnObject(obj);
    }

    @Test
    public void testSwallowedExceptionListener() {
        pool.setSwallowedExceptionListener(null); // must simply return
        final List<Exception> swallowedExceptions = new ArrayList<Exception>();
        /*
         * A simple listener, that will throw a OOM on 3rd exception.
         */
        final SwallowedExceptionListener listener = new SwallowedExceptionListener() {
            @Override
            public void onSwallowException(final Exception e) {
                if (swallowedExceptions.size() == 2) {
                    throw new OutOfMemoryError();
                }
                swallowedExceptions.add(e);
            }
        };
        pool.setSwallowedExceptionListener(listener);

        final Exception e1 = new Exception();
        final Exception e2 = new ArrayIndexOutOfBoundsException();

        pool.swallowException(e1);
        pool.swallowException(e2);

        try {
            pool.swallowException(e1);
            fail("Not supposed to get here");
        } catch (final OutOfMemoryError oom) {
            // expected
        }

        assertEquals(2, swallowedExceptions.size());
    }

    // POOL-248
    @Test(expected=IllegalStateException.class)
    public void testMultipleReturnOfSameObject() throws Exception {
        final GenericObjectPool<String> pool = new GenericObjectPool<String>(
                factory, new GenericObjectPoolConfig());

        Assert.assertEquals(0, pool.getNumActive());
        Assert.assertEquals(0, pool.getNumIdle());

        final String obj = pool.borrowObject();

        Assert.assertEquals(1, pool.getNumActive());
        Assert.assertEquals(0, pool.getNumIdle());

        pool.returnObject(obj);

        Assert.assertEquals(0, pool.getNumActive());
        Assert.assertEquals(1, pool.getNumIdle());

        pool.returnObject(obj);

        Assert.assertEquals(0, pool.getNumActive());
        Assert.assertEquals(1, pool.getNumIdle());
    }

    // POOL-259
    @Test
    public void testClientWaitStats() throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        // Give makeObject a little latency
        factory.setMakeLatency(200);
        final GenericObjectPool<String> pool = new GenericObjectPool<String>(
                factory, new GenericObjectPoolConfig());
        final String s = pool.borrowObject();
        // First borrow waits on create, so wait time should be at least 200 ms
        // Allow 100ms error in clock times
        Assert.assertTrue(pool.getMaxBorrowWaitTimeMillis() >= 100);
        Assert.assertTrue(pool.getMeanBorrowWaitTimeMillis() >= 100);
        pool.returnObject(s);
        pool.borrowObject();
        // Second borrow does not have to wait on create, average should be about 100
        Assert.assertTrue(pool.getMaxBorrowWaitTimeMillis() > 100);
        Assert.assertTrue(pool.getMeanBorrowWaitTimeMillis() < 200);
        Assert.assertTrue(pool.getMeanBorrowWaitTimeMillis() > 20);
    }

    // POOL-276
    @Test
    public void testValidationOnCreateOnly() throws Exception {
        pool.setMaxTotal(1);
        pool.setTestOnCreate(true);
        pool.setTestOnBorrow(false);
        pool.setTestOnReturn(false);
        pool.setTestWhileIdle(false);

        final String o1 = pool.borrowObject();
        Assert.assertEquals("0", o1);
        final Timer t = new Timer();
        t.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        pool.returnObject(o1);
                    }
                }, 3000);

        final String o2 = pool.borrowObject();
        Assert.assertEquals("0", o2);

        Assert.assertEquals(1, factory.validateCounter);
    }

    /**
     * Verifies that when a factory's makeObject produces instances that are not
     * discernible by equals, the pool can handle them.
     *
     * JIRA: POOL-283
     */
    @Test
    public void testEqualsIndiscernible() throws Exception {
        final HashSetFactory factory = new HashSetFactory();
        final GenericObjectPool<HashSet<String>> pool = new GenericObjectPool<HashSet<String>>(
                factory, new GenericObjectPoolConfig());
        final HashSet<String> s1 = pool.borrowObject();
        final HashSet<String> s2 = pool.borrowObject();
        pool.returnObject(s1);
        pool.returnObject(s2);
        pool.close();
    }

    /**
     * Verifies that when a borrowed object is mutated in a way that does not
     * preserve equality and hashcode, the pool can recognized it on return.
     *
     * JIRA: POOL-284
     */
    @Test
    public void testMutable() throws Exception {
        final HashSetFactory factory = new HashSetFactory();
        final GenericObjectPool<HashSet<String>> pool = new GenericObjectPool<HashSet<String>>(
                factory, new GenericObjectPoolConfig());
        final HashSet<String> s1 = pool.borrowObject();
        final HashSet<String> s2 = pool.borrowObject();
        s1.add("One");
        s2.add("One");
        pool.returnObject(s1);
        pool.returnObject(s2);
        pool.close();
    }

    /**
     * Verifies that returning an object twice (without borrow in between) causes ISE
     * but does not re-validate or re-passivate the instance.
     *
     * JIRA: POOL-285
     */
    @Test
    public void testMultipleReturn() throws Exception {
        final WaiterFactory<String> factory = new WaiterFactory<String>(0, 0, 0, 0, 0, 0);
        final GenericObjectPool<Waiter> pool = new GenericObjectPool<Waiter>(factory);
        pool.setTestOnReturn(true);
        final Waiter waiter = pool.borrowObject();
        pool.returnObject(waiter);
        Assert.assertEquals(1, waiter.getValidationCount());
        Assert.assertEquals(1, waiter.getPassivationCount());
        try {
            pool.returnObject(waiter);
            fail("Expecting IllegalStateException from multiple return");
        } catch (final IllegalStateException ex) {
            // Exception is expected, now check no repeat validation/passivation
            Assert.assertEquals(1, waiter.getValidationCount());
            Assert.assertEquals(1, waiter.getPassivationCount());
        }
    }

    public void testPreparePool() throws Exception {
        pool.setMinIdle(1);
        pool.setMaxTotal(1);
        pool.preparePool();
        Assert.assertEquals(1, pool.getNumIdle());
        final String obj = pool.borrowObject();
        pool.preparePool();
        Assert.assertEquals(0, pool.getNumIdle());
        pool.setMinIdle(0);
        pool.returnObject(obj);
        pool.preparePool();
        Assert.assertEquals(0, pool.getNumIdle());
    }

    private static final class DummyFactory
            extends BasePooledObjectFactory<Object> {
        @Override
        public Object create() throws Exception {
            return null;
        }
        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<Object>(value);
        }
    }

    /**
     * Factory that creates HashSets.  Note that this means
     *  0) All instances are initially equal (not discernible by equals)
     *  1) Instances are mutable and mutation can cause change in identity / hashcode.
     */
    private static final class HashSetFactory
            extends BasePooledObjectFactory<HashSet<String>> {
        @Override
        public HashSet<String> create() throws Exception {
            return new HashSet<String>();
        }
        @Override
        public PooledObject<HashSet<String>> wrap(final HashSet<String> value) {
            return new DefaultPooledObject<HashSet<String>>(value);
        }
    }

    private static class InvalidFactory
            extends BasePooledObjectFactory<Object> {

        @Override
        public Object create() throws Exception {
            return new Object();
        }
        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<Object>(value);
        }

        @Override
        public boolean validateObject(final PooledObject<Object> obj) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                // Ignore
            }
            return false;
        }
    }

    private static class EvictionThread<T> extends Thread {

        private final GenericObjectPool<T> pool;

        public EvictionThread(final GenericObjectPool<T> pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            try {
                pool.evict();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void testFailingFactoryDoesNotBlockThreads() throws Exception {

        final CreateFailFactory factory = new CreateFailFactory();
        final GenericObjectPool<String> createFailFactoryPool =
                new GenericObjectPool<String>(factory);

        createFailFactoryPool.setMaxTotal(1);

        // Try and borrow the first object from the pool
        final WaitingTestThread thread1 = new WaitingTestThread(createFailFactoryPool, 0);
        thread1.start();

        // Wait for thread to reach semaphore
        while(!factory.hasQueuedThreads()) {
            Thread.sleep(200);
        }

        // Try and borrow the second object from the pool
        final WaitingTestThread thread2 = new WaitingTestThread(createFailFactoryPool, 0);
        thread2.start();
        // Pool will not call factory since maximum number of object creations
        // are already queued.

        // Thread 2 will wait on an object being returned to the pool
        // Give thread 2 a chance to reach this state
        Thread.sleep(1000);

        // Release thread1
        factory.release();
        // Pre-release thread2
        factory.release();

        // Both threads should now complete.
        boolean threadRunning = true;
        int count = 0;
        while (threadRunning && count < 15) {
            threadRunning = thread1.isAlive();
            threadRunning = thread2.isAlive();
            Thread.sleep(200);
            count++;
        }
        Assert.assertFalse(thread1.isAlive());
        Assert.assertFalse(thread2.isAlive());

        Assert.assertTrue(thread1._thrown instanceof UnsupportedCharsetException);
        Assert.assertTrue(thread2._thrown instanceof UnsupportedCharsetException);
    }

    private static class CreateFailFactory extends BasePooledObjectFactory<String> {

        private final Semaphore semaphore = new Semaphore(0);

        @Override
        public String create() throws Exception {
            semaphore.acquire();
            throw new UnsupportedCharsetException("wibble");
        }

        @Override
        public PooledObject<String> wrap(final String obj) {
            return new DefaultPooledObject<String>(obj);
        }

        public void release() {
            semaphore.release();
        }

        public boolean hasQueuedThreads() {
            return semaphore.hasQueuedThreads();
        }
    }
    
	private BasePooledObjectFactory<String> createBasePooledObjectfactory() {
		return new BasePooledObjectFactory<String>() {
			@Override
			public String create() {
				// fake
				return null;
			}

			@Override
			public PooledObject<String> wrap(String obj) {
				// fake
				return null;
			}
		};
	}

	@Test
	public void testGetFactoryType() {
		GenericObjectPool<String> pool = new GenericObjectPool<String>(createBasePooledObjectfactory());
		Assert.assertNotNull((pool.getFactoryType()));
	}

	@Test
	@Ignore
	public void testGetFactoryType_PoolUtilssynchronizedPooledFactory() {
		GenericObjectPool<String> pool = new GenericObjectPool<String>(
				PoolUtils.synchronizedPooledFactory(createBasePooledObjectfactory()));
		Assert.assertNotNull((pool.getFactoryType()));
	}

	@Test
	@Ignore
	public void testGetFactoryType_SynchronizedPooledObjectFactory() {
		GenericObjectPool<String> pool = new GenericObjectPool<String>(
				new TestSynchronizedPooledObjectFactory<String>(createBasePooledObjectfactory()));
		Assert.assertNotNull((pool.getFactoryType()));
	}

}
