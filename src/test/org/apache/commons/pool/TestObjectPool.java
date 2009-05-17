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
package org.apache.commons.pool;

import junit.framework.TestCase;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
        // StackObjectPool, SoftReferenceObjectPool also validate on add
        if (pool instanceof StackObjectPool || 
                pool instanceof SoftReferenceObjectPool) {
            expectedMethods.add(new MethodCall(
                    "validateObject", ZERO).returned(Boolean.TRUE));
        }
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
        // StackObjectPool, SofReferenceObjectPool also validate on add
        if (pool instanceof StackObjectPool || 
                pool instanceof SoftReferenceObjectPool) {
            expectedMethods.add(new MethodCall(
                    "validateObject", ONE).returned(Boolean.TRUE));
        }
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
        if (pool instanceof GenericObjectPool) {
            ((GenericObjectPool) pool).setTestOnBorrow(true);
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
        try {
            obj = pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // Expected - newly created object will also fail to activate
        }
        // Idle object fails activation, new one created, also fails
        expectedMethods.add(new MethodCall("makeObject").returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", ONE));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());

        // when validateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject();
        clear(factory, expectedMethods);

        factory.setValidateObjectFail(true);
        expectedMethods.add(new MethodCall("activateObject", ZERO));
        expectedMethods.add(new MethodCall("validateObject", ZERO));
        try {
            obj = pool.borrowObject();
        } catch (NoSuchElementException ex) {
            // Expected - newly created object will also fail to validate
        }
        // Idle object is activated, but fails validation.
        // New instance is created, activated and then fails validation
        expectedMethods.add(new MethodCall("makeObject").returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", ONE));
        expectedMethods.add(new MethodCall("validateObject", ONE));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        // Second activate and validate are missing from expectedMethods
        assertTrue(factory.getMethodCalls().containsAll(expectedMethods));
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

        /// Test correct behavior code paths
        obj = pool.borrowObject();
        clear(factory, expectedMethods);

        // returned object should be passivated
        pool.returnObject(obj);
        // StackObjectPool, SoftReferenceObjectPool also validate on return
        if (pool instanceof StackObjectPool || 
                pool instanceof SoftReferenceObjectPool) {
            expectedMethods.add(new MethodCall(
                    "validateObject", obj).returned(Boolean.TRUE));
        }
        expectedMethods.add(new MethodCall("passivateObject", obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of returnObject
        reset(pool, factory, expectedMethods);
        pool.addObject();
        pool.addObject();
        pool.addObject();
        assertEquals(3, pool.getNumIdle());
        // passivateObject should swallow exceptions and not add the object to the pool
        obj = pool.borrowObject();
        pool.borrowObject();
        assertEquals(1, pool.getNumIdle());
        assertEquals(2, pool.getNumActive());
        clear(factory, expectedMethods);
        factory.setPassivateObjectFail(true);
        pool.returnObject(obj);
        // StackObjectPool, SoftReferenceObjectPool also validate on return
        if (pool instanceof StackObjectPool || 
                pool instanceof SoftReferenceObjectPool) {
            expectedMethods.add(new MethodCall(
                    "validateObject", obj).returned(Boolean.TRUE));
        }
        expectedMethods.add(new MethodCall("passivateObject", obj));
        removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        assertEquals(1, pool.getNumIdle());   // Not returned
        assertEquals(1, pool.getNumActive()); // But not in active count

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
        try {
            pool.invalidateObject(obj);
            fail("Expecting destroy exception to propagate");
        } catch (PrivateException ex) {
            // Expected
        }
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
}
