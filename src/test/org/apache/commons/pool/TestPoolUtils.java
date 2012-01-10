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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Unit tests for {@link PoolUtils}.
 *
 * @author Sandy McArthur
 * @version $Id$
 */
public class TestPoolUtils extends TestCase {

    /** Period between checks for minIdle tests. Increase this if you happen to get too many false failures. */
    private static final int CHECK_PERIOD = 300;

    /** Times to let the minIdle check run. */
    private static final int CHECK_COUNT = 4;

    /** Sleep time to let the minIdle tests run CHECK_COUNT times. */
    private static final int CHECK_SLEEP_PERIOD = CHECK_PERIOD * (CHECK_COUNT - 1) + CHECK_PERIOD / 2;

    public void testJavaBeanInstantiation() {
        new PoolUtils();
    }

    public void testAdaptKeyedPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.adapt((KeyedPoolableObjectFactory<Object, Object>)null);
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedPoolableObjectFactoryKey() throws Exception {
        try {
            PoolUtils.adapt((KeyedPoolableObjectFactory<Object, Object>)null, new Object());
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory, key) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedPoolableObjectFactory<Object, Object> proxy = createProxy(KeyedPoolableObjectFactory.class, (List<String>)null);
            PoolUtils.adapt(proxy, null);
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory, key) must not allow null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedPoolableObjectFactory<Object, Object> kpof =
                createProxy(KeyedPoolableObjectFactory.class, calledMethods);

        final PoolableObjectFactory<Object> pof = PoolUtils.adapt(kpof);
        final List<String> expectedMethods = invokeEveryMethod(pof);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.adapt((PoolableObjectFactory<?>)null);
            fail("PoolUtils.adapt(PoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        final PoolableObjectFactory<?> pof =
                createProxy(PoolableObjectFactory.class, calledMethods);

        final KeyedPoolableObjectFactory<?, ?> kpof = PoolUtils.adapt(pof);
        final List<String> expectedMethods = invokeEveryMethod(kpof);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptKeyedObjectPool() throws Exception {
        try {
            PoolUtils.adapt((KeyedObjectPool<Object ,?>)null);
            fail("PoolUtils.adapt(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedObjectPoolKey() throws Exception {
        try {
            PoolUtils.adapt((KeyedObjectPool<Object, Object>)null, new Object());
            fail("PoolUtils.adapt(KeyedObjectPool, key) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object, Object> proxy = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.adapt(proxy, null);
            fail("PoolUtils.adapt(KeyedObjectPool, key) must not allow a null key.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> kop = createProxy(KeyedObjectPool.class, calledMethods);

        final ObjectPool<Object> op = PoolUtils.adapt(kop, new Object());
        final List<String> expectedMethods = invokeEveryMethod(op);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptObjectPool() throws Exception {
        try {
            PoolUtils.adapt((ObjectPool<?>)null);
            fail("PoolUtils.adapt(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> op = createProxy(ObjectPool.class, calledMethods);

        final KeyedObjectPool<Object, Object> kop = PoolUtils.adapt(op);
        final List<String> expectedMethods = invokeEveryMethod(kop);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testCheckedPoolObjectPool() throws Exception {
        try {
            PoolUtils.checkedPool((ObjectPool<Object>)null, Object.class);
            fail("PoolUtils.checkedPool(ObjectPool, Class) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            final ObjectPool<?> proxy = createProxy(ObjectPool.class, (List<String>)null);
            PoolUtils.checkedPool(proxy, null);
            fail("PoolUtils.checkedPool(ObjectPool, Class) must not allow a null type.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        ObjectPool<Object> op = createProxy(ObjectPool.class, calledMethods);

        ObjectPool<Object> cop = PoolUtils.checkedPool(op, Object.class);
        final List<String> expectedMethods = invokeEveryMethod(cop);
        assertEquals(expectedMethods, calledMethods);

        op = new BaseObjectPool<Object>() {
            @Override
            public Object borrowObject() throws Exception {
                return new Integer(0);
            }
            @Override
            public void returnObject(Object obj) {}
            @Override
            public void invalidateObject(Object obj) {}
        };
        // cast to ObjectPool to be able to use String.class
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final ObjectPool checkedPool = PoolUtils.checkedPool((ObjectPool)op, String.class);
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> cop2 = checkedPool;

        try {
            cop2.borrowObject();
            fail("borrowObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop2.returnObject(new Integer(1));
            fail("returnObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop2.invalidateObject(new Integer(2));
            fail("invalidateObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
    }

    public void testCheckedPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.checkedPool((KeyedObjectPool<Object, Object>)null, Object.class);
            fail("PoolUtils.checkedPool(KeyedObjectPool, Class) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            PoolUtils.checkedPool((KeyedObjectPool<?, ?>)createProxy(KeyedObjectPool.class, (List<String>)null), null);
            fail("PoolUtils.checkedPool(KeyedObjectPool, Class) must not allow a null type.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        KeyedObjectPool<Object, Object> op = createProxy(KeyedObjectPool.class, calledMethods);

        KeyedObjectPool<Object, Object> cop = PoolUtils.checkedPool(op, Object.class);
        final List<String> expectedMethods = invokeEveryMethod(cop);
        assertEquals(expectedMethods, calledMethods);


        op = new BaseKeyedObjectPool<Object, Object>() {
            @Override
            public Integer borrowObject(Object key) {
                return new Integer(0);
            }

            @Override
            public void returnObject(Object key, Object obj) {}

            @Override
            public void invalidateObject(Object key, Object obj) {}
        };
        @SuppressWarnings("rawtypes")
        final KeyedObjectPool rawPool = op;
        @SuppressWarnings("unchecked")
        KeyedObjectPool<Object, Object> cop2 = PoolUtils.checkedPool(rawPool, String.class);

        try {
            cop2.borrowObject(null);
            fail("borrowObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop2.returnObject(null, new Integer(1));
            fail("returnObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop2.invalidateObject(null, new Integer(2));
            fail("invalidateObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
    }

    public void testCheckMinIdleObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, 1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            final ObjectPool<?> pool = createProxy(ObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, -1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final PoolableObjectFactory<Object> pof = createProxy(PoolableObjectFactory.class, calledMethods);
        final ObjectPool<Object> op = new GenericObjectPool<Object>(pof);
        PoolUtils.checkMinIdle(op, 2, 100);
        Thread.sleep(400);
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

    public void testCheckMinIdleKeyedObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, new Object(), 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, (Object)null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, new Object(), -1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        final Object key = new Object();

        // Test that the minIdle check doesn't add too many idle objects
        @SuppressWarnings("unchecked")
        final KeyedPoolableObjectFactory<Object, Object> kpof = createProxy(KeyedPoolableObjectFactory.class, calledMethods);
        final KeyedObjectPool<Object, Object> kop = new GenericKeyedObjectPool<Object, Object>(kpof);
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
                final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, calledMethods);
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

    public void testCheckMinIdleKeyedObjectPoolKeys() throws Exception {
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<String, Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.checkMinIdle(pool, (Collection<String>)null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Because this isn't determinist and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                final List<String> calledMethods = new ArrayList<String>();
                @SuppressWarnings("unchecked")
                final KeyedObjectPool<String, ?> pool = createProxy(KeyedObjectPool.class, calledMethods);
                final Collection<String> keys = new ArrayList<String>(2);
                keys.add("one");
                keys.add("two");
                final Map<String, TimerTask> tasks = PoolUtils.checkMinIdle(pool, keys, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                final Iterator<TimerTask> iter = tasks.values().iterator();
                while (iter.hasNext()) {
                    final TimerTask task = iter.next();
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

    public void testPrefillObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, 1);
            fail("PoolUtils.prefill(ObjectPool,int) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        final ObjectPool<?> pool = createProxy(ObjectPool.class, calledMethods);

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

    public void testPrefillKeyedObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, new Object(), 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.prefill(pool, (Object)null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = createProxy(KeyedObjectPool.class, calledMethods);

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

    public void testPrefillKeyedObjectPoolCollection() throws Exception {
        try {
            @SuppressWarnings("unchecked")
            final KeyedObjectPool<String, Object> pool = createProxy(KeyedObjectPool.class, (List<String>)null);
            PoolUtils.prefill(pool, (Collection<String>)null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Collection,int) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Number, Object> pool = createProxy(KeyedObjectPool.class, calledMethods);

        final Set<Number> keys = new HashSet<Number>();
        PoolUtils.prefill(pool, keys, 0);
        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        keys.add(new Integer(1));
        keys.add(new Long(2));
        keys.add(new Double(3.1415926));
        PoolUtils.prefill(pool, keys, 3);
        for (int i=0; i < keys.size() * 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

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

    public void testSynchronizedPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.synchronizedPool((KeyedObjectPool<Object, Object>)null);
            fail("PoolUtils.synchronizedPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> kop = createProxy(KeyedObjectPool.class, calledMethods);

        final KeyedObjectPool<Object, Object> skop = PoolUtils.synchronizedPool(kop);
        final List<String> expectedMethods = invokeEveryMethod(skop);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedPoolableFactory((PoolableObjectFactory<Object>)null);
            fail("PoolUtils.synchronizedPoolableFactory(PoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final PoolableObjectFactory<Object> pof =
                createProxy(PoolableObjectFactory.class, calledMethods);

        final PoolableObjectFactory<Object> spof = PoolUtils.synchronizedPoolableFactory(pof);
        final List<String> expectedMethods = invokeEveryMethod(spof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryKeyedPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedPoolableFactory((KeyedPoolableObjectFactory<Object, Object>)null);
            fail("PoolUtils.synchronizedPoolableFactory(KeyedPoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List<String> calledMethods = new ArrayList<String>();
        @SuppressWarnings("unchecked")
        final KeyedPoolableObjectFactory<Object, Object> kpof =
                createProxy(KeyedPoolableObjectFactory.class, calledMethods);

        final KeyedPoolableObjectFactory<Object, Object> skpof = PoolUtils.synchronizedPoolableFactory(kpof);
        final List<String> expectedMethods = invokeEveryMethod(skpof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

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

        try {
            PoolUtils.erodingPool((ObjectPool<Object>)null, 0);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");
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
                    o = new Integer(1);
                }
                return o;
            }
        };

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final ObjectPool<Object> pool = PoolUtils.erodingPool((ObjectPool<Object>)createProxy(ObjectPool.class, handler), factor);

        final List<String> expectedMethods = new ArrayList<String>();
        assertEquals(expectedMethods, calledMethods);

        Object o = pool.borrowObject();
        expectedMethods.add("borrowObject");

        assertEquals(expectedMethods, calledMethods);

        pool.returnObject(o);
        expectedMethods.add("returnObject");
        assertEquals(expectedMethods, calledMethods);

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
        assertEquals(expectedMethods, calledMethods);
    }

    public void testErodingPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null);
            fail("PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 1f);
            fail("PoolUtils.erodingPool(KeyedObjectPool, float) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 0);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 1f, true);
            fail("PoolUtils.erodingPool(KeyedObjectPool, float, boolean) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 0, false);
            fail("PoolUtils.erodingPool(ObjectPool, float, boolean) must not allow a non-positive factor.");
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
                    o = new Integer(1);
                }
                return o;
            }
        };

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)createProxy(KeyedObjectPool.class, handler), factor);

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
    }
    
    public void testErodingPerKeyKeyedObjectPool() throws Exception {
        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 1, true);
            fail("PoolUtils.erodingPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 0, true);
            fail("PoolUtils.erodingPool(ObjectPool, float) must not allow a non-positive factor.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        try {
            PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)null, 1f, true);
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
                    o = new Integer(1);
                }
                return o;
            }
        };

        // If the logic behind PoolUtils.erodingPool changes then this will need to be tweaked.
        float factor = 0.01f; // about ~9 seconds until first discard
        @SuppressWarnings("unchecked")
        final KeyedObjectPool<Object, Object> pool = PoolUtils.erodingPool((KeyedObjectPool<Object, Object>)createProxy(KeyedObjectPool.class, handler), factor, true);

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
        @SuppressWarnings("unchecked")
        final PoolableObjectFactory<Object> proxy = createProxy(PoolableObjectFactory.class, (List<String>)null);
        op.setFactory(proxy);
        op.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "close",
                "getNumActive", "getNumIdle", "invalidateObject",
                "returnObject", "setFactory", "toString"
        });
        return expectedMethods;
    }

    private static List<String> invokeEveryMethod(KeyedObjectPool<Object, Object> kop) throws Exception {
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
        @SuppressWarnings("unchecked")
        final KeyedPoolableObjectFactory<Object, Object> proxy = createProxy(KeyedPoolableObjectFactory.class, (List<String>)null);
        kop.setFactory(proxy);
        kop.toString();

        final List<String> expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "clear", "close",
                "getNumActive", "getNumActive", "getNumIdle", "getNumIdle", "invalidateObject",
                "returnObject", "setFactory", "toString"
        });
        return expectedMethods;
    }

    private static List<String> invokeEveryMethod(PoolableObjectFactory<?> pof) throws Exception {
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

    private static List<String> invokeEveryMethod(KeyedPoolableObjectFactory<?, ?> kpof) throws Exception {
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

    /**
     * Call sites can use <code>@SuppressWarnings("unchecked")</code>
     */
    private static <T> T createProxy(final Class<T> clazz, final List<String> logger) {
        return createProxy(clazz, new MethodCallLogger(logger));
    }

    /**
     * Call sites can use <code>@SuppressWarnings("unchecked")</code>
     */
    private static <T> T createProxy(final Class<T> clazz, final InvocationHandler handler) {        
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler));
    }

    private static class MethodCallLogger implements InvocationHandler {
        private final List<String> calledMethods;

        MethodCallLogger(final List<String> calledMethods) {
            this.calledMethods = calledMethods;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            calledMethods.add(method.getName());
            if (boolean.class.equals(method.getReturnType())) {
                return Boolean.FALSE;
            } else if (int.class.equals(method.getReturnType())) {
                return new Integer(0);
            } else if (long.class.equals(method.getReturnType())) {
                return new Long(0);
            } else if (Object.class.equals(method.getReturnType())) {
                return new Object();
            } else {
                return null;
            }
        }
    }
}
