/*
 * $Id: TestStackObjectPool.java,v 1.8 2003/03/07 20:28:36 rwaldhoff Exp $
 * $Revision: 1.8 $
 * $Date: 2003/03/07 20:28:36 $
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

import java.util.BitSet;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 1.8 $ $Date: 2003/03/07 20:28:36 $
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

