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
import java.util.Arrays;

import org.apache.commons.pool2.UsageTracking;

/**
 * Provides proxy objects using Java reflection.
 *
 * @param <T> type of the pooled object to be proxied
 *
 * @since 2.0
 */
public class JdkProxySource<T> implements ProxySource<T> {

    private final ClassLoader classLoader;
    private final Class<?>[] interfaces;


    /**
     * Create a new proxy source for the given interfaces.
     *
     * @param classLoader The class loader with which to create the proxy
     * @param interfaces  The interfaces to proxy
     */
    public JdkProxySource(final ClassLoader classLoader, final Class<?>[] interfaces) {
        this.classLoader = classLoader;
        // Defensive copy
        this.interfaces = new Class<?>[interfaces.length];
        System.arraycopy(interfaces, 0, this.interfaces, 0, interfaces.length);
    }


    @Override
    public T createProxy(final T pooledObject, final UsageTracking<T> usageTracking) {
        @SuppressWarnings("unchecked")
        final
        T proxy = (T) Proxy.newProxyInstance(classLoader, interfaces,
                new JdkProxyHandler<>(pooledObject, usageTracking));
        return proxy;
    }


    @Override
    public T resolveProxy(final T proxy) {
        @SuppressWarnings("unchecked")
        final
        JdkProxyHandler<T> jdkProxyHandler =
                (JdkProxyHandler<T>) Proxy.getInvocationHandler(proxy);
        return jdkProxyHandler.disableProxy();
    }


    /**
     * @since 2.4.3
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("JdkProxySource [classLoader=");
        builder.append(classLoader);
        builder.append(", interfaces=");
        builder.append(Arrays.toString(interfaces));
        builder.append("]");
        return builder.toString();
    }
}
