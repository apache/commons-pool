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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

import org.junit.Assert;
import junit.framework.AssertionFailedError;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import org.junit.Test;

/**
 * Unit tests for {@link PoolUtils}.
 *
 * @version $Revision$
 */
public class TestPoolUtils {

    /** Period between checks for minIdle tests. Increase this if you happen to get too many false failures. */
    private static final int CHECK_PERIOD = 300;

    /** Times to let the minIdle check run. */
    private static final int CHECK_COUNT = 4;

    /** Sleep time to let the minIdle tests run CHECK_COUNT times. */
    private static final int CHECK_SLEEP_PERIOD = CHECK_PERIOD * (CHECK_COUNT - 1) + CHECK_PERIOD / 2;

    @Test
    public void testCheckRethrow() {
        try {
            PoolUtils.checkRethrow(new Exception());
        } catch (Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
        try {
            PoolUtils.checkRethrow(new ThreadDeath());
            fail("PoolUtils.checkRethrow(Throwable) must rethrow ThreadDeath.");
        } catch (ThreadDeath td) {
            // expected
        } catch (Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
        try {
            PoolUtils.checkRethrow(new InternalError()); // InternalError extends VirtualMachineError
            fail("PoolUtils.checkRethrow(Throwable) must rethrow VirtualMachineError.");
        } catch (VirtualMachineError td) {
            // expected
        } catch (Throwable t) {
            fail("PoolUtils.checkRethrow(Throwable) must rethrow only ThreadDeath and VirtualMachineError.");
        }
    }

    @Test
    public void testJavaBeanInstantiation() {
        Assert.assertNotNull(new PoolUtils());
    }

    @Test
    public void testCheckMinIdleObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, 1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final ObjectPool<Object> pool = createProxy(ObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, -1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final PooledObjectFactory<Object> pof = createProxy(PooledObjectFactory.class, calledMethods);
        final ObjectPool<Object> op = new GenericObjectPool<Object>(pof);
        PoolUtils.checkMinIdle(op, 2, 100);
        Thread.sleep(1000);
        assertEquals(2, op.getNumIdle());
        op.close();
        int makeObjectCount = 0;
        final Iterator<String> iter = calledMethods.iterator();
        while (iter.hasNext()) {
            final String methodName = iter.next();
            if ("makeObject".equals(methodName)) {
                makeObjectCount++;
            }
        }
        assertEquals("makeObject should have been called two time", 2, makeObjectCount);

        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                calledMethods.clear();
                @SuppressWarnings("unchecked")
                final ObjectPool<Object> pool = createProxy(ObjectPool.class, calledMethods);
                final TimerTask task = PoolUtils.checkMinIdle(pool, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                task.cancel();
                task.toString();

                final List<String> expectedMethods = new ArrayList<String>();
                for (int i=0; i < CHECK_COUNT; i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                expectedMethods.add("toString");
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testCheckMinIdleKeyedObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, new Object(), 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, (Object)null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, new Object(), -1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        final Object key = new Object();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final KeyedPooledObjectFactory<Object,Object> kpof =
            createProxy(KeyedPooledObjectFactory.class, calledMethods);
        final KeyedObjectPool<Object,Object> kop =
                new GenericKeyedObjectPool<Object,Object>(kpof);
        PoolUtils.checkMinIdle(kop, key, 2, 100);
        Thread.sleep(400);
        assertEquals(2, kop.getNumIdle(key));
        assertEquals(2, kop.getNumIdle());
        kop.close();
        int makeObjectCount = 0;
        final Iterator<String> iter = calledMethods.iterator();
        while (iter.hasNext()) {
            final String methodName = iter.next();
            if ("makeObject".equals(methodName)) {
                makeObjectCount++;
            }
        }
        assertEquals("makeObject should have been called two time", 2, makeObjectCount);

        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                calledMethods.clear();
                @SuppressWarnings("unchecked")
                final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, calledMethods);
                final TimerTask task = PoolUtils.checkMinIdle(pool, key, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                task.cancel();
                task.toString();

                final List<String> expectedMethods = new ArrayList<String>();
                for (int i=0; i < CHECK_COUNT; i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                expectedMethods.add("toString");
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testCheckMinIdleKeyedObjectPoolKeys() throws Exception {
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, (Collection<?>) null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, (Collection<?>) Collections.emptyList(), 1, 1);
        } catch (IllegalArgumentException iae) {
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must accept empty lists.");
        }

        // Because this isn't deterministic and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                final List<String> calledMethods = new ArrayList<String>();
                @SuppressWarnings("unchecked")
                final KeyedObjectPool<String,Object> pool = createProxy(KeyedObjectPool.class, calledMethods);
                final Collection<String> keys = new ArrayList<String>(2);
                keys.add("one");
                keys.add("two");
                final Map<String, TimerTask> tasks = PoolUtils.checkMinIdle(pool, keys, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                for (TimerTask task : tasks.values()) {
                    task.cancel();
                }

                final List<String> expectedMethods = new ArrayList<String>();
                for (int i=0; i < CHECK_COUNT * keys.size(); i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    @Test
    public void testPrefillObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, 1);
            fail("PoolUtils.prefill(ObjectPool,int) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> pool = createProxy(ObjectPool.class, calledMethods);

        PoolUtils.prefill(pool, 0);
        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        PoolUtils.prefill(pool, 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testPrefillKeyedObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, new Object(), 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.prefill(pool, (Object)null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object,Object> pool = createProxy(KeyedObjectPool.class, calledMethods);

        PoolUtils.prefill(pool, new Object(), 0);
        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        PoolUtils.prefill(pool, new Object(), 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testPrefillKeyedObjectPoolCollection() throws Exception {
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<String,String> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.prefill(pool, (Collection<String>)null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Collection,int) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<String,Object> pool = createProxy(KeyedObjectPool.class, calledMethods);

        final Set<String> keys = new HashSet<String>();
        PoolUtils.prefill(pool, keys, 0);
        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        keys.add("one");
        keys.add("two");
        keys.add("three");
        PoolUtils.prefill(pool, keys, 3);
        for (int i=0; i < keys.size() * 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testSynchronizedPoolObjectPool() throws Exception {
        try {
            PoolUtils.synchronizedPool((ObjectPool<Object>)null);
            fail("PoolUtils.synchronizedPool(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> op = createProxy(ObjectPool.class, calledMethods);

        final ObjectPool<Object> sop = PoolUtils.synchronizedPool(op);
        final List<String> expectedMethods = invokeEveryMethod(sop);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.synchronizedPool((KeyedObjectPool<Object,Object>)null);
            fail("PoolUtils.synchronizedPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object,Object> kop = createProxy(KeyedObjectPool.class, calledMethods);

        final KeyedObjectPool<Object,Object> skop = PoolUtils.synchronizedPool(kop);
        final List<String> expectedMethods = invokeEveryMethod(skop);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolableFactoryPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedPooledFactory((PooledObjectFactory<Object>)null);
            fail("PoolUtils.synchronizedPoolableFactory(PoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final PooledObjectFactory<Object> pof =
                createProxy(PooledObjectFactory.class, calledMethods);

        final PooledObjectFactory<Object> spof = PoolUtils.synchronizedPooledFactory(pof);
        final List<String> expectedMethods = invokeEveryMethod(spof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testSynchronizedPoolableFactoryKeyedPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedKeyedPooledFactory((KeyedPooledObjectFactory<Object,Object>)null);
            fail("PoolUtils.synchronizedPoolableFactory(KeyedPoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedPooledObjectFactory<Object,Object> kpof =
                createProxy(KeyedPooledObjectFactory.class, calledMethods);

        final KeyedPooledObjectFactory<Object,Object> skpof = PoolUtils.synchronizedKeyedPooledFactory(kpof);
        final List<String> expectedMethods = invokeEveryMethod(skpof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    @Test
    public void testErodingPoolObjectPool() throws Exception {
        try {
            PoolUtils.erodingPool((ObjectPool<Object>)null);
            fail("PoolUtils.erodingPool(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((ObjectPool<Object>)null, 1f);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
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

        try {
            @SuppressWarnings({"unchecked", "unused"})
            Object o = PoolUtils.erodingPool(createProxy(ObjectPool.class, handler), -1f);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> pool = PoolUtils.erodingPool(
                createProxy(ObjectPool.class, handler), factor);

        final List<String> expectedMethods = new ArrayList<String>();
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

        for (int i=0; i < 5; i ++) {
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
        pool.close();
        expectedMethods.add("clear");
        expectedMethods.add("close");
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testErodingObjectPoolDefaultFactor() {
        @SuppressWarnings("unchecked")
        ObjectPool<Object> internalPool = createProxy(ObjectPool.class, new InvocationHandler() {
            @Override
            public Object invoke(Object arg0, Method arg1, Object[] arg2)
                    throws Throwable {
                return null;
            }
        });
        ObjectPool<Object> pool = PoolUtils.erodingPool(internalPool);
        String expectedToString = "ErodingObjectPool{factor=ErodingFactor{factor=1.0, idleHighWaterMark=1}, pool=" + internalPool + "}";
        // The factor is not exposed, but will be printed in the toString() method
        // In this case since we didn't pass one, the default 1.0f will be printed
        assertEquals(expectedToString, pool.toString());
    }

    @Test
    public void testErodingPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null);
            fail("PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null, 1f);
            fail("PoolUtils.erodingPool(KeyedObjectPool, float) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null, 1f, true);
            fail("PoolUtils.erodingPool(KeyedObjectPool, float, boolean) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
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

        try {
            @SuppressWarnings({"unchecked", "unused"})
            Object o = PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), 0f);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            @SuppressWarnings({"unchecked", "unused"})
            Object o = PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), 0f, false);
            fail("PoolUtils.erodingPool(ObjectPool, float, boolean) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object,Object> pool =
            PoolUtils.erodingPool(createProxy(KeyedObjectPool.class, handler), factor);

        final List<String> expectedMethods = new ArrayList<String>();
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

        for (int i=0; i < 5; i ++) {
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
        pool.close();
        expectedMethods.add("clear");
        expectedMethods.add("close");
        assertEquals(expectedMethods, calledMethods);
    }

    @Test
    public void testErodingPoolKeyedObjectPoolDefaultFactor() {
        @SuppressWarnings("unchecked")
        KeyedObjectPool<Object, Object> internalPool = createProxy(KeyedObjectPool.class, new InvocationHandler() {
            @Override
            public Object invoke(Object arg0, Method arg1, Object[] arg2)
                    throws Throwable {
                return null;
            }
        });
        KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool(internalPool);
        String expectedToString = "ErodingKeyedObjectPool{factor=ErodingFactor{factor=1.0, idleHighWaterMark=1}, keyedPool=" + internalPool + "}";
        // The factor is not exposed, but will be printed in the toString() method
        // In this case since we didn't pass one, the default 1.0f will be printed
        assertEquals(expectedToString, pool.toString());
    }

    @Test
    public void testErodingPerKeyKeyedObjectPool() throws Exception {
        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null, 1f, true);
            fail("PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null, 0f, true);
            fail("PoolUtils.erodingPool(ObjectPool, float, boolean) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object,Object>)null, 1f, true);
            fail("PoolUtils.erodingPool(KeyedObjectPool, float, boolean) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
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
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object,Object> pool = PoolUtils.erodingPool(
                createProxy(KeyedObjectPool.class, handler), factor, true);

        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        final Object key = "key";

        Object o = pool.borrowObject(key);
        expectedMethods.add("borrowObject");

        assertEquals(expectedMethods, calledMethods);

        pool.returnObject(key, o);
        expectedMethods.add("returnObject");
        assertEquals(expectedMethods, calledMethods);

        for (int i=0; i < 5; i ++) {
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

        String expectedToString = "ErodingPerKeyKeyedObjectPool{factor="+factor+", keyedPool=null}";
        assertEquals(expectedToString, pool.toString());
    }

    /**
     * Tests the {@link PoolUtils} timer holder.
     */
    @Test
    public void testTimerHolder() {
        PoolUtils.TimerHolder h = new PoolUtils.TimerHolder();
        assertNotNull(h);
        assertNotNull(PoolUtils.TimerHolder.MIN_IDLE_TIMER);
    }

    private static List<String> invokeEveryMethod(ObjectPool<Object> op) throws Exception {
        op.addObject();
        op.borrowObject();
        op.clear();
        op.close();
        op.getNumActive();
        op.getNumIdle();
        op.invalidateObject(new Object());
        op.returnObject(new Object());
        op.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "close",
                "getNumActive", "getNumIdle", "invalidateObject",
                "returnObject", "toString"
        });
        return expectedMethods;
    }

    private static List<String> invokeEveryMethod(KeyedObjectPool<Object,Object> kop) throws Exception {
        kop.addObject(null);
        kop.borrowObject(null);
        kop.clear();
        kop.clear(null);
        kop.close();
        kop.getNumActive();
        kop.getNumActive(null);
        kop.getNumIdle();
        kop.getNumIdle(null);
        kop.invalidateObject(null, new Object());
        kop.returnObject(null, new Object());
        kop.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "clear", "close",
                "getNumActive", "getNumActive", "getNumIdle", "getNumIdle", "invalidateObject",
                "returnObject", "toString"
        });
        return expectedMethods;
    }

    private static <T> List<String> invokeEveryMethod(PooledObjectFactory<T> pof) throws Exception {
        pof.activateObject(null);
        pof.destroyObject(null);
        pof.makeObject();
        pof.passivateObject(null);
        pof.validateObject(null);
        pof.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "activateObject", "destroyObject", "makeObject",
                "passivateObject", "validateObject", "toString",
        });
        return expectedMethods;
    }

    private static <K,V> List<String> invokeEveryMethod(KeyedPooledObjectFactory<K,V> kpof) throws Exception {
        kpof.activateObject(null, null);
        kpof.destroyObject(null, null);
        kpof.makeObject(null);
        kpof.passivateObject(null, null);
        kpof.validateObject(null, null);
        kpof.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "activateObject", "destroyObject", "makeObject",
                "passivateObject", "validateObject", "toString",
        });
        return expectedMethods;
    }

    private static <T> T createProxy(final Class<T> clazz, final List<String> logger) {
        return createProxy(clazz, new MethodCallLogger(logger));
    }

    private static <T> T createProxy(final Class<T> clazz, final InvocationHandler handler) {
        @SuppressWarnings("unchecked")
        T ret = (T) Proxy.newProxyInstance(
                clazz.getClassLoader(), new Class[] { clazz }, handler);
        return ret;
    }

    private static class MethodCallLogger implements InvocationHandler {
        private final List<String> calledMethods;

        MethodCallLogger(final List<String> calledMethods) {
            this.calledMethods = calledMethods;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            calledMethods.add(method.getName());
            if (boolean.class.equals(method.getReturnType())) {
                return Boolean.FALSE;
            } else if (int.class.equals(method.getReturnType())) {
                return Integer.valueOf(0);
            } else if (long.class.equals(method.getReturnType())) {
                return Long.valueOf(0);
            } else if (Object.class.equals(method.getReturnType())) {
                return new Object();
            } else if (PooledObject.class.equals(method.getReturnType())) {
                return new DefaultPooledObject<Object>(new Object());
            } else {
                return null;
            }
        }
    }
}
