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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.pool3.impl.GenericObjectPool;
import org.apache.commons.pool3.impl.SoftReferenceObjectPool;
import org.junit.jupiter.api.Test;

/**
 * Abstract test case for {@link ObjectPool} implementations.
 */
public abstract class AbstractTestObjectPool {

    /**
     * Create an {@code ObjectPool} with the specified factory.
     * The pool should be in a default configuration and conform to the expected
     * behaviors described in {@link ObjectPool}.
     * Generally speaking there should be no limits on the various object counts.
     *
     * @param <E> The exception type throws by the pool
     * @param factory The factory to be used by the object pool
     * @return the newly created empty pool
     * @throws UnsupportedOperationException if the pool being tested does not
     *                                       follow pool contracts.
     */
    protected abstract <T, E extends Exception> ObjectPool<T, E> makeEmptyPool(PooledObjectFactory<T, E> factory) throws UnsupportedOperationException;

    @Test
    public void testClosedPoolBehavior() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            final String o1 = pool.borrowObject();
            final String o2 = pool.borrowObject();

            pool.close();

            assertThrows(IllegalStateException.class, pool::addObject, "A closed pool must throw an IllegalStateException when addObject is called.");

            assertThrows(IllegalStateException.class, pool::borrowObject, "A closed pool must throw an IllegalStateException when borrowObject is called.");

            // The following should not throw exceptions just because the pool is closed.
            if (pool.getNumIdle() >= 0) {
                assertEquals( 0, pool.getNumIdle(),"A closed pool shouldn't have any idle objects.");
            }
            if (pool.getNumActive() >= 0) {
                assertEquals( 2, pool.getNumActive(),"A closed pool should still keep count of active objects.");
            }
            pool.returnObject(o1);
            if (pool.getNumIdle() >= 0) {
                assertEquals( 0, pool.getNumIdle(),"returnObject should not add items back into the idle object pool for a closed pool.");
            }
            if (pool.getNumActive() >= 0) {
                assertEquals( 1, pool.getNumActive(),"A closed pool should still keep count of active objects.");
            }
            pool.invalidateObject(o2);
            if (pool.getNumIdle() >= 0) {
                assertEquals( 0, pool.getNumIdle(),"invalidateObject must not add items back into the idle object pool.");
            }
            if (pool.getNumActive() >= 0) {
                assertEquals( 0, pool.getNumActive(),"A closed pool should still keep count of active objects.");
            }
            pool.clear();
            pool.close();
        });

    }

    @Test
    void testPOFAddObjectMakeThrowsException(){
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            // makeObject Exceptions should be propagated to client code from addObject
            doThrow(new PrivateException("makeObject")).when(factory).makeObject();

            final Exception ex = assertThrows(PrivateException.class, pool::addObject, "Expected addObject to propagate makeObject exception.");
            assertEquals("makeObject", ex.getMessage());

            verify(factory).makeObject();
        });
    }

    @Test
    void testPOFAddObjectPassivateThrowsException() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            // passivateObject Exceptions should be propagated to client code from addObject
            doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(any());
            final Exception ex = assertThrows(PrivateException.class, pool::addObject, "Expected addObject to propagate passivateObject exception.");
            assertEquals("passivateObject", ex.getMessage());

            verify(factory).makeObject();

            // StackObjectPool, SofReferenceObjectPool also validate on add
            if (pool instanceof SoftReferenceObjectPool) {
                verify(factory).validateObject(any());
            }
            verify(factory).passivateObject(any());
        });
    }

    @Test
    public void testPOFAddObjectUsage() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            assertEquals(0, pool.getNumActive());
            assertEquals(0, pool.getNumIdle());

            // addObject should make a new object, passivate it and put it in the pool
            pool.addObject();


            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle());
            verify(factory).makeObject();

            // StackObjectPool, SoftReferenceObjectPool also validate on add
            if (pool instanceof SoftReferenceObjectPool) {
                verify(factory).validateObject(any());
            }

            verify(factory).passivateObject(any());
        });
    }

    @Test
    void testPOFBorrowObjectMakeThrowsException(){
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            if (pool instanceof GenericObjectPool) {
                ((GenericObjectPool<String, PrivateException>) pool).setTestOnBorrow(true);
            }

            // makeObject Exceptions should be propagated to client code from borrowObject
            doThrow(new PrivateException("makeObject")).when(factory).makeObject();

            final Exception ex = assertThrows(PrivateException.class, pool::borrowObject, "Expected borrowObject to propagate makeObject exception.");
            assertEquals("makeObject", ex.getMessage());
            verify(factory).makeObject();
        });
    }

    @Test
    void testPOFBorrowObjectActivateThrowsException() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            if (pool instanceof GenericObjectPool) {
                ((GenericObjectPool<String, PrivateException>) pool).setTestOnBorrow(true);
            }

            // when activateObject fails in borrowObject, a new object should be borrowed/created
            pool.addObject();

            doThrow(new PrivateException("activateObject")).when(factory).activateObject(any());

            assertThrows(NoSuchElementException.class, pool::borrowObject, "Expecting NoSuchElementException");

            // Idle object fails activation, new one created, also fails
            verify(factory, times(2)).makeObject();
            verify(factory, times(2)).activateObject(any());

        });
    }

    @Test
    void testPOFBorrowObjectValidateThrowsException() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            if (pool instanceof GenericObjectPool) {
                ((GenericObjectPool<String, PrivateException>) pool).setTestOnBorrow(true);
            }

            // when validateObject fails in borrowObject, a new object should be borrowed/created
            pool.addObject();

            doThrow(new PrivateException("validateObject")).when(factory).validateObject(any());

            // Expected NoSuchElementException - newly created object will also fail to validate
            assertThrows(NoSuchElementException.class, pool::borrowObject, "Expecting NoSuchElementException");

            // Idle object is activated, but fails validation.
            // New instance is created, activated and then fails validation

            verify(factory, times(2)).makeObject();
            verify(factory, times(2)).activateObject(any());
            //verify(factory, times(2)).validateObject(any());


        });
    }

    @Test
    public void testPOFBorrowObjectUsages() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            if (pool instanceof GenericObjectPool) {
                ((GenericObjectPool<String, PrivateException>) pool).setTestOnBorrow(true);
            }

            // Test correct behavior code paths
            // existing idle object should be activated and validated
            pool.addObject();
            String obj = pool.borrowObject();

            verify(factory).activateObject(any());
            //verify(factory, times(1)).validateObject(any());
            pool.returnObject(obj);

        });

    }

    @Test
    public void testPOFClearUsages() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            // Test correct behavior code paths
            pool.addObjects(5);
            pool.clear();

            // Test exception handling clear should swallow destroy object failures
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(any());

            assertDoesNotThrow(() -> {
                pool.addObjects(5);
                pool.clear();
            });

        });

    }

    @Test
    public void testPOFCloseUsages() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        ObjectPool<String, PrivateException> pool;
        try {
            pool = makeEmptyPool(factory);

            // Test correct behavior code paths
            pool.addObjects(5);
            pool.close();

            // Test exception handling close should swallow failures
            try {
                pool = makeEmptyPool(factory);
            } catch (final UnsupportedOperationException uoe) {
                return; // test not supported
            }
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(any());

            pool.addObjects(5);
            assertDoesNotThrow(pool::close);
        } catch (final UnsupportedOperationException ignored) {
            // test not supported
        }

    }

    @Test
    public void testPOFInvalidateObjectUsages() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            // Test correct behavior code paths

            String obj = pool.borrowObject();

            // invalidated object should be destroyed
            pool.invalidateObject(obj);
            verify(factory).destroyObject(any());

            // Test exception handling of invalidateObject

            final String obj2 = pool.borrowObject();

            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(any());

            assertThrows(PrivateException.class, () -> pool.invalidateObject(obj2));

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                // This block will be executed after the delay
            }, 250, TimeUnit.MILLISECONDS);
            scheduler.shutdown();
        });

    }

    @Test
    public void testPOFReturnObjectUsages() {
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            // Test correct behavior code paths
            String obj = pool.borrowObject();

            // returned object should be passivated
            pool.returnObject(obj);

            // StackObjectPool, SoftReferenceObjectPool also validate on return
            if (pool instanceof SoftReferenceObjectPool) {
                verify(factory, times(2)).validateObject(any());
            }
            verify(factory).passivateObject(any());
        });

    }

    @Test
    void testPOFReturnObjectPassivateThrowsException(){
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {
            pool.addObject();
            pool.addObject();
            pool.addObject();
            assertEquals(3, pool.getNumIdle());
            verify(factory, times(3)).passivateObject(any());
            verify(factory, times(3)).validateObject(any());


            // passivateObject should swallow exceptions and not add the object to the pool
            String obj = pool.borrowObject();
            pool.borrowObject();

            assertEquals(1, pool.getNumIdle());
            assertEquals(2, pool.getNumActive());
            verify(factory, times(5)).validateObject(any());

            doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(any());
            pool.returnObject(obj);

            // StackObjectPool, SoftReferenceObjectPool also validate on return
            if (pool instanceof SoftReferenceObjectPool) {
                verify(factory, times(6)).validateObject(any());
            }
            verify(factory, times(4)).passivateObject(any());
            assertEquals(1, pool.getNumIdle());   // Not returned
            assertEquals(1, pool.getNumActive()); // But not in active count
        });
    }

    @Test
    void testPOFReturnObjectDestroyThrowsException(){
        final MethodCallPoolableObjectFactory<String> factory = spy(new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString()));
        withPool(factory, pool -> {

            // destroyObject should swallow exceptions too
            String  obj = pool.borrowObject();

            doThrow(new PrivateException("passivateObject")).when(factory).passivateObject(any());
            doThrow(new PrivateException("destroyObject")).when(factory).destroyObject(any());

            assertDoesNotThrow(() -> pool.returnObject(obj));
        });
    }

    @Test
    public void testToString() {
        final PooledObjectFactory<String, PrivateException> factory = new MethodCallPoolableObjectFactory<>(() -> UUID.randomUUID().toString());
        withPool(factory, pool -> assertDoesNotThrow(pool::toString));
    }

    private<K, E extends Exception> void withPool(PooledObjectFactory<K, E> factory, Consumer<ObjectPool<K, E>> poolConsumer) {
        try (final ObjectPool<K, E> pool = makeEmptyPool(factory)) {
            poolConsumer.accept(pool);
        }
        catch (final UnsupportedOperationException ignore) {
            // test not supported
        }
    }
}
