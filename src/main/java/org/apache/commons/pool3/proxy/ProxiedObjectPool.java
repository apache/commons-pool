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
package org.apache.commons.pool3.proxy;

import java.util.NoSuchElementException;

import org.apache.commons.pool3.ObjectPool;
import org.apache.commons.pool3.UsageTracking;

/**
 * Create a new object pool where the pooled objects are wrapped in proxies
 * allowing better control of pooled objects and in particular the prevention
 * of the continued use of an object by a client after that client returns the
 * object to the pool.
 *
 * @param <T> type of the pooled object
 * @param <E> type of the exception
 *
 * @since 2.0
 */
public class ProxiedObjectPool<T, E extends Exception> implements ObjectPool<T, E> {

    private final ObjectPool<T, E> pool;
    private final ProxySource<T> proxySource;

    /**
     * Constructs a new proxied object pool.
     *
     * @param pool  The object pool to wrap
     * @param proxySource The source of the proxy objects
     */
    public ProxiedObjectPool(final ObjectPool<T, E> pool, final ProxySource<T> proxySource) {
        this.pool = pool;
        this.proxySource = proxySource;
    }

    @Override
    public void addObject() throws E, IllegalStateException, UnsupportedOperationException {
        pool.addObject();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T borrowObject() throws E, NoSuchElementException, IllegalStateException {
        UsageTracking<T> usageTracking = null;
        if (pool instanceof UsageTracking) {
            usageTracking = (UsageTracking<T>) pool;
        }
        return proxySource.createProxy(pool.borrowObject(), usageTracking);
    }

    @Override
    public void clear() throws E, UnsupportedOperationException {
        pool.clear();
    }

    @Override
    public void close() {
        pool.close();
    }

    @Override
    public int getNumActive() {
        return pool.getNumActive();
    }

    @Override
    public int getNumIdle() {
        return pool.getNumIdle();
    }

    @Override
    public void invalidateObject(final T proxy) throws E {
        pool.invalidateObject(proxySource.resolveProxy(proxy));
    }

    @Override
    public void returnObject(final T proxy) throws E {
        pool.returnObject(proxySource.resolveProxy(proxy));
    }

    /**
     * @since 2.4.3
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("ProxiedObjectPool [pool=");
        builder.append(pool);
        builder.append(", proxySource=");
        builder.append(proxySource);
        builder.append("]");
        return builder.toString();
    }
}
