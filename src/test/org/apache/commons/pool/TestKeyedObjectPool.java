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
 * @version $Revision: 1.8 $ $Date: 2004/02/28 11:46:11 $
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
    
    public void testBaseBorrow() throws Exception {
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

    public void testBaseBorrowReturn() throws Exception {
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

    public void testBaseNumActiveNumIdle() throws Exception {
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
        
        assertEquals(0,_pool.getNumActive("xyzzy12345"));
        assertEquals(0,_pool.getNumIdle("xyzzy12345"));
    }

    public void testBaseNumActiveNumIdle2() throws Exception {
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

    public void testBaseClear() throws Exception {
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

    public void testBaseInvalidateObject() throws Exception {
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

    public void testBaseAddObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(IllegalArgumentException e) {
            return; // skip this test if unsupported
        }
        Object key = makeKey(0);
        try {
            assertEquals(0,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(0,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
            _pool.addObject(key);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(1,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
            Object obj = _pool.borrowObject(key);
            assertEquals(getNthObject(key,0),obj);
            assertEquals(0,_pool.getNumIdle());
            assertEquals(1,_pool.getNumActive());
            assertEquals(0,_pool.getNumIdle(key));
            assertEquals(1,_pool.getNumActive(key));
            _pool.returnObject(key,obj);
            assertEquals(1,_pool.getNumIdle());
            assertEquals(0,_pool.getNumActive());
            assertEquals(1,_pool.getNumIdle(key));
            assertEquals(0,_pool.getNumActive(key));
        } catch(UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        }
    }

    private KeyedObjectPool _pool = null;
}
