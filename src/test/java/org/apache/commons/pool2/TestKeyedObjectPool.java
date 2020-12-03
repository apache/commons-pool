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
package org.apache.commons.pool2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract test case for {@link ObjectPool} implementations.
 */
public abstract class TestKeyedObjectPool {

    /**
     * Create an {@code KeyedObjectPool} with the specified factory.
     * The pool should be in a default configuration and conform to the expected
     * behaviors described in {@link KeyedObjectPool}.
     * Generally speaking there should be no limits on the various object counts.
     *
     * @param factory Factory to use to associate with the pool
     * @return The newly created empty pool
     */
    protected abstract KeyedObjectPool<Object,Object> makeEmptyPool(
            KeyedPooledObjectFactory<Object,Object> factory);

    protected static final String KEY = "key";

    private KeyedObjectPool<Object,Object> _pool = null;

    protected abstract Object makeKey(int n);

    protected abstract boolean isFifo();

    protected abstract boolean isLifo();

    @AfterEach
    public void tearDown() throws Exception {
        _pool = null;
    }

    /**
     * Create an {@link KeyedObjectPool} instance
     * that can contain at least <i>minCapacity</i>
     * idle and active objects, or
     * throw {@link IllegalArgumentException}
     * if such a pool cannot be created.
     * @param minCapacity Minimum capacity of the pool to create
     *
     * @return the newly created keyed object pool
     */
    protected abstract KeyedObjectPool<Object,Object> makeEmptyPool(int minCapacity);

    /**
     * Return what we expect to be the n<sup>th</sup>
     * object (zero indexed) created by the pool
     * for the given key.
     * @param key Key for the object to be obtained
     * @param n   index of the object to be obtained
     *
     * @return the requested object
     */
    protected abstract Object getNthObject(Object key, int n);

    @Test
    public void testClosedPoolBehavior() throws Exception {
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(new TestFactory());
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }

        final Object o1 = pool.borrowObject(KEY);
        final Object o2 = pool.borrowObject(KEY);

        pool.close();

        try {
            pool.addObject(KEY);
            fail("A closed pool must throw an IllegalStateException when addObject is called.");
        } catch (final IllegalStateException ise) {
            // expected
        }

        try {
            pool.borrowObject(KEY);
            fail("A closed pool must throw an IllegalStateException when borrowObject is called.");
        } catch (final IllegalStateException ise) {
            // expected
        }

        // The following should not throw exceptions just because the pool is closed.
        assertEquals( 0, pool.getNumIdle(KEY),"A closed pool shouldn't have any idle objects.");
        assertEquals( 0, pool.getNumIdle(),"A closed pool shouldn't have any idle objects.");
        pool.getNumActive();
        pool.getNumActive(KEY);
        pool.returnObject(KEY, o1);
        assertEquals( 0, pool.getNumIdle(KEY),"returnObject should not add items back into the idle object pool for a closed pool.");
        assertEquals( 0, pool.getNumIdle(),"returnObject should not add items back into the idle object pool for a closed pool.");
        pool.invalidateObject(KEY, o2);
        pool.clear(KEY);
        pool.clear();
        pool.close();
    }

    // Deliberate choice to create a new object in case future unit tests check
    // for a specific object.
    private final Integer ZERO = new Integer(0);
    private final Integer ONE = new Integer(1);

    @Test
    public void testKPOFAddObjectUsage() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        // addObject should make a new object, passivate it and put it in the pool
        pool.addObject(KEY);
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ZERO));
        expectedMethods.add(new MethodCall("passivateObject", KEY, ZERO));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of addObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(true);
        try {
            pool.addObject(KEY);
            fail("Expected addObject to propagate makeObject exception.");
        } catch (final PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject", KEY));
        assertEquals(expectedMethods, factory.getMethodCalls());

        clear(factory, expectedMethods);

        // passivateObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(false);
        factory.setPassivateObjectFail(true);
        try {
            pool.addObject(KEY);
            fail("Expected addObject to propagate passivateObject exception.");
        } catch (final PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("passivateObject", KEY, ONE));
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFBorrowObjectUsages() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();
        Object obj;

        if (pool instanceof GenericKeyedObjectPool) {
            ((GenericKeyedObjectPool<Object,Object>) pool).setTestOnBorrow(true);
        }

        /// Test correct behavior code paths

        // existing idle object should be activated and validated
        pool.addObject(KEY);
        clear(factory, expectedMethods);
        obj = pool.borrowObject(KEY);
        expectedMethods.add(new MethodCall("activateObject", KEY, ZERO));
        expectedMethods.add(new MethodCall("validateObject", KEY, ZERO).returned(Boolean.TRUE));
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.returnObject(KEY, obj);

        //// Test exception handling of borrowObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from borrowObject
        factory.setMakeObjectFail(true);
        try {
            obj = pool.borrowObject(KEY);
            fail("Expected borrowObject to propagate makeObject exception.");
        } catch (final PrivateException pe) {
            // expected
        }
        expectedMethods.add(new MethodCall("makeObject", KEY));
        assertEquals(expectedMethods, factory.getMethodCalls());


        // when activateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject(KEY);
        clear(factory, expectedMethods);

        factory.setActivateObjectFail(true);
        expectedMethods.add(new MethodCall("activateObject", KEY, obj));
        try {
            pool.borrowObject(KEY);
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException e) {
            //Activate should fail
        }
        // After idle object fails validation, new on is created and activation
        // fails again for the new one.
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", KEY, ONE));
        TestObjectPool.removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());

        // when validateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject(KEY);
        clear(factory, expectedMethods);

        factory.setValidateObjectFail(true);
        // testOnBorrow is on, so this will throw when the newly created instance
        // fails validation
        try {
            pool.borrowObject(KEY);
            fail("Expecting NoSuchElementException");
        } catch (final NoSuchElementException ex) {
            // expected
        }
        // Activate, then validate for idle instance
        expectedMethods.add(new MethodCall("activateObject", KEY, ZERO));
        expectedMethods.add(new MethodCall("validateObject", KEY, ZERO));
        // Make new instance, activate succeeds, validate fails
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", KEY, ONE));
        expectedMethods.add(new MethodCall("validateObject", KEY, ONE));
        TestObjectPool.removeDestroyObjectCall(factory.getMethodCalls());
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFReturnObjectUsages() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();
        Object obj;

        /// Test correct behavior code paths
        obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);

        // returned object should be passivated
        pool.returnObject(KEY, obj);
        expectedMethods.add(new MethodCall("passivateObject", KEY, obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of returnObject
        reset(pool, factory, expectedMethods);

        // passivateObject should swallow exceptions and not add the object to the pool
        pool.addObject(KEY);
        pool.addObject(KEY);
        pool.addObject(KEY);
        assertEquals(3, pool.getNumIdle(KEY));
        obj = pool.borrowObject(KEY);
        obj = pool.borrowObject(KEY);
        assertEquals(1, pool.getNumIdle(KEY));
        assertEquals(2, pool.getNumActive(KEY));
        clear(factory, expectedMethods);
        factory.setPassivateObjectFail(true);
        pool.returnObject(KEY, obj);
        expectedMethods.add(new MethodCall("passivateObject", KEY, obj));
        TestObjectPool.removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        assertEquals(1, pool.getNumIdle(KEY));   // Not added
        assertEquals(1, pool.getNumActive(KEY)); // But not active

        reset(pool, factory, expectedMethods);
        obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);
        factory.setPassivateObjectFail(true);
        factory.setDestroyObjectFail(true);
        try {
            pool.returnObject(KEY, obj);
            if (!(pool instanceof GenericKeyedObjectPool)) { // ugh, 1.3-compat
                fail("Expecting destroyObject exception to be propagated");
            }
        } catch (final PrivateException ex) {
            // Expected
        }
        pool.close();
    }

    @Test
    public void testKPOFInvalidateObjectUsages() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();
        Object obj;

        /// Test correct behavior code paths

        obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);

        // invalidated object should be destroyed
        pool.invalidateObject(KEY, obj);
        expectedMethods.add(new MethodCall("destroyObject", KEY, obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        //// Test exception handling of invalidateObject
        reset(pool, factory, expectedMethods);
        obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        try {
            pool.invalidateObject(KEY, obj);
            fail("Expecting destroy exception to propagate");
        } catch (final PrivateException ex) {
            // Expected
        }
        Thread.sleep(250); // could be defered
        TestObjectPool.removeDestroyObjectCall(factory.getMethodCalls());
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFClearUsages() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object,Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        /// Test correct behavior code paths
        pool.addObjects(KEY, 5);
        pool.clear();

        //// Test exception handling clear should swallow destroy object failures
        reset(pool, factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        pool.addObjects(KEY, 5);
        pool.clear();
        pool.close();
    }

    @Test
    public void testKPOFCloseUsages() throws Exception {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        KeyedObjectPool<Object, Object> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        /// Test correct behavior code paths
        pool.addObjects(KEY, 5);
        pool.close();

        //// Test exception handling close should swallow failures
        try (final KeyedObjectPool<Object, Object> pool2 = makeEmptyPool(factory)) {
            reset(pool2, factory, expectedMethods);
            factory.setDestroyObjectFail(true);
            pool2.addObjects(KEY, 5);
        }
    }

    @Test
    public void testToString() throws Exception {
        final FailingKeyedPooledObjectFactory factory =
                new FailingKeyedPooledObjectFactory();
        try (final KeyedObjectPool<Object,Object> pool = makeEmptyPool(factory)) {
            pool.toString();
        } catch(final UnsupportedOperationException uoe) {
            return; // test not supported
        }
    }

    @Test
    public void testBaseBorrowReturn() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        Object obj0 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,0),obj0);
        Object obj1 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,1),obj1);
        Object obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        _pool.returnObject(keya,obj2);
        obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        _pool.returnObject(keya,obj1);
        obj1 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,1),obj1);
        _pool.returnObject(keya,obj0);
        _pool.returnObject(keya,obj2);
        obj2 = _pool.borrowObject(keya);
        if (isLifo()) {
            assertEquals(getNthObject(keya,2),obj2);
        }
        if (isFifo()) {
            assertEquals(getNthObject(keya,0),obj2);
        }
        obj0 = _pool.borrowObject(keya);
        if (isLifo()) {
            assertEquals(getNthObject(keya,0),obj0);
        }
        if (isFifo()) {
            assertEquals(getNthObject(keya,2),obj0);
        }
        _pool.close();
    }

    @Test
    public void testBaseBorrow() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        final Object keyb = makeKey(1);
        assertEquals(getNthObject(keya,0),_pool.borrowObject(keya),"1");
        assertEquals(getNthObject(keyb,0),_pool.borrowObject(keyb),"2");
        assertEquals(getNthObject(keyb,1),_pool.borrowObject(keyb),"3");
        assertEquals(getNthObject(keya,1),_pool.borrowObject(keya),"4");
        assertEquals(getNthObject(keyb,2),_pool.borrowObject(keyb),"5");
        assertEquals(getNthObject(keya,2),_pool.borrowObject(keya),"6");
        _pool.close();
    }

    @Test
    public void testBaseNumActiveNumIdle() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        final Object obj0 = _pool.borrowObject(keya);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        final Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj1);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(1,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));

        assertEquals(0,_pool.getNumActive("xyzzy12345"));
        assertEquals(0,_pool.getNumIdle("xyzzy12345"));

        _pool.close();
    }

    @Test
    public void testBaseNumActiveNumIdle2() throws Exception {
        try {
            _pool = makeEmptyPool(6);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        final Object keyb = makeKey(1);
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(0,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        final Object objA0 = _pool.borrowObject(keya);
        final Object objB0 = _pool.borrowObject(keyb);

        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(1,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        final Object objA1 = _pool.borrowObject(keya);
        final Object objB1 = _pool.borrowObject(keyb);

        assertEquals(4,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(2,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        _pool.returnObject(keya,objA0);
        _pool.returnObject(keyb,objB0);

        assertEquals(2,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(1,_pool.getNumIdle(keya));
        assertEquals(1,_pool.getNumActive(keyb));
        assertEquals(1,_pool.getNumIdle(keyb));

        _pool.returnObject(keya,objA1);
        _pool.returnObject(keyb,objB1);

        assertEquals(0,_pool.getNumActive());
        assertEquals(4,_pool.getNumIdle());
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));
        assertEquals(0,_pool.getNumActive(keyb));
        assertEquals(2,_pool.getNumIdle(keyb));

        _pool.close();
    }

    @Test
    public void testBaseClear() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        final Object obj0 = _pool.borrowObject(keya);
        final Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj1);
        _pool.returnObject(keya,obj0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));
        _pool.clear(keya);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        final Object obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        _pool.close();
    }

    @Test
    public void testBaseInvalidateObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        final Object obj0 = _pool.borrowObject(keya);
        final Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.invalidateObject(keya,obj0);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.invalidateObject(keya,obj1);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.close();
    }

    @Test
    public void testBaseAddObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object key = makeKey(0);
        try {
            assertEquals(0,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(0,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
            _pool.addObject(key);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(1,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
            final Object obj = _pool.borrowObject(key);
            assertEquals(getNthObject(key,0),obj);
            assertEquals(0,_pool.getNumIdle());
            assertEquals(1,_pool.getNumActive());
            assertEquals(0,_pool.getNumIdle(key));
            assertEquals(1,_pool.getNumActive(key));
            _pool.returnObject(key,obj);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(1,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
        } catch(final UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        } finally {
            _pool.close();
        }
    }


    private void reset(final KeyedObjectPool<Object,Object> pool, final FailingKeyedPooledObjectFactory factory, final List<MethodCall> expectedMethods) throws Exception {
        pool.clear();
        clear(factory, expectedMethods);
        factory.reset();
    }

    private void clear(final FailingKeyedPooledObjectFactory factory, final List<MethodCall> expectedMethods) {
        factory.getMethodCalls().clear();
        expectedMethods.clear();
    }

    private static class TestFactory
            extends BaseKeyedPooledObjectFactory<Object,Object> {
        @Override
        public Object create(final Object key) throws Exception {
            return new Object();
        }
        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<>(value);
        }
    }

    protected static class FailingKeyedPooledObjectFactory implements KeyedPooledObjectFactory<Object,Object> {
        private final List<MethodCall> methodCalls = new ArrayList<>();
        private int count = 0;
        private boolean makeObjectFail;
        private boolean activateObjectFail;
        private boolean validateObjectFail;
        private boolean passivateObjectFail;
        private boolean destroyObjectFail;

        public FailingKeyedPooledObjectFactory() {
        }

        public void reset() {
            count = 0;
            getMethodCalls().clear();
            setMakeObjectFail(false);
            setActivateObjectFail(false);
            setValidateObjectFail(false);
            setPassivateObjectFail(false);
            setDestroyObjectFail(false);
        }

        public List<MethodCall> getMethodCalls() {
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

        public void setMakeObjectFail(final boolean makeObjectFail) {
            this.makeObjectFail = makeObjectFail;
        }

        public boolean isDestroyObjectFail() {
            return destroyObjectFail;
        }

        public void setDestroyObjectFail(final boolean destroyObjectFail) {
            this.destroyObjectFail = destroyObjectFail;
        }

        public boolean isValidateObjectFail() {
            return validateObjectFail;
        }

        public void setValidateObjectFail(final boolean validateObjectFail) {
            this.validateObjectFail = validateObjectFail;
        }

        public boolean isActivateObjectFail() {
            return activateObjectFail;
        }

        public void setActivateObjectFail(final boolean activateObjectFail) {
            this.activateObjectFail = activateObjectFail;
        }

        public boolean isPassivateObjectFail() {
            return passivateObjectFail;
        }

        public void setPassivateObjectFail(final boolean passivateObjectFail) {
            this.passivateObjectFail = passivateObjectFail;
        }

        @Override
        public PooledObject<Object> makeObject(final Object key) throws Exception {
            final MethodCall call = new MethodCall("makeObject", key);
            methodCalls.add(call);
            final int originalCount = this.count++;
            if (makeObjectFail) {
                throw new PrivateException("makeObject");
            }
            // Deliberate choice to create new object in case future unit test
            // checks for a specific object
            final Integer obj = new Integer(originalCount);
            call.setReturned(obj);
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void activateObject(final Object key, final PooledObject<Object> obj) throws Exception {
            methodCalls.add(new MethodCall("activateObject", key, obj.getObject()));
            if (activateObjectFail) {
                throw new PrivateException("activateObject");
            }
        }

        @Override
        public boolean validateObject(final Object key, final PooledObject<Object> obj) {
            final MethodCall call = new MethodCall("validateObject", key, obj.getObject());
            methodCalls.add(call);
            if (validateObjectFail) {
                throw new PrivateException("validateObject");
            }
            final boolean r = true;
            call.returned(Boolean.valueOf(r));
            return r;
        }

        @Override
        public void passivateObject(final Object key, final PooledObject<Object> obj) throws Exception {
            methodCalls.add(new MethodCall("passivateObject", key, obj.getObject()));
            if (passivateObjectFail) {
                throw new PrivateException("passivateObject");
            }
        }

        @Override
        public void destroyObject(final Object key, final PooledObject<Object> obj) throws Exception {
            methodCalls.add(new MethodCall("destroyObject", key, obj.getObject()));
            if (destroyObjectFail) {
                throw new PrivateException("destroyObject");
            }
        }
    }
}
