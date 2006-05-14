/*
 * Copyright 1999-2004,2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

/**
 * Abstract {@link TestCase} for {@link ObjectPool} implementations.
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestObjectPool extends TestCase {
    public TestObjectPool(String testName) {
        super(testName);
    }

    /**
     * Create an <code>ObjectPool</code> with the specified factory.
     * The pool should be in a default configuration and conform to the expected
     * behaviors described in {@link ObjectPool}.
     * Generally speaking there should be no limits on the various object counts.
     * @throws UnsupportedOperationException if the pool being tested does not follow pool contracts.
     */
    protected abstract ObjectPool makeEmptyPool(PoolableObjectFactory factory) throws UnsupportedOperationException;

    /**
     * Create an <code>ObjectPool</code> with the specified factory and
     * limit on the number of concurrently active objects.
     * @throws UnsupportedOperationException if the pool being tested does not have a max active limit.
     */
    protected abstract ObjectPool makeEmptyPoolWithActiveLimit(PoolableObjectFactory factory, int activeLimit) throws UnsupportedOperationException;

    public void testClosedPoolBehavior() throws Exception {
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(new MethodCallPoolableObjectFactory());
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        Object o1 = pool.borrowObject();
        Object o2 = pool.borrowObject();

        pool.close();

        try {
            pool.addObject();
            fail("A closed pool must throw an IllegalStateException when addObject is called.");
        } catch (IllegalStateException ise) {
            // expected
        }

        try {
            pool.borrowObject();
            fail("A closed pool must throw an IllegalStateException when borrowObject is called.");
        } catch (IllegalStateException ise) {
            // expected
        }

        // The following should not throw exceptions just because the pool is closed.
        if (pool.getNumIdle() >= 0) {
            assertEquals("A closed pool shouldn't have any idle objects.", 0, pool.getNumIdle());
        }
        if (pool.getNumActive() >= 0) {
            assertEquals("A closed pool should still keep count of active objects.", 2, pool.getNumActive());
        }
        pool.returnObject(o1);
        if (pool.getNumIdle() >= 0) {
            assertEquals("returnObject should not add items back into the idle object pool for a closed pool.", 0, pool.getNumIdle());
        }
        if (pool.getNumActive() >= 0) {
            assertEquals("A closed pool should still keep count of active objects.", 1, pool.getNumActive());
        }
        pool.invalidateObject(o2);
        if (pool.getNumIdle() >= 0) {
            assertEquals("invalidateObject must not add items back into the idle object pool.", 0, pool.getNumIdle());
        }
        if (pool.getNumActive() >= 0) {
            assertEquals("A closed pool should still keep count of active objects.", 0, pool.getNumActive());
        }
        pool.clear();
        pool.close();
    }

    private final Integer ZERO = new Integer(0);
    private final Integer ONE = new Integer(1);

    public void testPOFAddObjectUsage() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        // addObject should make a new object, pasivate it and put it in the pool
        pool.addObject();
        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());
        expectedMethods.add(new MethodCall("makeObject").returned(ZERO));
        expectedMethods.add(new MethodCall("passivateObject", ZERO));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of addObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(true);
        try {
            pool.addObject();
            fail("Expected addObject to propagate makeObject exception.");
        } catch (PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject"));
        assertEquals(expectedMethods, factory.getMethodCalls());

        clear(factory, expectedMethods);

        // passivateObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(false);
        factory.setPassivateObjectFail(true);
        try {
            pool.addObject();
            fail("Expected addObject to propagate passivateObject exception.");
        } catch (PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject").returned(ONE));
        expectedMethods.add(new MethodCall("passivateObject", ONE));
        assertEquals(expectedMethods, factory.getMethodCalls());
    }

    public void testPOFBorrowObjectUsages() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();
        Object obj;

        /// Test correct behavior code paths

        // existing idle object should be activated and validated
        pool.addObject();
        clear(factory, expectedMethods);
        obj = pool.borrowObject();
        expectedMethods.add(new MethodCall("activateObject", ZERO));
        expectedMethods.add(new MethodCall("validateObject", ZERO).returned(Boolean.TRUE));
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.returnObject(obj);

        //// Test exception handling of borrowObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from borrowObject
        factory.setMakeObjectFail(true);
        try {
            obj = pool.borrowObject();
            fail("Expected borrowObject to propagate makeObject exception.");
        } catch (PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject"));
        assertEquals(expectedMethods, factory.getMethodCalls());


        // when activateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject();
        clear(factory, expectedMethods);

        factory.setActivateObjectFail(true);
        expectedMethods.add(new MethodCall("activateObject", obj));
        obj = pool.borrowObject();
        expectedMethods.add(new MethodCall("makeObject").returned(ONE));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.returnObject(obj);

        // when validateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject();
        clear(factory, expectedMethods);

        factory.setValidateObjectFail(true);
        expectedMethods.add(new MethodCall("activateObject", ZERO));
        expectedMethods.add(new MethodCall("validateObject", ZERO));
        obj = pool.borrowObject();
        expectedMethods.add(new MethodCall("makeObject").returned(ONE));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.returnObject(obj);
    }

    public void testPOFReturnObjectUsages() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();
        Object obj;
        int idleCount;

        /// Test correct behavior code paths
        obj = pool.borrowObject();
        clear(factory, expectedMethods);

        // returned object should be passivated
        pool.returnObject(obj);
        expectedMethods.add(new MethodCall("passivateObject", obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of returnObject
        reset(pool, factory, expectedMethods);

        // passivateObject should swallow exceptions and not add the object to the pool
        idleCount = pool.getNumIdle();
        obj = pool.borrowObject();
        clear(factory, expectedMethods);
        factory.setPassivateObjectFail(true);
        pool.returnObject(obj);
        expectedMethods.add(new MethodCall("passivateObject", obj));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        assertEquals(idleCount, pool.getNumIdle());

        // destroyObject should swallow exceptions too
        reset(pool, factory, expectedMethods);
        obj = pool.borrowObject();
        clear(factory, expectedMethods);
        factory.setPassivateObjectFail(true);
        factory.setDestroyObjectFail(true);
        pool.returnObject(obj);
    }

    public void testPOFInvalidateObjectUsages() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();
        Object obj;

        /// Test correct behavior code paths

        obj = pool.borrowObject();
        clear(factory, expectedMethods);

        // invalidated object should be destroyed
        pool.invalidateObject(obj);
        expectedMethods.add(new MethodCall("destroyObject", obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of invalidateObject
        reset(pool, factory, expectedMethods);
        obj = pool.borrowObject();
        clear(factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        pool.invalidateObject(obj);
        Thread.sleep(250); // could be defered
        removeDestroyObjectCall(factory.getMethodCalls());
        assertEquals(expectedMethods, factory.getMethodCalls());
    }

    public void testPOFClearUsages() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();

        /// Test correct behavior code paths
        PoolUtils.prefill(pool, 5);
        pool.clear();

        //// Test exception handling clear should swallow destory object failures
        reset(pool, factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        PoolUtils.prefill(pool, 5);
        pool.clear();
    }

    public void testPOFCloseUsages() throws Exception {
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        ObjectPool pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List expectedMethods = new ArrayList();

        /// Test correct behavior code paths
        PoolUtils.prefill(pool, 5);
        pool.close();


        //// Test exception handling close should swallow failures
        try {
            pool = makeEmptyPool(factory);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        reset(pool, factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        PoolUtils.prefill(pool, 5);
        pool.close();
    }

    public void testSetFactory() throws Exception {
        ObjectPool pool;
        try {
            pool = makeEmptyPool(new MethodCallPoolableObjectFactory());
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final MethodCallPoolableObjectFactory factory = new MethodCallPoolableObjectFactory();
        try {
            pool.setFactory(factory);
        } catch (UnsupportedOperationException uoe) {
            return;
        }
    }

    public void testToString() {
        ObjectPool pool;
        try {
            pool = makeEmptyPool(new MethodCallPoolableObjectFactory());
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }
        pool.toString();
    }

    static void removeDestroyObjectCall(List calls) {
        Iterator iter = calls.iterator();
        while (iter.hasNext()) {
            MethodCall call = (MethodCall)iter.next();
            if ("destroyObject".equals(call.getName())) {
                iter.remove();
            }
        }
    }

    private static void reset(final ObjectPool pool, final MethodCallPoolableObjectFactory factory, final List expectedMethods) throws Exception {
        pool.clear();
        clear(factory, expectedMethods);
        factory.reset();
    }

    private static void clear(final MethodCallPoolableObjectFactory factory, final List expectedMethods) {
        factory.getMethodCalls().clear();
        expectedMethods.clear();
    }

    public void testMaxActiveLimit() throws Exception {
        final int activeLimit = 15;
        final MaxActiveLimitTesterFactory factory = new MaxActiveLimitTesterFactory();
        final ObjectPool pool;
        try {
            pool = makeEmptyPoolWithActiveLimit(factory, activeLimit);
        } catch (UnsupportedOperationException uoe) {
            return; // test not supported
        }

        final ThreadGroup tg = new ThreadGroup("MaxActiveLimitTesters");
        final Object lock = new Object();
        final Runnable runnable = new MaxActiveLimitTester(pool, lock);
        final Thread[] workers = new Thread[100];
        synchronized (lock) {
            for (int i=0; i < workers.length; i++) {
                Thread t = new Thread(tg, runnable);
                workers[i] = t;
                t.start();
            }
        }
        for (int i=0; i < workers.length; i++) {
            workers[i].join();
        }
        assertEquals("Too many objects concurrently activated.", activeLimit, factory.getMaxActive());
    }

    private static class MaxActiveLimitTesterFactory extends BasePoolableObjectFactory {
        private final Object lock = new Object();
        private int count = 0;

        private final Set activeSet = new HashSet();
        private final Set passiveSet = new HashSet();
        private int maxActive = 0;
        private int maxPassive = 0;

        public Object makeObject() throws Exception {
            synchronized (lock) {
                final Integer integer = new Integer(count++);
                if (!activeSet.add(integer)) {
                    throw new AssertionError("umm, sync issue!");
                }
                return integer;
            }
        }

        public void activateObject(final Object obj) throws Exception {
            synchronized (lock) {
                if (!passiveSet.remove(obj)) {
                    throw new AssertionError("Should never activate an object that isn't passive!");
                }
                activeSet.add(obj);
                maxActive = Math.max(maxActive, activeSet.size());
            }
        }

        public void passivateObject(Object obj) throws Exception {
            synchronized (lock) {
                if (!activeSet.remove(obj)) {
                    throw new Error("Should never passivate an object that isn't active!");
                }
                passiveSet.add(obj);
                maxPassive = Math.max(maxPassive, passiveSet.size());
            }
        }

        public void destroyObject(Object obj) throws Exception {
            synchronized (lock) {
                activeSet.remove(obj);
                passiveSet.remove(obj);
            }
        }

        public int getCount() {
            return count;
        }

        public int getMaxActive() {
            return maxActive;
        }

        public int getMaxPassive() {
            return maxPassive;
        }
    }

    private static class MaxActiveLimitTester implements Runnable {
        private final ObjectPool pool;
        private final Object lock;

        MaxActiveLimitTester(final ObjectPool pool, final Object lock) {
            this.pool = pool;
            this.lock = lock;
        }

        public void run() {
            synchronized (lock) { // wait for it...
            } // go!
            final long end = System.currentTimeMillis() + (30 * 1000);
            while (System.currentTimeMillis() < end) {
                try {
                    final Object o = pool.borrowObject();
                    Thread.yield();
                    pool.returnObject(o);
                } catch (Exception e) {
                }
            }
        }
    }
}
