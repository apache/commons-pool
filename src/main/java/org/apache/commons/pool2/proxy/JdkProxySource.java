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

import java.lang.reflect.Proxy;

import org.apache.commons.pool2.UsageTracking;

/**
 * @since 2.0
 */
class JdkProxySource<T> implements ProxySource<T> {

    private final ClassLoader classLoader;
    private final Class<?>[] interfaces;


    JdkProxySource(ClassLoader classLoader, Class<?>[] interfaces) {
        this.classLoader = classLoader;
        this.interfaces = interfaces;
    }


    @Override
    public T createProxy(T pooledObject, UsageTracking<T> usageTracking) {
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(classLoader, interfaces,
                new JdkProxyHandler<T>(pooledObject, usageTracking));
        return proxy;
    }


    @Override
    public T resolveProxy(T proxy) {
        @SuppressWarnings("unchecked")
        JdkProxyHandler<T> jdkProxyHandler =
                (JdkProxyHandler<T>) Proxy.getInvocationHandler(proxy);
        T pooledObject = jdkProxyHandler.disableProxy();
        return pooledObject;
    }
}
