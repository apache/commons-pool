/*
 * $Id: TestGenericObjectPool.java,v 1.14 2003/04/23 18:21:13 rwaldhoff Exp $
 * $Revision: 1.14 $
 * $Date: 2003/04/23 18:21:13 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.pool.impl;

import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 1.14 $ $Date: 2003/04/23 18:21:13 $
 */
public class TestGenericObjectPool extends TestObjectPool {
    public TestGenericObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestGenericObjectPool.class);
    }

    protected ObjectPool makeEmptyPool(int mincap) {
       GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
        pool.setMaxActive(mincap);
        pool.setMaxIdle(mincap);
        return pool;
    }
    
    protected Object getNthObject(int n) {
        return String.valueOf(n);
    }

    public void setUp() throws Exception {
        super.setUp();
        pool = new GenericObjectPool(new SimpleFactory());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        pool = null;
    }

    public void testWithInitiallyInvalid() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory(false));
        pool.setTestOnBorrow(true);
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected 
        }
    }

    public void testZeroMaxActive() throws Exception {
        pool.setMaxActive(0);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
        Object obj = pool.borrowObject();
        assertEquals(getNthObject(0),obj);
        pool.returnObject(obj);
    }

    public void testNegativeMaxActive() throws Exception {
        pool.setMaxActive(-1);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
        Object obj = pool.borrowObject();
        assertEquals(getNthObject(0),obj);
        pool.returnObject(obj);
    }

    public void testMaxIdle() throws Exception {
        pool.setMaxActive(100);
        pool.setMaxIdle(8);
        Object[] active = new Object[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject();
        }
        assertEquals(100,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        for(int i=0;i<100;i++) {
            pool.returnObject(active[i]);
            assertEquals(99 - i,pool.getNumActive());
            assertEquals((i < 8 ? i+1 : 8),pool.getNumIdle());
        }
    }

    public void testMaxActive() throws Exception {
        pool.setMaxActive(3);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);

        pool.borrowObject();
        pool.borrowObject();
        pool.borrowObject();
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }

    public void testInvalidWhenExhaustedAction() throws Exception {
        try {
            pool.setWhenExhaustedAction(Byte.MAX_VALUE);
            fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // expected
        }

        try {
            ObjectPool pool = new GenericObjectPool(
                new SimpleFactory(),
                GenericObjectPool.DEFAULT_MAX_ACTIVE, 
                Byte.MAX_VALUE,
                GenericObjectPool.DEFAULT_MAX_WAIT, 
                GenericObjectPool.DEFAULT_MAX_IDLE,
                false,
                false,
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                false
            );
            fail("Expected IllegalArgumentException");
        } catch(IllegalArgumentException e) {
            // expected
        }
    }

    public void testSettersAndGetters() throws Exception {
        GenericObjectPool pool = new GenericObjectPool();
        {
            pool.setFactory(new SimpleFactory());
        }
        {
            pool.setMaxActive(123);
            assertEquals(123,pool.getMaxActive());
        }
        {
            pool.setMaxIdle(12);
            assertEquals(12,pool.getMaxIdle());
        }
        {
            pool.setMaxWait(1234L);
            assertEquals(1234L,pool.getMaxWait());
        }
        {
            pool.setMinEvictableIdleTimeMillis(12345L);
            assertEquals(12345L,pool.getMinEvictableIdleTimeMillis());
        }
        {
            pool.setNumTestsPerEvictionRun(11);
            assertEquals(11,pool.getNumTestsPerEvictionRun());
        }
        {
            pool.setTestOnBorrow(true);
            assertTrue(pool.getTestOnBorrow());
            pool.setTestOnBorrow(false);
            assertTrue(!pool.getTestOnBorrow());
        }
        {
            pool.setTestOnReturn(true);
            assertTrue(pool.getTestOnReturn());
            pool.setTestOnReturn(false);
            assertTrue(!pool.getTestOnReturn());
        }
        {
            pool.setTestWhileIdle(true);
            assertTrue(pool.getTestWhileIdle());
            pool.setTestWhileIdle(false);
            assertTrue(!pool.getTestWhileIdle());
        }
        {
            pool.setTimeBetweenEvictionRunsMillis(11235L);
            assertEquals(11235L,pool.getTimeBetweenEvictionRunsMillis());
        }
        {
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK,pool.getWhenExhaustedAction());
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_FAIL,pool.getWhenExhaustedAction());
            pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW,pool.getWhenExhaustedAction());
        }
    }
    
    public void testDefaultConfiguration() throws Exception {
        GenericObjectPool pool = new GenericObjectPool();
        assertConfiguration(new GenericObjectPool.Config(),pool);
    }

    public void testConstructors() throws Exception {
        {
            GenericObjectPool pool = new GenericObjectPool();
            assertConfiguration(new GenericObjectPool.Config(),pool);
        }
        {
            GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
            assertConfiguration(new GenericObjectPool.Config(),pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxIdle = 3;
            expected.maxWait = 5L;
            expected.minEvictableIdleTimeMillis = 7L;
            expected.numTestsPerEvictionRun = 9;
            expected.testOnBorrow = true;
            expected.testOnReturn = true;
            expected.testWhileIdle = true;
            expected.timeBetweenEvictionRunsMillis = 11L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxWait = 5L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive,expected.whenExhaustedAction,expected.maxWait);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxWait = 5L;
            expected.testOnBorrow = true;
            expected.testOnReturn = true;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive,expected.whenExhaustedAction,expected.maxWait,expected.testOnBorrow,expected.testOnReturn);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxIdle = 3;
            expected.maxWait = 5L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive,expected.whenExhaustedAction,expected.maxWait,expected.maxIdle);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxIdle = 3;
            expected.maxWait = 5L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            expected.testOnBorrow = true;
            expected.testOnReturn = true;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive,expected.whenExhaustedAction,expected.maxWait,expected.maxIdle,expected.testOnBorrow,expected.testOnReturn);
            assertConfiguration(expected,pool);
        }
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxIdle = 3;
            expected.maxWait = 5L;
            expected.minEvictableIdleTimeMillis = 7L;
            expected.numTestsPerEvictionRun = 9;
            expected.testOnBorrow = true;
            expected.testOnReturn = true;
            expected.testWhileIdle = true;
            expected.timeBetweenEvictionRunsMillis = 11L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive, expected.whenExhaustedAction, expected.maxWait, expected.maxIdle, expected.testOnBorrow, expected.testOnReturn, expected.timeBetweenEvictionRunsMillis, expected.numTestsPerEvictionRun, expected.minEvictableIdleTimeMillis, expected.testWhileIdle);
            assertConfiguration(expected,pool);
        }
    }

    public void testSetConfig() throws Exception {
        GenericObjectPool.Config expected = new GenericObjectPool.Config();
        GenericObjectPool pool = new GenericObjectPool();
        assertConfiguration(expected,pool);
        expected.maxActive = 2;
        expected.maxIdle = 3;
        expected.maxWait = 5L;
        expected.minEvictableIdleTimeMillis = 7L;
        expected.numTestsPerEvictionRun = 9;
        expected.testOnBorrow = true;
        expected.testOnReturn = true;
        expected.testWhileIdle = true;
        expected.timeBetweenEvictionRunsMillis = 11L;
        expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        pool.setConfig(expected);
        assertConfiguration(expected,pool);
    }

    public void testDebugInfo() throws Exception {
        GenericObjectPool pool = new GenericObjectPool();
        assertNotNull(pool.debugInfo());
    }

    public void testStartAndStopEvictor() throws Exception {
        // set up pool without evictor
        pool.setMaxIdle(6);
        pool.setMaxActive(6);
        pool.setNumTestsPerEvictionRun(6);
        pool.setMinEvictableIdleTimeMillis(100L);

        for(int j=0;j<2;j++) {
            // populate the pool
            {
                Object[] active = new Object[6];
                for(int i=0;i<6;i++) {
                    active[i] = pool.borrowObject();
                }
                for(int i=0;i<6;i++) {
                    pool.returnObject(active[i]);
                }
            }
    
            // note that it stays populated
            assertEquals("Should have 6 idle",6,pool.getNumIdle());
    
            // start the evictor
            pool.setTimeBetweenEvictionRunsMillis(50L);
            
            // wait a second (well, .2 seconds)
            try { Thread.sleep(200L); } catch(Exception e) { }
            
            // assert that the evictor has cleared out the pool
            assertEquals("Should have 0 idle",0,pool.getNumIdle());
    
            // stop the evictor 
            pool.startEvictor(0L);
        }
    }

    public void testEvictionWithNegativeNumTests() throws Exception {
        // when numTestsPerEvictionRun is negative, it represents a fraction of the idle objects to test
        pool.setMaxIdle(6);
        pool.setMaxActive(6);
        pool.setNumTestsPerEvictionRun(-2);
        pool.setMinEvictableIdleTimeMillis(100L);
        pool.setTimeBetweenEvictionRunsMillis(100L);

        Object[] active = new Object[6];
        for(int i=0;i<6;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<6;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(100L); } catch(Exception e) { }
        assertTrue("Should at most 6 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 6);
        try { Thread.sleep(100L); } catch(Exception e) { }
        assertTrue("Should at most 3 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 3);
        try { Thread.sleep(100L); } catch(Exception e) { }
        assertTrue("Should be at most 2 idle, found " + pool.getNumIdle(),pool.getNumIdle() <= 2);
        try { Thread.sleep(100L); } catch(Exception e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());
    }

    public void testEviction() throws Exception {
        pool.setMaxIdle(500);
        pool.setMaxActive(500);
        pool.setNumTestsPerEvictionRun(100);
        pool.setMinEvictableIdleTimeMillis(500L);
        pool.setTimeBetweenEvictionRunsMillis(500L);

        Object[] active = new Object[500];
        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<500;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(Exception e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 500);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 400);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 300);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 200);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 100);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());

        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject();
        }
        for(int i=0;i<500;i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(1000L); } catch(Exception e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 500);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 400);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 300);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 200);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 100);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());
    }

    public void testThreaded1() throws Exception {
        pool.setMaxActive(15);
        pool.setMaxIdle(15);
        pool.setMaxWait(1000L);
        TestThread[] threads = new TestThread[20];
        for(int i=0;i<20;i++) {
            threads[i] = new TestThread(pool,100,50);
            Thread t = new Thread(threads[i]);
            t.start();
        }
        for(int i=0;i<20;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(Exception e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail();
            }
        }
    }

    class TestThread implements Runnable {
        java.util.Random _random = new java.util.Random();
        ObjectPool _pool = null;
        boolean _complete = false;
        boolean _failed = false;
        int _iter = 100;
        int _delay = 50;

        public TestThread(ObjectPool pool) {
            _pool = pool;
        }

        public TestThread(ObjectPool pool, int iter) {
            _pool = pool;
            _iter = iter;
        }

        public TestThread(ObjectPool pool, int iter, int delay) {
            _pool = pool;
            _iter = iter;
            _delay = delay;
        }

        public boolean complete() {
            return _complete;
        }

        public boolean failed() {
            return _failed;
        }

        public void run() {
            for(int i=0;i<_iter;i++) {
                try {
                    Thread.sleep((long)_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                Object obj = null;
                try {
                    obj = _pool.borrowObject();
                } catch(Exception e) {
                    _failed = true;
                    _complete = true;
                    break;
                }

                try {
                    Thread.sleep((long)_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                try {
                    _pool.returnObject(obj);
                } catch(Exception e) {
                    _failed = true;
                    _complete = true;
                    break;
                }
            }
            _complete = true;
        }
    }
    
    public void testAddObject() throws Exception {
        assertEquals("should be zero idle", 0, pool.getNumIdle());
    	pool.addObject();
		assertEquals("should be one idle", 1, pool.getNumIdle());
		assertEquals("should be zero active", 0, pool.getNumActive());
		Object obj = pool.borrowObject();
		assertEquals("should be zero idle", 0, pool.getNumIdle());
		assertEquals("should be one active", 1, pool.getNumActive());
		pool.returnObject(obj);
		assertEquals("should be one idle", 1, pool.getNumIdle());
		assertEquals("should be zero active", 0, pool.getNumActive());
    }
    
    private GenericObjectPool pool = null;

    static class SimpleFactory implements PoolableObjectFactory {
        public SimpleFactory() {
            this(true);
        }
        public SimpleFactory(boolean valid) {
            this.valid = valid;
        }
        public Object makeObject() { return String.valueOf(counter++); }
        public void destroyObject(Object obj) { }
        public boolean validateObject(Object obj) { return valid; }
        public void activateObject(Object obj) { }
        public void passivateObject(Object obj) { }
        int counter = 0;
        boolean valid = true;
    }

    private void assertConfiguration(GenericObjectPool.Config expected, GenericObjectPool actual) throws Exception {
        assertEquals("testOnBorrow",expected.testOnBorrow,actual.getTestOnBorrow());
        assertEquals("testOnReturn",expected.testOnReturn,actual.getTestOnReturn());
        assertEquals("testWhileIdle",expected.testWhileIdle,actual.getTestWhileIdle());
        assertEquals("whenExhaustedAction",expected.whenExhaustedAction,actual.getWhenExhaustedAction());
        assertEquals("maxActive",expected.maxActive,actual.getMaxActive());
        assertEquals("maxIdle",expected.maxIdle,actual.getMaxIdle());
        assertEquals("maxWait",expected.maxWait,actual.getMaxWait());
        assertEquals("minEvictableIdleTimeMillis",expected.minEvictableIdleTimeMillis,actual.getMinEvictableIdleTimeMillis());
        assertEquals("numTestsPerEvictionRun",expected.numTestsPerEvictionRun,actual.getNumTestsPerEvictionRun());
        assertEquals("timeBetweenEvictionRunsMillis",expected.timeBetweenEvictionRunsMillis,actual.getTimeBetweenEvictionRunsMillis());
    }

}


