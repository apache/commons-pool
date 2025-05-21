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
package org.apache.commons.pool3.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import org.apache.commons.pool3.BasePooledObjectFactory;
import org.apache.commons.pool3.ObjectPool;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;
import org.apache.commons.pool3.impl.AbandonedConfig;
import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericObjectPool;
import org.apache.commons.pool3.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractTestProxiedObjectPool {

    protected interface TestObject {
        String getData();
        void setData(String data);
    }

    private static final class TestObjectFactory extends
            BasePooledObjectFactory<TestObject, RuntimeException> {

        private final RuntimeException exceptionToThrow;

        private TestObjectFactory(RuntimeException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public TestObject create() {
            return new TestObjectImpl(exceptionToThrow);
        }
        @Override
        public PooledObject<TestObject> wrap(final TestObject value) {
            return new DefaultPooledObject<>(value);
        }
    }

    private static final class TestObjectImpl implements TestObject {

        private final RuntimeException exceptionToThrow;
        private String data;

        private TestObjectImpl(RuntimeException exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public String getData() {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return data;
        }

        @Override
        public void setData(final String data) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            this.data = data;
        }
    }

    private static final String DATA1 = "data1";

    private static final Duration ABANDONED_TIMEOUT_SECS = Duration.ofSeconds(3);

    private StringWriter log;

    protected abstract ProxySource<TestObject> getProxySource(boolean unwrapInvocationTargetException);

    private ProxiedObjectPool<TestObject, RuntimeException> createProxiedObjectPool() {
        return createProxiedObjectPool(false, null);
    }

    private ProxiedObjectPool<TestObject, RuntimeException> createProxiedObjectPool(
            boolean unwrapInvocationTargetException, RuntimeException exceptionToThrow) {
        final PrintWriter pw = new PrintWriter(log);
        final AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setLogAbandoned(true);
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setUseUsageTracking(true);
        abandonedConfig.setRemoveAbandonedTimeout(ABANDONED_TIMEOUT_SECS);
        abandonedConfig.setLogWriter(pw);

        final GenericObjectPoolConfig<TestObject> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(3);

        final PooledObjectFactory<TestObject, RuntimeException> factory = new TestObjectFactory(exceptionToThrow);

        final ObjectPool<TestObject, RuntimeException> innerPool = new GenericObjectPool<>(factory, config, abandonedConfig);

        return new ProxiedObjectPool<>(innerPool, getProxySource(unwrapInvocationTargetException));
    }

    @BeforeEach
    public void setUp() {
        log = new StringWriter();
    }

    @Test
    public void testAccessAfterInvalidate() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        final TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.invalidateObject(obj);

        assertNotNull(obj);

        assertThrows(IllegalStateException.class,
                obj::getData);

    }

    @Test
    public void testAccessAfterReturn() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        final TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(obj);

        assertNotNull(obj);

        assertThrows(IllegalStateException.class,
                obj::getData);
    }

    @Test
    public void testBorrowObject() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        final TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Make sure proxied methods are working
        obj.setData(DATA1);
        assertEquals(DATA1, obj.getData());

        pool.returnObject(obj);
    }

    @Test
    public void testPassThroughMethods01() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        pool.addObject();

        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());

        pool.clear();

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
    }

    @Test
    public void testPassThroughMethods02() {
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        pool.close();

        assertThrows(IllegalStateException.class,
                () -> pool.addObject());
    }

    @Test
    public void testUsageTracking() throws InterruptedException {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool();

        final TestObject obj = pool.borrowObject();
        assertNotNull(obj);

        // Use the object to trigger collection of last used stack trace
        obj.setData(DATA1);

        // Sleep long enough for the object to be considered abandoned
        Thread.sleep(ABANDONED_TIMEOUT_SECS.plusSeconds(2).toMillis());

        // Borrow another object to trigger the abandoned object processing
        pool.borrowObject();

        final String logOutput = log.getBuffer().toString();

        assertTrue(logOutput.contains("Pooled object created"));
        assertTrue(logOutput.contains("The last code to use this object was"));
    }

    @Test
    public void testUnwrapInvocationTargetExceptionTrue() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool(true, new MyException());

        final TestObject object = pool.borrowObject();
        assertThrows(MyException.class, object::getData);
    }

    @Test
    public void testUnwrapInvocationTargetExceptionFalse() {
        @SuppressWarnings("resource")
        final ObjectPool<TestObject, RuntimeException> pool = createProxiedObjectPool(false, new MyException());

        final TestObject object = pool.borrowObject();
        assertThrows(getInvocationTargetExceptionType(), object::getData);
    }

    protected abstract Class<? extends Throwable> getInvocationTargetExceptionType();

    private static class MyException extends RuntimeException { }
}
