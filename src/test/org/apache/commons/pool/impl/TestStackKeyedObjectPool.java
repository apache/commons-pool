/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/impl/TestStackKeyedObjectPool.java,v $
 * $Revision: 1.11 $
 * $Date: 2003/10/09 21:45:57 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.pool.impl;

import java.util.BitSet;
import java.util.HashMap;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestKeyedObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 1.11 $ $Date: 2003/10/09 21:45:57 $
 */
public class TestStackKeyedObjectPool extends TestKeyedObjectPool {
    public TestStackKeyedObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestStackKeyedObjectPool.class);
    }

    protected KeyedObjectPool makeEmptyPool(int mincapacity) {
        StackKeyedObjectPool pool = new StackKeyedObjectPool(new SimpleFactory(),mincapacity);
        return pool;
    }
    
    protected Object getNthObject(Object key, int n) {
        return String.valueOf(key) + String.valueOf(n);
    }
    
    protected Object makeKey(int n) {
        return String.valueOf(n);
    }

    private StackKeyedObjectPool pool = null;

    public void setUp() throws Exception {
        super.setUp();
        pool = new StackKeyedObjectPool(
            new KeyedPoolableObjectFactory()  {
                int counter = 0;
                public Object makeObject(Object key) { return String.valueOf(key) + String.valueOf(counter++); }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) { return true; }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) { }
            }
            );
    }

    
    public void tearDown() throws Exception {
        super.tearDown();
        pool = null;
    }

    public void testCloseBug() throws Exception {
        {
            Object obj0 = pool.borrowObject("");
            Object obj1 = pool.borrowObject("");
            assertEquals(2,pool.getNumActive(""));
            assertEquals(0,pool.getNumIdle(""));
            pool.returnObject("",obj1);
            pool.returnObject("",obj0);
            assertEquals(0,pool.getNumActive(""));
            assertEquals(2,pool.getNumIdle(""));
        }
        {
            Object obj0 = pool.borrowObject("2");
            Object obj1 = pool.borrowObject("2");
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
    
    public void testPoolWithNullFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool(10);
        for(int i=0;i<10;i++) {
            pool.returnObject("X",new Integer(i));
        }
        for(int j=0;j<3;j++) {
            Integer[] borrowed = new Integer[10];
            BitSet found = new BitSet();
            for(int i=0;i<10;i++) {
                borrowed[i] = (Integer)(pool.borrowObject("X"));
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
            StackKeyedObjectPool pool = new StackKeyedObjectPool();
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(10);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(10,5);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null,10);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null,10,5);
        }
    }
    
    public void testToString() throws Exception {
        StackKeyedObjectPool pool = new StackKeyedObjectPool(new SimpleFactory());
        assertNotNull(pool.toString());
        Object obj = pool.borrowObject("key");
        assertNotNull(pool.toString());
        pool.returnObject("key",obj);
        assertNotNull(pool.toString());
    }
        
    public void testBorrowFromEmptyPoolWithNullFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
    
    public void testSetFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject("x");
        assertNotNull(obj);
        pool.returnObject("x",obj);
    }

    public void testCantResetFactoryWithActiveObjects() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
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
        KeyedObjectPool pool = new StackKeyedObjectPool();
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject("x");        
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject("x");        
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
    }

    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        pool.setFactory(
            new KeyedPoolableObjectFactory() {
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
            obj[i] = pool.borrowObject("key");
        }
        for(int i=0;i<10;i++) {
            pool.returnObject("key",obj[i]);
        }
        assertEquals(3,pool.getNumIdle("key"));
    }
 
    class SimpleFactory implements KeyedPoolableObjectFactory {
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
}
