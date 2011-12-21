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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestBaseObjectPool;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestStackObjectPool extends TestBaseObjectPool {
    public TestStackObjectPool(String testName) {
        super(testName);
    }

    protected ObjectPool makeEmptyPool(int mincap) {
        return new StackObjectPool(new SimpleFactory());
    }

    protected ObjectPool makeEmptyPool(final PoolableObjectFactory factory) {
        return new StackObjectPool(factory);
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

    /**
     * @deprecated - to be removed in pool 2.0
     */
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
    
    /**
     * @deprecated - to be removed in pool 2.0
     */
    public void testBorrowFromEmptyPoolWithNullFactory() throws Exception {
        ObjectPool pool = new StackObjectPool();
        try {
            pool.borrowObject();
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
    
    /**
     * @deprecated - to be removed in pool 2.0
     */
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

    /**
     * @deprecated - to be removed in pool 2.0
     */
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
    
    /**
     * @deprecated - to be removed in pool 2.0
     */
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

    /**
     * Verifies that validation failures when borrowing newly created instances
     * from the pool result in NoSuchElementExceptions and passivation failures
     * result in instances not being returned to the pool.
     */
    public void testBorrowWithSometimesInvalidObjects() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        factory.setValidateSelectively(true);  // Even numbers fail validation
        factory.setPassivateSelectively(true); // Multiples of 3 fail passivation
        ObjectPool pool = new StackObjectPool(factory, 20);
        Object[] obj = new Object[10];
        for(int i=0;i<10;i++) {
            Object object = null;
            int k = 0;
            while (object == null && k < 100) { // bound not really needed
                try {
                    k++;
                    object = pool.borrowObject();
                    if (((Integer) object).intValue() % 2 == 0) {
                        fail("Expecting NoSuchElementException");
                    } else {
                        obj[i] = object; 
                    }
                } catch (NoSuchElementException ex) {
                    // Should fail for evens
                }
            }
            assertEquals("Each time we borrow, get one more active.", i+1, pool.getNumActive());
        }
        // 1,3,5,...,19 pass validation, get checked out
        for(int i=0;i<10;i++) {
            pool.returnObject(obj[i]);
            assertEquals("Each time we return, get one less active.", 9-i, pool.getNumActive());
        }
        // 3, 9, 15 fail passivation.  
        assertEquals(7,pool.getNumIdle());
        assertEquals(new Integer(19), pool.borrowObject());
        assertEquals(new Integer(17), pool.borrowObject());
        assertEquals(new Integer(13), pool.borrowObject());
        assertEquals(new Integer(11), pool.borrowObject());
        assertEquals(new Integer(7), pool.borrowObject());
        assertEquals(new Integer(5), pool.borrowObject());
        assertEquals(new Integer(1), pool.borrowObject());     
    }
    
    /**
     * Verifies that validation and passivation failures returning objects are handled
     * properly - instances destroyed and not returned to the pool, but no exceptions propagated.
     */
    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory, 20);

        Object[] obj = new Object[10];
        for(int i=0;i<10;i++) {
            obj[i] = pool.borrowObject();
            assertEquals("Each time we borrow, get one more active.", i+1, pool.getNumActive());
            
        }
        
        factory.setValidateSelectively(true);  // Even numbers fail validation
        factory.setPassivateSelectively(true); // Multiples of 3 fail passivation

        for(int i=0;i<10;i++) {
            pool.returnObject(obj[i]);
            assertEquals("Each time we return, get one less active.", 9-i, pool.getNumActive());
        }
        // 0,2,4,6,8 fail validation, 3, 9 fail passivation - 3 left.
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
    
    /**
     * Verify that out of range constructor arguments are ignored.
     */
    public void testMaxIdleInitCapacityOutOfRange() throws Exception {
        SimpleFactory factory = new SimpleFactory();
        StackObjectPool pool = new StackObjectPool(factory, -1, 0);
        assertEquals(pool.getMaxSleeping(), StackObjectPool.DEFAULT_MAX_SLEEPING);
        pool.addObject();
        pool.close();
    }

    /**
     * Verifies that when returning objects cause maxSleeping exceeded, oldest instances
     * are destroyed to make room for returning objects.
     */
    public void testReturnObjectDiscardOrder() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory, 3);

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
        assertEquals("No returned objects should have been destroyed yet.", 0,  factory.getDestroyed().size());

        // cause the pool to discard a stale object.
        pool.returnObject(i3);
        assertEquals("One object should have been destroyed.", 1, factory.getDestroyed().size());

        // check to see what object was destroyed
        Integer d = (Integer)factory.getDestroyed().get(0);
        assertEquals("Destoryed object should be the stalest object.", i0, d);
    }
    
    /**
     * Verifies that exceptions thrown by factory activate method are not propagated to
     * the caller.  Objects that throw on activate are destroyed and if none succeed,
     * the caller gets NoSuchElementException.
     */
    public void testExceptionOnActivate() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory);
        pool.addObject();
        pool.addObject();
        factory.setThrowOnActivate(true);
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // expected
        }
        assertEquals(0, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
    }
    
    /**
     * Verifies that exceptions thrown by factory destroy are swallowed
     * by both addObject and returnObject.
     */
    public void testExceptionOnDestroy() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory, 2);
        factory.setThrowOnDestroy(true);
        for (int i = 0; i < 3; i++) {
            pool.addObject(); // Third one will destroy, exception should be swallowed
        }
        assertEquals(2, pool.getNumIdle());
        
        Object[] objects = new Object[3];
        for (int i = 0; i < 3; i++) {
            objects[i] = pool.borrowObject();
        }
        for (int i = 0; i < 3; i++) {
            pool.returnObject(objects[i]); // Third triggers destroy
        } 
        assertEquals(2, pool.getNumIdle());
    }
    
    /**
     * Verifies that addObject propagates exceptions thrown by
     * factory passivate, but returnObject swallows these.
     */
    public void testExceptionOnPassivate() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory, 2);
        factory.setThrowOnPassivate(true);
        
        // addObject propagates
        try {
            pool.addObject();
            fail("Expecting IntegerFactoryException");
        } catch (IntegerFactoryException ex) {
            assertEquals("passivateObject", ex.getType());
            assertEquals(0, ex.getValue());
        }
        assertEquals(0, pool.getNumIdle());
        
        // returnObject swallows 
        Object obj = pool.borrowObject();
        pool.returnObject(obj);
        assertEquals(0, pool.getNumIdle());
    }
    
    /**
     * Verifies that validation exceptions always propagate
     */
    public void testExceptionOnValidate() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory, 2);
        factory.setThrowOnValidate(true);
        
        // addObject
        try {
            pool.addObject();
            fail("Expecting IntegerFactoryException");
        } catch (IntegerFactoryException ex) {
            assertEquals("validateObject", ex.getType());
        }
        assertEquals(0, pool.getNumIdle());
        
        // returnObject 
        factory.setThrowOnValidate(false);
        Object obj = pool.borrowObject();
        factory.setThrowOnValidate(true);
        try {
            pool.returnObject(obj);
            fail("Expecting IntegerFactoryException");
        } catch (IntegerFactoryException ex) {
            assertEquals("validateObject", ex.getType());
        }
        assertEquals(0, pool.getNumIdle());
        
        // borrowObject - throws NoSuchElementException
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // Expected
        }
    }
    
    /**
     * Verifies that exceptions thrown by makeObject are propagated.
     */
    public void testExceptionOnMake() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        factory.setThrowOnMake(true);
        ObjectPool pool = new StackObjectPool(factory);
        try {
            pool.borrowObject();
            fail("Expecting IntegerFactoryException");
        } catch (IntegerFactoryException ex) {
            assertEquals("makeObject", ex.getType());
        }
        try {
            pool.addObject();
            fail("Expecting IntegerFactoryException");
        } catch (IntegerFactoryException ex) {
            assertEquals("makeObject", ex.getType());
        }
    }
    
    /**
     * Verifies NoSuchElementException when the factory returns a null object in borrowObject
     */
    public void testMakeNull() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory);
        factory.setMakeNull(true);
        try {
            pool.borrowObject();
            fail("Expecting NoSuchElementException");
        } catch (NoSuchElementException ex) {
            // Expected
        }
    }
    
    /**
     * Verifies that initIdleCapacity is not a hard limit, but maxIdle is.
     */
    public void testInitIdleCapacityExceeded() throws Exception {
        PoolableObjectFactory factory = new SimpleFactory();
        ObjectPool pool = new StackObjectPool(factory, 2, 1);
        pool.addObject();
        pool.addObject();
        assertEquals(2, pool.getNumIdle());
        pool.close();
        pool = new StackObjectPool(factory, 1, 2);
        pool.addObject();
        pool.addObject();
        assertEquals(1, pool.getNumIdle());
    }
    
    /**
     * Verifies close contract - idle instances are destroyed, returning instances
     * are destroyed, add/borrowObject throw IllegalStateException.
     */
    public void testClose() throws Exception {
        SelectiveFactory factory = new SelectiveFactory();
        ObjectPool pool = new StackObjectPool(factory);
        pool.addObject(); // 0
        pool.addObject(); // 1
        pool.addObject(); // 2
        Integer two = (Integer) pool.borrowObject();
        assertEquals(2, two.intValue());
        pool.close();
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumActive());
        List destroyed = factory.getDestroyed();
        assertEquals(2, destroyed.size());
        assertTrue(destroyed.contains(new Integer(0)));
        assertTrue(destroyed.contains(new Integer(0)));
        pool.returnObject(two);
        assertTrue(destroyed.contains(two));
        try {
            pool.addObject();
            fail("Expecting IllegalStateException");
        } catch (IllegalStateException ex) {
            // Expected
        }
        try {
            pool.borrowObject();
            fail("Expecting IllegalStateException");
        } catch (IllegalStateException ex) {
            // Expected
        }
    }

    /**
     * Simple factory that creates Integers. Validation and other factory methods
     * always succeed.
     */
    static class SimpleFactory implements PoolableObjectFactory {
        int counter = 0;
        public Object makeObject() { return String.valueOf(counter++); }
        public void destroyObject(Object obj) { }
        public boolean validateObject(Object obj) { return true; }
        public void activateObject(Object obj) { }
        public void passivateObject(Object obj) { }
    }
    
    /**
     * Integer factory that fails validation and other factory methods "selectively" and
     * tracks object destruction.
     */
    static class SelectiveFactory implements PoolableObjectFactory {
        private List destroyed = new ArrayList();
        private int counter = 0;
        private boolean validateSelectively = false;  // true <-> validate returns false for even Integers
        private boolean passivateSelectively = false; // true <-> passivate throws RTE if Integer = 0 mod 3
        private boolean throwOnDestroy = false;       // true <-> destroy throws RTE (always)
        private boolean throwOnActivate = false;      // true <-> activate throws RTE (always)
        private boolean throwOnMake = false;          // true <-> make throws RTE (always)
        private boolean throwOnValidate= false;       // true <-> validate throws RTE (always)
        private boolean throwOnPassivate = false;     // true <-> passivate throws RTE (always)
        private boolean makeNull = false;             // true <-> make returns null
        public Object makeObject() {
            if (throwOnMake) {
                final int next = counter + 1;
                throw new IntegerFactoryException("makeObject", next);
            } else {
                return makeNull? null : new Integer(counter++);
            }
        }
        public void destroyObject(Object obj) {
            if (throwOnDestroy) {
                final Integer integer = (Integer)obj;
                throw new IntegerFactoryException("destroyObject", integer.intValue());
            }
            destroyed.add(obj);
        }
        public boolean validateObject(Object obj) {
            if (throwOnValidate) {
                final Integer integer = (Integer)obj;
                throw new IntegerFactoryException("validateObject", integer.intValue());
            }
            if (validateSelectively) {
                // only odd objects are valid
                if(obj instanceof Integer) {
                    return ((((Integer)obj).intValue() % 2) == 1);
                } else {
                    return false;
                }
            }
            return true;
        }
        public void activateObject(Object obj) {
            if (throwOnActivate) {
                final Integer integer = (Integer)obj;
                throw new IntegerFactoryException("activateObject", integer.intValue());
            }
        }
        public void passivateObject(Object obj) { 
            if (throwOnPassivate) {
                final Integer integer = (Integer)obj;
                throw new IntegerFactoryException("passivateObject", integer.intValue());
            }
            if (passivateSelectively) {
                final Integer integer = (Integer)obj;
                if (integer.intValue() % 3 == 0) {
                    throw new IntegerFactoryException("passivateObject", integer.intValue());
                }
            }
        }
        public List getDestroyed() {
            return destroyed;
        }
        public void setCounter(int counter) {
            this.counter = counter;
        }
        public void setValidateSelectively(boolean validateSelectively) {
            this.validateSelectively = validateSelectively;
        }
        public void setPassivateSelectively(boolean passivateSelectively) {
            this.passivateSelectively = passivateSelectively;
        }
        public void setThrowOnDestroy(boolean throwOnDestroy) {
            this.throwOnDestroy = throwOnDestroy;
        }
        public void setThrowOnActivate(boolean throwOnActivate) {
            this.throwOnActivate = throwOnActivate;
        }
        public void setThrowOnMake(boolean throwOnMake) {
            this.throwOnMake = throwOnMake;
        }
        public void setThrowOnPassivate(boolean throwOnPassivate) {
            this.throwOnPassivate = throwOnPassivate;
        }
        public void setThrowOnValidate(boolean throwOnValidate) {
            this.throwOnValidate = throwOnValidate;
        }
        public void setMakeNull(boolean makeNull) {
            this.makeNull = makeNull;
        }
    }
    
    static class IntegerFactoryException extends RuntimeException {
        private String type;
        private int value;
        public IntegerFactoryException(String type, int value) {
            super(type + " failed. Value: " + value);
            this.type = type;
            this.value = value;
        }
        public String getType() {
            return type;
        }
        public int getValue() {
            return value;
        }
    }

    protected boolean isLifo() {
        return true;
    }

    protected boolean isFifo() {
        return false;
    }
}

