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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestBaseKeyedObjectPool;

import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestGenericKeyedObjectPool extends TestBaseKeyedObjectPool {
    public TestGenericKeyedObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestGenericKeyedObjectPool.class);
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

    public void setUp() throws Exception {
        super.setUp();
        pool = new GenericKeyedObjectPool(new SimpleFactory());
    }

    public void tearDown() throws Exception {
        super.tearDown();
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

        try { Thread.sleep(1000L); } catch(Exception e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 500);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 400);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 300);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 200);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 100);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertEquals("Should be zero idle, found " + pool.getNumIdle(""),0,pool.getNumIdle(""));

        for(int i=0;i<500;i++) {
            active[i] = pool.borrowObject("");
        }
        for(int i=0;i<500;i++) {
            pool.returnObject("",active[i]);
        }

        try { Thread.sleep(1000L); } catch(Exception e) { }
        assertTrue("Should be less than 500 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 500);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 400 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 400);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 300 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 300);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 200 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 200);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 100 idle, found " + pool.getNumIdle(""),pool.getNumIdle("") < 100);
        try { Thread.sleep(600L); } catch(Exception e) { }
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

        try { Thread.sleep(1000L); } catch(Exception e) { }
        assertTrue("Should be less than 1000 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 1000);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 900 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 900);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 800 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 800);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 700 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 700);
        try { Thread.sleep(600L); } catch(Exception e) { }
        assertTrue("Should be less than 600 idle, found " + pool.getNumIdle(),pool.getNumIdle() < 600);
        try { Thread.sleep(600L); } catch(Exception e) { }
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

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[5];
        active[0] = pool.borrowObject(key);

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=1 ; i<5 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(key, active[i]);
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

        String key = "A";

        pool.preparePool(key, true);

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        Object[] active = new Object[10];

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);

        for(int i=0 ; i<5 ; i++) {
            pool.returnObject(key, active[i]);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 10 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 10);

        for(int i=0 ; i<10 ; i++) {
            active[i] = pool.borrowObject(key);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        for(int i=0 ; i<10 ; i++) {
            pool.returnObject(key, active[i]);
        }

        try { Thread.sleep(150L); } catch(Exception e) { }
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

        try { Thread.sleep(1500L); } catch(Exception e) { }
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

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 0 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 0);

        Object active = pool.borrowObject(key);
        assertNotNull(active);

        try { Thread.sleep(150L); } catch(Exception e) { }
        assertTrue("Should be 5 idle, found " + pool.getNumIdle(),pool.getNumIdle() == 5);
    }

    public void testFIFO() throws Exception {
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

    class TestThread implements Runnable {
        java.util.Random _random = new java.util.Random();
        KeyedObjectPool _pool = null;
        boolean _complete = false;
        boolean _failed = false;
        int _iter = 100;
        int _delay = 50;

        public TestThread(KeyedObjectPool pool) {
            _pool = pool;
        }

        public TestThread(KeyedObjectPool pool, int iter) {
            _pool = pool;
            _iter = iter;
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
                    Thread.sleep((long)_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                Object obj = null;
                try {
                    obj = _pool.borrowObject(key);
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
                    _pool.returnObject(key,obj);
                } catch(Exception e) {
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
        public Object makeObject(Object key) { return String.valueOf(key) + String.valueOf(counter++); }
        public void destroyObject(Object key, Object obj) { }
        public boolean validateObject(Object key, Object obj) { return valid; }
        public void activateObject(Object key, Object obj) { }
        public void passivateObject(Object key, Object obj) { }
        int counter = 0;
        boolean valid;
    }

    protected boolean isLifo() {
        return false;
    }

    protected boolean isFifo() {
        return true;
    }

}


