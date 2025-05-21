/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 */
public class TestBaseObjectPool extends AbstractTestObjectPool {

    private static final class TestObjectPool extends BaseObjectPool<Object, RuntimeException> {

        @Override
        public Object borrowObject() {
            return null;
        }

        @Override
        public void invalidateObject(final Object obj) {
        }

        @Override
        public void returnObject(final Object obj) {
        }
    }

    private ObjectPool<String, RuntimeException> pool;

    /**
     * @param n Ignored by this implemented. Used by sub-classes.
     * @return the Nth object (zero indexed)
     */
    protected Object getNthObject(final int n) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    protected boolean isFifo() {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        return false;
    }

    protected boolean isLifo() {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        return false;
    }

    /**
     * @param minCapacity Ignored by this implemented. Used by sub-classes.
     * @return A newly created empty pool.
     *
     * @param <E> Type of exception thrown by the pool.
     */
    protected <E extends Exception> ObjectPool<String, E> makeEmptyPool(final int minCapacity) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    @Override
    protected <E extends Exception> ObjectPool<Object, E> makeEmptyPool(final PooledObjectFactory<Object, E> factory) {
        if (this.getClass() != TestBaseObjectPool.class) {
            fail("Subclasses of TestBaseObjectPool must reimplement this method.");
        }
        throw new UnsupportedOperationException("BaseObjectPool isn't a complete implementation.");
    }

    @Test
    public void testBaseAddObject() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        try {
            assertEquals(0, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            pool.addObject();
            assertEquals(1, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
            final String obj = pool.borrowObject();
            assertEquals(getNthObject(0), obj);
            assertEquals(0, pool.getNumIdle());
            assertEquals(1, pool.getNumActive());
            pool.returnObject(obj);
            assertEquals(1, pool.getNumIdle());
            assertEquals(0, pool.getNumActive());
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if one of those calls is unsupported
        } finally {
            pool.close();
        }
    }

    @Test
    public void testBaseBorrow() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        assertEquals(getNthObject(0), pool.borrowObject());
        assertEquals(getNthObject(1), pool.borrowObject());
        assertEquals(getNthObject(2), pool.borrowObject());
        pool.close();
    }

    @Test
    public void testBaseBorrowReturn() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        String obj0 = pool.borrowObject();
        assertEquals(getNthObject(0), obj0);
        String obj1 = pool.borrowObject();
        assertEquals(getNthObject(1), obj1);
        String obj2 = pool.borrowObject();
        assertEquals(getNthObject(2), obj2);
        pool.returnObject(obj2);
        obj2 = pool.borrowObject();
        assertEquals(getNthObject(2), obj2);
        pool.returnObject(obj1);
        obj1 = pool.borrowObject();
        assertEquals(getNthObject(1), obj1);
        pool.returnObject(obj0);
        pool.returnObject(obj2);
        obj2 = pool.borrowObject();
        if (isLifo()) {
            assertEquals(getNthObject(2), obj2);
        }
        if (isFifo()) {
            assertEquals(getNthObject(0), obj2);
        }

        obj0 = pool.borrowObject();
        if (isLifo()) {
            assertEquals(getNthObject(0), obj0);
        }
        if (isFifo()) {
            assertEquals(getNthObject(2), obj0);
        }
        pool.close();
    }

    @Test
    public void testBaseClear() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        final String obj0 = pool.borrowObject();
        final String obj1 = pool.borrowObject();
        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        pool.returnObject(obj1);
        pool.returnObject(obj0);
        assertEquals(0, pool.getNumActive());
        assertEquals(2, pool.getNumIdle());
        pool.clear();
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        final Object obj2 = pool.borrowObject();
        assertEquals(getNthObject(2), obj2);
        pool.close();
    }

    @Test
    public void testBaseClosePool() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        final String obj = pool.borrowObject();
        pool.returnObject(obj);

        pool.close();
        assertThrows(IllegalStateException.class, pool::borrowObject);
    }

    @Test
    public void testBaseInvalidateObject() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        final String obj0 = pool.borrowObject();
        final String obj1 = pool.borrowObject();
        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        pool.invalidateObject(obj0);
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        pool.invalidateObject(obj1);
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        pool.close();
    }

    @Test
    public void testBaseNumActiveNumIdle() {
        try {
            pool = makeEmptyPool(3);
        } catch (final UnsupportedOperationException e) {
            return; // skip this test if unsupported
        }
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        final String obj0 = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        final String obj1 = pool.borrowObject();
        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        pool.returnObject(obj1);
        assertEquals(1, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());
        pool.returnObject(obj0);
        assertEquals(0, pool.getNumActive());
        assertEquals(2, pool.getNumIdle());
        pool.close();
    }

    @Test
    public void testClose() {
        @SuppressWarnings("resource")
        final ObjectPool<Object, RuntimeException> pool = new TestObjectPool();

        pool.close();
        pool.close(); // should not error as of Pool 2.0.
    }

    @Test
    public void testUnsupportedOperations() {
        if (!getClass().equals(TestBaseObjectPool.class)) {
            return; // skip redundant tests
        }
        try (ObjectPool<Object, RuntimeException> pool = new TestObjectPool()) {

            assertTrue(pool.getNumIdle() < 0, "Negative expected.");
            assertTrue(pool.getNumActive() < 0, "Negative expected.");

            assertThrows(UnsupportedOperationException.class, pool::clear);
            assertThrows(UnsupportedOperationException.class, pool::addObject);
        }
    }
}
