/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/TestObjectPool.java,v $
 * $Revision: 1.5 $
 * $Date: 2003/08/22 14:33:30 $
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
 * http://www.apache.org/
 *
 */
package org.apache.commons.pool;

import junit.framework.TestCase;

/**
 * Abstract {@link TestCase} for {@link ObjectPool} implementations.
 * @author Rodney Waldhoff
 * @version $Revision: 1.5 $ $Date: 2003/08/22 14:33:30 $
 */
public abstract class TestObjectPool extends TestCase {
    public TestObjectPool(String testName) {
        super(testName);
    }

    /** 
     * Create an {@link ObjectPool} instance
     * that can contain at least <i>mincapacity</i>
     * idle and active objects, or
     * throw {@link IllegalArgumentException}
     * if such a pool cannot be created.
     */
    protected abstract ObjectPool makeEmptyPool(int mincapacity);

    /**
     * Return what we expect to be the n<sup>th</sup>
     * object (zero indexed) created by the _pool.
     */
    protected abstract Object getNthObject(int n);

    public void setUp() throws Exception {
    }
    
    public void tearDown() throws Exception {
        _pool = null;
    }
    
    public void testBaseBorrow() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(getNthObject(0),_pool.borrowObject());
        assertEquals(getNthObject(1),_pool.borrowObject());
        assertEquals(getNthObject(2),_pool.borrowObject());
    }

    public void testBaseAddObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        try {
            assertEquals(0,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            _pool.addObject();
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            Object obj = _pool.borrowObject();
            assertEquals(getNthObject(0),obj);
            assertEquals(0,_pool.getNumIdle());
            assertEquals(1,_pool.getNumActive());
            _pool.returnObject(obj);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
        } catch(UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        }
    }
    
    public void testBaseBorrowReturn() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object obj0 = _pool.borrowObject();
        assertEquals(getNthObject(0),obj0);
        Object obj1 = _pool.borrowObject();
        assertEquals(getNthObject(1),obj1);
        Object obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
        _pool.returnObject(obj2);
        obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
        _pool.returnObject(obj1);
        obj1 = _pool.borrowObject();
        assertEquals(getNthObject(1),obj1);
        _pool.returnObject(obj0);
        _pool.returnObject(obj2);
        obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
        obj0 = _pool.borrowObject();
        assertEquals(getNthObject(0),obj0);
    }

    public void testBaseNumActiveNumIdle() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        assertEquals(1,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.returnObject(obj1);
        assertEquals(1,_pool.getNumActive());
        assertEquals(1,_pool.getNumIdle());
        _pool.returnObject(obj0);
        assertEquals(0,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
    }

    public void testBaseClear() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.returnObject(obj1);
        _pool.returnObject(obj0);
        assertEquals(0,_pool.getNumActive());
        assertEquals(2,_pool.getNumIdle());
        _pool.clear();
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj2 = _pool.borrowObject();
        assertEquals(getNthObject(2),obj2);
    }

    public void testBaseInvalidateObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        Object obj0 = _pool.borrowObject();
        Object obj1 = _pool.borrowObject();
        assertEquals(2,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.invalidateObject(obj0);
        assertEquals(1,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
        _pool.invalidateObject(obj1);
        assertEquals(0,_pool.getNumActive());
        assertEquals(0,_pool.getNumIdle());
    }
    
    public void testBaseClosePool() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object obj = _pool.borrowObject();
        _pool.returnObject(obj);
        
        _pool.close();
        try {
            _pool.borrowObject();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
    }

    public void testBaseCantCloseTwice() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object obj = _pool.borrowObject();
        _pool.returnObject(obj);
        
        _pool.close();
        try {
            _pool.close();
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }
    }

    private ObjectPool _pool = null;
}
