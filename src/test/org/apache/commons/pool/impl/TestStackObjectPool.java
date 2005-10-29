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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
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


    public void testBorrowWithSometimesInvalidObjects() throws Exception {
        ObjectPool pool = new StackObjectPool(20);
        pool.setFactory(
            new PoolableObjectFactory() {
                // factory makes Integer objects
                int counter = 0;
                public Object makeObject() { return new Integer(counter++); }
                public void destroyObject(Object obj) { }
                public boolean validateObject(Object obj) {
                    // only odd objects are valid
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
            assertEquals("Each time we borrow, get one more active.", i+1, pool.getNumActive());
            
        }
        for(int i=0;i<10;i++) {
            pool.returnObject(obj[i]);
            assertEquals("Each time we borrow, get one less active.", 9-i, pool.getNumActive());
        }
        assertEquals(7,pool.getNumIdle());
    }
    
    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        ObjectPool pool = new StackObjectPool(20);

        class TestingPoolableObjectFactory implements PoolableObjectFactory {
            // factory makes Integer objects
            int counter = 0;
            boolean reject = false;
            public Object makeObject() { return new Integer(counter++); }
            public void destroyObject(Object obj) { }
            public boolean validateObject(Object obj) {
                if (reject) {
                    // only odd objects are valid
                    if(obj instanceof Integer) {
                        return ((((Integer)obj).intValue() % 2) == 1);
                    } else {
                        return false;
                    }
                } else {
                    return true;
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
        };
        
        TestingPoolableObjectFactory factory = new TestingPoolableObjectFactory();
        
        pool.setFactory(factory);

        Object[] obj = new Object[10];
        for(int i=0;i<10;i++) {
            obj[i] = pool.borrowObject();
            assertEquals("Each time we borrow, get one more active.", i+1, pool.getNumActive());
            
        }
        
        // now reject even numbers
        factory.reject = true;
        
        for(int i=0;i<10;i++) {
            pool.returnObject(obj[i]);
            assertEquals("Each time we borrow, get one less active.", 9-i, pool.getNumActive());
        }
        assertEquals(3,pool.getNumIdle());
    }
    
    public void testVariousConstructors() throws Exception {
        {
            StackObjectPool pool = new StackObjectPool();
            assertNotNull(pool);
        }
        {
            StackObjectPool pool = new StackObjectPool(10);
            assertNotNull(pool);
        }
        {
            StackObjectPool pool = new StackObjectPool(10,5);
            assertNotNull(pool);
        }
        {
            StackObjectPool pool = new StackObjectPool(null);
            assertNotNull(pool);
        }
        {
            StackObjectPool pool = new StackObjectPool(null,10);
            assertNotNull(pool);
        }
        {
            StackObjectPool pool = new StackObjectPool(null,10,5);
            assertNotNull(pool);
        }
    }

    private final List destroyed = new ArrayList();
    public void testReturnObjectDiscardOrder() throws Exception {
        // setup
        // We need a factory that tracks what was discarded.
        PoolableObjectFactory pof = new PoolableObjectFactory() {
            int i = 0;
            public Object makeObject() throws Exception {
                return new Integer(i++);
            }

            public void destroyObject(Object obj) throws Exception {
                destroyed.add(obj);
            }

            public boolean validateObject(Object obj) {
                return obj instanceof Integer;
            }

            public void activateObject(Object obj) throws Exception {
            }

            public void passivateObject(Object obj) throws Exception {
            }
        };
        ObjectPool pool = new StackObjectPool(pof, 3);

        // borrow more objects than the pool can hold
        Integer i0 = (Integer)pool.borrowObject();
        Integer i1 = (Integer)pool.borrowObject();
        Integer i2 = (Integer)pool.borrowObject();
        Integer i3 = (Integer)pool.borrowObject();

        // tests
        // return as many as the pool will hold.
        pool.returnObject(i0);
        pool.returnObject(i1);
        pool.returnObject(i2);

        // the pool should now be full.
        assertEquals("No returned objects should have been destroyed yet.",0, destroyed.size());

        // cause the pool to discard a returned object.
        pool.returnObject(i3);
        assertEquals("One object should have been destroyed.", 1, destroyed.size());

        // check to see what object was destroyed
        Integer d = (Integer)destroyed.get(0);
        assertEquals("Destoryed objects should have the stalest object.", i0, d);
    }

    private List testFactorySequenceStates = new ArrayList(5);
    public void testFactorySequence() throws Exception {
        // setup
        // We need a factory that tracks method call sequence.
        PoolableObjectFactory pof = new PoolableObjectFactory() {
            public Object makeObject() throws Exception {
                testFactorySequenceStates.add("makeObject");
                return new Object();
            }

            public void activateObject(Object obj) throws Exception {
                testFactorySequenceStates.add("activateObject");
            }

            public boolean validateObject(Object obj) {
                testFactorySequenceStates.add("validateObject");
                return true;
            }

            public void passivateObject(Object obj) throws Exception {
                testFactorySequenceStates.add("passivateObject");
            }

            public void destroyObject(Object obj) throws Exception {
                testFactorySequenceStates.add("destroyObject");
            }
        };

        ObjectPool pool = new StackObjectPool(pof, 1);

        // check the order in which the factory is called during borrow
        testFactorySequenceStates.clear();
        Object o = pool.borrowObject();
        List desiredSequence = Arrays.asList(new String[] {
                "makeObject",
                "activateObject",
                "validateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called when returning an object
        testFactorySequenceStates.clear();
        pool.returnObject(o);
        desiredSequence = Arrays.asList(new String[] {
                "validateObject",
                "passivateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called during borrow again
        testFactorySequenceStates.clear();
        o = pool.borrowObject();
        desiredSequence = Arrays.asList(new String[] {
                "activateObject",
                "validateObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);

        // check the order in which the factory is called when invalidating an object
        testFactorySequenceStates.clear();
        pool.invalidateObject(o);
        desiredSequence = Arrays.asList(new String[] {
                "destroyObject"
        });
        assertEquals("Wrong sequence", desiredSequence, testFactorySequenceStates);
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

