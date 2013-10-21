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

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

import org.apache.commons.pool2.UsageTracking;

/**
 * Provides proxy objects using CGLib.
 *
 * @param <T> type of the pooled object to be proxied
 *
 * @since 2.0
 */
public class CglibProxySource<T> implements ProxySource<T> {

    private final Class<? extends T> superclass;

    /**
     * Create a new proxy source for the given class.
     *
     * @param superclass The class to proxy
     */
    public CglibProxySource(Class<? extends T> superclass) {
        this.superclass = superclass;
    }


    @Override
    public T createProxy(T pooledObject, UsageTracking<T> usageTracking) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superclass);

        CglibProxyHandler<T> proxyInterceptor =
                new CglibProxyHandler<T>(pooledObject, usageTracking);
        enhancer.setCallback(proxyInterceptor);

        @SuppressWarnings("unchecked")
        T proxy = (T) enhancer.create();

        return proxy;
    }


    @Override
    public T resolveProxy(T proxy) {
        @SuppressWarnings("unchecked")
        CglibProxyHandler<T> cglibProxyHandler =
                (CglibProxyHandler<T>) ((Factory) proxy).getCallback(0);
        T pooledObject = cglibProxyHandler.disableProxy();
        return pooledObject;
    }
}
