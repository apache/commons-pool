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
package org.apache.commons.pool2.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;


public abstract class BaseTestProxiedKeyedObjectPool {

    private static final String KEY1 = "key1";
    private static final String DATA1 = "data1";

    private KeyedObjectPool<String,TestObject> pool = null;

    @Before
    public void setUp() {
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(3);

        KeyedPooledObjectFactory<String, TestObject> factory =
                new TestKeyedObjectFactory();

        KeyedObjectPool<String,TestObject> innerPool =
                new GenericKeyedObjectPool<String,TestObject>(
                        factory, config);

        pool = new ProxiedKeyedObjectPool<String,TestObject>(innerPool, getproxySource());
    }


    protected abstract ProxySource<TestObject> getproxySource();

    @Test
    public void testBorrowObject() throws Exception {
        TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(KEY1, obj);
    }


    @Test(expected=IllegalStateException.class)
    public void testAccessAfterReturn() throws Exception {
        TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(KEY1, obj);

        assertNotNull(obj);

        obj.getData();
    }


    @Test(expected=IllegalStateException.class)
    public void testAccessAfterInvalidate() throws Exception {
        TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.invalidateObject(KEY1, obj);

        assertNotNull(obj);

        obj.getData();
    }


    @Test
    public void testPassThroughMethods01() throws Exception {
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        pool.addObject(KEY1);

        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());

        pool.clear();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }


    @Test(expected=IllegalStateException.class)
    public void testPassThroughMethods02() throws Exception {
        pool.close();
        pool.addObject(KEY1);
    }

    private static class TestKeyedObjectFactory extends
            BaseKeyedPooledObjectFactory<String,TestObject> {

        @Override
        public TestObject create(String key) throws Exception {
            return new TestObjectImpl();
        }
        @Override
        public PooledObject<TestObject> wrap(TestObject value) {
            return new DefaultPooledObject<TestObject>(value);
        }
    }


    protected static interface TestObject {
        String getData();
        void setData(String data);
    }


    private static class TestObjectImpl implements TestObject {

        private String data;

        @Override
        public String getData() {
            return data;
        }

        @Override
        public void setData(String data) {
            this.data = data;
        }
    }

}
