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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.proxy.BaseTestProxiedObjectPool.TestObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



public abstract class BaseTestProxiedKeyedObjectPool {

    private static class TestKeyedObjectFactory extends
            BaseKeyedPooledObjectFactory<String,TestObject> {

        @Override
        public TestObject create(final String key) throws Exception {
            return new TestObjectImpl();
        }
        @Override
        public PooledObject<TestObject> wrap(final TestObject value) {
            return new DefaultPooledObject<>(value);
        }
    }
    protected interface TestObject {
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
        public void setData(final String data) {
            this.data = data;
        }
    }

    private static final String KEY1 = "key1";


    private static final String DATA1 = "data1";

    private static final Duration ABANDONED_TIMEOUT_SECS = Duration.ofSeconds(3);

    private KeyedObjectPool<String,TestObject> pool;

    private StringWriter log = null;


    protected abstract ProxySource<TestObject> getproxySource();


    @BeforeEach
    public void setUp() {
        log = new StringWriter();

        final PrintWriter pw = new PrintWriter(log);
        final AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setUseUsageTracking(true);
        abandonedConfig.setRemoveAbandonedTimeout(ABANDONED_TIMEOUT_SECS);
        abandonedConfig.setLogWriter(pw);

        final GenericKeyedObjectPoolConfig<TestObject> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotal(3);

        final KeyedPooledObjectFactory<String, TestObject> factory =
                new TestKeyedObjectFactory();

        @SuppressWarnings("resource")
        final KeyedObjectPool<String, TestObject> innerPool =
                new GenericKeyedObjectPool<>(
                        factory, config, abandonedConfig);

        pool = new ProxiedKeyedObjectPool<>(innerPool, getproxySource());
    }


    @Test
    public void testAccessAfterInvalidate() throws Exception {
        final TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.invalidateObject(KEY1, obj);

        assertNotNull(obj);

        assertThrows(IllegalStateException.class,
                obj::getData);

    }


    @Test
    public void testAccessAfterReturn() throws Exception {
        final TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(KEY1, obj);

        assertNotNull(obj);
        assertThrows(IllegalStateException.class,
                obj::getData);
    }

    @Test
    public void testBorrowObject() throws Exception {
        final TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(KEY1, obj);
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


    @Test
    public void testPassThroughMethods02() throws Exception {
        pool.close();
        assertThrows(IllegalStateException.class,
                () -> pool.addObject(KEY1));
    }

    @Test
    public void testUsageTracking() throws Exception {
        final TestObject obj = pool.borrowObject(KEY1);
        assertNotNull(obj);

        // Use the object to trigger collection of last used stack trace
        obj.setData(DATA1);

        // Sleep long enough for the object to be considered abandoned
        Thread.sleep(ABANDONED_TIMEOUT_SECS.plusSeconds(2).toMillis());

        // Borrow another object to trigger the abandoned object processing
        pool.borrowObject(KEY1);

        final String logOutput = log.getBuffer().toString();

        assertTrue(logOutput.contains("Pooled object created"));
        assertTrue(logOutput.contains("The last code to use this object was"));
    }

}
