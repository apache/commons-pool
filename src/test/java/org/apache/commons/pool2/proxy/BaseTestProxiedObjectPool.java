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
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;


public abstract class BaseTestProxiedObjectPool {

    private static final String DATA1 = "data1";
    private static final int ABANDONED_TIMEOUT_SECS = 3;

    private ObjectPool<TestObject> pool = null;
    private StringWriter log = null;

    @Before
    public void setUp() {
        log = new StringWriter();

        PrintWriter pw = new PrintWriter(log);
        AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setUseUsageTracking(true);
        abandonedConfig.setRemoveAbandonedTimeout(ABANDONED_TIMEOUT_SECS);
        abandonedConfig.setLogWriter(pw);

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(3);

        PooledObjectFactory<TestObject> factory = new TestObjectFactory();

        ObjectPool<TestObject> innerPool =
                new GenericObjectPool<TestObject>(factory, config, abandonedConfig);

        pool = new ProxiedObjectPool<TestObject>(innerPool, getproxySource());
    }


    protected abstract ProxySource<TestObject> getproxySource();

    @Test
    public void testBorrowObject() throws Exception {
        TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(obj);
    }


    @Test(expected=IllegalStateException.class)
    public void testAccessAfterReturn() throws Exception {
        TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(obj);

        assertNotNull(obj);

        obj.getData();
    }


    @Test(expected=IllegalStateException.class)
    public void testAccessAfterInvalidate() throws Exception {
        TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.invalidateObject(obj);

        assertNotNull(obj);

        obj.getData();
    }


    @Test
    public void testUsageTracking() throws Exception {
        TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Use the object to trigger collection of last used stack trace
        obj.setData(DATA1);

        // Sleep long enough for the object to be considered abandoned
        Thread.sleep((ABANDONED_TIMEOUT_SECS + 2) * 1000);

        // Borrow another object to trigger the abandoned object processing
        pool.borrowObject();

        String logOutput = log.getBuffer().toString();

        assertTrue(logOutput.contains("Pooled object created"));
        assertTrue(logOutput.contains("The last code to use this object was"));
    }


    @Test
    public void testPassThroughMethods01() throws Exception {
        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        pool.addObject();

        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());

        pool.clear();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }


    @Test(expected=IllegalStateException.class)
    public void testPassThroughMethods02() throws Exception {
        pool.close();
        pool.addObject();
    }

    private static class TestObjectFactory extends
            BasePooledObjectFactory<TestObject> {

        @Override
        public TestObject create() throws Exception {
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
