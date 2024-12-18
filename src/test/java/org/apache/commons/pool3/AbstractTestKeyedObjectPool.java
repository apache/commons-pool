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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Abstract test case for {@link ObjectPool} implementations.
 */
public abstract class AbstractTestKeyedObjectPool {

    protected static class FailingKeyedPooledObjectFactory<K>
        implements KeyedPooledObjectFactory<K, Integer, PrivateException> {

        private int counter = 0;

        @Override
        public void activateObject(final K key, final PooledObject<Integer> obj) {
            // do nothing
        }

        @Override
        public void destroyObject(final K key, final PooledObject<Integer> obj) {
            // do nothing
        }

        @Override
        public PooledObject<Integer> makeObject(final K key) {
            // Deliberate choice to create new object in case future unit test
            // checks for a specific object
            return new DefaultPooledObject<>(counter++);
        }

        @Override
        public void passivateObject(final K key, final PooledObject<Integer> obj) {
            // do nothing
        }

        @Override
        public boolean validateObject(final K key, final PooledObject<Integer> obj) {
            return true;
        }
    }

    private static final class TestFactory
        extends BaseKeyedPooledObjectFactory<String, String, RuntimeException> {

        @Override
        public String create(final String key) {
            return new Object().toString();
        }

        @Override
        public PooledObject<String> wrap(final String value) {
            return new DefaultPooledObject<>(value);
        }
    }

    protected static final String KEY = "key";

    /**
     * Return what we expect to be the n<sup>th</sup>
     * object (zero indexed) created by the pool
     * for the given key.
     * @param key Key for the object to be obtained
     * @param n   index of the object to be obtained
     * @return the requested object
     */
    protected abstract Object getNthObject(Object key, int n);

    protected abstract boolean isFifo();

    protected abstract boolean isLifo();

    /**
     * Creates an {@link KeyedObjectPool} instance
     * that can contain at least <em>minCapacity</em>
     * idle and active objects, or
     * throw {@link IllegalArgumentException}
     * if such a pool cannot be created.
     * @param minCapacity Minimum capacity of the pool to create
     * @return the newly created keyed object pool
     */
    protected abstract <K, V, E extends Exception> KeyedObjectPool<K, V, E> makeEmptyPool(int minCapacity);

    /**
     * Creates an {@code KeyedObjectPool} with the specified factory.
     * The pool should be in a default configuration and conform to the expected
     * behaviors described in {@link KeyedObjectPool}.
     * Generally speaking there should be no limits on the various object counts.
     *
     * @param <E>     The type of exception thrown by the pool
     * @param factory Factory to use to associate with the pool
     * @return The newly created empty pool
     */
    protected abstract <K, V, E extends Exception> KeyedObjectPool<K, V, E> makeEmptyPool(KeyedPooledObjectFactory<K, V, E> factory);

    protected abstract <K> K makeKey(K n);

    @Test
    public void testBaseAddObject() {
        withPool(3, pool -> {
            final String key = makeKey("0");
            assertEquals(0, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle(key));
            assertEquals(0, pool.getNumActive(key));
            pool.addObject(key);
            assertEquals(1, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle(key));
            assertEquals(0, pool.getNumActive(key));
            final String obj = pool.borrowObject(key);
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
        });
    }

    @Test
    public void testBaseBorrow() {
        withPool(3, pool -> {
            final String keyA = makeKey("0");
            final String keyB = makeKey("1");
            assertEquals(getNthObject(keyA, 0), pool.borrowObject(keyA), "1");
            assertEquals(getNthObject(keyB, 0), pool.borrowObject(keyB), "2");
            assertEquals(getNthObject(keyB, 1), pool.borrowObject(keyB), "3");
            assertEquals(getNthObject(keyA, 1), pool.borrowObject(keyA), "4");
            assertEquals(getNthObject(keyB, 2), pool.borrowObject(keyB), "5");
            assertEquals(getNthObject(keyA, 2), pool.borrowObject(keyA), "6");
        });
    }

    @Test
    public void testBaseBorrowReturn() {
        withPool(3, pool -> {
            final String keyA = makeKey("0");
            String obj0 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 0), obj0);
            String obj1 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 1), obj1);
            String obj2 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 2), obj2);
            pool.returnObject(keyA, obj2);
            obj2 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 2), obj2);
            pool.returnObject(keyA, obj1);
            obj1 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 1), obj1);
            pool.returnObject(keyA, obj0);
            pool.returnObject(keyA, obj2);
            obj2 = pool.borrowObject(keyA);
            if (isLifo()) {
                assertEquals(getNthObject(keyA, 2), obj2);
            }
            if (isFifo()) {
                assertEquals(getNthObject(keyA, 0), obj2);
            }
            obj0 = pool.borrowObject(keyA);
            if (isLifo()) {
                assertEquals(getNthObject(keyA, 0), obj0);
            }
            if (isFifo()) {
                assertEquals(getNthObject(keyA, 2), obj0);
            }
        });
    }

    @Test
    public void testBaseClear() {
        withPool(3, pool -> {
            final String keyA = makeKey("0");
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            final String obj0 = pool.borrowObject(keyA);
            final String obj1 = pool.borrowObject(keyA);
            assertEquals(2, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            pool.returnObject(keyA, obj1);
            pool.returnObject(keyA, obj0);
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(2, pool.getNumIdle(keyA));
            pool.clear(keyA);
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            final Object obj2 = pool.borrowObject(keyA);
            assertEquals(getNthObject(keyA, 2), obj2);
        });
    }

    @Test
    public void testBaseInvalidateObject() {
        withPool(3, pool -> {
            final String keyA = makeKey("0");
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            final String obj0 = pool.borrowObject(keyA);
            final String obj1 = pool.borrowObject(keyA);
            assertEquals(2, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            pool.invalidateObject(keyA, obj0);
            assertEquals(1, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            pool.invalidateObject(keyA, obj1);
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
        });
    }

    @Test
    public void testBaseNumActiveNumIdle() {
        withPool(3, pool -> {
            final String keyA = makeKey("0");
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            final String obj0 = pool.borrowObject(keyA);
            assertEquals(1, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            final String obj1 = pool.borrowObject(keyA);
            assertEquals(2, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            pool.returnObject(keyA, obj1);
            assertEquals(1, pool.getNumActive(keyA));
            assertEquals(1, pool.getNumIdle(keyA));
            pool.returnObject(keyA, obj0);
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(2, pool.getNumIdle(keyA));

            assertEquals(0, pool.getNumActive("test12345"));
            assertEquals(0, pool.getNumIdle("test12345"));
        });
    }

    @Test
    public void testBaseNumActiveNumIdle2() {

        withPool(6, pool -> {
            final String keyA  = makeKey("0");
            final String keyB = makeKey("1");
            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle());
            assertEquals(0, pool.getNumActive(keyA));
            assertEquals(0, pool.getNumIdle(keyA));
            assertEquals(0, pool.getNumActive(keyB));
            assertEquals(0, pool.getNumIdle(keyB));

            assertDoesNotThrow(() -> {
                final String objA0 = pool.borrowObject(keyA);
                final String objB0 = pool.borrowObject(keyB);

                assertEquals(2, pool.getNumActive());
                assertEquals(0, pool.getNumIdle());
                assertEquals(1, pool.getNumActive(keyA));
                assertEquals(0, pool.getNumIdle(keyA));
                assertEquals(1, pool.getNumActive(keyB));
                assertEquals(0, pool.getNumIdle(keyB));

                final String objA1 = pool.borrowObject(keyA);
                final String objB1 = pool.borrowObject(keyB);

                assertEquals(4, pool.getNumActive());
                assertEquals(0, pool.getNumIdle());
                assertEquals(2, pool.getNumActive(keyA));
                assertEquals(0, pool.getNumIdle(keyA));
                assertEquals(2, pool.getNumActive(keyB));
                assertEquals(0, pool.getNumIdle(keyB));

                pool.returnObject(keyA, objA0);
                pool.returnObject(keyB, objB0);

                assertEquals(2, pool.getNumActive());
                assertEquals(2, pool.getNumIdle());
                assertEquals(1, pool.getNumActive(keyA));
                assertEquals(1, pool.getNumIdle(keyA));
                assertEquals(1, pool.getNumActive(keyB));
                assertEquals(1, pool.getNumIdle(keyB));

                pool.returnObject(keyA, objA1);
                pool.returnObject(keyB, objB1);

                assertEquals(0, pool.getNumActive());
                assertEquals(4, pool.getNumIdle());
                assertEquals(0, pool.getNumActive(keyA));
                assertEquals(2, pool.getNumIdle(keyA));
                assertEquals(0, pool.getNumActive(keyB));
                assertEquals(2, pool.getNumIdle(keyB));
            });
        });
    }

    @Test
    public void testClosedPoolBehavior() {
        withPool(new TestFactory(), pool -> {
            final String o1 = pool.borrowObject(KEY);
            final String o2 = pool.borrowObject(KEY);

            pool.close();

            assertThrows(IllegalStateException.class, () -> pool.addObject(KEY),
                "A closed pool must throw an IllegalStateException when addObject is called.");

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
        });
    }

    @Test
    void testKPOFAddPassivateObjectThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(eq(KEY), any());

        withPool(factory, pool -> {
            // passivateObject Exceptions should be propagated to client code from addObject
            final Exception ex = assertThrows(PrivateException.class, () -> pool.addObject(KEY),
                "Expected addObject to propagate passivateObject exception.");
            assertEquals("passivateObject", ex.getMessage());
            verify(factory, times(1)).passivateObject(eq(KEY), any());
        });
    }

    @Test
    void testKPOFAddMakeObjectThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        doThrow(new PrivateException("makeObject")).when(factory).makeObject(KEY);

        withPool(factory, pool -> {

            // makeObject Exceptions should be propagated to client code from addObject
            final Exception ex = assertThrows(PrivateException.class, () -> pool.addObject(KEY),
                "Expected addObject to propagate makeObject exception.");
            assertEquals("makeObject", ex.getMessage());
            verify(factory).makeObject(KEY);
        });
    }

    @Test
    public void testKPOFAddObjectUsage() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());

        withPool(factory, pool -> {
            // addObject should make a new object, passivate it and put it in the pool
            pool.addObject(KEY);
            Mockito.verify(factory).makeObject(KEY);
            Mockito.verify(factory).passivateObject(eq(KEY), any());
        });
    }

    @Test
    void testKPOFBorrowObjectUsages() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {
            Integer obj;

            if (pool instanceof GenericKeyedObjectPool) {
                ((GenericKeyedObjectPool<String, Integer, PrivateException>) pool).setTestOnBorrow(true);
            }

            // Test correct behavior code paths

            // existing idle object should be activated and validated
            pool.addObject(KEY);
            obj = pool.borrowObject(KEY);

            verify(factory).activateObject(eq(KEY), any());
            verify(factory).validateObject(eq(KEY), any());
            pool.returnObject(KEY, obj);

        });

    }

    @Test
    void testKPOFBorrowMakeObjectThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        doThrow(new PrivateException("makeObject")).when(factory).makeObject(KEY);

        withPool(factory, pool -> {

            // makeObject Exceptions should be propagated to client code from borrowObject
            final Exception ex = assertThrows(PrivateException.class, () -> pool.borrowObject(KEY),
                "Expected borrowObject to propagate validateObject exception.");
            assertEquals("makeObject", ex.getMessage());
            verify(factory, times(1)).makeObject(KEY);

       });
    }

    @Test
    void testKPOFBorrowObjectValidateObjectThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        doThrow(new PrivateException("validateObject")).when(factory).validateObject(eq(KEY), any());

        withPool(factory, pool -> {

            if (pool instanceof GenericKeyedObjectPool) {
                ((GenericKeyedObjectPool<String, Integer, PrivateException>) pool).setTestOnBorrow(true);
            }

            // when validateObject fails in borrowObject, a new object should be borrowed/created
            pool.addObject(KEY);

            doThrow(new PrivateException("validateObject")).when(factory).validateObject(eq(KEY), any());

            // testOnBorrow is on, so this will throw when the newly created instance
            // fails validation
            assertThrows(NoSuchElementException.class, () -> pool.borrowObject(KEY));

            // Activate, then validate for idle instance
            verify(factory, times(2)).activateObject(eq(KEY), any());
            verify(factory, times(2)).validateObject(eq(KEY), any());

            // Make new instance, activate succeeds, validate fails
            verify(factory, times(2)).makeObject(KEY);

        });
    }

    @Test
    public void testKPOFBorrowActivateObjectThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {

            if (pool instanceof GenericKeyedObjectPool) {
                ((GenericKeyedObjectPool<String, Integer, PrivateException>) pool).setTestOnBorrow(true);
            }

            // when activateObject fails in borrowObject, a new object should be borrowed/created
            pool.addObject(KEY);
            doThrow(new PrivateException("activateObject")).when(factory).activateObject(eq(KEY), any());

            assertThrows(NoSuchElementException.class, () -> pool.borrowObject(KEY));

            // After idle object fails validation, new on is created and activation
            // fails again for the new one.
            verify(factory, times(2)).activateObject(eq(KEY), any());
            verify(factory, times(2)).makeObject(KEY);

    });
    }

    @Test
    public void testKPOFClearUsages() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {
            // Test correct behavior code paths
            pool.addObjects(KEY, 5);
            pool.clear();

            // Test exception handling clear should swallow destroy object failures
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(eq(KEY), any());
            assertDoesNotThrow(() -> {
                pool.addObjects(KEY, 5);
                pool.clear();
            });

    });
    }

    @Test
    public void testKPOFCloseUsages() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());

        withPool(factory, pool -> {
            // Test correct behavior code paths
            // Test correct behavior code paths
            pool.addObjects(KEY, 5);
            pool.close();

            // Test exception handling close should swallow failures
            try (final KeyedObjectPool<String, Integer, PrivateException> pool2 = makeEmptyPool(factory)) {
                doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(eq(KEY), any());
                assertDoesNotThrow(() -> pool2.addObjects(KEY, 5));
            }
        });
    }

    @Test
    public void testKPOFInvalidateObjectUsages() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {
            Integer obj;

            // Test correct behavior code paths
            obj = pool.borrowObject(KEY);

            // invalidated object should be destroyed
            pool.invalidateObject(KEY, obj);
            verify(factory).destroyObject(eq(KEY), any());

            // Test exception handling of invalidateObject
            final Integer obj2 = pool.borrowObject(KEY);
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(eq(KEY), any());
            assertThrows(PrivateException.class, () -> pool.invalidateObject(KEY, obj2), "Expecting destroy exception to propagate");
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                // This block will be executed after the delay
            }, 250, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
            }
        );
    }

    @Test
    public void testKPOFReturnObjectPassivateThrowsException() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {

            // passivateObject should swallow exceptions and not add the object to the pool
            pool.addObject(KEY);
            pool.addObject(KEY);
            pool.addObject(KEY);

            verify(factory, times(3)).passivateObject(eq(KEY), any());
            assertEquals(3, pool.getNumIdle(KEY));

            pool.borrowObject(KEY);
            Integer obj = pool.borrowObject(KEY);

            assertEquals(1, pool.getNumIdle(KEY));
            assertEquals(2, pool.getNumActive(KEY));

            doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(eq(KEY), any());

            pool.returnObject(KEY, obj);

            verify(factory, times(4)).passivateObject(eq(KEY), any());
            assertEquals(1, pool.getNumIdle(KEY));   // Not added
            assertEquals(1, pool.getNumActive(KEY)); // But not active

            obj = pool.borrowObject(KEY);
            doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(eq(KEY), any());
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(eq(KEY), any());
            try {
                pool.returnObject(KEY, obj);
                assertInstanceOf(GenericKeyedObjectPool.class, pool,
                    "Expecting destroyObject exception to be propagated");
            }
            catch (final PrivateException ex) {
                // Expected
            }
        });
    }

    @Test
    public void testKPOFReturnObjectUsages() {
        final FailingKeyedPooledObjectFactory<String> factory = spy(new FailingKeyedPooledObjectFactory<>());
        withPool(factory, pool -> {
                // Test correct behavior code paths
                Integer obj = pool.borrowObject(KEY);

                // returned object should be passivated
                pool.returnObject(KEY, obj);
                verify(factory).passivateObject(eq(KEY), any());
            }
        );
    }

    @Test
    void testToString() {
        final FailingKeyedPooledObjectFactory<String> factory = new FailingKeyedPooledObjectFactory<>();
        withPool(factory, pool -> assertDoesNotThrow(pool::toString));
    }

    private<K, V, E extends Exception> void withPool(KeyedPooledObjectFactory<K, V, E> factory, Consumer<KeyedObjectPool<K, V, E>> poolConsumer) {
        try (final KeyedObjectPool<K, V, E> pool = makeEmptyPool(factory)) {
            poolConsumer.accept(pool);
        }
        catch (final UnsupportedOperationException ignore) {
            // test not supported
        }
    }

    private void withPool(int minCapacity, Consumer<KeyedObjectPool<String, String, RuntimeException>> poolConsumer) {
        try (final KeyedObjectPool<String, String, RuntimeException> pool = makeEmptyPool(minCapacity)) {
            poolConsumer.accept(pool);
        } catch (final UnsupportedOperationException ignore) {
            // test not supported
        }
    }
}