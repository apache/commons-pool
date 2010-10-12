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
import java.util.Random;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestBaseKeyedObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestStackKeyedObjectPool extends TestBaseKeyedObjectPool {
    public TestStackKeyedObjectPool(String testName) {
        super(testName);
    }

    @Override
    protected KeyedObjectPool<Object,Object> makeEmptyPool(int mincapacity) {
        StackKeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(new SimpleFactory(),mincapacity);
        return pool;
    }

    @Override
    protected KeyedObjectPool<Object,Object> makeEmptyPool(KeyedPoolableObjectFactory<Object,Object> factory) {
        return new StackKeyedObjectPool<Object,Object>(factory);
    }

    @Override
    protected Object getNthObject(Object key, int n) {
        return String.valueOf(key) + String.valueOf(n);
    }

    @Override
    protected Object makeKey(int n) {
        return String.valueOf(n);
    }

    private StackKeyedObjectPool<String,String> pool = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        pool = new StackKeyedObjectPool<String,String>(
            new KeyedPoolableObjectFactory<String,String>()  {
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
        pool.setMaxSleeping(2);
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

    public void testVariousConstructors() throws Exception {
        {
            StackKeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(new SimpleFactory());
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(new SimpleFactory(),10);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(new SimpleFactory(),10,5);
            assertNotNull(pool);
        }
    }

    @Override
    public void testToString() throws Exception {
        StackKeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(new SimpleFactory());
        assertNotNull(pool.toString());
        Object obj = pool.borrowObject("key");
        assertNotNull(pool.toString());
        pool.returnObject("key",obj);
        assertNotNull(pool.toString());
    }

    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        KeyedObjectPool<Object,Object> pool = new StackKeyedObjectPool<Object,Object>(
            new KeyedPoolableObjectFactory<Object,Object>() {
                int counter = 0;
                public Object makeObject(Object key) { return new Integer(counter++); }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) {
                    if(obj instanceof Integer) {
                        return ((((Integer)obj).intValue() % 2) == 1);
                    } else {
                        return false;
                    }
                }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) {
                    if(obj instanceof Integer) {
                        if((((Integer)obj).intValue() % 3) == 0) {
                            throw new RuntimeException("Couldn't passivate");
                        }
                    } else {
                        throw new RuntimeException("Couldn't passivate");
                    }
                }
            }
        );

        Object[] obj = new Object[10];
        for(int i=0;i<10;i++) {
            Object object = null;
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

    class SimpleFactory implements KeyedPoolableObjectFactory<Object,Object> {
        HashMap<Object,Integer> map = new HashMap<Object,Integer>();
        public Object makeObject(Object key) {
            int counter = 0;
            Integer Counter = map.get(key);
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

    @Override
    protected boolean isLifo() {
        return true;
    }

    @Override
    protected boolean isFifo() {
        return false;
    }
}
