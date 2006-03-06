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
package org.apache.commons.pool;

import junit.framework.TestCase;

/**
 * Abstract {@link TestCase} for {@link ObjectPool} implementations.
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
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
    
    /**
     * Is the implementations LIFO?
     * @return
     */
    protected abstract boolean isLifo();
    
    /**
     * Is the implementationn FIFO?
     * @return
     */
    protected abstract boolean isFifo();

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
        if (isLifo()) {
            assertEquals(getNthObject(2),obj2);
        }
        if (isFifo()) {
            assertEquals(getNthObject(0),obj2);
        }
            
        obj0 = _pool.borrowObject();
        if (isLifo()) {
            assertEquals(getNthObject(0),obj0);
        }
        if (isFifo()) {
            assertEquals(getNthObject(2),obj0);
        }
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

    private ObjectPool _pool = null;
}
