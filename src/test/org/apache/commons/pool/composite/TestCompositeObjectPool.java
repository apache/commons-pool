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

package org.apache.commons.pool.composite;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;
import org.apache.commons.pool.PrivateException;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test {@link CompositeObjectPool} and it's components.
 *
 * @author Sandy McArthur
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
public class TestCompositeObjectPool extends TestObjectPool {
    private CompositeObjectPool pool = null;

    public TestCompositeObjectPool(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeObjectPool.class);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    protected ObjectPool makeEmptyPool(final PoolableObjectFactory factory) {
        final CompositeObjectPoolFactory copf = new CompositeObjectPoolFactory(factory);
        return copf.createPool();
    }

    public void testConstructors() {
        try {
            new CompositeObjectPool(null, new GrowManager(), new FifoLender(), new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null PoolableObjectFactory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPool(new MethodCallPoolableObjectFactory(), null, new FifoLender(), new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null Manager.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPool(new MethodCallPoolableObjectFactory(), new GrowManager(), null, new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null Lender.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPool(new MethodCallPoolableObjectFactory(), new GrowManager(), new FifoLender(), null, false);
            fail("IllegalArgumentException expected on null Tracker.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPool(new MethodCallPoolableObjectFactory(), null, new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            fail("IllegalArgumentException expected on null List.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testSetFactory() throws Exception {
        super.testSetFactory();
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final CompositeObjectPoolFactory copf = new CompositeObjectPoolFactory(factory);
        final CompositeObjectPool pool = (CompositeObjectPool)copf.createPool();
        pool.setFactory(factory);
    }

    public void testToString() {
        super.testToString();
        final ObjectPool pool = new CompositeObjectPool(new MethodCallPoolableObjectFactory(), new FailManager(), new FifoLender(), new DebugTracker(), false);
        pool.toString();
    }

    // Test Managers --------------------------------------

    /**
     * Make sure {@link FailManager} throws an exception.
     */
    public void testFailManager() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new FailManager(), new FifoLender(), new DebugTracker(), false);
        try {
            pool.borrowObject();
            fail("Should have thrown a NoSuchElementException");
        } catch (NoSuchElementException nsee) {
            // correct
        }
    }

    /**
     * Make sure {@link GrowManager} makes a new object.
     */
    public void testGrowManager() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new FifoLender(), new DebugTracker(), false);
        Object obj = null;
        obj = pool.borrowObject();
        assertEquals(new Integer(0), obj);
    }

    /**
     * Make sure {@link IdleLimitManager} prevents the idle pool from getting too large.
     */
    public void testIdleLimitManager() throws Exception {
        final IdleLimitManager manager = new IdleLimitManager(new GrowManager());
        manager.setMaxIdle(2);
        pool = new CompositeObjectPool(new IntegerFactory(), manager, new FifoLender(), new DebugTracker(), false);

        assertEquals(0, pool.getNumIdle());

        pool.addObject();
        assertEquals(1, pool.getNumIdle());

        pool.addObject();
        assertEquals(2, pool.getNumIdle());

        pool.addObject();
        assertEquals(2, pool.getNumIdle());

        pool.clear();
        assertEquals(0, pool.getNumIdle());

        final Integer a = (Integer)pool.borrowObject();
        final Integer b = (Integer)pool.borrowObject();
        final Integer c = (Integer)pool.borrowObject();

        pool.returnObject(a);
        assertEquals(1, pool.getNumIdle());

        pool.returnObject(b);
        assertEquals(2, pool.getNumIdle());

        pool.returnObject(c);
        assertEquals(2, pool.getNumIdle());
    }

    /**
     * Make sure {@link IdleLimitManager} discards the "most idle" object when discarding objects because the pool
     * is full.
     */
    public void testIdleLimitManagerDiscardsMostIdle() throws Exception {
        // FIFO test - the oldest object would be the next one returned.
        {   // create some scope
            final IdleLimitManager manager = new IdleLimitManager(new GrowManager());
            manager.setMaxIdle(2);
            pool = new CompositeObjectPool(new IntegerFactory(), manager, new FifoLender(), new DebugTracker(), false);
            pool.addObject();
            pool.addObject();
            pool.addObject();

            final Integer one = (Integer)pool.borrowObject();
            final Integer two = (Integer)pool.borrowObject();
            final Integer three = (Integer)pool.borrowObject();

            assertEquals(1, one.intValue());
            assertEquals(2, two.intValue());
            assertEquals(3, three.intValue());
        }

        // LIFO test - the oldest object would be the last one returned.
        {   // create some scope
            final IdleLimitManager manager = new IdleLimitManager(new GrowManager());
            manager.setMaxIdle(2);
            pool = new CompositeObjectPool(new IntegerFactory(), manager, new LifoLender(), new DebugTracker(), false);
            pool.addObject();
            pool.addObject();
            pool.addObject();

            final Integer two = (Integer)pool.borrowObject();
            final Integer one = (Integer)pool.borrowObject();
            final Integer three = (Integer)pool.borrowObject();

            assertEquals(2, two.intValue());
            assertEquals(1, one.intValue());
            assertEquals(3, three.intValue());
        }
    }

    /**
     * Make sure {@link FailLimitManager} actually limits the number of active objects.
     */
    public void testFailLimitManager() throws Exception {
        final ActiveLimitManager manager = new FailLimitManager(new GrowManager());
        manager.setMaxActive(2);
        pool = new CompositeObjectPool(new IntegerFactory(), manager, new FifoLender(), new DebugTracker(), false);

        assertEquals(0, pool.getNumActive());

        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        Integer one = (Integer)pool.borrowObject();
        assertEquals(2, pool.getNumActive());

        try {
            pool.borrowObject();
            fail("Should have thrown a NoSuchElementException");
        } catch(NoSuchElementException nsee) {
            // expected
        }
        assertEquals(2, pool.getNumActive());

        pool.returnObject(zero);
        assertEquals(1, pool.getNumActive());

        zero = (Integer)pool.borrowObject();
        assertEquals(2, pool.getNumActive());

        try {
            pool.borrowObject();
            fail("Should have thrown a NoSuchElementException");
        } catch(NoSuchElementException nsee) {
            // expected
        }
        assertEquals(2, pool.getNumActive());
    }

    /**
     * Make sure that {@link WaitLimitManager} both times out and returns an object once one is available.
     */
    public void testWaitLimitManager() throws Exception {
        final WaitLimitManager manager = new WaitLimitManager(new GrowManager());
        manager.setMaxActive(1);
        manager.setMaxWaitMillis(100);
        pool = new CompositeObjectPool(new IntegerFactory(), manager, new FifoLender(), new DebugTracker(), false);

        assertEquals(0, pool.getNumActive());

        final Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        // Test that the max wait
        try {
            pool.borrowObject();
            fail("Should have thrown a NoSuchElementException");
        } catch(NoSuchElementException nsee) {
            // expected
        }

        // test that if an object is returned while waiting it works.
        // What happens is:
        // this thread locks pool.pool and starts Thread t.
        // this thread will get wait for an object to become available and relase the lock on pool.pool
        // Thread t will then be able to lock on pool.pool and return an object
        // this thread will then be able to borrow an object and should reutrn.
        final List actualOrder = new ArrayList();
        final Runnable r = new Runnable() {
            public void run() {
                try {
                    synchronized(pool.getPool()) {
                        pool.returnObject(zero);
                        actualOrder.add("returned");
                    }
                } catch (Exception e) {
                    waitFailed = true;
                }
            }
        };
        final Thread t = new Thread(r);

        synchronized (pool.getPool()) {
            t.start();

            actualOrder.add("waiting");
            assertEquals(zero, pool.borrowObject());
            actualOrder.add("borrowed");
        }

        assertEquals("Wait failed", false, waitFailed);

        List expectedOrder = new ArrayList();
        expectedOrder.add("waiting");
        expectedOrder.add("returned");
        expectedOrder.add("borrowed");

        assertEquals(expectedOrder, actualOrder);
    }
    private boolean waitFailed = false;

    // Test Trackers --------------------------------------

    /**
     * Make sure {@link SimpleTracker} counts the number of borrowed and returned objects and that an
     * {@link IllegalStateException} is thrown when more objects are returned than were borrowed.
     */
    public void testSimpleTracker() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        assertEquals(0, pool.getNumActive());
        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        Integer one = (Integer)pool.borrowObject();
        assertEquals(2, pool.getNumActive());
        pool.returnObject(zero);
        assertEquals(1, pool.getNumActive());
        pool.returnObject(one);
        assertEquals(0, pool.getNumActive());

        try {
            pool.returnObject(new Object());
            fail("Should have thrown an IllegalStateException.");
        } catch(IllegalStateException ise) {
            // expected
        }
    }

    /**
     * Make sure {@link ReferenceTracker} detects when an active object is "lost". Make sure an
     * {@link IllegalStateException} is thrown with an object that wasn't borrowed is returned.
     * This covers the core funtionality of {@link DebugTracker} too; it's unique features aren't easily testable.
     */
    public void testReferenceTracker() throws Exception {
        // setup
        final SortedSet borrowedIds = new TreeSet();
        final SortedSet removedIds = new TreeSet();
        final Tracker tracker = new ReferenceTracker() {
            protected void referenceToBeRemoved(final IdentityReference ref) {
                removedIds.add(new Integer(ref.getKey().hashCode()));
            }
        };
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new FifoLender(), tracker, false);

        // borrow some objects
        List objs = new ArrayList(10);
        for (int i=0; i < 10 ; i++) {
            Object obj = pool.borrowObject();
            borrowedIds.add(new Integer(System.identityHashCode(obj)));
            objs.add(obj);
            obj = null; // prevent leak
        }
        assertEquals(10, pool.getNumActive());
        objs.clear();


        // garbage collect it
        while (!removedIds.containsAll(borrowedIds)) {
            new Object(); // create garbage
            System.gc();
            pool.getNumActive(); // works the reference queue
        }
        assertEquals(10, removedIds.size());
        assertEquals(0, pool.getNumActive());

        // make sure we didn't pick up anything extra
        assertEquals(borrowedIds, removedIds);

        try {
            pool.returnObject(new Object());
            fail("Should have thrown an IllegalStateException. Cannot reutrn an object that wasn't borrowed.");
        } catch(IllegalStateException ise) {
            // expected
        }

        // Make sure we don't accept the same object returned twice
        final Object obj = pool.borrowObject();
        pool.returnObject(obj);
        try {
            pool.returnObject(obj);
            fail("Should have thrown an IllegalStateException. Cannot return the same object twice.");
        } catch(IllegalStateException ise) {
            // expected
        }
    }

    // Test Lenders ---------------------------------------

    /**
     * Make sure {@link FifoLender} is a FIFO.
     */
    public void testFifoLender() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        Integer one = (Integer)pool.borrowObject();
        Integer two = (Integer)pool.borrowObject();

        pool.returnObject(zero);
        pool.returnObject(one);
        pool.returnObject(two);

        assertEquals(zero, pool.borrowObject());
        assertEquals(one, pool.borrowObject());
        assertEquals(two, pool.borrowObject());
    }

    /**
     * Make sure {@link LifoLender} is a LIFO.
     */
    public void testLifoLender() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new LifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        Integer one = (Integer)pool.borrowObject();
        Integer two = (Integer)pool.borrowObject();

        pool.returnObject(two);
        pool.returnObject(one);
        pool.returnObject(zero);

        assertEquals(zero, pool.borrowObject());
        assertEquals(one, pool.borrowObject());
        assertEquals(two, pool.borrowObject());
    }

    /**
     * Make sure {@link SoftLender} responds to memory pressure.
     * <p>Note: this test is not completely deterministic and could fail erroneously.
     */
    public void testSoftLender() throws Exception {
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), new SoftLender(new FifoLender()), new SimpleTracker(), false);

        Object zero = pool.borrowObject();
        Object one = pool.borrowObject();

        pool.returnObject(zero);
        zero = null;
        pool.returnObject(one);
        one = null;

        /* This requires a bit of memory pressure to reliablly force a {@link SoftReference} to break. */
        List garbage = new LinkedList();
        Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            try {
                garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory()/2)]);
            } catch (OutOfMemoryError oome) {
                System.gc();
            }
            System.gc();
        }
        garbage.clear();
        System.gc();

        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
    }

    /**
     * Make sure {@link IdleEvictorLender} waits at least the minimum amount of time before evicting objects.
     * <p>Note: this test is not completely deterministic and could fail erroneously.
     */
    public void testIdleEvictorLender() throws Exception {
        final IdleEvictorLender lender = new IdleEvictorLender(new FifoLender());
        lender.setIdleTimeoutMillis(20L);
        pool = new CompositeObjectPool(new IntegerFactory(), new GrowManager(), lender, new SimpleTracker(), false);

        pool.addObject();
        assertEquals(1, pool.getNumIdle());
        Thread.sleep(100L);
        assertEquals(0, pool.getNumIdle());

        // XXX This could be more extensive
    }

    /**
     * Make sure {@link InvalidEvictorLender} only evicts objects that are no longer valid.
     * <p>Note: this test is not completely deterministic and could fail erroneously.
     */
    public void testInvalidEvictorLender() throws Exception {
        final InvalidEvictorLender lender = new InvalidEvictorLender(new FifoLender());
        lender.setValidationFrequencyMillis(20L);
        final IntegerFactory factory = new IntegerFactory();
        pool = new CompositeObjectPool(factory, new GrowManager(), lender, new SimpleTracker(), false);

        pool.addObject();
        pool.addObject();
        assertEquals(2, pool.getNumIdle());
        Thread.sleep(100L);
        assertEquals(2, pool.getNumIdle());

        factory.setEvenValid(false);
        Thread.sleep(100L);
        assertEquals(1, pool.getNumIdle());

        factory.setOddValid(false);
        Thread.sleep(100L);
        assertEquals(0, pool.getNumIdle());
    }

    // Test exceptions from PoolableObjectFactory ---------

    /**
     * Make sure {@link CompositeObjectPool#borrowObject()} doesn't mask exceptions from
     * {@link PoolableObjectFactory#makeObject()}.
     */
    public void testExceptionOnNewObject() throws Exception {
        MethodCallPoolableObjectFactory pof = new MethodCallPoolableObjectFactory();
        pof.setMakeObjectFail(true);
        pool = new CompositeObjectPool(pof, new GrowManager(), new SoftLender(new FifoLender()), new SimpleTracker(), false);

        try {
            pool.borrowObject();
            fail("Should have thrown a PrivateException");
        } catch (PrivateException pe) {
            // expected
        }

        try {
            pool.addObject();
            fail("Should have thrown a PrivateException");
        } catch (PrivateException pe) {
            // expected
        }
    }

    public void testExceptionOnActivateObject() throws Exception {
        MethodCallPoolableObjectFactory pof = new MethodCallPoolableObjectFactory();
        pof.setActivateObjectFail(true);
        pool = new CompositeObjectPool(pof, new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        pool.returnObject(zero);

        assertEquals(1, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());

        // Should successed even though activation failed by discarding zero and making one.
        final Integer one = (Integer)pool.borrowObject();
        assertEquals(new Integer(1), one);
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumActive());
    }

    public void testExceptionOnValidateObject() throws Exception {
        MethodCallPoolableObjectFactory pof = new MethodCallPoolableObjectFactory();
        pof.setValidateObjectFail(true);
        pool = new CompositeObjectPool(pof, new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        pool.returnObject(zero);

        assertEquals(1, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());

        assertEquals(new Integer(1), pool.borrowObject());
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumActive());
    }

    public void testExceptionOnPasivateObject() throws Exception {
        PoolableObjectFactory pof = new IntegerFactory() {
            public void passivateObject(final Object obj) throws Exception {
                throw new PrivateException("Cannot passivate objects.");
            }
        };
        pool = new CompositeObjectPool(pof, new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        pool.returnObject(zero);

        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
    }

    public void testExceptionOnDestroyObject() throws Exception {
        MethodCallPoolableObjectFactory pof = new MethodCallPoolableObjectFactory();
        pof.setDestroyObjectFail(true);
        pool = new CompositeObjectPool(pof, new GrowManager(), new FifoLender(), new SimpleTracker(), false);

        Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        pool.invalidateObject(zero);

        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
    }

    // Utility classes ------------------------------------

    private static class IntegerFactory extends BasePoolableObjectFactory {
        private int count = 0;
        private boolean oddValid = true;
        private boolean evenValid = true;

        public Object makeObject() throws Exception {
            return new Integer(count++);
        }

        public boolean validateObject(final Object obj) {
            final Integer num = (Integer)obj;
            if (num.intValue() % 2 == 0) {
                return evenValid;
            } else {
                return oddValid;
            }
        }

        public void setValid(final boolean valid) {
            setEvenValid(valid);
            setOddValid(valid);
        }

        public void setOddValid(final boolean oddValid) {
            this.oddValid = oddValid;
        }

        public void setEvenValid(final boolean evenValid) {
            this.evenValid = evenValid;
        }
    }
}