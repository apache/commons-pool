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

package org.apache.commons.pool.composite;

import org.apache.commons.pool.TestObjectPoolFactory;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.MethodCall;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * Tests for {@link CompositeObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestCompositeObjectPoolFactory extends TestObjectPoolFactory {
    private static final long FUZZ = 5L;

    public TestCompositeObjectPoolFactory(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeObjectPoolFactory.class);
    }

    protected ObjectPoolFactory makeFactory(final PoolableObjectFactory objectFactory) throws UnsupportedOperationException {
        return new CompositeObjectPoolFactory(objectFactory);
    }

    public void testCreatePoolWithNullConfig() {
        try {
            CompositeObjectPoolFactory.createPool(null);
            fail("Expected an IllegalArgumentException when factory config is null.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testBorrowPolicy() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        try {
            copf.setBorrowPolicy(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testBorrowPolicyNull() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setBorrowPolicy(BorrowPolicy.NULL);

        final ObjectPool pool = copf.createPool();

        Object a = pool.borrowObject();
        pool.returnObject(a);
        Object b = pool.borrowObject();
        assertNotSame("BorrowPolicy.NULL should not return previously borrowed objects.", a, b);
    }

    public void testBorrowPolicyNullWithExhaustionFail() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setBorrowPolicy(BorrowPolicy.NULL);
        copf.setExhaustionPolicy(ExhaustionPolicy.FAIL);

        try {
            copf.createPool();
            fail("Cannot create a pool with both BorrowPolicy.NULL and ExhaustionPolicy.FAIL.");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    public void testBorrowPolicyFifo() throws Exception {
        testBorrowPolicyFifo(BorrowPolicy.FIFO);
    }

    public void testBorrowPolicySoftFifo() throws Exception {
        testBorrowPolicyFifo(BorrowPolicy.SOFT_FIFO);
    }

    private void testBorrowPolicyFifo(final BorrowPolicy policy) throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setBorrowPolicy(policy);

        final ObjectPool pool = copf.createPool();

        Object a = pool.borrowObject();
        Object b = pool.borrowObject();
        pool.returnObject(a);
        pool.returnObject(b);
        Object aa = pool.borrowObject();
        Object bb = pool.borrowObject();
        assertSame(a, aa);
        assertSame(b, bb);
    }

    public void testBorrowPolicyLifo() throws Exception {
        testBorrowPolicyLifo(BorrowPolicy.LIFO);
    }

    public void testBorrowPolicySoftLifo() throws Exception {
        testBorrowPolicyLifo(BorrowPolicy.SOFT_LIFO);
    }

    private void testBorrowPolicyLifo(final BorrowPolicy policy) throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setBorrowPolicy(policy);

        final ObjectPool pool = copf.createPool();

        Object a = pool.borrowObject();
        Object b = pool.borrowObject();
        pool.returnObject(a);
        pool.returnObject(b);
        Object aa = pool.borrowObject();
        Object bb = pool.borrowObject();
        assertSame(a, bb);
        assertSame(b, aa);
    }

    public void testExhaustionPolicy() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        try {
            copf.setExhaustionPolicy(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testExhaustionPolicyFail() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setExhaustionPolicy(ExhaustionPolicy.FAIL);
        final ObjectPool pool = copf.createPool();

        try {
            pool.borrowObject();
            fail("new pools should be empty and fail with ExhaustionPolicy.FAIL.");
        } catch (NoSuchElementException nsee) {
            // expected
        }

        pool.addObject();
        pool.borrowObject();
    }

    public void testExhaustionPolicyGrow() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setExhaustionPolicy(ExhaustionPolicy.GROW);
        final ObjectPool pool = copf.createPool();

        assertEquals(0, pool.getNumIdle());
        pool.borrowObject();
    }

    public void testLimitPolicy() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        try {
            copf.setLimitPolicy(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testLimitPolicyFail() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setMaxActive(1);
        copf.setLimitPolicy(LimitPolicy.FAIL);
        copf.setMaxWaitMillis(1000);
        final ObjectPool pool = copf.createPool();

        Object a = pool.borrowObject();
        long startTime = System.currentTimeMillis();
        try {
            pool.borrowObject();
            fail();
        } catch (NoSuchElementException nsee) {
            // expected
        }
        assertTrue("borrowObject shouldn't take long to fail.", startTime + 1000 - FUZZ > System.currentTimeMillis());

        pool.returnObject(a);
        pool.borrowObject();
    }

    public void testLimitPolicyWait() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setMaxActive(1);
        copf.setLimitPolicy(LimitPolicy.WAIT);
        copf.setMaxWaitMillis(1000);
        final ObjectPool pool = copf.createPool();

        Object a = pool.borrowObject();
        long startTime = System.currentTimeMillis();
        try {
            pool.borrowObject();
            fail();
        } catch (NoSuchElementException nsee) {
            // expected
        }
        assertTrue("borrowObject shouldn't take a while to fail.", startTime + 1000 - FUZZ < System.currentTimeMillis());

        pool.returnObject(a);
        pool.borrowObject();
    }

    public void testTrackingPolicy() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        try {
            copf.setTrackingPolicy(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testTrackingPolicyNull() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setTrackingPolicy(TrackingPolicy.NULL);
        final ObjectPool pool = copf.createPool();

        assertTrue(pool.getNumActive() < 0);
        Object a = pool.borrowObject();
        assertTrue(pool.getNumActive() < 0);
        pool.returnObject(a);
        assertTrue(pool.getNumActive() < 0);
    }

    public void testTrackingPolicySimple() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setTrackingPolicy(TrackingPolicy.SIMPLE);
        final ObjectPool pool = copf.createPool();

        assertEquals(0, pool.getNumActive());
        Object a = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        pool.returnObject(a);
        assertEquals(0, pool.getNumActive());
    }

    public void testTrackingPolicyReference() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory(new BasePoolableObjectFactory() {
            public Object makeObject() throws Exception {
                return new Object();
            }
        });
        copf.setTrackingPolicy(TrackingPolicy.REFERENCE);
        final ObjectPool pool = copf.createPool();

        assertEquals(0, pool.getNumActive());
        Object a = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        a = null;
        List garbage = new LinkedList();
        Runtime runtime = Runtime.getRuntime();
        while (pool.getNumActive() > 0) {
            try {
                garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory()/2)]);
            } catch (OutOfMemoryError oome) {
                System.gc();
            }
            System.gc();
        }
        garbage.clear();
        System.gc();
        assertEquals(0, pool.getNumActive());
    }

    public void testTrackingPolicyDebug() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory(new BasePoolableObjectFactory() {
            public Object makeObject() throws Exception {
                return new Object();
            }
        });
        copf.setTrackingPolicy(TrackingPolicy.DEBUG);
        final ObjectPool pool = copf.createPool();

        assertEquals(0, pool.getNumActive());
        Object a = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        a = null;
        List garbage = new LinkedList();
        Runtime runtime = Runtime.getRuntime();
        while (pool.getNumActive() > 0) {
            try {
                garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory()/2)]);
            } catch (OutOfMemoryError oome) {
                System.gc();
            }
            System.gc();
        }
        garbage.clear();
        System.gc();
        assertEquals(0, pool.getNumActive());
    }

    public void testMaxIdle() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setMaxIdle(5);
        final ObjectPool pool = copf.createPool();

        for (int i=1; i<10; i++) {
            pool.addObject();
            assertEquals(Math.min(5, i), pool.getNumIdle());
        }
    }

    public void testValidateOnReturn() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setValidateOnReturn(true);
        final CompositeObjectPool pool = (CompositeObjectPool)copf.createPool();
        final MethodCallPoolableObjectFactory factory = (MethodCallPoolableObjectFactory)pool.getFactory();

        final Object a = pool.borrowObject();
        factory.getMethodCalls().clear();
        pool.returnObject(a);

        final List expectedMethods = new ArrayList();
        expectedMethods.add(new MethodCall("validateObject", a).returned(Boolean.TRUE));
        expectedMethods.add(new MethodCall("passivateObject", a));
        assertEquals(expectedMethods, factory.getMethodCalls());
    }

    public void testEvictIdleMillis() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setEvictIdleMillis(50L);
        final ObjectPool pool = copf.createPool();

        pool.addObject();
        Thread.sleep(75L);
        assertEquals(0, pool.getNumIdle());
    }

    public void testEvictInvalidFrequencyMillis() throws Exception {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.setEvictInvalidFrequencyMillis(50L);
        final CompositeObjectPool pool = (CompositeObjectPool)copf.createPool();
        final MethodCallPoolableObjectFactory factory = (MethodCallPoolableObjectFactory)pool.getFactory();

        pool.addObject();
        Thread.sleep(75L);
        assertEquals(1, pool.getNumIdle());
        factory.setValid(false);
        Thread.sleep(75L);
        assertEquals(0, pool.getNumIdle());
    }

    public void testSetFactory() {
        CompositeObjectPoolFactory factory = (CompositeObjectPoolFactory)makeFactory();

        try {
            factory.setFactory(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testToString() {
        super.testToString();
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)makeFactory();
        copf.toString();
        copf.setMaxActive(1);
        copf.toString();
        copf.setLimitPolicy(LimitPolicy.WAIT);
        copf.toString();
        copf.setEvictIdleMillis(1);
        copf.toString();
        copf.setEvictInvalidFrequencyMillis(1);
        copf.toString();
    }
}
