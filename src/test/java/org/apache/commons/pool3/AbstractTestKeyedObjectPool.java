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
package org.apache.commons.pool3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Abstract test case for {@link ObjectPool} implementations.
 */
public abstract class AbstractTestKeyedObjectPool {

    protected static class FailingKeyedPooledObjectFactory implements KeyedPooledObjectFactory<Object, Object, PrivateException> {
        private final List<MethodCall> methodCalls = new ArrayList<>();
        private int count;
        private boolean makeObjectFail;
        private boolean activateObjectFail;
        private boolean validateObjectFail;
        private boolean passivateObjectFail;
        private boolean destroyObjectFail;

        public FailingKeyedPooledObjectFactory() {
        }

        @Override
        public void activateObject(final Object key, final PooledObject<Object> obj) {
            methodCalls.add(new MethodCall("activateObject", key, obj.getObject()));
            if (activateObjectFail) {
                throw new PrivateException("activateObject");
            }
        }

        @Override
        public void destroyObject(final Object key, final PooledObject<Object> obj) {
            methodCalls.add(new MethodCall("destroyObject", key, obj.getObject()));
            if (destroyObjectFail) {
                throw new PrivateException("destroyObject");
            }
        }

        public int getCurrentCount() {
            return count;
        }

        public List<MethodCall> getMethodCalls() {
            return methodCalls;
        }

        public boolean isActivateObjectFail() {
            return activateObjectFail;
        }

        public boolean isDestroyObjectFail() {
            return destroyObjectFail;
        }

        public boolean isMakeObjectFail() {
            return makeObjectFail;
        }

        public boolean isPassivateObjectFail() {
            return passivateObjectFail;
        }

        public boolean isValidateObjectFail() {
            return validateObjectFail;
        }

        @Override
        public PooledObject<Object> makeObject(final Object key) {
            final MethodCall call = new MethodCall("makeObject", key);
            methodCalls.add(call);
            final int originalCount = this.count++;
            if (makeObjectFail) {
                throw new PrivateException("makeObject");
            }
            // Deliberate choice to create new object in case future unit test
            // checks for a specific object
            final Integer obj = Integer.valueOf(originalCount);
            call.setReturned(obj);
            return new DefaultPooledObject<>(obj);
        }

        @Override
        public void passivateObject(final Object key, final PooledObject<Object> obj) {
            methodCalls.add(new MethodCall("passivateObject", key, obj.getObject()));
            if (passivateObjectFail) {
                throw new PrivateException("passivateObject");
            }
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

        public void setActivateObjectFail(final boolean activateObjectFail) {
            this.activateObjectFail = activateObjectFail;
        }

        public void setCurrentCount(final int count) {
            this.count = count;
        }

        public void setDestroyObjectFail(final boolean destroyObjectFail) {
            this.destroyObjectFail = destroyObjectFail;
        }

        public void setMakeObjectFail(final boolean makeObjectFail) {
            this.makeObjectFail = makeObjectFail;
        }

        public void setPassivateObjectFail(final boolean passivateObjectFail) {
            this.passivateObjectFail = passivateObjectFail;
        }

        public void setValidateObjectFail(final boolean validateObjectFail) {
            this.validateObjectFail = validateObjectFail;
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
    }

    private static final class TestFactory extends BaseKeyedPooledObjectFactory<Object, Object, RuntimeException> {
        @Override
        public Object create(final Object key) {
            return new Object();
        }

        @Override
        public PooledObject<Object> wrap(final Object value) {
            return new DefaultPooledObject<>(value);
        }
    }

    private static final String KEY = "key";

    private KeyedObjectPool<Object, Object, RuntimeException> pool;

    // Deliberate choice to create a new object in case future unit tests check
    // for a specific object.
    private final Integer ZERO = Integer.valueOf(0);

    private final Integer ONE = Integer.valueOf(1);

    private void clear(final FailingKeyedPooledObjectFactory factory, final List<MethodCall> expectedMethods) {
        factory.getMethodCalls().clear();
        expectedMethods.clear();
    }

    /**
     * Return what we expect to be the n<sup>th</sup> object (zero indexed) created by the pool for the given key.
     *
     * @param key Key for the object to be obtained
     * @param n   index of the object to be obtained
     * @return the requested object
     */
    protected abstract Object getNthObject(Object key, int n);

    protected abstract boolean isFifo();

    protected abstract boolean isLifo();

    /**
     * Creates an {@link KeyedObjectPool} instance that can contain at least <em>minCapacity</em> idle and active objects, or throw
     * {@link IllegalArgumentException} if such a pool cannot be created.
     *
     * @param <E> Type of exception thrown by this pool.
     * @param minCapacity Minimum capacity of the pool to create
     * @return the newly created keyed object pool
     */
    protected abstract <E extends Exception> KeyedObjectPool<Object, Object, E> makeEmptyPool(int minCapacity);

    /**
     * Creates an {@code KeyedObjectPool} with the specified factory. The pool should be in a default configuration and conform to the expected behaviors
     * described in {@link KeyedObjectPool}. Generally speaking there should be no limits on the various object counts.
     *
     * @param <E>     The type of exception thrown by the pool
     * @param factory Factory to use to associate with the pool
     * @return The newly created empty pool
     */
    protected abstract <E extends Exception> KeyedObjectPool<Object, Object, E> makeEmptyPool(KeyedPooledObjectFactory<Object, Object, E> factory);

    protected abstract Object makeKey(int n);

    private <E extends Exception> void reset(final KeyedObjectPool<Object, Object, E> pool, final FailingKeyedPooledObjectFactory factory,
            final List<MethodCall> expectedMethods) throws E {
        pool.clear();
        clear(factory, expectedMethods);
        factory.reset();
    }

    @AfterEach
    public void tearDown() {
        pool = null;
    }

    @Test
    public void testBaseAddObject() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object key = makeKey(0);
        try {
            assertEquals(0, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle(key));
            assertEquals(0, pool.getNumActive(key));
            pool.addObject(key);
            assertEquals(1, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle(key));
            assertEquals(0, pool.getNumActive(key));
            final Object obj = pool.borrowObject(key);
            assertEquals(getNthObject(key, 0), obj);
            assertEquals(0, pool.getNumIdle());
            assertEquals(1, pool.getNumActive());
            assertEquals(0, pool.getNumIdle(key));
            assertEquals(1, pool.getNumActive(key));
            pool.returnObject(key, obj);
            assertEquals(1, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle(key));
            assertEquals(0, pool.getNumActive(key));
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        } finally {
            pool.close();
        }
    }

    @Test
    public void testBaseBorrow() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        final Object keyb = makeKey(1);
        assertEquals(getNthObject(keya, 0), pool.borrowObject(keya), "1");
        assertEquals(getNthObject(keyb, 0), pool.borrowObject(keyb), "2");
        assertEquals(getNthObject(keyb, 1), pool.borrowObject(keyb), "3");
        assertEquals(getNthObject(keya, 1), pool.borrowObject(keya), "4");
        assertEquals(getNthObject(keyb, 2), pool.borrowObject(keyb), "5");
        assertEquals(getNthObject(keya, 2), pool.borrowObject(keya), "6");
        pool.close();
    }

    @Test
    public void testBaseBorrowReturn() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        Object obj0 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 0), obj0);
        Object obj1 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 1), obj1);
        Object obj2 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 2), obj2);
        pool.returnObject(keya, obj2);
        obj2 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 2), obj2);
        pool.returnObject(keya, obj1);
        obj1 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 1), obj1);
        pool.returnObject(keya, obj0);
        pool.returnObject(keya, obj2);
        obj2 = pool.borrowObject(keya);
        if (isLifo()) {
            assertEquals(getNthObject(keya, 2), obj2);
        }
        if (isFifo()) {
            assertEquals(getNthObject(keya, 0), obj2);
        }
        obj0 = pool.borrowObject(keya);
        if (isLifo()) {
            assertEquals(getNthObject(keya, 0), obj0);
        }
        if (isFifo()) {
            assertEquals(getNthObject(keya, 2), obj0);
        }
        pool.close();
    }

    @Test
    public void testBaseClear() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        final Object obj0 = pool.borrowObject(keya);
        final Object obj1 = pool.borrowObject(keya);
        assertEquals(2, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        pool.returnObject(keya, obj1);
        pool.returnObject(keya, obj0);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(2, pool.getNumIdle(keya));
        pool.clear(keya);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        final Object obj2 = pool.borrowObject(keya);
        assertEquals(getNthObject(keya, 2), obj2);
        pool.close();
    }

    @Test
    public void testBaseInvalidateObject() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        final Object obj0 = pool.borrowObject(keya);
        final Object obj1 = pool.borrowObject(keya);
        assertEquals(2, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        pool.invalidateObject(keya, obj0);
        assertEquals(1, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        pool.invalidateObject(keya, obj1);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        pool.close();
    }

    @Test
    public void testBaseNumActiveNumIdle() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        final Object obj0 = pool.borrowObject(keya);
        assertEquals(1, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        final Object obj1 = pool.borrowObject(keya);
        assertEquals(2, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        pool.returnObject(keya, obj1);
        assertEquals(1, pool.getNumActive(keya));
        assertEquals(1, pool.getNumIdle(keya));
        pool.returnObject(keya, obj0);
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(2, pool.getNumIdle(keya));

        assertEquals(0, pool.getNumActive("xyzzy12345"));
        assertEquals(0, pool.getNumIdle("xyzzy12345"));

        pool.close();
    }

    @Test
    public void testBaseNumActiveNumIdle2() {
        try {
            pool = makeEmptyPool(6);
        } catch (final UnsupportedOperationException uoe) {
            return; // skip this test if unsupported
        }
        final Object keya = makeKey(0);
        final Object keyb = makeKey(1);
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        assertEquals(0, pool.getNumActive(keyb));
        assertEquals(0, pool.getNumIdle(keyb));

        final Object objA0 = pool.borrowObject(keya);
        final Object objB0 = pool.borrowObject(keyb);

        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        assertEquals(1, pool.getNumActive(keyb));
        assertEquals(0, pool.getNumIdle(keyb));

        final Object objA1 = pool.borrowObject(keya);
        final Object objB1 = pool.borrowObject(keyb);

        assertEquals(4, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        assertEquals(2, pool.getNumActive(keya));
        assertEquals(0, pool.getNumIdle(keya));
        assertEquals(2, pool.getNumActive(keyb));
        assertEquals(0, pool.getNumIdle(keyb));

        pool.returnObject(keya, objA0);
        pool.returnObject(keyb, objB0);

        assertEquals(2, pool.getNumActive());
        assertEquals(2, pool.getNumIdle());
        assertEquals(1, pool.getNumActive(keya));
        assertEquals(1, pool.getNumIdle(keya));
        assertEquals(1, pool.getNumActive(keyb));
        assertEquals(1, pool.getNumIdle(keyb));

        pool.returnObject(keya, objA1);
        pool.returnObject(keyb, objB1);

        assertEquals(0, pool.getNumActive());
        assertEquals(4, pool.getNumIdle());
        assertEquals(0, pool.getNumActive(keya));
        assertEquals(2, pool.getNumIdle(keya));
        assertEquals(0, pool.getNumActive(keyb));
        assertEquals(2, pool.getNumIdle(keyb));

        pool.close();
    }

    @Test
    public void testClosedPoolBehavior() {
        final KeyedObjectPool<Object, Object, RuntimeException> pool;
        try {
            pool = makeEmptyPool(new TestFactory());
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }

        final Object o1 = pool.borrowObject(KEY);
        final Object o2 = pool.borrowObject(KEY);

        pool.close();

        assertThrows(IllegalStateException.class, () -> pool.addObject(KEY), "A closed pool must throw an IllegalStateException when addObject is called.");

        assertThrows(IllegalStateException.class, () -> pool.borrowObject(KEY),
                "A closed pool must throw an IllegalStateException when borrowObject is called.");

        // The following should not throw exceptions just because the pool is closed.
        assertEquals(0, pool.getNumIdle(KEY), "A closed pool shouldn't have any idle objects.");
        assertEquals(0, pool.getNumIdle(), "A closed pool shouldn't have any idle objects.");
        pool.getNumActive();
        pool.getNumActive(KEY);
        pool.returnObject(KEY, o1);
        assertEquals(0, pool.getNumIdle(KEY), "returnObject should not add items back into the idle object pool for a closed pool.");
        assertEquals(0, pool.getNumIdle(), "returnObject should not add items back into the idle object pool for a closed pool.");
        pool.invalidateObject(KEY, o2);
        pool.clear(KEY);
        pool.clear();
        pool.close();
    }

    @Test
    public void testKPOFAddObjectUsage() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        // addObject should make a new object, passivate it and put it in the pool
        pool.addObject(KEY);
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ZERO));
        expectedMethods.add(new MethodCall("passivateObject", KEY, ZERO));
        assertEquals(expectedMethods, factory.getMethodCalls());

        // Test exception handling of addObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(true);
        assertThrows(PrivateException.class, () -> pool.addObject(KEY), "Expected addObject to propagate makeObject exception.");
        expectedMethods.add(new MethodCall("makeObject", KEY));
        assertEquals(expectedMethods, factory.getMethodCalls());

        clear(factory, expectedMethods);

        // passivateObject Exceptions should be propagated to client code from addObject
        factory.setMakeObjectFail(false);
        factory.setPassivateObjectFail(true);
        assertThrows(PrivateException.class, () -> pool.addObject(KEY), "Expected addObject to propagate passivateObject exception.");
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("passivateObject", KEY, ONE));
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFBorrowObjectUsages() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        if (pool instanceof GenericKeyedObjectPool) {
            ((GenericKeyedObjectPool<Object, Object, PrivateException>) pool).setTestOnBorrow(true);
        }

        // Test correct behavior code paths

        // existing idle object should be activated and validated
        pool.addObject(KEY);
        clear(factory, expectedMethods);
        final Object obj = pool.borrowObject(KEY);
        expectedMethods.add(new MethodCall("activateObject", KEY, ZERO));
        expectedMethods.add(new MethodCall("validateObject", KEY, ZERO).returned(Boolean.TRUE));
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.returnObject(KEY, obj);

        // Test exception handling of borrowObject
        reset(pool, factory, expectedMethods);

        // makeObject Exceptions should be propagated to client code from borrowObject
        factory.setMakeObjectFail(true);
        assertThrows(PrivateException.class, () -> pool.borrowObject(KEY), "Expected borrowObject to propagate makeObject exception.");
        expectedMethods.add(new MethodCall("makeObject", KEY));
        assertEquals(expectedMethods, factory.getMethodCalls());

        // when activateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject(KEY);
        clear(factory, expectedMethods);

        factory.setActivateObjectFail(true);
        expectedMethods.add(new MethodCall("activateObject", KEY, obj));
        assertThrows(NoSuchElementException.class, () -> pool.borrowObject(KEY));
        // After idle object fails validation, new on is created and activation
        // fails again for the new one.
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", KEY, ONE));
        AbstractTestObjectPool.removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());

        // when validateObject fails in borrowObject, a new object should be borrowed/created
        reset(pool, factory, expectedMethods);
        pool.addObject(KEY);
        clear(factory, expectedMethods);

        factory.setValidateObjectFail(true);
        // testOnBorrow is on, so this will throw when the newly created instance
        // fails validation
        assertThrows(NoSuchElementException.class, () -> pool.borrowObject(KEY));
        // Activate, then validate for idle instance
        expectedMethods.add(new MethodCall("activateObject", KEY, ZERO));
        expectedMethods.add(new MethodCall("validateObject", KEY, ZERO));
        // Make new instance, activate succeeds, validate fails
        expectedMethods.add(new MethodCall("makeObject", KEY).returned(ONE));
        expectedMethods.add(new MethodCall("activateObject", KEY, ONE));
        expectedMethods.add(new MethodCall("validateObject", KEY, ONE));
        AbstractTestObjectPool.removeDestroyObjectCall(factory.getMethodCalls());
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFClearUsages() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        // Test correct behavior code paths
        pool.addObjects(KEY, 5);
        pool.clear();

        // Test exception handling clear should swallow destroy object failures
        reset(pool, factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        pool.addObjects(KEY, 5);
        pool.clear();
        pool.close();
    }

    @Test
    public void testKPOFCloseUsages() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        // Test correct behavior code paths
        pool.addObjects(KEY, 5);
        pool.close();

        // Test exception handling close should swallow failures
        try (KeyedObjectPool<Object, Object, PrivateException> pool2 = makeEmptyPool(factory)) {
            reset(pool2, factory, expectedMethods);
            factory.setDestroyObjectFail(true);
            pool2.addObjects(KEY, 5);
        }
    }

    @Test
    public void testKPOFInvalidateObjectUsages() throws InterruptedException {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();

        // Test correct behavior code paths

        final Object obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);

        // invalidated object should be destroyed
        pool.invalidateObject(KEY, obj);
        expectedMethods.add(new MethodCall("destroyObject", KEY, obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        // Test exception handling of invalidateObject
        reset(pool, factory, expectedMethods);
        final Object obj2 = pool.borrowObject(KEY);
        clear(factory, expectedMethods);
        factory.setDestroyObjectFail(true);
        assertThrows(PrivateException.class, () -> pool.invalidateObject(KEY, obj2), "Expecting destroy exception to propagate");
        Thread.sleep(250); // could be defered
        AbstractTestObjectPool.removeDestroyObjectCall(factory.getMethodCalls());
        assertEquals(expectedMethods, factory.getMethodCalls());
        pool.close();
    }

    @Test
    public void testKPOFReturnObjectUsages() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        final KeyedObjectPool<Object, Object, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
        final List<MethodCall> expectedMethods = new ArrayList<>();
        Object obj;

        // Test correct behavior code paths
        obj = pool.borrowObject(KEY);
        clear(factory, expectedMethods);

        // returned object should be passivated
        pool.returnObject(KEY, obj);
        expectedMethods.add(new MethodCall("passivateObject", KEY, obj));
        assertEquals(expectedMethods, factory.getMethodCalls());

        // Test exception handling of returnObject
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
        AbstractTestObjectPool.removeDestroyObjectCall(factory.getMethodCalls()); // The exact timing of destroyObject is flexible here.
        assertEquals(expectedMethods, factory.getMethodCalls());
        assertEquals(1, pool.getNumIdle(KEY)); // Not added
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
    public void testToString() {
        final FailingKeyedPooledObjectFactory factory = new FailingKeyedPooledObjectFactory();
        try (KeyedObjectPool<Object, Object, PrivateException> pool = makeEmptyPool(factory)) {
            pool.toString();
        } catch (final UnsupportedOperationException uoe) {
            return; // test not supported
        }
    }
}
