/*
 * Copyright 2006 The Apache Software Foundation.
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

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.Collection;
import java.util.Map;
import java.util.Iterator;

/**
 * Unit tests for {@link Pools}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestPools extends TestCase {
    
    /** Period between checks for minIdle tests. Increase this if you happen to get too many false failures. */
    private static final int CHECK_PERIOD = 300;

    /** Times to let the minIdle check run. */
    private static final int CHECK_COUNT = 4;

    /** Sleep time to let the minIdle tests run CHECK_COUNT times. */
    private static final int CHECK_SLEEP_PERIOD = CHECK_PERIOD * (CHECK_COUNT - 1) + CHECK_PERIOD / 2;

    public void testAdaptKeyedPoolableObjectFactory() throws Exception {
        try {
            Pools.adapt((KeyedPoolableObjectFactory)null);
            fail("Pools.adapt(KeyedPoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedPoolableObjectFactoryKey() throws Exception {
        try {
            Pools.adapt((KeyedPoolableObjectFactory)null, new Object());
            fail("Pools.adapt(KeyedPoolableObjectFactory, key) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            Pools.adapt((KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, null), null);
            fail("Pools.adapt(KeyedPoolableObjectFactory, key) must not allow null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedPoolableObjectFactory kpof =
                (KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, calledMethods);

        final PoolableObjectFactory pof = Pools.adapt(kpof);
        pof.activateObject(null);
        pof.destroyObject(null);
        pof.makeObject();
        pof.passivateObject(null);
        pof.validateObject(null);

        final List expectedMethods = new ArrayList();
        expectedMethods.add("activateObject");
        expectedMethods.add("destroyObject");
        expectedMethods.add("makeObject");
        expectedMethods.add("passivateObject");
        expectedMethods.add("validateObject");

        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptPoolableObjectFactory() throws Exception {
        try {
            Pools.adapt((PoolableObjectFactory)null);
            fail("Pools.adapt(PoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final PoolableObjectFactory pof =
                (PoolableObjectFactory)createProxy(PoolableObjectFactory.class, calledMethods);

        final KeyedPoolableObjectFactory kpof = Pools.adapt(pof);
        kpof.activateObject(null, null);
        kpof.destroyObject(null, null);
        kpof.makeObject(null);
        kpof.passivateObject(null, null);
        kpof.validateObject(null, null);

        final List expectedMethods = new ArrayList();
        expectedMethods.add("activateObject");
        expectedMethods.add("destroyObject");
        expectedMethods.add("makeObject");
        expectedMethods.add("passivateObject");
        expectedMethods.add("validateObject");

        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptKeyedObjectPool() throws Exception {
        try {
            Pools.adapt((KeyedObjectPool)null);
            fail("Pools.adapt(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedObjectPoolKey() throws Exception {
        try {
            Pools.adapt((KeyedObjectPool)null, new Object());
            fail("Pools.adapt(KeyedObjectPool, key) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            Pools.adapt((KeyedObjectPool)createProxy(KeyedObjectPool.class, null), null);
            fail("Pools.adapt(KeyedObjectPool, key) must not allow a null key.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedObjectPool kop = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        final ObjectPool op = Pools.adapt(kop);
        op.addObject();
        op.borrowObject();
        op.clear();
        op.close();
        op.getNumActive();
        op.getNumIdle();
        op.invalidateObject(null);
        op.returnObject(null);
        op.setFactory((PoolableObjectFactory)createProxy(PoolableObjectFactory.class, null));

        final List expectedMethods = new ArrayList();
        expectedMethods.add("addObject");
        expectedMethods.add("borrowObject");
        expectedMethods.add("clear");
        expectedMethods.add("close");
        expectedMethods.add("getNumActive");
        expectedMethods.add("getNumIdle");
        expectedMethods.add("invalidateObject");
        expectedMethods.add("returnObject");
        expectedMethods.add("setFactory");

        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptObjectPool() throws Exception {
        try {
            Pools.adapt((ObjectPool)null);
            fail("Pools.adapt(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final ObjectPool op = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        final KeyedObjectPool kop = Pools.adapt(op);
        kop.addObject(null);
        kop.borrowObject(null);
        kop.clear();
        kop.clear(null);
        kop.close();
        kop.getNumActive();
        kop.getNumActive(null);
        kop.getNumIdle();
        kop.getNumIdle(null);
        kop.invalidateObject(null, null);
        kop.returnObject(null, null);
        kop.setFactory((KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, null));

        final List expectedMethods = new ArrayList();
        expectedMethods.add("addObject");
        expectedMethods.add("borrowObject");
        expectedMethods.add("clear");
        expectedMethods.add("clear");
        expectedMethods.add("close");
        expectedMethods.add("getNumActive");
        expectedMethods.add("getNumActive");
        expectedMethods.add("getNumIdle");
        expectedMethods.add("getNumIdle");
        expectedMethods.add("invalidateObject");
        expectedMethods.add("returnObject");
        expectedMethods.add("setFactory");

        assertEquals(expectedMethods, calledMethods);
    }

    public void testCheckMinIdleObjectPool() throws Exception {
        final List calledMethods = new ArrayList();
        final ObjectPool pool = (ObjectPool)createProxy(ObjectPool.class, calledMethods);
        final TimerTask task = Pools.checkMinIdle(pool, 1, CHECK_PERIOD); // checks minIdle immediately

        Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
        task.cancel();

        final List expectedMethods = new ArrayList();
        for (int i=0; i < CHECK_COUNT; i++) {
            expectedMethods.add("getNumIdle");
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
    }

    public void testCheckMinIdleKeyedObjectPool() throws Exception {
        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);
        final Object key = new Object();
        final TimerTask task = Pools.checkMinIdle(pool, key, 1, CHECK_PERIOD); // checks minIdle immediately

        Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
        task.cancel();

        final List expectedMethods = new ArrayList();
        for (int i=0; i < CHECK_COUNT; i++) {
            expectedMethods.add("getNumIdle");
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
    }

    public void testCheckMinIdleKeyedObjectPoolKeys() throws Exception {
        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);
        final Collection keys = new ArrayList(2);
        keys.add("one");
        keys.add("two");
        final Map tasks = Pools.checkMinIdle(pool, keys, 1, CHECK_PERIOD); // checks minIdle immediately

        Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
        final Iterator iter = tasks.values().iterator();
        while (iter.hasNext()) {
            final TimerTask task = (TimerTask)iter.next();
            task.cancel();
        }

        final List expectedMethods = new ArrayList();
        for (int i=0; i < CHECK_COUNT * keys.size(); i++) {
            expectedMethods.add("getNumIdle");
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
    }

    public void testPrefillObjectPool() throws Exception {
        final List calledMethods = new ArrayList();
        final ObjectPool pool = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        Pools.prefill(pool, 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        Pools.prefill(pool, 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testPrefillKeyedObjectPool() throws Exception {
        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        Pools.prefill(pool, new Object(), 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        Pools.prefill(pool, new Object(), 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testPrefillKeyedObjectPoolCollection() throws Exception {
        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        final Set keys = new HashSet();
        Pools.prefill(pool, keys, 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        keys.add(new Integer(1));
        keys.add("two");
        keys.add(new Double(3.1415926));
        Pools.prefill(pool, keys, 3);
        for (int i=0; i < keys.size() * 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testSynchronizedPoolObjectPool() throws Exception {
        try {
            Pools.synchronizedPool((ObjectPool)null);
            fail("Pools.synchronizedPool(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolKeyedObjectPool() throws Exception {
        try {
            Pools.synchronizedPool((KeyedObjectPool)null);
            fail("Pools.synchronizedPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryPoolableObjectFactory() throws Exception {
        try {
            Pools.synchronizedPoolableFactory((PoolableObjectFactory)null);
            fail("Pools.synchronizedPoolableFactory(PoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryKeyedPoolableObjectFactory() throws Exception {
        try {
            Pools.synchronizedPoolableFactory((KeyedPoolableObjectFactory)null);
            fail("Pools.synchronizedPoolableFactory(KeyedPoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    private static Object createProxy(final Class clazz, final List logger) {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                new MethodCallLogger(logger));
    }

    private static class MethodCallLogger implements InvocationHandler {
        private final List calledMethods;

        MethodCallLogger(final List calledMethods) {
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
