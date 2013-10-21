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

import java.util.NoSuchElementException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.UsageTracking;

/**
 * Create a new object pool where the pooled objects are wrapped in proxies
 * allowing better control of pooled objects and in particular the prevention
 * of the continued use of an object by a client after that client returns the
 * object to the pool.
 *
 * @param <T> type of the pooled object
 *
 * @since 2.0
 */
public class ProxiedObjectPool<T> implements ObjectPool<T> {

    private final ObjectPool<T> pool;
    private final ProxySource<T> proxySource;


    /**
     * Create a new proxied object pool.
     *
     * @param pool  The object pool to wrap
     * @param proxySource The source of the proxy objects
     */
    public ProxiedObjectPool(ObjectPool<T> pool, ProxySource<T> proxySource) {
        this.pool = pool;
        this.proxySource = proxySource;
    }


    // --------------------------------------------------- ObjectPool<T> methods

    @SuppressWarnings("unchecked")
    @Override
    public T borrowObject() throws Exception, NoSuchElementException,
            IllegalStateException {
        UsageTracking<T> usageTracking = null;
        if (pool instanceof UsageTracking) {
            usageTracking = (UsageTracking<T>) pool;
        }
        T pooledObject = pool.borrowObject();
        T proxy = proxySource.createProxy(pooledObject, usageTracking);
        return proxy;
    }


    @Override
    public void returnObject(T proxy) throws Exception {
        T pooledObject = proxySource.resolveProxy(proxy);
        pool.returnObject(pooledObject);
    }


    @Override
    public void invalidateObject(T proxy) throws Exception {
        T pooledObject = proxySource.resolveProxy(proxy);
        pool.invalidateObject(pooledObject);
    }


    @Override
    public void addObject() throws Exception, IllegalStateException,
            UnsupportedOperationException {
        pool.addObject();
    }


    @Override
    public int getNumIdle() {
        return pool.getNumIdle();
    }


    @Override
    public int getNumActive() {
        return pool.getNumActive();
    }


    @Override
    public void clear() throws Exception, UnsupportedOperationException {
        pool.clear();
    }


    @Override
    public void close() {
        pool.close();
    }
}
