/*
 * $Id: TestKeyedObjectPool.java,v 1.1 2002/10/31 15:04:24 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2002/10/31 15:04:24 $
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

package org.apache.commons.pool;

import junit.framework.*;

/**
 * Abstract {@link TestCase} for {@link ObjectPool} implementations.
 * @author Rodney Waldhoff
 * @version $Revision: 1.1 $ $Date: 2002/10/31 15:04:24 $
 */
public abstract class TestKeyedObjectPool extends TestCase {
    public TestKeyedObjectPool(String testName) {
        super(testName);
    }

    /** 
     * Create an {@link KeyedObjectPool} instance
     * that can contain at least <i>mincapacity</i>
     * idle and active objects, or
     * throw {@link IllegalArgumentException}
     * if such a pool cannot be created.
     */
    protected abstract KeyedObjectPool makeEmptyPool(int mincapacity);

    /**
     * Return what we expect to be the n<sup>th</sup>
     * object (zero indexed) created by the pool
     * for the given key.
     */
    protected abstract Object getNthObject(Object key, int n);

    protected abstract Object makeKey(int n);

    public void setUp() throws Exception {
    }
    
    public void tearDown() throws Exception {
        _pool = null;
    }
    
    public void testBorrow() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        Object keyb = makeKey(1);
        assertEquals("1",getNthObject(keya,0),_pool.borrowObject(keya));
        assertEquals("2",getNthObject(keyb,0),_pool.borrowObject(keyb));
        assertEquals("3",getNthObject(keyb,1),_pool.borrowObject(keyb));
        assertEquals("4",getNthObject(keya,1),_pool.borrowObject(keya));
        assertEquals("5",getNthObject(keyb,2),_pool.borrowObject(keyb));
        assertEquals("6",getNthObject(keya,2),_pool.borrowObject(keya));
    }

    public void testBorrowReturn() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        Object obj0 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,0),obj0);
        Object obj1 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,1),obj1);
        Object obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        _pool.returnObject(keya,obj2);
        obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        _pool.returnObject(keya,obj1);
        obj1 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,1),obj1);
        _pool.returnObject(keya,obj0);
        _pool.returnObject(keya,obj2);
        obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
        obj0 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,0),obj0);
    }

    public void testNumActiveNumIdle() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        Object obj0 = _pool.borrowObject(keya);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj1);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(1,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));
    }

    public void testNumActiveNumIdle2() throws Exception {
        try {
            _pool = makeEmptyPool(6);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        Object keyb = makeKey(1);
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(0,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        Object objA0 = _pool.borrowObject(keya);
        Object objB0 = _pool.borrowObject(keyb);

        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(1,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        Object objA1 = _pool.borrowObject(keya);
        Object objB1 = _pool.borrowObject(keyb);

        assertEquals(4,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        assertEquals(2,_pool.getNumActive(keyb));
        assertEquals(0,_pool.getNumIdle(keyb));

        _pool.returnObject(keya,objA0);
        _pool.returnObject(keyb,objB0);

        assertEquals(2,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(1,_pool.getNumIdle(keya));
        assertEquals(1,_pool.getNumActive(keyb));
        assertEquals(1,_pool.getNumIdle(keyb));

        _pool.returnObject(keya,objA1);
        _pool.returnObject(keyb,objB1);

        assertEquals(0,_pool.getNumActive());
        assertEquals(4,_pool.getNumIdle());
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));
        assertEquals(0,_pool.getNumActive(keyb));
        assertEquals(2,_pool.getNumIdle(keyb));
    }

    public void testClear() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        Object obj0 = _pool.borrowObject(keya);
        Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.returnObject(keya,obj1);
        _pool.returnObject(keya,obj0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(2,_pool.getNumIdle(keya));
        _pool.clear(keya);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        Object obj2 = _pool.borrowObject(keya);
        assertEquals(getNthObject(keya,2),obj2);
    }

    public void testInvalidateObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object keya = makeKey(0);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        Object obj0 = _pool.borrowObject(keya);
        Object obj1 = _pool.borrowObject(keya);
        assertEquals(2,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.invalidateObject(keya,obj0);
        assertEquals(1,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
        _pool.invalidateObject(keya,obj1);
        assertEquals(0,_pool.getNumActive(keya));
        assertEquals(0,_pool.getNumIdle(keya));
    }

    private KeyedObjectPool _pool = null;
}
