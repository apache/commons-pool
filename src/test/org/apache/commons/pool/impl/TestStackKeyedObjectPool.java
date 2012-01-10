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

import java.util.BitSet;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestBaseKeyedObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Id$
 */
public class TestStackKeyedObjectPool extends TestBaseKeyedObjectPool<String, String> {
    public TestStackKeyedObjectPool(String testName) {
        super(testName);
    }

    @Override
    protected KeyedObjectPool<String, String> makeEmptyPool(int mincapacity) {
        StackKeyedObjectPool<String, String> pool = new StackKeyedObjectPool<String, String>(new SimpleFactory(),mincapacity);
        return pool;
    }

    @Override
    protected KeyedObjectPool<Object, Integer> makeEmptyPool(KeyedPoolableObjectFactory<Object, Integer> factory) {
        return new StackKeyedObjectPool<Object, Integer>(factory);
    }

    @Override
    protected String getNthObject(Object key, int n) {
        return String.valueOf(key) + String.valueOf(n);
    }

    @Override
    protected String makeKey(int n) {
        return String.valueOf(n);
    }

    private StackKeyedObjectPool<String, String> pool = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        pool = new StackKeyedObjectPool<String, String>(
            new KeyedPoolableObjectFactory<String, String>()  {
                int counter = 0;
                public String makeObject(String key) { return String.valueOf(key) + String.valueOf(counter++); }
                public void destroyObject(String key, String obj) { }
                public boolean validateObject(String key, String obj) { return true; }
                public void activateObject(String key, String obj) { }
                public void passivateObject(String key, String obj) { }
            }
            );
    }


    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        pool = null;
    }

    public void testCloseBug() throws Exception {
        {
            String obj0 = pool.borrowObject("");
            String obj1 = pool.borrowObject("");
            assertEquals(2,pool.getNumActive(""));
            assertEquals(0,pool.getNumIdle(""));
            pool.returnObject("",obj1);
            pool.returnObject("",obj0);
            assertEquals(0,pool.getNumActive(""));
            assertEquals(2,pool.getNumIdle(""));
        }
        {
            String obj0 = pool.borrowObject("2");
            String obj1 = pool.borrowObject("2");
            assertEquals(2,pool.getNumActive("2"));
            assertEquals(0,pool.getNumIdle("2"));
            pool.returnObject("2",obj1);
            pool.returnObject("2",obj0);
            assertEquals(0,pool.getNumActive("2"));
            assertEquals(2,pool.getNumIdle("2"));
        }
        pool.close();
    }

    public void testIdleCap() throws Exception {
        String[] active = new String[100];
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
    
    /**
     * Verifies maxSleeping contract: When returnObject triggers maxSleeping exceeded,
     * the bottom (oldest) instance in the pool is destroyed to make room for the newly
     * returning instance, which is pushed onto the idle object stack.
     */
    public void testRemoveOldest() throws Exception {
        pool._maxSleeping = 2;
        String obj0 = pool.borrowObject("");
        String obj1 = pool.borrowObject("");
        String obj2 = pool.borrowObject("");
        pool.returnObject("", obj0); // Push 0 onto bottom of stack
        pool.returnObject("", obj1); // Push 1
        pool.returnObject("", obj2); // maxSleeping exceeded -> 0 destroyed, 2 pushed
        assertEquals("2", pool.borrowObject("")); // 2 was pushed on top
        assertEquals("1", pool.borrowObject("")); // 1 still there
        assertEquals("3", pool.borrowObject("")); // New instance created (0 is gone)
    }

    public void testPoolWithNullFactory() throws Exception {
        KeyedObjectPool<String, Integer> pool = new StackKeyedObjectPool<String, Integer>(10);
        for(int i=0;i<10;i++) {
            pool.returnObject("X",new Integer(i));
        }
        for(int j=0;j<3;j++) {
            Integer[] borrowed = new Integer[10];
            BitSet found = new BitSet();
            for(int i=0;i<10;i++) {
                borrowed[i] = pool.borrowObject("X");
                assertNotNull(borrowed);
                assertTrue(!found.get(borrowed[i].intValue()));
                found.set(borrowed[i].intValue());
            }
            for(int i=0;i<10;i++) {
                pool.returnObject("X",borrowed[i]);
            }
        }
        pool.invalidateObject("X",pool.borrowObject("X"));
        pool.invalidateObject("X",pool.borrowObject("X"));
        pool.clear("X");
        pool.clear();
    }

    public void testVariousConstructors() throws Exception {
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>();
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>(10);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>(10,5);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>(null);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>(null,10);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object, Object> pool = new StackKeyedObjectPool<Object, Object>(null,10,5);
            assertNotNull(pool);
        }
    }

    @Override
    public void testToString() throws Exception {
        StackKeyedObjectPool<String, String> pool = new StackKeyedObjectPool<String, String>(new SimpleFactory());
        assertNotNull(pool.toString());
        String obj = pool.borrowObject("key");
        assertNotNull(pool.toString());
        pool.returnObject("key",obj);
        assertNotNull(pool.toString());
    }

    public void testBorrowFromEmptyPoolWithNullFactory() throws Exception {
        KeyedObjectPool<String, Object> pool = new StackKeyedObjectPool<String, Object>();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }

    public void testSetFactory() throws Exception {
        KeyedObjectPool<String, String> pool = new StackKeyedObjectPool<String, String>();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.setFactory(new SimpleFactory());
        String obj = pool.borrowObject("x");
        assertNotNull(obj);
        pool.returnObject("x",obj);
    }

    public void testCantResetFactoryWithActiveObjects() throws Exception {
        KeyedObjectPool<String, String> pool = new StackKeyedObjectPool<String, String>();
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject("x");
        assertNotNull(obj);

        try {
            pool.setFactory(new SimpleFactory());
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
    }

    public void testCanResetFactoryWithoutActiveObjects() throws Exception {
        KeyedObjectPool<String, String> pool = new StackKeyedObjectPool<String, String>();
        {
            pool.setFactory(new SimpleFactory());
            String obj = pool.borrowObject("x");
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
        {
            pool.setFactory(new SimpleFactory());
            String obj = pool.borrowObject("x");
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
    }

    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        KeyedObjectPool<String, Integer> pool = new StackKeyedObjectPool<String, Integer>(
            new KeyedPoolableObjectFactory<String, Integer>() {
                int counter = 0;
                public Integer makeObject(String key) { return new Integer(counter++); }
                public void destroyObject(String key, Integer obj) { }
                public boolean validateObject(String key, Integer obj) {
                    return ((obj.intValue() % 2) == 1);
                }
                public void activateObject(String key, Integer obj) { }
                public void passivateObject(String key, Integer obj) {
                    if((obj.intValue() % 3) == 0) {
                        throw new RuntimeException("Couldn't passivate");
                    }
                }
            }
        );

        Integer[] obj = new Integer[10];
        for(int i=0;i<10;i++) {
            Integer object = null;
            int k = 0;
            while (object == null && k < 100) { // bound not really needed
                try {
                    k++;
                    object = pool.borrowObject("key");
                    obj[i] = object;
                } catch (NoSuchElementException ex) {
                    // Expected for evens, which fail validation
                }
            }
            assertEquals("Each time we borrow, get one more active.", i+1, pool.getNumActive());
        }
        // 1,3,5,...,19 pass validation, get checked out
        for(int i=0;i<10;i++) {
            pool.returnObject("key",obj[i]);
            assertEquals("Each time we borrow, get one less active.", 9-i, pool.getNumActive());
        }
        // 3, 9, 15 fail passivation.  
        assertEquals(7,pool.getNumIdle());
        assertEquals(new Integer(19), pool.borrowObject("key"));
        assertEquals(new Integer(17), pool.borrowObject("key"));
        assertEquals(new Integer(13), pool.borrowObject("key"));
        assertEquals(new Integer(11), pool.borrowObject("key"));
        assertEquals(new Integer(7), pool.borrowObject("key"));
        assertEquals(new Integer(5), pool.borrowObject("key"));
        assertEquals(new Integer(1), pool.borrowObject("key"));   
    }

    class SimpleFactory implements KeyedPoolableObjectFactory<String, String> {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        public String makeObject(String key) {
            int counter = 0;
            Integer Counter = map.get(key);
            if(null != Counter) {
                counter = Counter.intValue();
            }
            map.put(key,new Integer(counter + 1));
            return String.valueOf(key) + String.valueOf(counter);
        }
        public void destroyObject(String key, String obj) { }
        public boolean validateObject(String key, String obj) { return true; }
        public void activateObject(String key, String obj) { }
        public void passivateObject(String key, String obj) { }
    }

    @Override
    protected boolean isLifo() {
        return true;
    }

    @Override
    protected boolean isFifo() {
        return false;
    }
}
