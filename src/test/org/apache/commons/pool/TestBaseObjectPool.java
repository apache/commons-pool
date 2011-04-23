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
package org.apache.commons.pool;


/**
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision$ $Date$ 
 */
public class TestBaseObjectPool extends TestObjectPool {
    private ObjectPool _pool = null;

    public TestBaseObjectPool(String testName) {
        super(testName);
    }

    protected ObjectPool makeEmptyPool(int mincapacity) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    protected ObjectPool makeEmptyPool(final PoolableObjectFactory factory) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    protected Object getNthObject(final int n) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    protected boolean isLifo() {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        return false;
    }

    protected boolean isFifo() {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        return false;
    }

    // tests
    public void testUnsupportedOperations() throws Exception {
        if (!getClass().equals(TestBaseObjectPool.class)) {
            return; // skip redundant tests
        }
        ObjectPool pool = new BaseObjectPool() { 
            public Object borrowObject() {
                return null;
            }
            public void returnObject(Object obj) {
            }
            public void invalidateObject(Object obj) {
            }            
        };   

        assertTrue("Negative expected.", pool.getNumIdle() < 0);
        assertTrue("Negative expected.", pool.getNumActive() < 0);

        try {
            pool.clear();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.addObject();
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }

        try {
            pool.setFactory(null);
            fail("Expected UnsupportedOperationException");
        } catch(UnsupportedOperationException e) {
            // expected
        }
    }

    public void testClose() throws Exception {
        ObjectPool pool = new BaseObjectPool() {
            public Object borrowObject() {
                return null;
            }
            public void returnObject(Object obj) {
            }
            public void invalidateObject(Object obj) {
            }
        };

        pool.close();
        pool.close(); // should not error as of Pool 2.0.
    }

    public void testBaseBorrow() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        assertEquals(getNthObject(0),_pool.borrowObject());
        assertEquals(getNthObject(1),_pool.borrowObject());
        assertEquals(getNthObject(2),_pool.borrowObject());
    }

    public void testBaseAddObject() throws Exception {
        try {
            _pool = makeEmptyPool(3);
        } catch(UnsupportedOperationException e) {
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
        } catch(UnsupportedOperationException e) {
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
        } catch(UnsupportedOperationException e) {
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
        } catch(UnsupportedOperationException e) {
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
        } catch(UnsupportedOperationException e) {
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
        } catch(UnsupportedOperationException e) {
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
}
