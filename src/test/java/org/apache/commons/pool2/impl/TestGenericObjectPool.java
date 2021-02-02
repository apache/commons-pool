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
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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
import java.util.concurrent.TimeUnit;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 */
public class TestGenericObjectPool extends TestBaseObjectPool {

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
                    obj = genericObjectPool.borrowObject();
                } else {
                    genericObjectPool.evict();
                }
            } catch (final Exception e) { /* Ignore */}
        }
    }

    private static class CreateErrorFactory extends BasePooledObjectFactory<String> {

        private final Semaphore semaphore = new Semaphore(0);

        @Override
        public String create() throws Exception {
            semaphore.acquire();
            throw new UnknownError("wiggle");
        }

        public boolean hasQueuedThreads() {
            return semaphore.hasQueuedThreads();
        }

        public void release() {
            semaphore.release();
        }

        @Override
        public PooledObject<String> wrap(final String obj) {
            return new DefaultPooledObject<>(obj);
        }
    }

    private static class CreateFailFactory extends BasePooledObjectFactory<String> {

        private final Semaphore semaphore = new Semaphore(0);

        @Override
        public String create() throws Exception {
            semaphore.acquire();
            throw new UnsupportedCharsetException("wibble");
        }

        public boolean hasQueuedThreads() {
            return semaphore.hasQueuedThreads();
        }

        public void release() {
            semaphore.release();
        }

        @Override
        public PooledObject<String> wrap(final String obj) {
            return new DefaultPooledObject<>(obj);
        }
    }

    private static final class DummyFactory
            extends BasePooledObjectFactory<Object> {
        @Override
        public Object create() throws Exception {
            return null;
        }
        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<>(value);
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

    /**
     * Factory that creates HashSets.  Note that this means
     *  0) All instances are initially equal (not discernible by equals)
     *  1) Instances are mutable and mutation can cause change in identity / hashcode.
     */
    private static final class HashSetFactory
            extends BasePooledObjectFactory<HashSet<String>> {
        @Override
        public HashSet<String> create() throws Exception {
            return new HashSet<>();
        }
        @Override
        public PooledObject<HashSet<String>> wrap(final HashSet<String> value) {
            return new DefaultPooledObject<>(value);
        }
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
        public boolean complete() {
            return done;
        }
        @Override
        public void run() {
            try {
                pool.invalidateObject(obj);
            } catch (final IllegalStateException ex) {
                // Ignore
            } catch (final Exception ex) {
                fail("Unexpected exception " + ex.toString());
            } finally {
                done = true;
            }
        }
    }

    private static class InvalidFactory
            extends BasePooledObjectFactory<Object> {

        @Override
        public Object create() throws Exception {
            return new Object();
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

        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<>(value);
        }
    }

    public static class SimpleFactory implements PooledObjectFactory<String> {
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
        private void doWait(final long latency) {
            try {
                Thread.sleep(latency);
            } catch (final InterruptedException ex) {
                // ignore
            }
        }
        public synchronized int getMakeCounter() {
            return makeCounter;
        }
        public synchronized boolean isThrowExceptionOnActivate() {
            return exceptionOnActivate;
        }
        public synchronized boolean isValidationEnabled() {
            return enableValidation;
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
            return new DefaultPooledObject<>(String.valueOf(counter));
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
        public synchronized void setDestroyLatency(final long destroyLatency) {
            this.destroyLatency = destroyLatency;
        }
        public synchronized void setEvenValid(final boolean valid) {
            evenValid = valid;
        }
        public synchronized void setMakeLatency(final long makeLatency) {
            this.makeLatency = makeLatency;
        }
        public synchronized void setMaxTotal(final int maxTotal) {
            this.maxTotal = maxTotal;
        }
        public synchronized void setOddValid(final boolean valid) {
            oddValid = valid;
        }

        public synchronized void setThrowExceptionOnActivate(final boolean b) {
            exceptionOnActivate = b;
        }

        public synchronized void setThrowExceptionOnDestroy(final boolean b) {
            exceptionOnDestroy = b;
        }

        public synchronized void setThrowExceptionOnPassivate(final boolean bool) {
            exceptionOnPassivate = bool;
        }

        public synchronized void setValid(final boolean valid) {
            setEvenValid(valid);
            setOddValid(valid);
        }

        public synchronized void setValidateLatency(final long validateLatency) {
            this.validateLatency = validateLatency;
        }

        public synchronized void setValidationEnabled(final boolean b) {
            enableValidation = b;
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
    }

    public static class TestEvictionPolicy<T> implements EvictionPolicy<T> {

        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public boolean evict(final EvictionConfig config, final PooledObject<T> underTest,
                final int idleCount) {
            return callCount.incrementAndGet() > 1500;
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

    /*
     * Very simple test thread that just tries to borrow an object from
     * the provided pool returns it after a wait
     */
    static class WaitingTestThread extends Thread {
        private final GenericObjectPool<String> _pool;
        private final long _pause;
        private Throwable _thrown;

        private long preBorrowMillis; // just before borrow
        private long postBorrowMillis; //  borrow returned
        private long postReturnMillis; // after object was returned
        private long endedMillis;
        private String objectId;

        public WaitingTestThread(final GenericObjectPool<String> pool, final long pause) {
            _pool = pool;
            _pause = pause;
            _thrown = null;
        }

        @Override
        public void run() {
            try {
                preBorrowMillis = System.currentTimeMillis();
                final String obj = _pool.borrowObject();
                objectId = obj;
                postBorrowMillis = System.currentTimeMillis();
                Thread.sleep(_pause);
                _pool.returnObject(obj);
                postReturnMillis = System.currentTimeMillis();
            } catch (final Throwable e) {
                _thrown = e;
            } finally{
                endedMillis = System.currentTimeMillis();
            }
        }
    }

    private static final boolean DISPLAY_THREAD_DETAILS=
        Boolean.valueOf(System.getProperty("TestGenericObjectPool.display.thread.details", "false")).booleanValue();
    // To pass this to a Maven test, use:
    // mvn test -DargLine="-DTestGenericObjectPool.display.thread.details=true"
    // @see https://issues.apache.org/jira/browse/SUREFIRE-121

    protected GenericObjectPool<String> genericObjectPool = null;

    private SimpleFactory simpleFactory = null;

    private void assertConfiguration(final GenericObjectPoolConfig<?> expected, final GenericObjectPool<?> actual) throws Exception {
        assertEquals(Boolean.valueOf(expected.getTestOnCreate()),
                Boolean.valueOf(actual.getTestOnCreate()),"testOnCreate");
        assertEquals(Boolean.valueOf(expected.getTestOnBorrow()),
                Boolean.valueOf(actual.getTestOnBorrow()),"testOnBorrow");
        assertEquals(Boolean.valueOf(expected.getTestOnReturn()),
                Boolean.valueOf(actual.getTestOnReturn()),"testOnReturn");
        assertEquals(Boolean.valueOf(expected.getTestWhileIdle()),
                Boolean.valueOf(actual.getTestWhileIdle()),"testWhileIdle");
        assertEquals(Boolean.valueOf(expected.getBlockWhenExhausted()),
                Boolean.valueOf(actual.getBlockWhenExhausted()),"whenExhaustedAction");
        assertEquals(expected.getMaxTotal(),actual.getMaxTotal(),"maxTotal");
        assertEquals(expected.getMaxIdle(),actual.getMaxIdle(),"maxIdle");
        assertEquals(expected.getMaxWaitMillis(),actual.getMaxWaitMillis(),"maxWait");
        assertEquals(expected.getMinEvictableIdleTimeMillis(),actual.getMinEvictableIdleTimeMillis(),"minEvictableIdleTimeMillis");
        assertEquals(expected.getNumTestsPerEvictionRun(),actual.getNumTestsPerEvictionRun(),"numTestsPerEvictionRun");
        assertEquals(expected.getEvictorShutdownTimeoutMillis(),actual.getEvictorShutdownTimeoutMillis(),"evictorShutdownTimeoutMillis");
        assertEquals(expected.getTimeBetweenEvictionRunsMillis(),actual.getTimeBetweenEvictionRunsMillis(),"timeBetweenEvictionRunsMillis");
    }

    private void checkEvict(final boolean lifo) throws Exception {
        // yea this is hairy but it tests all the code paths in GOP.evict()
        genericObjectPool.setSoftMinEvictableIdleTimeMillis(10);
        genericObjectPool.setMinIdle(2);
        genericObjectPool.setTestWhileIdle(true);
        genericObjectPool.setLifo(lifo);
        genericObjectPool.addObjects(5);
        genericObjectPool.evict();
        simpleFactory.setEvenValid(false);
        simpleFactory.setOddValid(false);
        simpleFactory.setThrowExceptionOnActivate(true);
        genericObjectPool.evict();
        genericObjectPool.addObjects(5);
        simpleFactory.setThrowExceptionOnActivate(false);
        simpleFactory.setThrowExceptionOnPassivate(true);
        genericObjectPool.evict();
        simpleFactory.setThrowExceptionOnPassivate(false);
        simpleFactory.setEvenValid(true);
        simpleFactory.setOddValid(true);
        Thread.sleep(125);
        genericObjectPool.evict();
        assertEquals(2, genericObjectPool.getNumIdle());
    }

    private void checkEvictionOrder(final boolean lifo) throws Exception {
        checkEvictionOrderPart1(lifo);
        tearDown();
        setUp();
        checkEvictionOrderPart2(lifo);
    }

    private void checkEvictionOrderPart1(final boolean lifo) throws Exception {
        genericObjectPool.setNumTestsPerEvictionRun(2);
        genericObjectPool.setMinEvictableIdleTimeMillis(100);
        genericObjectPool.setLifo(lifo);
        for (int i = 0; i < 5; i++) {
            genericObjectPool.addObject();
            Thread.sleep(100);
        }
        // Order, oldest to youngest, is "0", "1", ...,"4"
        genericObjectPool.evict(); // Should evict "0" and "1"
        final Object obj = genericObjectPool.borrowObject();
        assertTrue(!obj.equals("0"),"oldest not evicted");
        assertTrue(!obj.equals("1"),"second oldest not evicted");
        // 2 should be next out for FIFO, 4 for LIFO
        assertEquals(lifo ? "4" : "2" , obj,"Wrong instance returned");
    }

    private void checkEvictionOrderPart2(final boolean lifo) throws Exception {
        // Two eviction runs in sequence
        genericObjectPool.setNumTestsPerEvictionRun(2);
        genericObjectPool.setMinEvictableIdleTimeMillis(100);
        genericObjectPool.setLifo(lifo);
        for (int i = 0; i < 5; i++) {
            genericObjectPool.addObject();
            Thread.sleep(100);
        }
        genericObjectPool.evict(); // Should evict "0" and "1"
        genericObjectPool.evict(); // Should evict "2" and "3"
        final Object obj = genericObjectPool.borrowObject();
        assertEquals("4", obj,"Wrong instance remaining in pool");
    }

    private void checkEvictorVisiting(final boolean lifo) throws Exception {
        VisitTracker<Object> obj;
        VisitTrackerFactory<Object> trackerFactory = new VisitTrackerFactory<>();
        try (GenericObjectPool<VisitTracker<Object>> trackerPool = new GenericObjectPool<>(trackerFactory)) {
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
            obj = trackerPool.borrowObject();
            trackerPool.returnObject(obj);
            obj = trackerPool.borrowObject();
            trackerPool.returnObject(obj);
            // borrow, return, borrow, return
            // FIFO will move 0 and 1 to end
            // LIFO, 7 out, then in, then out, then in
            trackerPool.evict(); // Should visit 2 and 3 in either case
            for (int i = 0; i < 8; i++) {
                final VisitTracker<Object> tracker = trackerPool.borrowObject();
                if (tracker.getId() >= 4) {
                    assertEquals( 0, tracker.getValidateCount(),"Unexpected instance visited " + tracker.getId());
                } else {
                    assertEquals( 1, tracker.getValidateCount(),
                            "Instance " + tracker.getId() + " visited wrong number of times.");
                }
            }
        }

        trackerFactory = new VisitTrackerFactory<>();
        try (GenericObjectPool<VisitTracker<Object>> trackerPool = new GenericObjectPool<>(trackerFactory)) {
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
            // FIFO 3,4,5,6,7,0,1,2
            // LIFO 7,6,5,4,3,2,1,0
            // In either case, pointer should be at 6
            trackerPool.evict();
            // Should hit 6,7,0 - 0 for second time
            for (int i = 0; i < 8; i++) {
                final VisitTracker<Object> tracker = trackerPool.borrowObject();
                if (tracker.getId() != 0) {
                    assertEquals( 1, tracker.getValidateCount(),
                            "Instance " + tracker.getId() + " visited wrong number of times.");
                } else {
                    assertEquals( 2, tracker.getValidateCount(),
                            "Instance " + tracker.getId() + " visited wrong number of times.");
                }
            }
        }

        // Randomly generate a pools with random numTests
        // and make sure evictor cycles through elements appropriately
        final int[] smallPrimes = { 2, 3, 5, 7 };
        final Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                try (GenericObjectPool<VisitTracker<Object>> trackerPool = new GenericObjectPool<>(trackerFactory)) {
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
                    final int cycleCount = (runs * trackerPool.getNumTestsPerEvictionRun()) / instanceCount;

                    // Look at elements and make sure they are visited cycleCount
                    // or cycleCount + 1 times
                    VisitTracker<Object> tracker = null;
                    int visitCount = 0;
                    for (int k = 0; k < instanceCount; k++) {
                        tracker = trackerPool.borrowObject();
                        assertTrue(trackerPool.getNumActive() <= trackerPool.getMaxTotal());
                        visitCount = tracker.getValidateCount();
                        assertTrue(visitCount >= cycleCount && visitCount <= cycleCount + 1);
                    }
                }
            }
        }
    }

    private BasePooledObjectFactory<String> createDefaultPooledObjectFactory() {
        return new BasePooledObjectFactory<String>() {
            @Override
            public String create() {
                // fake
                return null;
            }

            @Override
            public PooledObject<String> wrap(final String obj) {
                // fake
                return new DefaultPooledObject<>(obj);
            }
        };
    }

    private BasePooledObjectFactory<String> createNullPooledObjectFactory() {
        return new BasePooledObjectFactory<String>() {
            @Override
            public String create() {
                // fake
                return null;
            }

            @Override
            public PooledObject<String> wrap(final String obj) {
                // fake
                return null;
            }
        };
    }

    private BasePooledObjectFactory<String> createSlowObjectFactory(final long elapsedTimeMillis) {
        return new BasePooledObjectFactory<String>() {
            @Override
            public String create() throws Exception {
                Thread.sleep(elapsedTimeMillis);
                return "created";
            }

            @Override
            public PooledObject<String> wrap(final String obj) {
                // fake
                return new DefaultPooledObject<>(obj);
            }
        };
    }

    @Override
    protected Object getNthObject(final int n) {
        return String.valueOf(n);
    }

    @Override
    protected boolean isFifo() {
        return false;
    }

    @Override
    protected boolean isLifo() {
        return true;
    }

    @Override
    protected ObjectPool<String> makeEmptyPool(final int minCap) {
       final GenericObjectPool<String> mtPool =
               new GenericObjectPool<>(new SimpleFactory());
       mtPool.setMaxTotal(minCap);
       mtPool.setMaxIdle(minCap);
       return mtPool;
    }

    @Override
    protected ObjectPool<Object> makeEmptyPool(
            final PooledObjectFactory<Object> fac) {
        return new GenericObjectPool<>(fac);
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
                fail("Thread " + i + " failed: " + threads[i]._error.toString());
            }
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        simpleFactory = new SimpleFactory();
        genericObjectPool = new GenericObjectPool<>(simpleFactory);
    }

    @AfterEach
    public void tearDown() throws Exception {
        final String poolName = genericObjectPool.getJmxName().toString();
        genericObjectPool.clear();
        genericObjectPool.close();
        genericObjectPool = null;
        simpleFactory = null;

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
        assertEquals( 0, registeredPoolCount,msg.toString());

        // Make sure that EvictionTimer executor is shut down
        final Field evictorExecutorField = EvictionTimer.class.getDeclaredField("executor");
        evictorExecutorField.setAccessible(true);
        assertNull(evictorExecutorField.get(null));
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testAddObject() throws Exception {
        assertEquals( 0, genericObjectPool.getNumIdle(),"should be zero idle");
        genericObjectPool.addObject();
        assertEquals( 1, genericObjectPool.getNumIdle(),"should be one idle");
        assertEquals( 0, genericObjectPool.getNumActive(),"should be zero active");
        final String obj = genericObjectPool.borrowObject();
        assertEquals( 0, genericObjectPool.getNumIdle(),"should be zero idle");
        assertEquals( 1, genericObjectPool.getNumActive(),"should be one active");
        genericObjectPool.returnObject(obj);
        assertEquals( 1, genericObjectPool.getNumIdle(),"should be one idle");
        assertEquals( 0, genericObjectPool.getNumActive(),"should be zero active");
    }


    /*
     * Note: This test relies on timing for correct execution. There *should* be
     * enough margin for this to work correctly on most (all?) systems but be
     * aware of this if you see a failure of this test.
     */
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testBorrowObjectFairness() throws Exception {

        final int numThreads = 40;
        final int maxTotal = 40;

        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxTotal);
        config.setFairness(true);
        config.setLifo(false);

        genericObjectPool = new GenericObjectPool(simpleFactory, config);

        // Exhaust the pool
        final String[] objects = new String[maxTotal];
        for (int i = 0; i < maxTotal; i++) {
            objects[i] = genericObjectPool.borrowObject();
        }

        // Start and park threads waiting to borrow objects
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            threads[i] = new TestThread(genericObjectPool, 1, 0, 2000, false, String.valueOf(i % maxTotal));
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
            genericObjectPool.returnObject(objects[i]);
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
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testBrokenFactoryShouldNotBlockPool() {
        final int maxTotal = 1;

        simpleFactory.setMaxTotal(maxTotal);
        genericObjectPool.setMaxTotal(maxTotal);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setTestOnBorrow(true);

        // First borrow object will need to create a new object which will fail
        // validation.
        String obj = null;
        Exception ex = null;
        simpleFactory.setValid(false);
        try {
            obj = genericObjectPool.borrowObject();
        } catch (final Exception e) {
            ex = e;
        }
        // Failure expected
        assertNotNull(ex);
        assertTrue(ex instanceof NoSuchElementException);
        assertNull(obj);

        // Configure factory to create valid objects so subsequent borrows work
        simpleFactory.setValid(true);

        // Subsequent borrows should be OK
        try {
            obj = genericObjectPool.borrowObject();
        } catch (final Exception e1) {
            fail();
        }
        assertNotNull(obj);
        try {
            genericObjectPool.returnObject(obj);
        } catch (final Exception e) {
            fail();
        }
    }

    // POOL-259
    @Test
    public void testClientWaitStats() throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        // Give makeObject a little latency
        factory.setMakeLatency(200);
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(factory, new GenericObjectPoolConfig<String>())) {
            final String s = pool.borrowObject();
            // First borrow waits on create, so wait time should be at least 200 ms
            // Allow 100ms error in clock times
            assertTrue(pool.getMaxBorrowWaitTimeMillis() >= 100);
            assertTrue(pool.getMeanBorrowWaitTimeMillis() >= 100);
            pool.returnObject(s);
            pool.borrowObject();
            // Second borrow does not have to wait on create, average should be about 100
            assertTrue(pool.getMaxBorrowWaitTimeMillis() > 100);
            assertTrue(pool.getMeanBorrowWaitTimeMillis() < 200);
            assertTrue(pool.getMeanBorrowWaitTimeMillis() > 20);
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testCloseMultiplePools1() throws Exception {
        try (final GenericObjectPool<String> genericObjectPool2 = new GenericObjectPool<>(simpleFactory)) {
            genericObjectPool.setTimeBetweenEvictionRunsMillis(1);
            genericObjectPool2.setTimeBetweenEvictionRunsMillis(1);
        }
        genericObjectPool.close();
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testCloseMultiplePools2() throws Exception {
        try (final GenericObjectPool<String> genericObjectPool2 = new GenericObjectPool<>(simpleFactory)) {
            // Ensure eviction takes a long time, during which time EvictionTimer.executor's queue is empty
            simpleFactory.setDestroyLatency(1000L);
            // Ensure there is an object to evict, so that above latency takes effect
            genericObjectPool.setTimeBetweenEvictionRunsMillis(1);
            genericObjectPool2.setTimeBetweenEvictionRunsMillis(1);
            genericObjectPool.setMinEvictableIdleTimeMillis(1);
            genericObjectPool2.setMinEvictableIdleTimeMillis(1);
            genericObjectPool.addObject();
            genericObjectPool2.addObject();
            // Close both pools
        }
        genericObjectPool.close();
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testConcurrentBorrowAndEvict() throws Exception {

        genericObjectPool.setMaxTotal(1);
        genericObjectPool.addObject();

        for (int i = 0; i < 5000; i++) {
            final ConcurrentBorrowAndEvictThread one =
                    new ConcurrentBorrowAndEvictThread(true);
            final ConcurrentBorrowAndEvictThread two =
                    new ConcurrentBorrowAndEvictThread(false);

            one.start();
            two.start();
            one.join();
            two.join();

            genericObjectPool.returnObject(one.obj);

            /* Uncomment this for a progress indication
            if (i % 10 == 0) {
                System.out.println(i/10);
            }
            */
        }
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
        genericObjectPool.setMaxTotal(nObjects);
        genericObjectPool.setMaxIdle(nObjects);
        final String[] obj = new String[nObjects];
        for (int i = 0; i < nObjects; i++) {
            obj[i] = genericObjectPool.borrowObject();
        }
        for (int i = 0; i < nObjects; i++) {
            if (i % 2 == 0) {
                genericObjectPool.returnObject(obj[i]);
            }
        }
        final int nThreads = 20;
        final int nIterations = 60;
        final InvalidateThread[] threads = new InvalidateThread[nThreads];
        // Randomly generated list of distinct invalidation targets
        final ArrayList<Integer> targets = new ArrayList<>();
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
                threads[i] = new InvalidateThread(genericObjectPool, obj[targ.intValue()]);
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
        assertEquals(nIterations, genericObjectPool.getDestroyedCount());
    }

    @Test
    public void testConstructorNullFactory() {
        // add dummy assert (won't be invoked because of IAE) to avoid "unused" warning
        assertThrows(IllegalArgumentException.class,
                () -> new GenericObjectPool<>(null));
    }


    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
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
        try (GenericObjectPool<Object> dummyPool = new GenericObjectPool<>(dummyFactory)) {
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
            assertEquals(Boolean.valueOf(BaseObjectPoolConfig.DEFAULT_LIFO), Boolean.valueOf(dummyPool.getLifo()));
        }

        final GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
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
        try (GenericObjectPool<Object> dummyPool = new GenericObjectPool<>(dummyFactory, config)) {
            assertEquals(maxIdle, dummyPool.getMaxIdle());
            assertEquals(maxWait, dummyPool.getMaxWaitMillis());
            assertEquals(minIdle, dummyPool.getMinIdle());
            assertEquals(maxTotal, dummyPool.getMaxTotal());
            assertEquals(minEvictableIdleTimeMillis, dummyPool.getMinEvictableIdleTimeMillis());
            assertEquals(numTestsPerEvictionRun, dummyPool.getNumTestsPerEvictionRun());
            assertEquals(Boolean.valueOf(testOnBorrow), Boolean.valueOf(dummyPool.getTestOnBorrow()));
            assertEquals(Boolean.valueOf(testOnReturn), Boolean.valueOf(dummyPool.getTestOnReturn()));
            assertEquals(Boolean.valueOf(testWhileIdle), Boolean.valueOf(dummyPool.getTestWhileIdle()));
            assertEquals(timeBetweenEvictionRunsMillis, dummyPool.getTimeBetweenEvictionRunsMillis());
            assertEquals(Boolean.valueOf(blockWhenExhausted), Boolean.valueOf(dummyPool.getBlockWhenExhausted()));
            assertEquals(Boolean.valueOf(lifo), Boolean.valueOf(dummyPool.getLifo()));
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testDefaultConfiguration() throws Exception {
        assertConfiguration(new GenericObjectPoolConfig<>(),genericObjectPool);
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
        try (final GenericObjectPool<HashSet<String>> pool = new GenericObjectPool<>(factory,
                new GenericObjectPoolConfig<HashSet<String>>())) {
            final HashSet<String> s1 = pool.borrowObject();
            final HashSet<String> s2 = pool.borrowObject();
            pool.returnObject(s1);
            pool.returnObject(s2);
        }
    }

    @Test
    public void testErrorFactoryDoesNotBlockThreads() throws Exception {

        final CreateErrorFactory factory = new CreateErrorFactory();
        try (final GenericObjectPool<String> createFailFactoryPool = new GenericObjectPool<>(factory)) {

            createFailFactoryPool.setMaxTotal(1);

            // Try and borrow the first object from the pool
            final WaitingTestThread thread1 = new WaitingTestThread(createFailFactoryPool, 0);
            thread1.start();

            // Wait for thread to reach semaphore
            while (!factory.hasQueuedThreads()) {
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
            assertFalse(thread1.isAlive());
            assertFalse(thread2.isAlive());

            assertTrue(thread1._thrown instanceof UnknownError);
            assertTrue(thread2._thrown instanceof UnknownError);
        }
    }

    /**
     * Tests addObject contention between ensureMinIdle triggered by
     * the Evictor with minIdle &gt; 0 and borrowObject.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictAddObjects() throws Exception {
        simpleFactory.setMakeLatency(300);
        simpleFactory.setMaxTotal(2);
        genericObjectPool.setMaxTotal(2);
        genericObjectPool.setMinIdle(1);
        genericObjectPool.borrowObject(); // numActive = 1, numIdle = 0
        // Create a test thread that will run once and try a borrow after
        // 150ms fixed delay
        final TestThread<String> borrower = new TestThread<>(genericObjectPool, 1, 150, false);
        final Thread borrowerThread = new Thread(borrower);
        // Set evictor to run in 100 ms - will create idle instance
        genericObjectPool.setTimeBetweenEvictionRunsMillis(100);
        borrowerThread.start();  // Off to the races
        borrowerThread.join();
        assertTrue(!borrower.failed());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictFIFO() throws Exception {
        checkEvict(false);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEviction() throws Exception {
        genericObjectPool.setMaxIdle(500);
        genericObjectPool.setMaxTotal(500);
        genericObjectPool.setNumTestsPerEvictionRun(100);
        genericObjectPool.setMinEvictableIdleTimeMillis(250L);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(500L);
        genericObjectPool.setTestWhileIdle(true);

        final String[] active = new String[500];
        for (int i = 0; i < 500; i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        for (int i = 0; i < 500; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 500,"Should be less than 500 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 400,"Should be less than 400 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 300,"Should be less than 300 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 200,"Should be less than 200 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 100,"Should be less than 100 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertEquals(0,genericObjectPool.getNumIdle(),"Should be zero idle, found " + genericObjectPool.getNumIdle());

        for (int i = 0; i < 500; i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        for (int i = 0; i < 500; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 500,"Should be less than 500 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 400,"Should be less than 400 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 300,"Should be less than 300 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 200,"Should be less than 200 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() < 100,"Should be less than 100 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(600L); } catch(final InterruptedException e) { }
        assertEquals(0,genericObjectPool.getNumIdle(),"Should be zero idle, found " + genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictionInvalid() throws Exception {

        try (final GenericObjectPool<Object> invalidFactoryPool = new GenericObjectPool<>(new InvalidFactory())) {

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
            final Thread t = new EvictionThread<>(invalidFactoryPool);
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
            assertEquals( 0, invalidFactoryPool.getNumIdle(),"Idle count different than expected.");
            assertEquals( 0, invalidFactoryPool.getNumActive(),"Total count different than expected.");
        }
    }

    /**
     * Test to make sure evictor visits least recently used objects first,
     * regardless of FIFO/LIFO.
     *
     * JIRA: POOL-86
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictionOrder() throws Exception {
        checkEvictionOrder(false);
        tearDown();
        setUp();
        checkEvictionOrder(true);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictionPolicy() throws Exception {
        genericObjectPool.setMaxIdle(500);
        genericObjectPool.setMaxTotal(500);
        genericObjectPool.setNumTestsPerEvictionRun(500);
        genericObjectPool.setMinEvictableIdleTimeMillis(250L);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(500L);
        genericObjectPool.setTestWhileIdle(true);

        // ClassNotFoundException
        try {
            genericObjectPool.setEvictionPolicyClassName(Long.toString(System.currentTimeMillis()));
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // InstantiationException
        try {
            genericObjectPool.setEvictionPolicyClassName(java.io.Serializable.class.getName());
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // IllegalAccessException
        try {
            genericObjectPool.setEvictionPolicyClassName(java.util.Collections.class.getName());
            fail("setEvictionPolicyClassName must throw an error if the class name is invalid.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            genericObjectPool.setEvictionPolicyClassName(java.lang.String.class.getName());
            fail("setEvictionPolicyClassName must throw an error if a class that does not "
                    + "implement EvictionPolicy is specified.");
        } catch (final IllegalArgumentException e) {
            // expected
        }

        genericObjectPool.setEvictionPolicy(new TestEvictionPolicy<String>());
        assertEquals(TestEvictionPolicy.class.getName(), genericObjectPool.getEvictionPolicyClassName());

        genericObjectPool.setEvictionPolicyClassName(TestEvictionPolicy.class.getName());
        assertEquals(TestEvictionPolicy.class.getName(), genericObjectPool.getEvictionPolicyClassName());

        final String[] active = new String[500];
        for (int i = 0; i < 500; i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        for (int i = 0; i < 500; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        // Eviction policy ignores first 1500 attempts to evict and then always
        // evicts. After 1s, there should have been two runs of 500 tests so no
        // evictions
        try { Thread.sleep(1000L); } catch(final InterruptedException e) { }
        assertEquals( 500, genericObjectPool.getNumIdle(),"Should be 500 idle");
        // A further 1s wasn't enough so allow 2s for the evictor to clear out
        // all of the idle objects.
        try { Thread.sleep(2000L); } catch(final InterruptedException e) { }
        assertEquals( 0, genericObjectPool.getNumIdle(),"Should be 0 idle");
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictionSoftMinIdle() throws Exception {
        class TimeTest extends BasePooledObjectFactory<TimeTest> {
            private final long createTimeMillis;

            public TimeTest() {
                createTimeMillis = System.currentTimeMillis();
            }

            @Override
            public TimeTest create() throws Exception {
                return new TimeTest();
            }

            public long getCreateTimeMillis() {
                return createTimeMillis;
            }

            @Override
            public PooledObject<TimeTest> wrap(final TimeTest value) {
                return new DefaultPooledObject<>(value);
            }
        }

        try (final GenericObjectPool<TimeTest> timePool = new GenericObjectPool<>(new TimeTest())) {

            timePool.setMaxIdle(5);
            timePool.setMaxTotal(5);
            timePool.setNumTestsPerEvictionRun(5);
            timePool.setMinEvictableIdleTimeMillis(3000L);
            timePool.setSoftMinEvictableIdleTimeMillis(1000L);
            timePool.setMinIdle(2);

            final TimeTest[] active = new TimeTest[5];
            final Long[] creationTime = new Long[5];
            for (int i = 0; i < 5; i++) {
                active[i] = timePool.borrowObject();
                creationTime[i] = Long.valueOf((active[i]).getCreateTimeMillis());
            }

            for (int i = 0; i < 5; i++) {
                timePool.returnObject(active[i]);
            }

            // Soft evict all but minIdle(2)
            Thread.sleep(1500L);
            timePool.evict();
            assertEquals( 2, timePool.getNumIdle(),"Idle count different than expected.");

            // Hard evict the rest.
            Thread.sleep(2000L);
            timePool.evict();
            assertEquals( 0, timePool.getNumIdle(),"Idle count different than expected.");
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictionWithNegativeNumTests() throws Exception {
        // when numTestsPerEvictionRun is negative, it represents a fraction of the idle objects to test
        genericObjectPool.setMaxIdle(6);
        genericObjectPool.setMaxTotal(6);
        genericObjectPool.setNumTestsPerEvictionRun(-2);
        genericObjectPool.setMinEvictableIdleTimeMillis(50L);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(100L);

        final String[] active = new String[6];
        for (int i = 0; i < 6; i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        for (int i = 0; i < 6; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() <= 6,"Should at most 6 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() <= 3,"Should at most 3 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() <= 2,"Should be at most 2 idle, found " + genericObjectPool.getNumIdle());
        try { Thread.sleep(100L); } catch(final InterruptedException e) { }
        assertEquals(0,genericObjectPool.getNumIdle(),"Should be zero idle, found " + genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictLIFO() throws Exception {
        checkEvict(true);
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

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testEvictWhileEmpty() throws Exception {
        genericObjectPool.evict();
        genericObjectPool.evict();
        genericObjectPool.close();
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testExceptionOnActivateDuringBorrow() throws Exception {
        final String obj1 = genericObjectPool.borrowObject();
        final String obj2 = genericObjectPool.borrowObject();
        genericObjectPool.returnObject(obj1);
        genericObjectPool.returnObject(obj2);
        simpleFactory.setThrowExceptionOnActivate(true);
        simpleFactory.setEvenValid(false);
        // Activation will now throw every other time
        // First attempt throws, but loop continues and second succeeds
        final String obj = genericObjectPool.borrowObject();
        assertEquals(1, genericObjectPool.getNumActive());
        assertEquals(0, genericObjectPool.getNumIdle());

        genericObjectPool.returnObject(obj);
        simpleFactory.setValid(false);
        // Validation will now fail on activation when borrowObject returns
        // an idle instance, and then when attempting to create a new instance
        try {
            genericObjectPool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        assertEquals(0, genericObjectPool.getNumActive());
        assertEquals(0, genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testExceptionOnDestroyDuringBorrow() throws Exception {
        simpleFactory.setThrowExceptionOnDestroy(true);
        genericObjectPool.setTestOnBorrow(true);
        genericObjectPool.borrowObject();
        simpleFactory.setValid(false); // Make validation fail on next borrow attempt
        try {
            genericObjectPool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        assertEquals(1, genericObjectPool.getNumActive());
        assertEquals(0, genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testExceptionOnDestroyDuringReturn() throws Exception {
        simpleFactory.setThrowExceptionOnDestroy(true);
        genericObjectPool.setTestOnReturn(true);
        final String obj1 = genericObjectPool.borrowObject();
        genericObjectPool.borrowObject();
        simpleFactory.setValid(false); // Make validation fail
        genericObjectPool.returnObject(obj1);
        assertEquals(1, genericObjectPool.getNumActive());
        assertEquals(0, genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testExceptionOnPassivateDuringReturn() throws Exception {
        final String obj = genericObjectPool.borrowObject();
        simpleFactory.setThrowExceptionOnPassivate(true);
        genericObjectPool.returnObject(obj);
        assertEquals(0,genericObjectPool.getNumIdle());
    }

    @Test
    public void testFailingFactoryDoesNotBlockThreads() throws Exception {

        final CreateFailFactory factory = new CreateFailFactory();
        try (final GenericObjectPool<String> createFailFactoryPool = new GenericObjectPool<>(factory)) {

            createFailFactoryPool.setMaxTotal(1);

            // Try and borrow the first object from the pool
            final WaitingTestThread thread1 = new WaitingTestThread(createFailFactoryPool, 0);
            thread1.start();

            // Wait for thread to reach semaphore
            while (!factory.hasQueuedThreads()) {
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
            assertFalse(thread1.isAlive());
            assertFalse(thread2.isAlive());

            assertTrue(thread1._thrown instanceof UnsupportedCharsetException);
            assertTrue(thread2._thrown instanceof UnsupportedCharsetException);
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testFIFO() throws Exception {
        genericObjectPool.setLifo(false);
        genericObjectPool.addObject(); // "0"
        genericObjectPool.addObject(); // "1"
        genericObjectPool.addObject(); // "2"
        assertEquals( "0", genericObjectPool.borrowObject(),"Oldest");
        assertEquals( "1", genericObjectPool.borrowObject(),"Middle");
        assertEquals( "2", genericObjectPool.borrowObject(),"Youngest");
        final String o = genericObjectPool.borrowObject();
        assertEquals( "3", o,"new-3");
        genericObjectPool.returnObject(o);
        assertEquals( o, genericObjectPool.borrowObject(),"returned-3");
        assertEquals( "4", genericObjectPool.borrowObject(),"new-4");
    }

    @Test
    public void testGetFactoryType_DefaultPooledObjectFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(createDefaultPooledObjectFactory())) {
            assertNotNull((pool.getFactoryType()));
        }
    }

    @Test
    public void testGetFactoryType_NullPooledObjectFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(createNullPooledObjectFactory())) {
            assertNotNull((pool.getFactoryType()));
        }
    }

    @Test
    public void testGetFactoryType_PoolUtilsSynchronizedDefaultPooledFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(
                PoolUtils.synchronizedPooledFactory(createDefaultPooledObjectFactory()))) {
            assertNotNull((pool.getFactoryType()));
        }
    }

    @Test
    public void testGetFactoryType_PoolUtilsSynchronizedNullPooledFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(
                PoolUtils.synchronizedPooledFactory(createNullPooledObjectFactory()))) {
            assertNotNull((pool.getFactoryType()));
        }
    }

    @Test
    public void testGetFactoryType_SynchronizedDefaultPooledObjectFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(
                new TestSynchronizedPooledObjectFactory<>(createDefaultPooledObjectFactory()))) {
            assertNotNull((pool.getFactoryType()));
        }
    }

    @Test
    public void testGetFactoryType_SynchronizedNullPooledObjectFactory() {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(
                new TestSynchronizedPooledObjectFactory<>(createNullPooledObjectFactory()))) {
            assertNotNull((pool.getFactoryType()));
        }
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
    public void testInvalidateFreesCapacity() throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(factory)) {
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
    }

    /**
     * Ensure the pool is registered.
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testJmxRegistration() {
        final ObjectName oname = genericObjectPool.getJmxName();
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final Set<ObjectName> result = mbs.queryNames(oname, null);
        assertEquals(1, result.size());
        genericObjectPool.jmxUnregister();

        final GenericObjectPoolConfig<String> config = new GenericObjectPoolConfig<>();
        config.setJmxEnabled(false);
        try (final GenericObjectPool<String> poolWithoutJmx = new GenericObjectPool<>(simpleFactory, config)) {
            assertNull(poolWithoutJmx.getJmxName());
            config.setJmxEnabled(true);
            poolWithoutJmx.jmxUnregister();
        }

        config.setJmxNameBase(null);
        try (final GenericObjectPool<String> poolWithDefaultJmxNameBase = new GenericObjectPool<>(simpleFactory, config)) {
            assertNotNull(poolWithDefaultJmxNameBase.getJmxName());
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testLIFO() throws Exception {
        final String o;
        genericObjectPool.setLifo(true);
        genericObjectPool.addObject(); // "0"
        genericObjectPool.addObject(); // "1"
        genericObjectPool.addObject(); // "2"
        assertEquals( "2", genericObjectPool.borrowObject(),"Youngest");
        assertEquals( "1", genericObjectPool.borrowObject(),"Middle");
        assertEquals( "0", genericObjectPool.borrowObject(),"Oldest");
        o = genericObjectPool.borrowObject();
        assertEquals( "3", o,"new-3");
        genericObjectPool.returnObject(o);
        assertEquals( o, genericObjectPool.borrowObject(),"returned-3");
        assertEquals( "4", genericObjectPool.borrowObject(),"new-4");
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
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMakeConcurrentWithReturn() throws Exception {
        genericObjectPool.setTestOnBorrow(true);
        simpleFactory.setValid(true);
        // Borrow and return an instance, with a short wait
        final WaitingTestThread thread1 = new WaitingTestThread(genericObjectPool, 200);
        thread1.start();
        Thread.sleep(50); // wait for validation to succeed
        // Slow down validation and borrow an instance
        simpleFactory.setValidateLatency(400);
        final String instance = genericObjectPool.borrowObject();
        // Now make sure that we have not leaked an instance
        assertEquals(simpleFactory.getMakeCounter(), genericObjectPool.getNumIdle() + 1);
        genericObjectPool.returnObject(instance);
        assertEquals(simpleFactory.getMakeCounter(), genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxIdle() throws Exception {
        genericObjectPool.setMaxTotal(100);
        genericObjectPool.setMaxIdle(8);
        final String[] active = new String[100];
        for(int i=0;i<100;i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        assertEquals(100,genericObjectPool.getNumActive());
        assertEquals(0,genericObjectPool.getNumIdle());
        for(int i=0;i<100;i++) {
            genericObjectPool.returnObject(active[i]);
            assertEquals(99 - i,genericObjectPool.getNumActive());
            assertEquals((i < 8 ? i+1 : 8),genericObjectPool.getNumIdle());
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxIdleZero() throws Exception {
        genericObjectPool.setMaxTotal(100);
        genericObjectPool.setMaxIdle(0);
        final String[] active = new String[100];
        for(int i=0;i<100;i++) {
            active[i] = genericObjectPool.borrowObject();
        }
        assertEquals(100,genericObjectPool.getNumActive());
        assertEquals(0,genericObjectPool.getNumIdle());
        for(int i=0;i<100;i++) {
            genericObjectPool.returnObject(active[i]);
            assertEquals(99 - i,genericObjectPool.getNumActive());
            assertEquals(0, genericObjectPool.getNumIdle());
        }
    }

    /**
     * Showcasing a possible deadlock situation as reported in POOL-356
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    @SuppressWarnings("rawtypes")
    public void testMaxIdleZeroUnderLoad() {
        // Config
        final int numThreads = 199; // And main thread makes a round 200.
        final int numIter = 20;
        final int delay = 25;
        final int maxTotal = 10;

        simpleFactory.setMaxTotal(maxTotal);
        genericObjectPool.setMaxTotal(maxTotal);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(-1);

        // this is important to trigger POOL-356
        genericObjectPool.setMaxIdle(0);

        // Start threads to borrow objects
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            // Factor of 2 on iterations so main thread does work whilst other
            // threads are running. Factor of 2 on delay so average delay for
            // other threads == actual delay for main thread
            threads[i] = new TestThread<>(genericObjectPool, numIter * 2, delay * 2);
            final Thread t = new Thread(threads[i]);
            t.start();
        }
        // Give the threads a chance to start doing some work
        try {
            Thread.sleep(100);
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
                obj = genericObjectPool.borrowObject();
                // Under load, observed _numActive > _maxTotal
                if (genericObjectPool.getNumActive() > genericObjectPool.getMaxTotal()) {
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
                        genericObjectPool.returnObject(obj);
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
                threads[i]._error.printStackTrace();
                fail("Thread "+i+" failed: "+threads[i]._error.toString());
            }
        }
    }

    /**
     * POOL-376
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testNoInvalidateNPE() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setTestOnCreate(true);
        genericObjectPool.setMaxWaitMillis(-1);
        final String obj = genericObjectPool.borrowObject();
        // Make validation fail - this will cause create() to return null
        simpleFactory.setValid(false);
        // Create a take waiter
        final WaitingTestThread wtt = new WaitingTestThread(genericObjectPool, 200);
        wtt.start();
        // Give wtt time to start
        Thread.sleep(200);
        genericObjectPool.invalidateObject(obj);
        // Now allow create to succeed so waiter can be served
        simpleFactory.setValid(true);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxTotal() throws Exception {
        genericObjectPool.setMaxTotal(3);
        genericObjectPool.setBlockWhenExhausted(false);

        genericObjectPool.borrowObject();
        genericObjectPool.borrowObject();
        genericObjectPool.borrowObject();
        try {
            genericObjectPool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
    }

    /**
     * Verifies that maxTotal is not exceeded when factory destroyObject
     * has high latency, testOnReturn is set and there is high incidence of
     * validation failures.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxTotalInvariant() throws Exception {
        final int maxTotal = 15;
        simpleFactory.setEvenValid(false);     // Every other validation fails
        simpleFactory.setDestroyLatency(100);  // Destroy takes 100 ms
        simpleFactory.setMaxTotal(maxTotal); // (makes - destroys) bound
        simpleFactory.setValidationEnabled(true);
        genericObjectPool.setMaxTotal(maxTotal);
        genericObjectPool.setMaxIdle(-1);
        genericObjectPool.setTestOnReturn(true);
        genericObjectPool.setMaxWaitMillis(1000L);
        runTestThreads(5, 10, 50, genericObjectPool);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    @SuppressWarnings("rawtypes")
    public void testMaxTotalUnderLoad() {
        // Config
        final int numThreads = 199; // And main thread makes a round 200.
        final int numIter = 20;
        final int delay = 25;
        final int maxTotal = 10;

        simpleFactory.setMaxTotal(maxTotal);
        genericObjectPool.setMaxTotal(maxTotal);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(-1);

        // Start threads to borrow objects
        final TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            // Factor of 2 on iterations so main thread does work whilst other
            // threads are running. Factor of 2 on delay so average delay for
            // other threads == actual delay for main thread
            threads[i] = new TestThread<>(genericObjectPool, numIter * 2, delay * 2);
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
                obj = genericObjectPool.borrowObject();
                // Under load, observed _numActive > _maxTotal
                if (genericObjectPool.getNumActive() > genericObjectPool.getMaxTotal()) {
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
                        genericObjectPool.returnObject(obj);
                    } catch (final Exception e) {
                        // Ignore
                    }
                }
            }
        }

        for (int i = 0; i < numThreads; i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(final InterruptedException e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail("Thread " + i + " failed: " + threads[i]._error.toString());
            }
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxTotalZero() throws Exception {
        genericObjectPool.setMaxTotal(0);
        genericObjectPool.setBlockWhenExhausted(false);

        try {
            genericObjectPool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
    }

    /*
     * Test multi-threaded pool access.
     * Multiple threads, but maxTotal only allows half the threads to succeed.
     *
     * This test was prompted by Continuum build failures in the Commons DBCP test case:
     * TestPerUserPoolDataSource.testMultipleThreads2()
     * Let's see if the this fails on Continuum too!
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMaxWaitMultiThreaded() throws Exception {
        final long maxWait = 500; // wait for connection
        final long holdTime = 2 * maxWait; // how long to hold connection
        final int threads = 10; // number of threads to grab the object initially
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setMaxWaitMillis(maxWait);
        genericObjectPool.setMaxTotal(threads);
        // Create enough threads so half the threads will have to wait
        final WaitingTestThread wtt[] = new WaitingTestThread[threads * 2];
        for (int i = 0; i < wtt.length; i++) {
            wtt[i] = new WaitingTestThread(genericObjectPool,holdTime);
        }
        final long originMillis = System.currentTimeMillis() - 1000;
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
                        "PreBorrow: " + (wt.preBorrowMillis - originMillis) +
                        " PostBorrow: " + (wt.postBorrowMillis != 0 ? wt.postBorrowMillis - originMillis : -1) +
                        " BorrowTime: " + (wt.postBorrowMillis != 0 ? wt.postBorrowMillis - wt.preBorrowMillis : -1) +
                        " PostReturn: " + (wt.postReturnMillis != 0 ? wt.postReturnMillis - originMillis : -1) +
                        " Ended: " + (wt.endedMillis - originMillis) +
                        " ObjId: " + wt.objectId
                        );
            }
        }
        assertEquals(wtt.length / 2, failed,"Expected half the threads to fail");
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMinIdle() throws Exception {
        genericObjectPool.setMaxIdle(500);
        genericObjectPool.setMinIdle(5);
        genericObjectPool.setMaxTotal(10);
        genericObjectPool.setNumTestsPerEvictionRun(0);
        genericObjectPool.setMinEvictableIdleTimeMillis(50L);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(100L);
        genericObjectPool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        final String[] active = new String[5];
        active[0] = genericObjectPool.borrowObject();

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        for(int i=1 ; i<5 ; i++) {
            active[i] = genericObjectPool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        for(int i=0 ; i<5 ; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 10,"Should be 10 idle, found " + genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testMinIdleMaxTotal() throws Exception {
        genericObjectPool.setMaxIdle(500);
        genericObjectPool.setMinIdle(5);
        genericObjectPool.setMaxTotal(10);
        genericObjectPool.setNumTestsPerEvictionRun(0);
        genericObjectPool.setMinEvictableIdleTimeMillis(50L);
        genericObjectPool.setTimeBetweenEvictionRunsMillis(100L);
        genericObjectPool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        final String[] active = new String[10];

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        for (int i = 0; i < 5; i++) {
            active[i] = genericObjectPool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 5,"Should be 5 idle, found " + genericObjectPool.getNumIdle());

        for(int i = 0 ; i < 5 ; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 10,"Should be 10 idle, found " + genericObjectPool.getNumIdle());

        for (int i = 0; i < 10; i++) {
            active[i] = genericObjectPool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 0,"Should be 0 idle, found " + genericObjectPool.getNumIdle());

        for (int i = 0; i < 10; i++) {
            genericObjectPool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(final InterruptedException e) { }
        assertTrue(genericObjectPool.getNumIdle() == 10,"Should be 10 idle, found " + genericObjectPool.getNumIdle());
    }

    /**
     * Verifies that returning an object twice (without borrow in between) causes ISE
     * but does not re-validate or re-passivate the instance.
     *
     * JIRA: POOL-285
     */
    @Test
    public void testMultipleReturn() throws Exception {
        final WaiterFactory<String> factory = new WaiterFactory<>(0, 0, 0, 0, 0, 0);
        try (final GenericObjectPool<Waiter> pool = new GenericObjectPool<>(factory)) {
            pool.setTestOnReturn(true);
            final Waiter waiter = pool.borrowObject();
            pool.returnObject(waiter);
            assertEquals(1, waiter.getValidationCount());
            assertEquals(1, waiter.getPassivationCount());
            try {
                pool.returnObject(waiter);
                fail("Expecting IllegalStateException from multiple return");
            } catch (final IllegalStateException ex) {
                // Exception is expected, now check no repeat validation/passivation
                assertEquals(1, waiter.getValidationCount());
                assertEquals(1, waiter.getPassivationCount());
            }
        }
    }

    // POOL-248
    @Test
    public void testMultipleReturnOfSameObject() throws Exception {
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(simpleFactory, new GenericObjectPoolConfig<String>())) {

            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle());

            final String obj = pool.borrowObject();

            assertEquals(1, pool.getNumActive());
            assertEquals(0, pool.getNumIdle());

            pool.returnObject(obj);

            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle());

            assertThrows(IllegalStateException.class,
                    () -> pool.returnObject(obj));

            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle());
        }
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
        try (final GenericObjectPool<HashSet<String>> pool = new GenericObjectPool<>(factory,
                new GenericObjectPoolConfig<HashSet<String>>())) {
            final HashSet<String> s1 = pool.borrowObject();
            final HashSet<String> s2 = pool.borrowObject();
            s1.add("One");
            s2.add("One");
            pool.returnObject(s1);
            pool.returnObject(s2);
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testNegativeMaxTotal() throws Exception {
        genericObjectPool.setMaxTotal(-1);
        genericObjectPool.setBlockWhenExhausted(false);
        final String obj = genericObjectPool.borrowObject();
        assertEquals(getNthObject(0),obj);
        genericObjectPool.returnObject(obj);
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
        try (final GenericObjectPool<AtomicInteger> pool = new GenericObjectPool<>(factory)) {
            pool.setMaxTotal(maxTotal);
            pool.setMaxIdle(maxTotal);
            pool.setTestOnBorrow(true);
            pool.setBlockWhenExhausted(true);
            pool.setMaxWaitMillis(-1);
            runTestThreads(numThreads, iterations, delay, pool);
            assertEquals(0, pool.getDestroyedByBorrowValidationCount());
        }
    }

    public void testPreparePool() throws Exception {
        genericObjectPool.setMinIdle(1);
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.preparePool();
        assertEquals(1, genericObjectPool.getNumIdle());
        final String obj = genericObjectPool.borrowObject();
        genericObjectPool.preparePool();
        assertEquals(0, genericObjectPool.getNumIdle());
        genericObjectPool.setMinIdle(0);
        genericObjectPool.returnObject(obj);
        genericObjectPool.preparePool();
        assertEquals(0, genericObjectPool.getNumIdle());
    }

    @Test/* maxWaitMillis x2 + padding */
    @Timeout(value = 1200, unit = TimeUnit.MILLISECONDS)
    public void testReturnBorrowObjectWithingMaxWaitMillis() throws Exception {
        final long maxWaitMillis = 500;

        try (final GenericObjectPool<String> createSlowObjectFactoryPool = new GenericObjectPool<>(
                createSlowObjectFactory(60000))) {
            createSlowObjectFactoryPool.setMaxTotal(1);
            createSlowObjectFactoryPool.setMaxWaitMillis(maxWaitMillis);

            // thread1 tries creating a slow object to make pool full.
            final WaitingTestThread thread1 = new WaitingTestThread(createSlowObjectFactoryPool, 0);
            thread1.start();

            // Wait for thread1's reaching to create().
            Thread.sleep(100);

            // another one tries borrowObject. It should return within maxWaitMillis.
            try {
                createSlowObjectFactoryPool.borrowObject(maxWaitMillis);
                fail("borrowObject must fail due to timeout by maxWaitMillis");
            } catch (final NoSuchElementException e) {
                // ignore
            }

            assertTrue(thread1.isAlive());
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
    //@Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testReturnObject() throws Exception {

        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setMaxIdle(-1);
        final String active = genericObjectPool.borrowObject();

        assertEquals(1, genericObjectPool.getNumActive());
        assertEquals(0, genericObjectPool.getNumIdle());

        final Thread t = new Thread() {

            @Override
            public void run() {
                genericObjectPool.close();
            }

        };
        t.start();

        genericObjectPool.returnObject(active);

        // Wait for the close() thread to complete
        while (t.isAlive()) {
            Thread.sleep(50);
        }

        assertEquals(0, genericObjectPool.getNumIdle());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testSetConfig() throws Exception {
        final GenericObjectPoolConfig<String> expected = new GenericObjectPoolConfig<>();
        assertConfiguration(expected,genericObjectPool);
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
        genericObjectPool.setConfig(expected);
        assertConfiguration(expected,genericObjectPool);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testSettersAndGetters() throws Exception {
        {
            // The object receives an Exception during its creation to prevent
            // memory leaks. See BaseGenericObjectPool constructor for more details.
            assertTrue(false == "".equals(genericObjectPool.getCreationStackTrace()));
        }
        {
            assertEquals(0, genericObjectPool.getBorrowedCount());
        }
        {
            assertEquals(0, genericObjectPool.getReturnedCount());
        }
        {
            assertEquals(0, genericObjectPool.getCreatedCount());
        }
        {
            assertEquals(0, genericObjectPool.getDestroyedCount());
        }
        {
            assertEquals(0, genericObjectPool.getDestroyedByEvictorCount());
        }
        {
            assertEquals(0, genericObjectPool.getDestroyedByBorrowValidationCount());
        }
        {
            assertEquals(0, genericObjectPool.getMeanActiveTimeMillis());
        }
        {
            assertEquals(0, genericObjectPool.getMeanIdleTimeMillis());
        }
        {
            assertEquals(0, genericObjectPool.getMeanBorrowWaitTimeMillis());
        }
        {
            assertEquals(0, genericObjectPool.getMaxBorrowWaitTimeMillis());
        }
        {
            assertEquals(0, genericObjectPool.getNumIdle());
        }
        {
            genericObjectPool.setMaxTotal(123);
            assertEquals(123,genericObjectPool.getMaxTotal());
        }
        {
            genericObjectPool.setMaxIdle(12);
            assertEquals(12,genericObjectPool.getMaxIdle());
        }
        {
            genericObjectPool.setMaxWaitMillis(1234L);
            assertEquals(1234L,genericObjectPool.getMaxWaitMillis());
        }
        {
            genericObjectPool.setMinEvictableIdleTimeMillis(12345L);
            assertEquals(12345L,genericObjectPool.getMinEvictableIdleTimeMillis());
        }
        {
            genericObjectPool.setNumTestsPerEvictionRun(11);
            assertEquals(11,genericObjectPool.getNumTestsPerEvictionRun());
        }
        {
            genericObjectPool.setTestOnBorrow(true);
            assertTrue(genericObjectPool.getTestOnBorrow());
            genericObjectPool.setTestOnBorrow(false);
            assertTrue(!genericObjectPool.getTestOnBorrow());
        }
        {
            genericObjectPool.setTestOnReturn(true);
            assertTrue(genericObjectPool.getTestOnReturn());
            genericObjectPool.setTestOnReturn(false);
            assertTrue(!genericObjectPool.getTestOnReturn());
        }
        {
            genericObjectPool.setTestWhileIdle(true);
            assertTrue(genericObjectPool.getTestWhileIdle());
            genericObjectPool.setTestWhileIdle(false);
            assertTrue(!genericObjectPool.getTestWhileIdle());
        }
        {
            genericObjectPool.setTimeBetweenEvictionRunsMillis(11235L);
            assertEquals(11235L,genericObjectPool.getTimeBetweenEvictionRunsMillis());
        }
        {
            genericObjectPool.setSoftMinEvictableIdleTimeMillis(12135L);
            assertEquals(12135L,genericObjectPool.getSoftMinEvictableIdleTimeMillis());
        }
        {
            genericObjectPool.setBlockWhenExhausted(true);
            assertTrue(genericObjectPool.getBlockWhenExhausted());
            genericObjectPool.setBlockWhenExhausted(false);
            assertFalse(genericObjectPool.getBlockWhenExhausted());
        }
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testStartAndStopEvictor() throws Exception {
        // set up pool without evictor
        genericObjectPool.setMaxIdle(6);
        genericObjectPool.setMaxTotal(6);
        genericObjectPool.setNumTestsPerEvictionRun(6);
        genericObjectPool.setMinEvictableIdleTimeMillis(100L);

        for (int j = 0; j < 2; j++) {
            // populate the pool
            {
                final String[] active = new String[6];
                for (int i = 0; i < 6; i++) {
                    active[i] = genericObjectPool.borrowObject();
                }
                for (int i = 0; i < 6; i++) {
                    genericObjectPool.returnObject(active[i]);
                }
            }

            // note that it stays populated
            assertEquals(6,genericObjectPool.getNumIdle(),"Should have 6 idle");

            // start the evictor
            genericObjectPool.setTimeBetweenEvictionRunsMillis(50L);

            // wait a second (well, .2 seconds)
            try { Thread.sleep(200L); } catch(final InterruptedException e) { }

            // assert that the evictor has cleared out the pool
            assertEquals(0,genericObjectPool.getNumIdle(),"Should have 0 idle");

            // stop the evictor
            genericObjectPool.startEvictor(0L);
        }
    }

    @Test
    public void testSwallowedExceptionListener() {
        genericObjectPool.setSwallowedExceptionListener(null); // must simply return
        final List<Exception> swallowedExceptions = new ArrayList<>();
        /*
         * A simple listener, that will throw a OOM on 3rd exception.
         */
        final SwallowedExceptionListener listener = e -> {
            if (swallowedExceptions.size() == 2) {
                throw new OutOfMemoryError();
            }
            swallowedExceptions.add(e);
        };
        genericObjectPool.setSwallowedExceptionListener(listener);

        final Exception e1 = new Exception();
        final Exception e2 = new ArrayIndexOutOfBoundsException();

        genericObjectPool.swallowException(e1);
        genericObjectPool.swallowException(e2);

        try {
            genericObjectPool.swallowException(e1);
            fail("Not supposed to get here");
        } catch (final OutOfMemoryError oom) {
            // expected
        }

        assertEquals(2, swallowedExceptions.size());
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testThreaded1() throws Exception {
        genericObjectPool.setMaxTotal(15);
        genericObjectPool.setMaxIdle(15);
        genericObjectPool.setMaxWaitMillis(1000L);
        runTestThreads(20, 100, 50, genericObjectPool);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testTimeoutNoLeak() throws Exception {
        genericObjectPool.setMaxTotal(2);
        genericObjectPool.setMaxWaitMillis(10);
        genericObjectPool.setBlockWhenExhausted(true);
        final String obj = genericObjectPool.borrowObject();
        final String obj2 = genericObjectPool.borrowObject();
        try {
            genericObjectPool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        genericObjectPool.returnObject(obj2);
        genericObjectPool.returnObject(obj);

        genericObjectPool.borrowObject();
        genericObjectPool.borrowObject();
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
    public void testValidationFailureOnReturnFreesCapacity() throws Exception {
        final SimpleFactory factory = new SimpleFactory();
        factory.setValid(false); // Validate will always fail
        factory.setValidationEnabled(true);
        try (final GenericObjectPool<String> pool = new GenericObjectPool<>(factory)) {
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
    }

    // POOL-276
    @Test
    public void testValidationOnCreateOnly() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setTestOnCreate(true);
        genericObjectPool.setTestOnBorrow(false);
        genericObjectPool.setTestOnReturn(false);
        genericObjectPool.setTestWhileIdle(false);

        final String o1 = genericObjectPool.borrowObject();
        assertEquals("0", o1);
        final Timer t = new Timer();
        t.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        genericObjectPool.returnObject(o1);
                    }
                }, 3000);

        final String o2 = genericObjectPool.borrowObject();
        assertEquals("0", o2);

        assertEquals(1, simpleFactory.validateCounter);
    }

    /**
     * Tests POOL-361
     */
    @Test
    public void testValidateOnCreate() throws Exception {
        genericObjectPool.setTestOnCreate(true);
        genericObjectPool.addObject();
        assertEquals(1, simpleFactory.validateCounter);
    }

    /**
     * Tests POOL-361
     */
    @Test
    public void testValidateOnCreateFailure() throws Exception {
        genericObjectPool.setTestOnCreate(true);
        genericObjectPool.setTestOnBorrow(false);
        genericObjectPool.setMaxTotal(2);
        simpleFactory.setValid(false);
        // Make sure failed validations do not leak capacity
        genericObjectPool.addObject();
        genericObjectPool.addObject();
        assertEquals(0, genericObjectPool.getNumIdle());
        assertEquals(0, genericObjectPool.getNumActive());
        simpleFactory.setValid(true);
        final String obj = genericObjectPool.borrowObject();
        assertNotNull(obj);
        genericObjectPool.addObject();
        // Should have one idle, one out now
        assertEquals(1, genericObjectPool.getNumIdle());
        assertEquals(1, genericObjectPool.getNumActive());
	}

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testWhenExhaustedBlock() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setMaxWaitMillis(10L);
        final String obj1 = genericObjectPool.borrowObject();
        assertNotNull(obj1);
        try {
            genericObjectPool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
        genericObjectPool.returnObject(obj1);
        genericObjectPool.close();
    }

    /**
     * POOL-189
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testWhenExhaustedBlockClosePool() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setMaxWaitMillis(-1);
        final Object obj1 = genericObjectPool.borrowObject();

        // Make sure an object was obtained
        assertNotNull(obj1);

        // Create a separate thread to try and borrow another object
        final WaitingTestThread wtt = new WaitingTestThread(genericObjectPool, 200);
        wtt.start();
        // Give wtt time to start
        Thread.sleep(200);

        // close the pool (Bug POOL-189)
        genericObjectPool.close();

        // Give interrupt time to take effect
        Thread.sleep(200);

        // Check thread was interrupted
        assertTrue(wtt._thrown instanceof InterruptedException);
    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testWhenExhaustedBlockInterrupt() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setBlockWhenExhausted(true);
        genericObjectPool.setMaxWaitMillis(-1);
        final String obj1 = genericObjectPool.borrowObject();

        // Make sure on object was obtained
        assertNotNull(obj1);

        // Create a separate thread to try and borrow another object
        final WaitingTestThread wtt = new WaitingTestThread(genericObjectPool, 200000);
        wtt.start();
        // Give wtt time to start
        Thread.sleep(200);
        wtt.interrupt();

        // Give interrupt time to take effect
        Thread.sleep(200);

        // Check thread was interrupted
        assertTrue(wtt._thrown instanceof InterruptedException);

        // Return object to the pool
        genericObjectPool.returnObject(obj1);

        // Bug POOL-162 - check there is now an object in the pool
        genericObjectPool.setMaxWaitMillis(10L);
        String obj2 = null;
        try {
             obj2 = genericObjectPool.borrowObject();
            assertNotNull(obj2);
        } catch(final NoSuchElementException e) {
            // Not expected
            fail("NoSuchElementException not expected");
        }
        genericObjectPool.returnObject(obj2);
        genericObjectPool.close();

    }

    @Test
    @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
    public void testWhenExhaustedFail() throws Exception {
        genericObjectPool.setMaxTotal(1);
        genericObjectPool.setBlockWhenExhausted(false);
        final String obj1 = genericObjectPool.borrowObject();
        assertNotNull(obj1);
        try {
            genericObjectPool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(final NoSuchElementException e) {
            // expected
        }
        genericObjectPool.returnObject(obj1);
        assertEquals(1, genericObjectPool.getNumIdle());
        genericObjectPool.close();
    }

    /**
     * Check that a pool that starts an evictor, but is never closed does not
     * leave EvictionTimer executor running. Confirmation check is in teardown.
     */
    @Test
    public void testAbandonedPool() throws Exception {
        final GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setJmxEnabled(false);
        GenericObjectPool<String> abandoned = new GenericObjectPool<>(simpleFactory, config);
        abandoned.setTimeBetweenEvictionRunsMillis(100); // Starts evictor

        // This is ugly, but forces gc to hit the pool
        final WeakReference<GenericObjectPool> ref = new WeakReference<>(abandoned);
        abandoned = null;
        while (ref.get() != null) {
            System.gc();
            Thread.sleep(100);
        }
    }

}
