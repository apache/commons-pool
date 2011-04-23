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

package org.apache.commons.pool.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestBaseKeyedObjectPool;
import org.apache.commons.pool.VisitTracker;
import org.apache.commons.pool.VisitTrackerFactory;
import org.apache.commons.pool.WaiterFactory;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestGenericKeyedObjectPool extends TestBaseKeyedObjectPool {
    public TestGenericKeyedObjectPool(String testName) {
        super(testName);
    }

    protected KeyedObjectPool makeEmptyPool(int mincapacity) {
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(
            new KeyedPoolableObjectFactory()  {
                HashMap map = new HashMap();
                public Object makeObject(Object key) {
                    int counter = 0;
                    Integer Counter = (Integer)(map.get(key));
                    if(null != Counter) {
                        counter = Counter.intValue();
                    }
                    map.put(key,new Integer(counter + 1));
                    return String.valueOf(key) + String.valueOf(counter);
                }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) { return true; }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) { }
            }
        );
        pool.setMaxActive(mincapacity);
        pool.setMaxIdle(mincapacity);
        return pool;
    }

    protected KeyedObjectPool makeEmptyPool(KeyedPoolableObjectFactory factory) {
        return new GenericKeyedObjectPool(factory);
    }

    protected Object getNthObject(Object key, int n) {
        return String.valueOf(key) + String.valueOf(n);
    }

    protected Object makeKey(int n) {
        return String.valueOf(n);
    }

    private GenericKeyedObjectPool pool = null;
    private final Integer zero = new Integer(0);
    private final Integer one = new Integer(1);
    private final Integer two = new Integer(2);

    public void setUp() throws Exception {
        super.setUp();
        pool = new GenericKeyedObjectPool(new SimpleFactory());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        pool.clear();
        pool.close();
        pool = null;
    }

    public void testNegativeMaxActive() throws Exception {
        pool.setMaxActive(-1);
        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);
        Object obj = pool.borrowObject("");
        assertEquals("0",obj);
        pool.returnObject("",obj);
    }

    public void testNumActiveNumIdle2() throws Exception {
        assertEquals(0,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        assertEquals(0,pool.getNumActive("A"));
        assertEquals(0,pool.getNumIdle("A"));
        assertEquals(0,pool.getNumActive("B"));
        assertEquals(0,pool.getNumIdle("B"));

        Object objA0 = pool.borrowObject("A");
        Object objB0 = pool.borrowObject("B");

        assertEquals(2,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        assertEquals(1,pool.getNumActive("A"));
        assertEquals(0,pool.getNumIdle("A"));
        assertEquals(1,pool.getNumActive("B"));
        assertEquals(0,pool.getNumIdle("B"));

        Object objA1 = pool.borrowObject("A");
        Object objB1 = pool.borrowObject("B");

        assertEquals(4,pool.getNumActive());
        assertEquals(0,pool.getNumIdle());
        assertEquals(2,pool.getNumActive("A"));
        assertEquals(0,pool.getNumIdle("A"));
        assertEquals(2,pool.getNumActive("B"));
        assertEquals(0,pool.getNumIdle("B"));

        pool.returnObject("A",objA0);
        pool.returnObject("B",objB0);

        assertEquals(2,pool.getNumActive());
        assertEquals(2,pool.getNumIdle());
        assertEquals(1,pool.getNumActive("A"));
        assertEquals(1,pool.getNumIdle("A"));
        assertEquals(1,pool.getNumActive("B"));
        assertEquals(1,pool.getNumIdle("B"));

        pool.returnObject("A",objA1);
        pool.returnObject("B",objB1);

        assertEquals(0,pool.getNumActive());
        assertEquals(4,pool.getNumIdle());
        assertEquals(0,pool.getNumActive("A"));
        assertEquals(2,pool.getNumIdle("A"));
        assertEquals(0,pool.getNumActive("B"));
        assertEquals(2,pool.getNumIdle("B"));
    }

    public void testMaxIdle() throws Exception {
        pool.setMaxActive(100);
        pool.setMaxIdle(8);
        Object[] active = new Object[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject("");
        }
        assertEquals(100,pool.getNumActive(""));
        assertEquals(0,pool.getNumIdle(""));
        for(int i=0;i<100;i++) {
            pool.returnObject("",active[i]);
            assertEquals(99 - i,pool.getNumActive(""));
            assertEquals((i < 8 ? i+1 : 8),pool.getNumIdle(""));
        }
        
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject("a");
        }
        assertEquals(100,pool.getNumActive("a"));
        assertEquals(0,pool.getNumIdle("a"));
        for(int i=0;i<100;i++) {
            pool.returnObject("a",active[i]);
            assertEquals(99 - i,pool.getNumActive("a"));
            assertEquals((i < 8 ? i+1 : 8),pool.getNumIdle("a"));
        }
        
        // total number of idle instances is twice maxIdle
        assertEquals(16, pool.getNumIdle());
        // Each pool is at the sup
        assertEquals(8, pool.getNumIdle(""));
        assertEquals(8, pool.getNumIdle("a"));
             
    }

    public void testMaxActive() throws Exception {
        pool.setMaxActive(3);
        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);

        pool.borrowObject("");
        pool.borrowObject("");
        pool.borrowObject("");
        try {
            pool.borrowObject("");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }

    public void testMaxActiveZero() throws Exception {
        pool.setMaxActive(0);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);

        try {
            pool.borrowObject("a");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
    
    public void testWhenExhaustedGrow() throws Exception {
        pool.setMaxActive(1);
        pool.setMaxTotal(1);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
        for (int i = 0; i < 10; i++) {
            pool.borrowObject("a");
        }
    }

    public void testMaxTotal() throws Exception {
        pool.setMaxActive(2);
        pool.setMaxTotal(3);
        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);

        Object o1 = pool.borrowObject("a");
        assertNotNull(o1);
        Object o2 = pool.borrowObject("a");
        assertNotNull(o2);
        Object o3 = pool.borrowObject("b");
        assertNotNull(o3);
        try {
            pool.borrowObject("c");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }

        assertEquals(0, pool.getNumIdle());

        pool.returnObject("b", o3);
        assertEquals(1, pool.getNumIdle());
        assertEquals(1, pool.getNumIdle("b"));

        Object o4 = pool.borrowObject("b");
        assertNotNull(o4);
        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumIdle("b"));
        
        pool.setMaxTotal(4);
        Object o5 = pool.borrowObject("b");
        assertNotNull(o5);
        
        assertEquals(2, pool.getNumActive("a"));
        assertEquals(2, pool.getNumActive("b"));
        assertEquals(pool.getMaxTotal(),
                pool.getNumActive("b") + pool.getNumActive("b"));
        assertEquals(pool.getNumActive(),
                pool.getMaxTotal());
    }

    public void testMaxTotalZero() throws Exception {
        pool.setMaxTotal(0);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL);

        try {
            pool.borrowObject("a");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }

    public void testMaxTotalLRU() throws Exception {
        pool.setMaxActive(2);
        pool.setMaxTotal(3);
//        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);

        Object o1 = pool.borrowObject("a");
        assertNotNull(o1);
        pool.returnObject("a", o1);
        Thread.sleep(25);

        Object o2 = pool.borrowObject("b");
        assertNotNull(o2);
        pool.returnObject("b", o2);
        Thread.sleep(25);

        Object o3 = pool.borrowObject("c");
        assertNotNull(o3);
        pool.returnObject("c", o3);
        Thread.sleep(25);

        Object o4 = pool.borrowObject("a");
        assertNotNull(o4);
        pool.returnObject("a", o4);
        Thread.sleep(25);

        assertSame(o1, o4);

        // this should cause b to be bumped out of the pool
        Object o5 = pool.borrowObject("d");
        assertNotNull(o5);
        pool.returnObject("d", o5);
        Thread.sleep(25);

        // now re-request b, we should get a different object because it should
        // have been expelled from pool (was oldest because a was requested after b)
        Object o6 = pool.borrowObject("b");
        assertNotNull(o6);
        pool.returnObject("b", o6);

        assertNotSame(o1, o6);

        // second a is still in there
        Object o7 = pool.borrowObject("a");
        assertNotNull(o7);
        pool.returnObject("a", o7);

        assertSame(o4, o7);
    }

    public void testSettersAndGetters() throws Exception {
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool();
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
            pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK,pool.getWhenExhaustedAction());
            pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_FAIL,pool.getWhenExhaustedAction());
            pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);
            assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW,pool.getWhenExhaustedAction());
        }
    }

    public void testEviction() throws Exception {
        pool.setMaxIdle(500);
        pool.setMaxActive(500);
        pool.setNumTestsPerEvictionRun(100);
        pool.setMinEvictableIdleTimeMillis(250L);
        pool.setTimeBetweenEvictionRunsMillis(500L);

        Object[] active = new Object[500];
        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject("");
        }
        for(int i=0;i<500;i++) {
            pool.returnObject("",active[i]);
        }

        try { Thread.sleep(1000L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 500);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 400);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 300);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 200);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 100);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(""),0,pool.getNumIdle(""));

        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject("");
        }
        for(int i=0;i<500;i++) {
            pool.returnObject("",active[i]);
        }

        try { Thread.sleep(1000L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 500);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 400);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 300);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 200);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 100);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(""),0,pool.getNumIdle(""));
    }

    public void testEviction2() throws Exception {
        pool.setMaxIdle(500);
        pool.setMaxActive(500);
        pool.setNumTestsPerEvictionRun(100);
        pool.setMinEvictableIdleTimeMillis(500L);
        pool.setTimeBetweenEvictionRunsMillis(500L);

        Object[] active = new Object[500];
        Object[] active2 = new Object[500];
        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject("");
            active2[i] = pool.borrowObject("2");
        }
        for(int i=0;i<500;i++) {
            pool.returnObject("",active[i]);
            pool.returnObject("2",active2[i]);
        }

        try { Thread.sleep(1100L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 1000 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 1000);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 900 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 900);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 800 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 800);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 700 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 700);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 600 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 600);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 500);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 400);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 300);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 200);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 100);
        try { Thread.sleep(600L); } catch(InterruptedException e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(),0,pool.getNumIdle());
    }

    /**
     * Kicks off <numThreads> test threads, each of which will go through
     * <iterations> borrow-return cycles with random delay times <= delay
     * in between.
     */
    public void runTestThreads(int numThreads, int iterations, int delay) {
        TestThread[] threads = new TestThread[numThreads];
        for(int i=0;i<numThreads;i++) {
            threads[i] = new TestThread(pool,iterations,delay);
            Thread t = new Thread(threads[i]);
            t.start();
        }
        for(int i=0;i<numThreads;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(500L);
                } catch(InterruptedException e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail("Thread failed: "+i+"\n"+getExceptionTrace(threads[i]._exception));
            }
        }
    }
    
    public void testThreaded1() throws Exception {
        pool.setMaxActive(15);
        pool.setMaxIdle(15);
        pool.setMaxWait(1000L);
        runTestThreads(20, 100, 50);
    }
    
    /**
     * Verifies that maxTotal is not exceeded when factory destroyObject
     * has high latency, testOnReturn is set and there is high incidence of
     * validation failures. 
     */
    public void testMaxTotalInvariant() throws Exception {
        int maxTotal = 15;
        SimpleFactory factory = new SimpleFactory();
        factory.setEvenValid(false);     // Every other validation fails
        factory.setDestroyLatency(100);  // Destroy takes 100 ms
        factory.setMaxActive(maxTotal);  // (makes - destroys) bound
        factory.setValidationEnabled(true);
        pool = new GenericKeyedObjectPool(factory);
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(-1);
        pool.setTestOnReturn(true);
        pool.setMaxWait(10000L);
        runTestThreads(5, 10, 50);
    }

    public void testMinIdle() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxActive(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);


        //Generate a random key
        String key = "A";

        pool.preparePool(key, true);

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[5];
        active[0] = pool.borrowObject(key);

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=1 ; i<5 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(key, active[i]);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
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

        String key = "A";

        pool.preparePool(key, true);
        assertTrue("Should be 5 idle, found " + 
                pool.getNumIdle(),pool.getNumIdle() == 5);

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[10];

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(key, active[i]);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);

        for(int i=0 ; i<10 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        for(int i=0 ; i<10 ; i++) {
            pool.returnObject(key, active[i]);
        }

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);
    }

    public void testMinIdleNoPopulateImmediately() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxActive(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(1000L);
        pool.setTestWhileIdle(true);


        //Generate a random key
        String key = "A";

        pool.preparePool(key, false);

        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        try { Thread.sleep(1500L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);
    }

    public void testMinIdleNoPreparePool() throws Exception {
        pool.setMaxIdle(500);
        pool.setMinIdle(5);
        pool.setMaxActive(10);
        pool.setNumTestsPerEvictionRun(0);
        pool.setMinEvictableIdleTimeMillis(50L);
        pool.setTimeBetweenEvictionRunsMillis(100L);
        pool.setTestWhileIdle(true);


        //Generate a random key
        String key = "A";

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        Object active = pool.borrowObject(key);
        assertNotNull(active);

        try { Thread.sleep(150L); } catch(InterruptedException e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);
    }

    public void testFIFO() throws Exception {
        pool.setLifo(false);
        final Object key = "key";
        pool.addObject(key); // "key0"
        pool.addObject(key); // "key1"
        pool.addObject(key); // "key2"
        assertEquals("Oldest", "key0", pool.borrowObject(key));
        assertEquals("Middle", "key1", pool.borrowObject(key));
        assertEquals("Youngest", "key2", pool.borrowObject(key));
        assertEquals("new-3", "key3", pool.borrowObject(key));
        pool.returnObject(key, "r");
        assertEquals("returned", "r", pool.borrowObject(key));
        assertEquals("new-4", "key4", pool.borrowObject(key));
    }
    
    public void testLIFO() throws Exception {
        pool.setLifo(true);
        final Object key = "key";
        pool.addObject(key); // "key0"
        pool.addObject(key); // "key1"
        pool.addObject(key); // "key2"
        assertEquals("Youngest", "key2", pool.borrowObject(key));
        assertEquals("Middle", "key1", pool.borrowObject(key));
        assertEquals("Oldest", "key0", pool.borrowObject(key));
        assertEquals("new-3", "key3", pool.borrowObject(key));
        pool.returnObject(key, "r");
        assertEquals("returned", "r", pool.borrowObject(key));
        assertEquals("new-4", "key4", pool.borrowObject(key));
    }
    
    /**
     * Test to make sure evictor visits least recently used objects first,
     * regardless of FIFO/LIFO 
     * 
     * JIRA: POOL-86
     */ 
    public void testEvictionOrder() throws Exception {
        checkEvictionOrder(false);
        checkEvictionOrder(true);
    }
    
    private void checkEvictionOrder(boolean lifo) throws Exception {
        SimpleFactory factory = new SimpleFactory();
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setNumTestsPerEvictionRun(2);
        pool.setMinEvictableIdleTimeMillis(100);
        pool.setLifo(lifo);
        
        for (int i = 0; i < 3; i ++) {
            Integer key = new Integer(i);
            for (int j = 0; j < 5; j++) {
                pool.addObject(key);
            }
        }
        
        // Make all evictable
        Thread.sleep(200);
        
        /* 
         * Initial state (Key, Object) pairs in order of age:
         * 
         * (0,0), (0,1), (0,2), (0,3), (0,4)
         * (1,5), (1,6), (1,7), (1,8), (1,9)
         * (2,10), (2,11), (2,12), (2,13), (2,14)
         */
        
        pool.evict(); // Kill (0,0),(0,1)
        assertEquals(3, pool.getNumIdle(zero));
        Object objZeroA = pool.borrowObject(zero);
        assertTrue(lifo ? objZeroA.equals("04") : objZeroA.equals("02"));
        assertEquals(2, pool.getNumIdle(zero));
        Object objZeroB = pool.borrowObject(zero);
        assertTrue(objZeroB.equals("03"));
        assertEquals(1, pool.getNumIdle(zero));
        
        pool.evict(); // Kill remaining 0 survivor and (1,5)
        assertEquals(0, pool.getNumIdle(zero));
        assertEquals(4, pool.getNumIdle(one));
        Object objOneA = pool.borrowObject(one);
        assertTrue(lifo ? objOneA.equals("19") : objOneA.equals("16"));
        assertEquals(3, pool.getNumIdle(one));
        Object objOneB = pool.borrowObject(one);
        assertTrue(lifo ? objOneB.equals("18") : objOneB.equals("17"));
        assertEquals(2, pool.getNumIdle(one));
        
        pool.evict(); // Kill remaining 1 survivors
        assertEquals(0, pool.getNumIdle(one));
        pool.evict(); // Kill (2,10), (2,11)
        assertEquals(3, pool.getNumIdle(two));
        Object objTwoA = pool.borrowObject(two);
        assertTrue(lifo ? objTwoA.equals("214") : objTwoA.equals("212"));
        assertEquals(2, pool.getNumIdle(two));
        pool.evict(); // All dead now
        assertEquals(0, pool.getNumIdle(two));  
        
        pool.evict(); // Should do nothing - make sure no exception
        // Currently 2 zero, 2 one and 1 two active. Return them
        pool.returnObject(zero, objZeroA);
        pool.returnObject(zero, objZeroB);
        pool.returnObject(one, objOneA);
        pool.returnObject(one, objOneB);
        pool.returnObject(two, objTwoA);
        // Remove all idle objects
        pool.clear();
        
        // Reload
        pool.setMinEvictableIdleTimeMillis(500);
        factory.counter = 0; // Reset counter
        for (int i = 0; i < 3; i ++) {
            Integer key = new Integer(i);
            for (int j = 0; j < 5; j++) {
                pool.addObject(key);
            }
            Thread.sleep(200);
        }
        
        // 0's are evictable, others not 
        pool.evict(); // Kill (0,0),(0,1)
        assertEquals(3, pool.getNumIdle(zero));
        pool.evict(); // Kill (0,2),(0,3)
        assertEquals(1, pool.getNumIdle(zero));
        pool.evict(); // Kill (0,4), leave (1,5)
        assertEquals(0, pool.getNumIdle(zero));
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        pool.evict(); // (1,6), (1,7)
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        pool.evict(); // (1,8), (1,9)
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        pool.evict(); // (2,10), (2,11)
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        pool.evict(); // (2,12), (2,13)
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        pool.evict(); // (2,14), (1,5)
        assertEquals(5, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        Thread.sleep(200); // Ones now timed out
        pool.evict(); // kill (1,6), (1,7) - (1,5) missed
        assertEquals(3, pool.getNumIdle(one));
        assertEquals(5, pool.getNumIdle(two));
        Object obj = pool.borrowObject(one);
        if (lifo) {
            assertEquals("19", obj);
        } else {
            assertEquals("15", obj);
        }
    }
    
    
    /**
     * Verifies that the evictor visits objects in expected order
     * and frequency. 
     */
    public void testEvictorVisiting() throws Exception {
        checkEvictorVisiting(true);
        checkEvictorVisiting(false);  
    }
    
    private void checkEvictorVisiting(boolean lifo) throws Exception {
        VisitTrackerFactory factory = new VisitTrackerFactory();
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setNumTestsPerEvictionRun(2);
        pool.setMinEvictableIdleTimeMillis(-1);
        pool.setTestWhileIdle(true);
        pool.setLifo(lifo);
        pool.setTestOnReturn(false);
        pool.setTestOnBorrow(false);
        for (int i = 0; i < 3; i ++) {
            factory.resetId();
            Integer key = new Integer(i);
            for (int j = 0; j < 8; j++) {
                pool.addObject(key);
            }
        }
        pool.evict(); // Visit oldest 2 - 00 and 01
        Object obj = pool.borrowObject(zero);
        pool.returnObject(zero, obj);
        obj = pool.borrowObject(zero);
        pool.returnObject(zero, obj);
        //  borrow, return, borrow, return 
        //  FIFO will move 0 and 1 to end - 2,3,4,5,6,7,0,1
        //  LIFO, 7 out, then in, then out, then in - 7,6,5,4,3,2,1,0
        pool.evict();  // Should visit 02 and 03 in either case
        for (int i = 0; i < 8; i++) {
            VisitTracker tracker = (VisitTracker) pool.borrowObject(zero);    
            if (tracker.getId() >= 4) {
                assertEquals("Unexpected instance visited " + tracker.getId(),
                        0, tracker.getValidateCount());
            } else {
                assertEquals("Instance " +  tracker.getId() + 
                        " visited wrong number of times.",
                        1, tracker.getValidateCount());
            }
        } 
        // 0's are all out
        
        pool.setNumTestsPerEvictionRun(3);
        
        pool.evict(); // 10, 11, 12
        pool.evict(); // 13, 14, 15
        
        obj = pool.borrowObject(one);
        pool.returnObject(one, obj);
        obj = pool.borrowObject(one);
        pool.returnObject(one, obj);
        obj = pool.borrowObject(one);
        pool.returnObject(one, obj);
        // borrow, return, borrow, return 
        //  FIFO 3,4,5,^,6,7,0,1,2
        //  LIFO 7,6,^,5,4,3,2,1,0
        // In either case, pointer should be at 6
        pool.evict();
        // LIFO - 16, 17, 20
        // FIFO - 16, 17, 10
        pool.evict();
        // LIFO - 21, 22, 23
        // FIFO - 11, 12, 20
        pool.evict();
        // LIFO - 24, 25, 26
        // FIFO - 21, 22, 23
        pool.evict();
        // LIFO - 27, skip, 10
        // FIFO - 24, 25, 26
        for (int i = 0; i < 8; i++) {
            VisitTracker tracker = (VisitTracker) pool.borrowObject(one);    
            if ((lifo && tracker.getId() > 0) || 
                    (!lifo && tracker.getId() > 2)) {
                assertEquals("Instance " +  tracker.getId() + 
                        " visited wrong number of times.",
                        1, tracker.getValidateCount());
            } else {
                assertEquals("Instance " +  tracker.getId() + 
                        " visited wrong number of times.",
                        2, tracker.getValidateCount());
            }
        } 
        
        // Randomly generate some pools with random numTests
        // and make sure evictor cycles through elements appropriately
        int[] smallPrimes = {2, 3, 5, 7};
        Random random = new Random();
        random.setSeed(System.currentTimeMillis());
        pool.setMaxIdle(-1);
        for (int i = 0; i < smallPrimes.length; i++) {
            pool.setNumTestsPerEvictionRun(smallPrimes[i]);
            for (int j = 0; j < 5; j++) {// Try the tests a few times
                pool.clear();
                assertEquals("NumIdle should be zero after clearing the pool",0,pool.getNumIdle());
                int zeroLength = 10 + random.nextInt(20);
                for (int k = 0; k < zeroLength; k++) {
                    pool.addObject(zero);
                }
                int oneLength = 10 + random.nextInt(20);
                for (int k = 0; k < oneLength; k++) {
                    pool.addObject(one);
                }
                int twoLength = 10 + random.nextInt(20);
                for (int k = 0; k < twoLength; k++) {
                    pool.addObject(two);
                }
                
                // Choose a random number of evictor runs
                int runs = 10 + random.nextInt(50);
                for (int k = 0; k < runs; k++) {
                    pool.evict();
                }
                
                // Total instances in pool
                int totalInstances = zeroLength + oneLength + twoLength;
                
                // Number of times evictor should have cycled through pools
                int cycleCount = (runs * pool.getNumTestsPerEvictionRun())
                    / totalInstances;
                
                // Look at elements and make sure they are visited cycleCount
                // or cycleCount + 1 times
                VisitTracker tracker = null;
                int visitCount = 0;
                for (int k = 0; k < zeroLength; k++) {
                    tracker = (VisitTracker) pool.borrowObject(zero); 
                    visitCount = tracker.getValidateCount();
                    if (visitCount < cycleCount || visitCount > cycleCount + 1){
                        fail(formatSettings("ZERO", "runs", runs, "lifo", lifo, "i", i, "j", j,
                                "k", k, "visitCount", visitCount, "cycleCount", cycleCount,
                                "totalInstances", totalInstances, zeroLength, oneLength, twoLength));
                    }
                }
                for (int k = 0; k < oneLength; k++) {
                    tracker = (VisitTracker) pool.borrowObject(one); 
                    visitCount = tracker.getValidateCount();
                    if (visitCount < cycleCount || visitCount > cycleCount + 1){
                        fail(formatSettings("ONE", "runs", runs, "lifo", lifo, "i", i, "j", j,
                                "k", k, "visitCount", visitCount, "cycleCount", cycleCount,
                                "totalInstances", totalInstances, zeroLength, oneLength, twoLength));
                    }
                }
                int visits[] = new int[twoLength];
                for (int k = 0; k < twoLength; k++) {
                    tracker = (VisitTracker) pool.borrowObject(two); 
                    visitCount = tracker.getValidateCount();
                    visits[k] = visitCount;
                    if (visitCount < cycleCount || visitCount > cycleCount + 1){
                        StringBuffer sb = new StringBuffer("Visits:");
                        for (int l = 0; l <= k; l++){
                            sb.append(visits[l]).append(' ');
                        }
                        fail(formatSettings("TWO "+sb.toString(), "runs", runs, "lifo", lifo, "i", i, "j", j,
                                "k", k, "visitCount", visitCount, "cycleCount", cycleCount,
                                "totalInstances", totalInstances, zeroLength, oneLength, twoLength));
                    }
                }
            }
        }
    }
    
    public void testConstructors() {
        
        // Make constructor arguments all different from defaults
        int maxActive = 1;
        int maxIdle = 2;
        long maxWait = 3;
        int minIdle = 4;
        int maxTotal = 5;
        long minEvictableIdleTimeMillis = 6;
        int numTestsPerEvictionRun = 7;
        boolean testOnBorrow = true;
        boolean testOnReturn = true;
        boolean testWhileIdle = true;
        long timeBetweenEvictionRunsMillis = 8;
        byte whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        boolean lifo = false;
        
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool();
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_ACTIVE, pool.getMaxActive());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_IDLE, pool.getMaxIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_WAIT, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,
                pool.getTestOnBorrow());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,
                pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,
                pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        config.lifo = lifo;
        config.maxActive = maxActive;
        config.maxIdle = maxIdle;
        config.minIdle = minIdle;
        config.maxTotal = maxTotal;
        config.maxWait = maxWait;
        config.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        config.numTestsPerEvictionRun = numTestsPerEvictionRun;
        config.testOnBorrow = testOnBorrow;
        config.testOnReturn = testOnReturn;
        config.testWhileIdle = testWhileIdle;
        config.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        config.whenExhaustedAction = whenExhaustedAction;
        pool = new GenericKeyedObjectPool(null, config);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(minIdle, pool.getMinIdle());
        assertEquals(maxTotal, pool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun, pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow,pool.getTestOnBorrow());
        assertEquals(testOnReturn,pool.getTestOnReturn());
        assertEquals(testWhileIdle,pool.getTestWhileIdle());
        assertEquals(timeBetweenEvictionRunsMillis,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(lifo, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_IDLE, pool.getMaxIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_WAIT, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,
                pool.getTestOnBorrow());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,
                pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,
                pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction, maxWait);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_IDLE, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,
                pool.getTestOnBorrow());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,
                pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                   maxWait, testOnBorrow, testOnReturn);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_IDLE, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow,pool.getTestOnBorrow());
        assertEquals(testOnReturn,pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,
                pool.getTestOnBorrow());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,
                pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());

        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle, testOnBorrow, testOnReturn);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow, pool.getTestOnBorrow());
        assertEquals(testOnReturn, pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE,
                pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());

        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle, testOnBorrow, testOnReturn,
                timeBetweenEvictionRunsMillis, numTestsPerEvictionRun,
                minEvictableIdleTimeMillis, testWhileIdle);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, pool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow, pool.getTestOnBorrow());
        assertEquals(testOnReturn, pool.getTestOnReturn());
        assertEquals(testWhileIdle,
                pool.getTestWhileIdle());
        assertEquals(timeBetweenEvictionRunsMillis,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle, maxTotal, testOnBorrow, testOnReturn,
                timeBetweenEvictionRunsMillis, numTestsPerEvictionRun,
                minEvictableIdleTimeMillis, testWhileIdle);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(GenericKeyedObjectPool.DEFAULT_MIN_IDLE, pool.getMinIdle());
        assertEquals(maxTotal, pool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow, pool.getTestOnBorrow());
        assertEquals(testOnReturn, pool.getTestOnReturn());
        assertEquals(testWhileIdle,
                pool.getTestWhileIdle());
        assertEquals(timeBetweenEvictionRunsMillis,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle, maxTotal, minIdle, testOnBorrow, testOnReturn,
                timeBetweenEvictionRunsMillis, numTestsPerEvictionRun,
                minEvictableIdleTimeMillis, testWhileIdle);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(minIdle, pool.getMinIdle());
        assertEquals(maxTotal, pool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow, pool.getTestOnBorrow());
        assertEquals(testOnReturn, pool.getTestOnReturn());
        assertEquals(testWhileIdle,
                pool.getTestWhileIdle());
        assertEquals(timeBetweenEvictionRunsMillis,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(GenericKeyedObjectPool.DEFAULT_LIFO, pool.getLifo());
        
        pool = new GenericKeyedObjectPool(null, maxActive, whenExhaustedAction,
                maxWait, maxIdle, maxTotal, minIdle, testOnBorrow, testOnReturn,
                timeBetweenEvictionRunsMillis, numTestsPerEvictionRun,
                minEvictableIdleTimeMillis, testWhileIdle, lifo);
        assertEquals(maxActive, pool.getMaxActive());
        assertEquals(maxIdle, pool.getMaxIdle());
        assertEquals(maxWait, pool.getMaxWait());
        assertEquals(minIdle, pool.getMinIdle());
        assertEquals(maxTotal, pool.getMaxTotal());
        assertEquals(minEvictableIdleTimeMillis,
                pool.getMinEvictableIdleTimeMillis());
        assertEquals(numTestsPerEvictionRun,
                pool.getNumTestsPerEvictionRun());
        assertEquals(testOnBorrow, pool.getTestOnBorrow());
        assertEquals(testOnReturn, pool.getTestOnReturn());
        assertEquals(testWhileIdle,
                pool.getTestWhileIdle());
        assertEquals(timeBetweenEvictionRunsMillis,
                pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(whenExhaustedAction,pool.getWhenExhaustedAction());
        assertEquals(lifo, pool.getLifo());  
    }
    
    public void testExceptionOnPassivateDuringReturn() throws Exception {
        SimpleFactory factory = new SimpleFactory();        
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        Object obj = pool.borrowObject("one");
        factory.setThrowExceptionOnPassivate(true);
        pool.returnObject("one", obj);
        assertEquals(0,pool.getNumIdle());
        pool.close();
    }
    
    public void testExceptionOnDestroyDuringBorrow() throws Exception {
        SimpleFactory factory = new SimpleFactory(); 
        factory.setThrowExceptionOnDestroy(true);
        factory.setValidationEnabled(true);
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setTestOnBorrow(true);
        pool.borrowObject("one");
        factory.setValid(false); // Make validation fail on next borrow attempt
        try {
            pool.borrowObject("one");
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // expected
        }
        assertEquals(1, pool.getNumActive("one"));
        assertEquals(0, pool.getNumIdle("one"));
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }
    
    public void testExceptionOnDestroyDuringReturn() throws Exception {
        SimpleFactory factory = new SimpleFactory(); 
        factory.setThrowExceptionOnDestroy(true);
        factory.setValidationEnabled(true);
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setTestOnReturn(true);
        Object obj1 = pool.borrowObject("one");
        pool.borrowObject("one");
        factory.setValid(false); // Make validation fail
        pool.returnObject("one", obj1);
        assertEquals(1, pool.getNumActive("one"));
        assertEquals(0, pool.getNumIdle("one"));
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }
    
    public void testExceptionOnActivateDuringBorrow() throws Exception {
        SimpleFactory factory = new SimpleFactory(); 
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        Object obj1 = pool.borrowObject("one");
        Object obj2 = pool.borrowObject("one");
        pool.returnObject("one", obj1);
        pool.returnObject("one", obj2);
        factory.setThrowExceptionOnActivate(true);
        factory.setEvenValid(false);  
        // Activation will now throw every other time
        // First attempt throws, but loop continues and second succeeds
        Object obj = pool.borrowObject("one");
        assertEquals(1, pool.getNumActive("one"));
        assertEquals(0, pool.getNumIdle("one"));
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        
        pool.returnObject("one", obj);
        factory.setValid(false);
        // Validation will now fail on activation when borrowObject returns
        // an idle instance, and then when attempting to create a new instance
        try {
            pool.borrowObject("one");
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // expected
        }
        assertEquals(0, pool.getNumActive("one"));
        assertEquals(0, pool.getNumIdle("one"));
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }
    
    public void testBlockedKeyDoesNotBlockPool() throws Exception {
        SimpleFactory factory = new SimpleFactory();
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
        pool.setMaxWait(5000);
        pool.setMaxActive(1);
        pool.setMaxTotal(-1);
        pool.borrowObject("one");
        long start = System.currentTimeMillis();
        // Needs to be in a separate thread as this will block
        Runnable simple = new SimpleTestThread(pool, "one");
        (new Thread(simple)).start();
        // This should be almost instant. If it isn't it means this thread got
        // stuck behind the thread created above which is bad.
        // Give other thread a chance to start
        Thread.sleep(1000);
        pool.borrowObject("two");
        long end = System.currentTimeMillis();
        // If it fails it will be more than 4000ms (5000 less the 1000 sleep)
        // If it passes it should be almost instant
        // Use 3000ms as the threshold - should avoid timing issues on most
        // (all? platforms)
        assertTrue ("Elapsed time: "+(end-start)+" should be less than 4000",(end-start) < 4000);
        
    }

    private static final boolean DISPLAY_THREAD_DETAILS=
        Boolean.valueOf(System.getProperty("TestGenericKeyedObjectPool.display.thread.details", "false")).booleanValue();
    // To pass this to a Maven test, use:
    // mvn test -DargLine="-DTestGenericKeyedObjectPool.display.thread.details=true"
    // @see http://jira.codehaus.org/browse/SUREFIRE-121

    /*
     * Test multi-threaded pool access.
     * Multiple keys, multiple threads, but maxActive only allows half the threads to succeed.
     * 
     * This test was prompted by Continuum build failures in the Commons DBCP test case:
     * TestSharedPoolDataSource.testMultipleThreads2()
     * Let's see if the this fails on Continuum too!
     */
    public void testMaxWaitMultiThreaded() throws Exception {
        final long maxWait = 500; // wait for connection
        final long holdTime = 2 * maxWait; // how long to hold connection
        final int keyCount = 4; // number of different keys
        final int threadsPerKey = 5; // number of threads to grab the key initially
        SimpleFactory factory = new SimpleFactory();
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(factory);
        pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
        pool.setMaxWait(maxWait);
        pool.setMaxActive(threadsPerKey);
        // Create enough threads so half the threads will have to wait
        WaitingTestThread wtt[] = new WaitingTestThread[keyCount * threadsPerKey * 2];
        for(int i=0; i < wtt.length; i++){
            wtt[i] = new WaitingTestThread(pool,Integer.toString(i % keyCount),holdTime);
        }
        long origin = System.currentTimeMillis()-1000;
        for(int i=0; i < wtt.length; i++){
            wtt[i].start();
        }
        int failed = 0;
        for(int i=0; i < wtt.length; i++){
            wtt[i].join();
            if (wtt[i]._thrown != null){
                failed++;
            }
        }
        if (DISPLAY_THREAD_DETAILS || wtt.length/2 != failed){
            System.out.println(
                    "MaxWait: "+maxWait
                    +" HoldTime: "+holdTime
                    +" KeyCount: "+keyCount
                    +" MaxActive: "+threadsPerKey
                    +" Threads: "+wtt.length
                    +" Failed: "+failed
                    );
            for(int i=0; i < wtt.length; i++){
                WaitingTestThread wt = wtt[i];
                System.out.println(
                        "Preborrow: "+(wt.preborrow-origin)
                        + " Postborrow: "+(wt.postborrow != 0 ? wt.postborrow-origin : -1)
                        + " BorrowTime: "+(wt.postborrow != 0 ? wt.postborrow-wt.preborrow : -1)
                        + " PostReturn: "+(wt.postreturn != 0 ? wt.postreturn-origin : -1)
                        + " Ended: "+(wt.ended-origin)
                        + " Key: "+(wt._key)
                        + " ObjId: "+wt.objectId
                        );
            }            
        }
        assertEquals("Expected half the threads to fail",wtt.length/2,failed);
    }

    /**
     * Test case for POOL-180.
     */
    public void testMaxActivePerKeyExceeded() {
        WaiterFactory factory = new WaiterFactory(0, 20, 0, 0, 0, 0, 8, 5, 0);
        pool = new GenericKeyedObjectPool(factory);
        pool.setMaxActive(5);
        pool.setMaxTotal(8);
        pool.setTestOnBorrow(true);
        pool.setMaxIdle(5);
        pool.setMaxWait(-1);
        runTestThreads(20, 300, 250);
    }
    
    /*
     * Very simple test thread that just tries to borrow an object from
     * the provided pool with the specified key and returns it
     */
    static class SimpleTestThread implements Runnable {
        private final KeyedObjectPool _pool;
        private final String _key;
        
        public SimpleTestThread(KeyedObjectPool pool, String key) {
            _pool = pool;
            _key = key;
        }

        public void run() {
            try {
                Object obj = _pool.borrowObject(_key);
                _pool.returnObject(_key, obj);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /*
     * Very simple test thread that just tries to borrow an object from
     * the provided pool with the specified key and returns it after a wait
     */
    static class WaitingTestThread extends Thread {
        private final KeyedObjectPool _pool;
        private final String _key;
        private final long _pause;
        private Throwable _thrown;
        
        private long preborrow; // just before borrow
        private long postborrow; //  borrow returned
        private long postreturn; // after object was returned
        private long ended;
        private String objectId;

        public WaitingTestThread(KeyedObjectPool pool, String key, long pause) {
            _pool = pool;
            _key = key;
            _pause = pause;
            _thrown = null;
        }

        public void run() {
            try {
                preborrow = System.currentTimeMillis();
                Object obj = _pool.borrowObject(_key);
                objectId=obj.toString();
                postborrow = System.currentTimeMillis();
                Thread.sleep(_pause);
                _pool.returnObject(_key, obj);
                postreturn = System.currentTimeMillis();
            } catch (Exception e) {
                _thrown = e;
            } finally{
                ended = System.currentTimeMillis();
            }
        }
    }
    
    static class TestThread implements Runnable {
        private final java.util.Random _random = new java.util.Random();
        
        // Thread config items
        private final KeyedObjectPool _pool;
        private final int _iter;
        private final int _delay;

        private volatile boolean _complete = false;
        private volatile boolean _failed = false;
        private volatile Exception _exception;

        public TestThread(KeyedObjectPool pool) {
            this(pool, 100, 50);
        }

        public TestThread(KeyedObjectPool pool, int iter) {
            this(pool, iter, 50);
        }

        public TestThread(KeyedObjectPool pool, int iter, int delay) {
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
                String key = String.valueOf(_random.nextInt(3));
                try {
                    Thread.sleep(_random.nextInt(_delay));
                } catch(InterruptedException e) {
                    // ignored
                }
                Object obj = null;
                try {
                    obj = _pool.borrowObject(key);
                } catch(Exception e) {
                    _exception = e;
                    _failed = true;
                    _complete = true;
                    break;
                }

                try {
                    Thread.sleep(_random.nextInt(_delay));
                } catch(InterruptedException e) {
                    // ignored
                }
                try {
                    _pool.returnObject(key,obj);
                } catch(Exception e) {
                    _exception = e;
                    _failed = true;
                    _complete = true;
                    break;
                }
            }
            _complete = true;
        }
    }

    static class SimpleFactory implements KeyedPoolableObjectFactory {
        public SimpleFactory() {
            this(true);
        }
        public SimpleFactory(boolean valid) {
            this.valid = valid;
        }
        public Object makeObject(Object key) {
            synchronized(this) {
                activeCount++;
                if (activeCount > maxActive) {
                    throw new IllegalStateException(
                        "Too many active instances: " + activeCount);
                }
            }
            return String.valueOf(key) + String.valueOf(counter++);
        }
        public void destroyObject(Object key, Object obj) throws Exception {
            doWait(destroyLatency);
            synchronized(this) {
                activeCount--;
            }
            if (exceptionOnDestroy) {
                throw new Exception();
            }
        }
        public boolean validateObject(Object key, Object obj) {
            if (enableValidation) { 
                return validateCounter++%2 == 0 ? evenValid : oddValid; 
            } else {
                return valid;
            }
        }
        public void activateObject(Object key, Object obj) throws Exception {
            if (exceptionOnActivate) {
                if (!(validateCounter++%2 == 0 ? evenValid : oddValid)) {
                    throw new Exception();
                }
            }
        }
        public void passivateObject(Object key, Object obj) throws Exception {
            if (exceptionOnPassivate) {
                throw new Exception();
            }
        }
        
        public void setMaxActive(int maxActive) {
            this.maxActive = maxActive;
        }
        public void setDestroyLatency(long destroyLatency) {
            this.destroyLatency = destroyLatency;
        }
        public void setValidationEnabled(boolean b) {
            enableValidation = b;
        }
        void setEvenValid(boolean valid) {
            evenValid = valid;
        }
        void setValid(boolean valid) {
            evenValid = valid;
            oddValid = valid;
        }
        
        public void setThrowExceptionOnActivate(boolean b) {
            exceptionOnActivate = b;
        }
        
        public void setThrowExceptionOnDestroy(boolean b) {
            exceptionOnDestroy = b;
        }
        
        public void setThrowExceptionOnPassivate(boolean b) {
            exceptionOnPassivate = b;
        }
        
        int counter = 0;
        boolean valid;
        
        int activeCount = 0;
        int validateCounter = 0;
        boolean evenValid = true;
        boolean oddValid = true;
        boolean enableValidation = false;
        long destroyLatency = 0;
        int maxActive = Integer.MAX_VALUE;
        boolean exceptionOnPassivate = false;
        boolean exceptionOnActivate = false;
        boolean exceptionOnDestroy = false;
        
        private void doWait(long latency) {
            try {
                Thread.sleep(latency);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
    }

    protected boolean isLifo() {
        return true;
    }

    protected boolean isFifo() {
        return false;
    }

    private String getExceptionTrace(Throwable t){
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    private String formatSettings(String title, String s, int i, String s0, boolean b0, String s1, int i1, String s2, int i2, String s3, int i3,
            String s4, int i4, String s5, int i5, String s6, int i6, int zeroLength, int oneLength, int twoLength){
        StringBuffer sb = new StringBuffer(80);
        sb.append(title).append(' ');
        sb.append(s).append('=').append(i).append(' ');
        sb.append(s0).append('=').append(b0).append(' ');
        sb.append(s1).append('=').append(i1).append(' ');
        sb.append(s2).append('=').append(i2).append(' ');
        sb.append(s3).append('=').append(i3).append(' ');
        sb.append(s4).append('=').append(i4).append(' ');
        sb.append(s5).append('=').append(i5).append(' ');
        sb.append(s6).append('=').append(i6).append(' ');
        sb.append("Lengths=").append(zeroLength).append(',').append(oneLength).append(',').append(twoLength).append(' ');
        return sb.toString();
    }
    
}


