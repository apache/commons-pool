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
     * Create an {@link ObjectPool} instance
     * that can contain at least <i>mincapacity</i>
     * idle and active objects, or
     * throw {@link IllegalArgumentException}
     * if such a pool cannot be created.
     */
    protected abstract ObjectPool makeEmptyPool(int mincapacity);

    /**
     * Create an <code>ObjectPool</code> with the specified factory.
     * The pool should be in a default configuration and conform to the expected
     * behaviors described in {@link ObjectPool}.
     * Generally speaking there should be no limits on the various object counts.
     */
    protected abstract ObjectPool makeEmptyPool(PoolableObjectFactory factory);

    /**
     * Return what we expect to be the n<sup>th</sup>
     * object (ZERO indexed) created by the _pool.
     */
    protected abstract Object getNthObject(int n);

    /**
     * Is the implementations LIFO?
     */
    protected abstract boolean isLifo();

    /**
     * Is the implementationn FIFO?
     */
    protected abstract boolean isFifo();

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
        _pool = null;
    }

    public void testBaseBorrow() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(getNthObject(0),_pool.borrowObject());
        assertEquals(getNthObject(1),_pool.borrowObject());
        assertEquals(getNthObject(2),_pool.borrowObject());
    }

    public void testBaseAddObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        try {
            assertEquals(0,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            _pool.addObject();
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            Object obj = _pool.borrowObject();
            assertEquals(getNthObject(0),obj);
            assertEquals(0,_pool.getNumIdle());
            assertEquals(1,_pool.getNumActive());
            _pool.returnObject(obj);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
        } catch(UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        }
    }

    public void testBaseBorrowReturn() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object obj0 = _pool.borrowObject();
        assertEquals(getNthObject(0),obj0);
        Object obj1 = _pool.borrowObject();
        assertEquals(getNthObject(1),obj1);
        Object obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
        _pool.returnObject(obj2);
        obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
        _pool.returnObject(obj1);
        obj1 = _pool.borrowObject();
        assertEquals(getNthObject(1),obj1);
        _pool.returnObject(obj0);
        _pool.returnObject(obj2);
        obj2 = _pool.borrowObject();
        if (isLifo()) {
            assertEquals(getNthObject(2),obj2);
        }
        if (isFifo()) {
            assertEquals(getNthObject(0),obj2);
        }

        obj0 = _pool.borrowObject();
        if (isLifo()) {
            assertEquals(getNthObject(0),obj0);
        }
        if (isFifo()) {
            assertEquals(getNthObject(2),obj0);
        }
    }

    public void testBaseNumActiveNumIdle() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        assertEquals(1,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.returnObject(obj1);
        assertEquals(1,_pool.getNumActive());
        assertEquals(1,_pool.getNumIdle());
        _pool.returnObject(obj0);
        assertEquals(0,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
    }

    public void testBaseClear() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.returnObject(obj1);
        _pool.returnObject(obj0);
        assertEquals(0,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
        _pool.clear();
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
    }

    public void testBaseInvalidateObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.invalidateObject(obj0);
        assertEquals(1,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.invalidateObject(obj1);
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
    }

    public void testBaseClosePool() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object obj = _pool.borrowObject();
        _pool.returnObject(obj);

        _pool.close();
        try {
            _pool.borrowObject();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
    }

    public void testClosedPoolBehavior() throws Exception {
        final ObjectPool pool = makeEmptyPool(new BasePoolableObjectFactory() {
            public Object makeObject() throws Exception {
                return new Object();
            }
        });
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
        assertEquals("A closed pool shouldn't have any idle objects.", 0, pool.getNumIdle());
        pool.getNumActive();
        pool.returnObject(o1);
        assertEquals("returnObject should not add items back into the idle object pool for a closed pool.", 0, pool.getNumIdle());
        pool.invalidateObject(o2);
        pool.clear();
        pool.close();
    }

    private final Integer ZERO = new Integer(0);
    private final Integer ONE = new Integer(1);

    public void testPOFAddObjectUsage() throws Exception {
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        final ObjectPool pool = makeEmptyPool(factory);
        final List expectedMethods = new ArrayList();

        // addObject should make a new object, pasivate it and put it in the pool
        pool.addObject();
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
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        final ObjectPool pool = makeEmptyPool(factory);
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
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        final ObjectPool pool = makeEmptyPool(factory);
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
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        final ObjectPool pool = makeEmptyPool(factory);
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
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        final ObjectPool pool = makeEmptyPool(factory);
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
        final FailingPoolableObjectFactory factory = new FailingPoolableObjectFactory();
        ObjectPool pool = makeEmptyPool(factory);
        final List expectedMethods = new ArrayList();

        /// Test correct behavior code paths
        PoolUtils.prefill(pool, 5);
        pool.close();


        //// Test exception handling close should swallow failures
        pool = makeEmptyPool(factory);
        reset(pool, factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        PoolUtils.prefill(pool, 5);
        pool.close();
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

    private void reset(final ObjectPool pool, final FailingPoolableObjectFactory factory, final List expectedMethods) throws Exception {
        pool.clear();
        clear(factory, expectedMethods);
        factory.reset();
    }

    private void clear(final FailingPoolableObjectFactory factory, final List expectedMethods) {
        factory.getMethodCalls().clear();
        expectedMethods.clear();
    }

    private ObjectPool _pool = null;

    private static class FailingPoolableObjectFactory implements PoolableObjectFactory {
        private final List methodCalls = new ArrayList();
        private int count = 0;
        private boolean makeObjectFail;
        private boolean activateObjectFail;
        private boolean validateObjectFail;
        private boolean passivateObjectFail;
        private boolean destroyObjectFail;

        public void reset() {
            count = 0;
            getMethodCalls().clear();
            setMakeObjectFail(false);
            setActivateObjectFail(false);
            setValidateObjectFail(false);
            setPassivateObjectFail(false);
            setDestroyObjectFail(false);
        }

        public List getMethodCalls() {
            return methodCalls;
        }

        public int getCurrentCount() {
            return count;
        }

        public void setCurrentCount(final int count) {
            this.count = count;
        }

        public boolean isMakeObjectFail() {
            return makeObjectFail;
        }

        public void setMakeObjectFail(boolean makeObjectFail) {
            this.makeObjectFail = makeObjectFail;
        }

        public boolean isDestroyObjectFail() {
            return destroyObjectFail;
        }

        public void setDestroyObjectFail(boolean destroyObjectFail) {
            this.destroyObjectFail = destroyObjectFail;
        }

        public boolean isValidateObjectFail() {
            return validateObjectFail;
        }

        public void setValidateObjectFail(boolean validateObjectFail) {
            this.validateObjectFail = validateObjectFail;
        }

        public boolean isActivateObjectFail() {
            return activateObjectFail;
        }

        public void setActivateObjectFail(boolean activateObjectFail) {
            this.activateObjectFail = activateObjectFail;
        }

        public boolean isPassivateObjectFail() {
            return passivateObjectFail;
        }

        public void setPassivateObjectFail(boolean passivateObjectFail) {
            this.passivateObjectFail = passivateObjectFail;
        }

        public Object makeObject() throws Exception {
            final MethodCall call = new MethodCall("makeObject");
            methodCalls.add(call);
            int count = this.count++;
            if (makeObjectFail) {
                throw new PrivateException("makeObject");
            }
            final Integer obj = new Integer(count);
            call.setReturned(obj);
            return obj;
        }

        public void activateObject(final Object obj) throws Exception {
            methodCalls.add(new MethodCall("activateObject", obj));
            if (activateObjectFail) {
                throw new PrivateException("activateObject");
            }
        }

        public boolean validateObject(final Object obj) {
            final MethodCall call = new MethodCall("validateObject", obj);
            methodCalls.add(call);
            if (validateObjectFail) {
                throw new PrivateException("validateObject");
            }
            final boolean r = true;
            call.returned(Boolean.valueOf(r));
            return r;
        }

        public void passivateObject(final Object obj) throws Exception {
            methodCalls.add(new MethodCall("passivateObject", obj));
            if (passivateObjectFail) {
                throw new PrivateException("passivateObject");
            }
        }

        public void destroyObject(final Object obj) throws Exception {
            methodCalls.add(new MethodCall("destroyObject", obj));
            if (destroyObjectFail) {
                throw new PrivateException("destroyObject");
            }
        }
    }
}
