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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.TestGenericKeyedObjectPool;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Unit tests for {@link PoolUtils}.
 *
 * TODO Replace our own mocking with a mocking library like Mockito.
 */
public class TestPoolUtils {

    private static class MethodCallLogger implements InvocationHandler {
        private final List<String> calledMethods;

        MethodCallLogger(final List<String> calledMethods) {
            this.calledMethods = calledMethods;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (calledMethods == null) {
                return null;
            }
            calledMethods.add(method.getName());
            if (boolean.class.equals(method.getReturnType())) {
                return Boolean.FALSE;
            }
            if (int.class.equals(method.getReturnType())) {
                return Integer.valueOf(0);
            }
            if (long.class.equals(method.getReturnType())) {
                return Long.valueOf(0);
            }
            if (Object.class.equals(method.getReturnType())) {
                return new Object();
            }
            if (PooledObject.class.equals(method.getReturnType())) {
                return new DefaultPooledObject<>(new Object());
            }
            return null;
        }
    }

    /** Period between checks for minIdle tests. Increase this if you happen to get too many false failures. */
    private static final int CHECK_PERIOD = 300;

    /** Times to let the minIdle check run. */
    private static final int CHECK_COUNT = 4;

    /** Sleep time to let the minIdle tests run CHECK_COUNT times. */
    private static final int CHECK_SLEEP_PERIOD = CHECK_PERIOD * (CHECK_COUNT - 1) + CHECK_PERIOD / 2;

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(final Class<T> clazz, final InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
    }

    private static <T> T createProxy(final Class<T> clazz, final List<String> logger) {
        return createProxy(clazz, new MethodCallLogger(logger));
    }

    private static List<String> invokeEveryMethod(final KeyedObjectPool<Object, Object> kop) throws Exception {
        kop.addObject(null);
        kop.borrowObject(null);
        kop.clear();
        kop.clear(null);
        kop.close();
        kop.getKeys();
        kop.getNumActive();
        kop.getNumActive(null);
        kop.getNumIdle();
        kop.getNumIdle(null);
        kop.invalidateObject(null, new Object());
        kop.returnObject(null, new Object());
        kop.toString();

        return Arrays.asList("addObject", "borrowObject", "clear", "clear", "close", "getKeys", "getNumActive", "getNumActive", "getNumIdle", "getNumIdle",
                "invalidateObject", "returnObject", "toString");
    }

    private static <K, V> List<String> invokeEveryMethod(final KeyedPooledObjectFactory<K, V, RuntimeException> kpof) throws Exception {
        kpof.activateObject(null, null);
        kpof.destroyObject(null, null);
        kpof.makeObject(null);
        kpof.passivateObject(null, null);
        kpof.validateObject(null, null);
        kpof.toString();

        return Arrays.asList("activateObject", "destroyObject", "makeObject", "passivateObject", "validateObject", "toString");
    }

    private static List<String> invokeEveryMethod(final ObjectPool<Object, RuntimeException> op) throws Exception {
        op.addObject();
        op.borrowObject();
        op.clear();
        op.close();
        op.getNumActive();
        op.getNumIdle();
        op.invalidateObject(new Object());
        op.returnObject(new Object());
        op.toString();

        return Arrays.asList("addObject", "borrowObject", "clear", "close", "getNumActive", "getNumIdle", "invalidateObject",
                "returnObject", "toString");
    }

    private static <T, E extends Exception> List<String> invokeEveryMethod(final PooledObjectFactory<T, E> pof) throws Exception {
        pof.activateObject(null);
        pof.destroyObject(null);
        pof.makeObject();
        pof.passivateObject(null);
        pof.validateObject(null);
        pof.toString();

        return Arrays.asList("activateObject", "destroyObject", "makeObject", "passivateObject", "validateObject", "toString");
    }

    @Test
    public void testCheckMinIdleKeyedObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(null, new Object(), 1, 1),
                "PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not allow null pool.");
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>) null)) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(pool, (Object) null, 1, 1),
                    "PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept null keys.");
        }
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>) null)) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(pool, new Object(), -1, 1),
                    "PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept negative min idle values.");
        }

        final List<String> calledMethods = new ArrayList<>();
        final Object key = new Object();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final KeyedPooledObjectFactory<Object, Object, RuntimeException> kpof = createProxy(KeyedPooledObjectFactory.class, calledMethods);
        try (final KeyedObjectPool<Object, Object> kop = new GenericKeyedObjectPool<>(kpof)) {
            PoolUtils.checkMinIdle(kop, key, 2, 100);
            Thread.sleep(400);
            assertEquals(2, kop.getNumIdle(key));
            assertEquals(2, kop.getNumIdle());
        }
        int makeObjectCount = 0;
        final Iterator<String> iter = calledMethods.iterator();
        while (iter.hasNext()) {
            final String methodName = iter.next();
            if ("makeObject".equals(methodName)) {
                makeObjectCount++;
            }
        }
        assertEquals(2, makeObjectCount, "makeObject should have been called two time");

        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                calledMethods.clear();
                try (@SuppressWarnings("unchecked")
                final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, calledMethods)) {
                    // checks minIdle immediately
                    final TimerTask task = PoolUtils.checkMinIdle(pool, key, 1, CHECK_PERIOD);

                    Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                    task.cancel();
                    task.toString();

                    final List<String> expectedMethods = new ArrayList<>();
                    for (int i = 0; i < CHECK_COUNT; i++) {
                        expectedMethods.add("getNumIdle");
                        expectedMethods.add("addObject");
                    }
                    expectedMethods.add("toString");
                    assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
                }
            } catch (final AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testCheckMinIdleKeyedObjectPoolKeys() throws Exception {
        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            final List<String> calledMethods = new ArrayList<>();
            try (@SuppressWarnings("unchecked")
            final KeyedObjectPool<String, Object> pool = createProxy(KeyedObjectPool.class, calledMethods)) {
                final Collection<String> keys = new ArrayList<>(2);
                keys.add("one");
                keys.add("two");
                // checks minIdle immediately
                final Map<String, TimerTask> tasks = PoolUtils.checkMinIdle(pool, keys, 1, CHECK_PERIOD);

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                for (final TimerTask task : tasks.values()) {
                    task.cancel();
                }

                final List<String> expectedMethods = new ArrayList<>();
                for (int i = 0; i < CHECK_COUNT * keys.size(); i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (final AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testCheckMinIdleKeyedObjectPoolKeysNulls() {
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>) null)) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(pool, (Collection<?>) null, 1, 1),
                    "PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must not accept null keys.");
        }

        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>) null)) {
            PoolUtils.checkMinIdle(pool, (Collection<?>) Collections.emptyList(), 1, 1);
        } catch (final IllegalArgumentException iae) {
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must accept empty lists.");
        }
    }

    @Test
    public void testCheckMinIdleObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(null, 1, 1),
                "PoolUtils.checkMinIdle(ObjectPool,,) must not allow null pool.");
        try (@SuppressWarnings("unchecked")
        final ObjectPool<Object, RuntimeException> pool = createProxy(ObjectPool.class, (List<String>) null)) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.checkMinIdle(pool, -1, 1),
                    "PoolUtils.checkMinIdle(ObjectPool,,) must not accept negative min idle values.");
        }

        final List<String> calledMethods = new ArrayList<>();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final PooledObjectFactory<Object, RuntimeException> pof = createProxy(PooledObjectFactory.class, calledMethods);
        try (final ObjectPool<Object, RuntimeException> op = new GenericObjectPool<>(pof)) {
            PoolUtils.checkMinIdle(op, 2, 100);
            Thread.sleep(1000);
            assertEquals(2, op.getNumIdle());
        }
        int makeObjectCount = 0;
        final Iterator<String> iter = calledMethods.iterator();
        while (iter.hasNext()) {
            final String methodName = iter.next();
            if ("makeObject".equals(methodName)) {
                makeObjectCount++;
            }
        }
        assertEquals(2, makeObjectCount, "makeObject should have been called two time");

        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                calledMethods.clear();
                try (@SuppressWarnings("unchecked")
                final ObjectPool<Object, RuntimeException> pool = createProxy(ObjectPool.class, calledMethods)) {
                    final TimerTask task = PoolUtils.checkMinIdle(pool, 1, CHECK_PERIOD); // checks minIdle immediately

                    Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                    task.cancel();
                    task.toString();

                    final List<String> expectedMethods = new ArrayList<>();
                    for (int i = 0; i < CHECK_COUNT; i++) {
                        expectedMethods.add("getNumIdle");
                        expectedMethods.add("addObject");
                    }
                    expectedMethods.add("toString");
                    assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
                }
            } catch (final AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testCheckRethrow() {
        try {
            PoolUtils.checkRethrow(new Exception());
        } catch (final Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
        try {
            PoolUtils.checkRethrow(new ThreadDeath());
            fail("PoolUtils.checkRethrow(Throwable) must rethrow ThreadDeath.");
        } catch (final ThreadDeath td) {
            // expected
        } catch (final Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
        try {
            PoolUtils.checkRethrow(new InternalError()); // InternalError extends VirtualMachineError
            fail("PoolUtils.checkRethrow(Throwable) must rethrow VirtualMachineError.");
        } catch (final VirtualMachineError td) {
            // expected
        } catch (final Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
    }

    @Test
    public void testErodingObjectPoolDefaultFactor() {
        try (@SuppressWarnings("unchecked")
        final ObjectPool<Object, RuntimeException> internalPool = createProxy(ObjectPool.class, (arg0, arg1, arg2) -> null);
                final ObjectPool<Object, RuntimeException> pool = PoolUtils.erodingPool(internalPool)) {
            final String expectedToString = "ErodingObjectPool{factor=ErodingFactor{factor=1.0, idleHighWaterMark=1}, pool=" +
                    internalPool + "}";
            // The factor is not exposed, but will be printed in the toString() method
            // In this case since we didn't pass one, the default 1.0f will be printed
            assertEquals(expectedToString, pool.toString());
        }
    }

    @Test
    public void testErodingPerKeyKeyedObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null, 1f, true),
                "PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null, 0f, true),
                "PoolUtils.erodingPool(ObjectPool, float, boolean) must not allow a non-positive factor.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null, 1f, true),
                "PoolUtils.erodingPool(KeyedObjectPool, float, boolean) must not allow a null pool.");

        final List<String> calledMethods = new ArrayList<>();
        final InvocationHandler handler = new MethodCallLogger(calledMethods) {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Object o = super.invoke(proxy, method, args);
                if (o instanceof Integer) {
                    // so getNumActive/getNumIdle are not zero.
                    o = Integer.valueOf(1);
                }
                return o;
            }
        };

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        final float factor = 0.01f; // about ~9 seconds until first discard
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), factor, true)) {

            final List<String> expectedMethods = new ArrayList<>();
            assertEquals(expectedMethods, calledMethods);

            final Object key = "key";

            Object o = pool.borrowObject(key);
            expectedMethods.add("borrowObject");

            assertEquals(expectedMethods, calledMethods);

            pool.returnObject(key, o);
            expectedMethods.add("returnObject");
            assertEquals(expectedMethods, calledMethods);

            for (int i = 0; i < 5; i++) {
                o = pool.borrowObject(key);
                expectedMethods.add("borrowObject");

                Thread.sleep(50);

                pool.returnObject(key, o);
                expectedMethods.add("returnObject");

                assertEquals(expectedMethods, calledMethods);

                expectedMethods.clear();
                calledMethods.clear();
            }

            Thread.sleep(10000); // 10 seconds

            o = pool.borrowObject(key);
            expectedMethods.add("borrowObject");
            pool.returnObject(key, o);
            expectedMethods.add("getNumIdle");
            expectedMethods.add("invalidateObject");
            assertEquals(expectedMethods, calledMethods);

            final String expectedToString = "ErodingPerKeyKeyedObjectPool{factor=" + factor + ", keyedPool=null}";
            assertEquals(expectedToString, pool.toString());
        }
    }

    @Test
    public void testErodingPoolKeyedObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null),
                "PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null, 1f),
                "PoolUtils.erodingPool(KeyedObjectPool, float) must not allow a null pool.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((KeyedObjectPool<Object, Object>) null, 1f, true),
                "PoolUtils.erodingPool(KeyedObjectPool, float, boolean) must not allow a null pool.");

        final List<String> calledMethods = new ArrayList<>();
        final InvocationHandler handler = new MethodCallLogger(calledMethods) {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Object o = super.invoke(proxy, method, args);
                if (o instanceof Integer) {
                    // so getNumActive/getNumIdle are not zero.
                    o = Integer.valueOf(1);
                }
                return o;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), 0f),
                "PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), 0f, false),
                "PoolUtils.erodingPool(ObjectPool, float, boolean) must not allow a non-positive factor.");

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        final float factor = 0.01f; // about ~9 seconds until first discard
        final List<String> expectedMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), factor)) {

            assertEquals(expectedMethods, calledMethods);

            final Object key = "key";

            pool.addObject(key);
            expectedMethods.add("addObject");

            Object o = pool.borrowObject(key);
            expectedMethods.add("borrowObject");

            assertEquals(expectedMethods, calledMethods);

            pool.returnObject(key, o);
            expectedMethods.add("returnObject");
            assertEquals(expectedMethods, calledMethods);

            // the invocation handler always returns 1
            assertEquals(1, pool.getNumActive());
            expectedMethods.add("getNumActive");
            assertEquals(1, pool.getNumIdle());
            expectedMethods.add("getNumIdle");

            for (int i = 0; i < 5; i++) {
                o = pool.borrowObject(key);
                expectedMethods.add("borrowObject");

                Thread.sleep(50);

                pool.returnObject(key, o);
                expectedMethods.add("returnObject");

                assertEquals(expectedMethods, calledMethods);

                expectedMethods.clear();
                calledMethods.clear();
            }

            Thread.sleep(10000); // 10 seconds

            o = pool.borrowObject(key);
            expectedMethods.add("borrowObject");
            pool.returnObject(key, o);
            expectedMethods.add("getNumIdle");
            expectedMethods.add("invalidateObject");
            pool.clear();
        }
        expectedMethods.add("clear");
        expectedMethods.add("close");
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testErodingPoolKeyedObjectPoolDefaultFactor() {
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> internalPool = createProxy(KeyedObjectPool.class,
                (arg0, arg1, arg2) -> null);
                final KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool(internalPool)) {
            final String expectedToString = "ErodingKeyedObjectPool{factor=ErodingFactor{factor=1.0, idleHighWaterMark=1}, keyedPool=" +
                    internalPool + "}";
            // The factor is not exposed, but will be printed in the toString() method
            // In this case since we didn't pass one, the default 1.0f will be printed
            assertEquals(expectedToString, pool.toString());
        }
    }

    @Test
    public void testErodingPoolObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((ObjectPool<Object, RuntimeException>) null),
                "PoolUtils.erodingPool(ObjectPool) must not allow a null pool.");

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool((ObjectPool<Object, RuntimeException>) null, 1f),
                "PoolUtils.erodingPool(ObjectPool, float) must not allow a null pool.");

        final List<String> calledMethods = new ArrayList<>();
        final InvocationHandler handler = new MethodCallLogger(calledMethods) {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Object o = super.invoke(proxy, method, args);
                if (o instanceof Integer) {
                    // so getNumActive/getNumIdle are not zero.
                    o = Integer.valueOf(1);
                }
                return o;
            }
        };

        assertThrows(IllegalArgumentException.class, () -> PoolUtils.erodingPool(createProxy(ObjectPool.class, handler), -1f),
                "PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        final float factor = 0.01f; // about ~9 seconds until first discard
        final List<String> expectedMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
        final ObjectPool<Object, RuntimeException> pool = PoolUtils.erodingPool(createProxy(ObjectPool.class, handler), factor)) {

            assertEquals(expectedMethods, calledMethods);

            pool.addObject();
            expectedMethods.add("addObject");

            Object o = pool.borrowObject();
            expectedMethods.add("borrowObject");

            assertEquals(expectedMethods, calledMethods);

            pool.returnObject(o);
            expectedMethods.add("returnObject");
            assertEquals(expectedMethods, calledMethods);

            // the invocation handler always returns 1
            assertEquals(1, pool.getNumActive());
            expectedMethods.add("getNumActive");
            assertEquals(1, pool.getNumIdle());
            expectedMethods.add("getNumIdle");

            for (int i = 0; i < 5; i++) {
                o = pool.borrowObject();
                expectedMethods.add("borrowObject");

                Thread.sleep(50);

                pool.returnObject(o);
                expectedMethods.add("returnObject");

                assertEquals(expectedMethods, calledMethods);

                expectedMethods.clear();
                calledMethods.clear();
            }

            Thread.sleep(10000); // 10 seconds

            o = pool.borrowObject();
            expectedMethods.add("borrowObject");
            pool.returnObject(o);
            expectedMethods.add("getNumIdle");
            expectedMethods.add("invalidateObject");
            pool.clear();
        }
        expectedMethods.add("clear");
        expectedMethods.add("close");
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testJavaBeanInstantiation() {
        assertNotNull(new PoolUtils());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPrefillKeyedObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.prefill(null, new Object(), 1),
                "PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null pool.");

        try (final KeyedObjectPool<Object, String> pool = new GenericKeyedObjectPool<>(new TestGenericKeyedObjectPool.SimpleFactory<>())) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.prefill(pool, (Object) null, 1),
                    "PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null key.");
        }

        final List<String> calledMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, calledMethods)) {

            PoolUtils.prefill(pool, new Object(), 0);
            final List<String> expectedMethods = new ArrayList<>();
            expectedMethods.add("addObjects");
            assertEquals(expectedMethods, calledMethods);

            calledMethods.clear();
            PoolUtils.prefill(pool, new Object(), 3);
            assertEquals(expectedMethods, calledMethods);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPrefillKeyedObjectPoolCollection() throws Exception {
        try (@SuppressWarnings("unchecked")
        final KeyedObjectPool<String, String> pool = createProxy(KeyedObjectPool.class, (List<String>) null)) {
            assertThrows(IllegalArgumentException.class, () -> PoolUtils.prefill(pool, (Collection<String>) null, 1),
                    "PoolUtils.prefill(KeyedObjectPool,Collection,int) must not accept null keys.");
        }

        final List<String> calledMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
            final KeyedObjectPool<String, Object> pool = createProxy(KeyedObjectPool.class, calledMethods)) {

            final Set<String> keys = new HashSet<>();
            PoolUtils.prefill(pool, keys, 0);
            final List<String> expectedMethods = new ArrayList<>();
            expectedMethods.add("addObjects");
            assertEquals(expectedMethods, calledMethods);

            calledMethods.clear();
            keys.add("one");
            keys.add("two");
            keys.add("three");
            final int count = 3;
            PoolUtils.prefill(pool, keys, count);
            assertEquals(expectedMethods, calledMethods);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPrefillObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.prefill(null, 1), "PoolUtils.prefill(ObjectPool,int) must not allow null pool.");

        final List<String> calledMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
        final ObjectPool<Object, RuntimeException> pool = createProxy(ObjectPool.class, calledMethods)) {

            PoolUtils.prefill(pool, 0);
            final List<String> expectedMethods = new ArrayList<>();
            expectedMethods.add("addObjects");
            assertEquals(expectedMethods, calledMethods);

            calledMethods.clear();
            PoolUtils.prefill(pool, 3);
            assertEquals(expectedMethods, calledMethods);
        }
    }

    @Test
    public void testSynchronizedPoolableFactoryKeyedPoolableObjectFactory() throws Exception {
        assertThrows(IllegalArgumentException.class,
            () -> PoolUtils.synchronizedKeyedPooledFactory((KeyedPooledObjectFactory<Object, Object, RuntimeException>) null),
            "PoolUtils.synchronizedPoolableFactory(KeyedPoolableObjectFactory) must not allow a null factory.");

        final List<String> calledMethods = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final KeyedPooledObjectFactory<Object, Object, RuntimeException> kpof = createProxy(KeyedPooledObjectFactory.class, calledMethods);

        final KeyedPooledObjectFactory<Object, Object, RuntimeException> skpof = PoolUtils.synchronizedKeyedPooledFactory(kpof);
        final List<String> expectedMethods = invokeEveryMethod(skpof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolableFactoryPoolableObjectFactory() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.synchronizedPooledFactory((PooledObjectFactory<Object, Exception>) null),
                "PoolUtils.synchronizedPoolableFactory(PoolableObjectFactory) must not allow a null factory.");

        final List<String> calledMethods = new ArrayList<>();
        @SuppressWarnings("unchecked")
        final PooledObjectFactory<Object, Exception> pof = createProxy(PooledObjectFactory.class, calledMethods);

        final PooledObjectFactory<Object, Exception> spof = PoolUtils.synchronizedPooledFactory(pof);
        final List<String> expectedMethods = invokeEveryMethod(spof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolKeyedObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.synchronizedPool((KeyedObjectPool<Object, Object>) null),
                "PoolUtils.synchronizedPool(KeyedObjectPool) must not allow a null pool.");

        final List<String> calledMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
            final KeyedObjectPool<Object, Object> kop = createProxy(KeyedObjectPool.class, calledMethods);
            final KeyedObjectPool<Object, Object> skop = PoolUtils.synchronizedPool(kop)) {
            final List<String> expectedMethods = invokeEveryMethod(skop);
            assertEquals(expectedMethods, calledMethods);
        }

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolObjectPool() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PoolUtils.synchronizedPool((ObjectPool<Object, RuntimeException>) null),
                "PoolUtils.synchronizedPool(ObjectPool) must not allow a null pool.");

        final List<String> calledMethods = new ArrayList<>();
        try (@SuppressWarnings("unchecked")
        final ObjectPool<Object, RuntimeException> op = createProxy(ObjectPool.class, calledMethods); final ObjectPool<Object, RuntimeException> sop = PoolUtils.synchronizedPool(op)) {
            final List<String> expectedMethods = invokeEveryMethod(sop);
            assertEquals(expectedMethods, calledMethods);

            // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
        }
    }

    /**
     * Tests the {@link PoolUtils} timer holder.
     */
    @Test
    public void testTimerHolder() {
        final PoolUtils.TimerHolder h = new PoolUtils.TimerHolder();
        assertNotNull(h);
        assertNotNull(PoolUtils.TimerHolder.MIN_IDLE_TIMER);
    }
}
