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

import java.util.BitSet;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 1.11 $ $Date: 2004/02/28 11:46:11 $
 */
public class TestStackObjectPool extends TestObjectPool {
    public TestStackObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestStackObjectPool.class);
    }

    protected ObjectPool makeEmptyPool(int mincap) {
        return new StackObjectPool(new SimpleFactory());
    }
    
    protected Object getNthObject(int n) {
        return String.valueOf(n);
    }

    public void testIdleCap() throws Exception {
        ObjectPool pool = makeEmptyPool(8);
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

    public void testPoolWithNullFactory() throws Exception {
        ObjectPool pool = new StackObjectPool(10);
        for(int i=0;i<10;i++) {
            pool.returnObject(new Integer(i));
        }
        for(int j=0;j<3;j++) {
            Integer[] borrowed = new Integer[10];
            BitSet found = new BitSet();
            for(int i=0;i<10;i++) {
                borrowed[i] = (Integer)(pool.borrowObject());
                assertNotNull(borrowed);
                assertTrue(!found.get(borrowed[i].intValue()));
                found.set(borrowed[i].intValue());
            }
            for(int i=0;i<10;i++) {
                pool.returnObject(borrowed[i]);
            }
        }
        pool.invalidateObject(pool.borrowObject());
        pool.invalidateObject(pool.borrowObject());
        pool.clear();        
    }
    
    public void testBorrowFromEmptyPoolWithNullFactory() throws Exception {
        ObjectPool pool = new StackObjectPool();
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
    
    public void testSetFactory() throws Exception {
        ObjectPool pool = new StackObjectPool();
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject();
        assertNotNull(obj);
        pool.returnObject(obj);
    }

    public void testCantResetFactoryWithActiveObjects() throws Exception {
        ObjectPool pool = new StackObjectPool();
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject();
        assertNotNull(obj);

        try {
            pool.setFactory(new SimpleFactory());
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }        
    }
    
    public void testCanResetFactoryWithoutActiveObjects() throws Exception {
        ObjectPool pool = new StackObjectPool();
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject();        
            assertNotNull(obj);
            pool.returnObject(obj);
        }
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject();        
            assertNotNull(obj);
            pool.returnObject(obj);
        }
    }

    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        ObjectPool pool = new StackObjectPool(20);
        pool.setFactory(
            new PoolableObjectFactory() {
                int counter = 0;
                public Object makeObject() { return new Integer(counter++); }
                public void destroyObject(Object obj) { }
                public boolean validateObject(Object obj) {
                    if(obj instanceof Integer) {
                        return ((((Integer)obj).intValue() % 2) == 1);
                    } else {
                        return false;
                    }
                }
                public void activateObject(Object obj) { }
                public void passivateObject(Object obj) { 
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
            obj[i] = pool.borrowObject();
        }
        for(int i=0;i<10;i++) {
            pool.returnObject(obj[i]);
        }
        assertEquals(3,pool.getNumIdle());
    }
    
    public void testVariousConstructors() throws Exception {
        {
            StackObjectPool pool = new StackObjectPool();
        }
        {
            StackObjectPool pool = new StackObjectPool(10);
        }
        {
            StackObjectPool pool = new StackObjectPool(10,5);
        }
        {
            StackObjectPool pool = new StackObjectPool(null);
        }
        {
            StackObjectPool pool = new StackObjectPool(null,10);
        }
        {
            StackObjectPool pool = new StackObjectPool(null,10,5);
        }
    }

        
    static class SimpleFactory implements PoolableObjectFactory {
        int counter = 0;
        public Object makeObject() { return String.valueOf(counter++); }
        public void destroyObject(Object obj) { }
        public boolean validateObject(Object obj) { return true; }
        public void activateObject(Object obj) { }
        public void passivateObject(Object obj) { }
    }
}

