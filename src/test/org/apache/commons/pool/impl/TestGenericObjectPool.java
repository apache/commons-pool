/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.commons.pool.impl;

import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.24 $ $Date: 2004/08/10 19:39:13 $
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
        pool.close();
        pool = null;
    }

    /**
     * Activation failure on existing object doesn't fail the borrow 
     */
    public void testActivationException() throws Exception {
        SimpleFactory factory = new SimpleFactory(true, false);
        factory.setThrowExceptionOnActivate(true);
        factory.setValidationEnabled(false);
        GenericObjectPool pool = new GenericObjectPool(factory);
        
        Object obj1 = pool.borrowObject();
        pool.returnObject(obj1);

        // obj1 was returned to the pool but failed to activate the second borrow
        // a new object obj2 needs te be created
        Object obj2 = pool.borrowObject();
        assertTrue(obj1 != obj2);        
    }

    /**
     * Activation failure on new object fails the borrow 
     */
    public void testActivationExceptionOnNewObject() throws Exception {
        SimpleFactory factory = new SimpleFactory(true, false);
        factory.setThrowExceptionOnActivate(true);
        factory.setValidationEnabled(false);
        GenericObjectPool pool = new GenericObjectPool(factory);
        
        Object obj1 = pool.borrowObject();
        try {
            Object obj2 = pool.borrowObject();
            System.out.println("obj1: " + obj1);
            System.out.println("obj2: " + obj2);
            fail("a second object should have been created and failed to activate");
        }
        catch (Exception e) {}
    }

    public void testWhenExhaustedGrow() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
        pool.setMaxActive(1);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
        Object obj1 = pool.borrowObject();
        assertNotNull(obj1);
        Object obj2 = pool.borrowObject();
        assertNotNull(obj2);
        pool.returnObject(obj2);
        pool.returnObject(obj1);
        pool.close();
    }

    public void testWhenExhaustedFail() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
        pool.setMaxActive(1);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);
        Object obj1 = pool.borrowObject();
        assertNotNull(obj1);
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.returnObject(obj1);
        pool.close();
    }

    public void testWhenExhaustedBlock() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
        pool.setMaxActive(1);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
        pool.setMaxWait(10L);
        Object obj1 = pool.borrowObject();
        assertNotNull(obj1);
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.returnObject(obj1);
        pool.close();
    }

    public void testEvictWhileEmpty() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory(true,false));
        pool.evict();
        pool.evict();
        pool.close();
    }

    public void testExceptionOnPassivateDuringReturn() throws Exception {
        SimpleFactory factory = new SimpleFactory();        
        GenericObjectPool pool = new GenericObjectPool(factory);
        Object obj = pool.borrowObject();
        factory.setThrowExceptionOnPassivate(true);
        pool.returnObject(obj);
        assertEquals(0,pool.getNumIdle());
        pool.close();
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

    public void testWithSometimesInvalid() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory(true,false));
        pool.setMaxIdle(10);
        pool.setTestOnBorrow(true);
        pool.setTestOnReturn(true);
        pool.returnObject(pool.borrowObject());  
        assertEquals(0,pool.getNumIdle());      
    }

    public void testSetFactoryWithActiveObjects() throws Exception {
        GenericObjectPool pool = new GenericObjectPool();
        pool.setMaxIdle(10);
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject();
        assertNotNull(obj);
        try {
            pool.setFactory(null);
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
        try {
            pool.setFactory(new SimpleFactory());
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
    }

    public void testSetFactoryWithNoActiveObjects() throws Exception {
        GenericObjectPool pool = new GenericObjectPool();
        pool.setMaxIdle(10);
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject();
        pool.returnObject(obj);
        assertEquals(1,pool.getNumIdle());
        pool.setFactory(new SimpleFactory());
        assertEquals(0,pool.getNumIdle());
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

    public void testMaxActiveZero() throws Exception {
        pool.setMaxActive(0);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);

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
            assertNotNull(pool);
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
        {
            GenericObjectPool.Config expected = new GenericObjectPool.Config();
            expected.maxActive = 2;
            expected.maxIdle = 3;
            expected.minIdle = 1;
            expected.maxWait = 5L;
            expected.minEvictableIdleTimeMillis = 7L;
            expected.numTestsPerEvictionRun = 9;
            expected.testOnBorrow = true;
            expected.testOnReturn = true;
            expected.testWhileIdle = true;
            expected.timeBetweenEvictionRunsMillis = 11L;
            expected.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
            GenericObjectPool pool = new GenericObjectPool(null,expected.maxActive, expected.whenExhaustedAction, expected.maxWait, expected.maxIdle, expected.minIdle, expected.testOnBorrow, expected.testOnReturn, expected.timeBetweenEvictionRunsMillis, expected.numTestsPerEvictionRun, expected.minEvictableIdleTimeMillis, expected.testWhileIdle);
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
        GenericObjectPool pool = new GenericObjectPool(new SimpleFactory());
        pool.setMaxIdle(3);
        assertNotNull(pool.debugInfo());
        Object obj = pool.borrowObject();
        assertNotNull(pool.debugInfo());
        pool.returnObject(obj);
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
        pool.setMinEvictableIdleTimeMillis(50L);
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
        pool.setMinEvictableIdleTimeMillis(250L);
        pool.setTimeBetweenEvictionRunsMillis(500L);
        pool.setTestWhileIdle(true);

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

    public void testEvictionSoftMinIdle() throws Exception {
        GenericObjectPool pool = null;
        
        class TimeTest extends BasePoolableObjectFactory {
            private final long createTime;
            public TimeTest() {
                createTime = System.currentTimeMillis();
            }
            public Object makeObject() throws Exception {
                return new TimeTest();
            }
            public long getCreateTime() {
                return createTime;
            }
        }
        
        pool = new GenericObjectPool(new TimeTest());
        
        pool.setMaxIdle(5);
        pool.setMaxActive(5);
        pool.setNumTestsPerEvictionRun(5);
        pool.setMinEvictableIdleTimeMillis(3000L);
        pool.setTimeBetweenEvictionRunsMillis(250L);
        pool.setTestWhileIdle(true);
        pool.setSoftMinEvictableIdleTimeMillis(1000L);
        pool.setMinIdle(2);
        
        Object[] active = new Object[5];
        Long[] creationTime = new Long[5] ;
        for(int i=0;i<5;i++) {
            active[i] = pool.borrowObject();
            creationTime[i] = new Long(((TimeTest)active[i]).getCreateTime());
        }
        
        for(int i=0;i<5;i++) {
            pool.returnObject(active[i]);
        }
        
        try { Thread.sleep(1500L); } catch(Exception e) { }
        assertTrue("Should be 2 OLD idle, found " + pool.getNumIdle(),pool.getNumIdle() == 2 &&
                ((TimeTest)pool.borrowObject()).getCreateTime() == creationTime[3].longValue() &&
                ((TimeTest)pool.borrowObject()).getCreateTime() == creationTime[4].longValue());
        
        try { Thread.sleep(2000L); } catch(Exception e) { }
        assertTrue("Should be 2 NEW idle , found " + pool.getNumIdle(),pool.getNumIdle() == 2 &&
                ((TimeTest)pool.borrowObject()).getCreateTime() != creationTime[0].longValue() &&
                ((TimeTest)pool.borrowObject()).getCreateTime() != creationTime[1].longValue());
    }

    public void testMinIdle() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxActive(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[5];
        active[0] = pool.borrowObject();

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=1 ; i<5 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);
    }

    public void testMinIdleMaxActive() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxActive(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[10];

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);

        for(int i=0 ; i<10 ; i++) {
            active[i] = pool.borrowObject();
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        for(int i=0 ; i<10 ; i++) {
            pool.returnObject(active[i]);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);
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

    public class SimpleFactory implements PoolableObjectFactory {
        public SimpleFactory() {
            this(true);
        }
        public SimpleFactory(boolean valid) {
            this(valid,valid);
        }
        public SimpleFactory(boolean evalid, boolean ovalid) {
            evenValid = evalid;
            oddValid = ovalid;
        }
        void setValid(boolean valid) {
            setEvenValid(valid);
            setOddValid(valid);            
        }
        void setEvenValid(boolean valid) {
            evenValid = valid;
        }
        void setOddValid(boolean valid) {
            oddValid = valid;
        }
        public void setThrowExceptionOnPassivate(boolean bool) {
            exceptionOnPassivate = bool;
        }
    
        public Object makeObject() { return String.valueOf(makeCounter++); }
        public void destroyObject(Object obj) { }
        public boolean validateObject(Object obj) {
            if (enableValidation) { 
                return validateCounter++%2 == 0 ? evenValid : oddValid; 
            }
            else {
                return true;
            }
        }
        public void activateObject(Object obj) throws Exception {
            if (exceptionOnActivate) {
                if (!(validateCounter++%2 == 0 ? evenValid : oddValid)) {
                    throw new Exception();
                }
            }
        }
        public void passivateObject(Object obj) throws Exception {
            if(exceptionOnPassivate) {
                throw new Exception();
            }
        }
        int makeCounter = 0;
        int validateCounter = 0;
        boolean evenValid = true;
        boolean oddValid = true;
        boolean exceptionOnPassivate = false;
        boolean exceptionOnActivate = false;
        boolean enableValidation = true;

        public boolean isThrowExceptionOnActivate() {
            return exceptionOnActivate;
        }

        public void setThrowExceptionOnActivate(boolean b) {
            exceptionOnActivate = b;
        }

        public boolean isValidationEnabled() {
            return enableValidation;
        }

        public void setValidationEnabled(boolean b) {
            enableValidation = b;
        }
    }
}


