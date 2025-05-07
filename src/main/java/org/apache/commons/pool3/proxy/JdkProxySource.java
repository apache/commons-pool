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

import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.apache.commons.pool3.UsageTracking;

/**
 * Provides proxy objects using Java reflection.
 *
 * @param <T> type of the pooled object to be proxied
 * @since 2.0
 */
public class JdkProxySource<T> extends AbstractProxySource<T> {

    /**
     * Builds instances of {@link JdkProxySource}.
     *
     * @param <T> type of the pooled object to be proxied.
     * @since 3.0.0
     */
    public static class Builder<T> extends AbstractBuilder<T, JdkProxySource<T>, Builder<T>> {

        private ClassLoader classLoader;
        private Class<?>[] interfaces;

        /**
         * Constructs a new instance.
         */
        public Builder() {
            // empty
        }

        @Override
        public JdkProxySource<T> get() {
            return new JdkProxySource<>(this);
        }

        /**
         * Sets the class loader to define the proxy class.
         *
         * @param classLoader the class loader to define the proxy class.
         * @return {@code this} instance.
         */
        public Builder<T> setClassLoader(final ClassLoader classLoader) {
            this.classLoader = classLoader;
            return asThis();
        }

        /**
         * Sets the list of interfaces for the proxy class.
         *
         * @param interfaces the list of interfaces for the proxy class.
         * @return {@code this} instance.
         */
        public Builder<T> setInterfaces(final Class<?>... interfaces) {
            this.interfaces = interfaces != null ? interfaces.clone() : null;
            return asThis();
        }
    }

    /**
     * Constructs a new builder of {@link CglibProxySource}.
     *
     * @param <T> type of the pooled object to be proxied.
     * @return a new builder of {@link CglibProxySource}.
     * @since 3.0.0
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** The class loader to define the proxy class. */
    private final ClassLoader classLoader;

    /** The list of interfaces for the proxy class to implement. */
    private final Class<?>[] interfaces;

    private JdkProxySource(final Builder<T> builder) {
        super(builder);
        this.classLoader = builder.classLoader;
        this.interfaces = builder.interfaces;
    }

    /**
     * Constructs a new proxy source for the given interfaces.
     * <p>
     * For additional features, use a {@link #builder()}.
     * </p>
     *
     * @param classLoader The class loader with which to create the proxy
     * @param interfaces  The interfaces to proxy
     */
    public JdkProxySource(final ClassLoader classLoader, final Class<?>[] interfaces) {
        super(builder());
        this.classLoader = classLoader;
        this.interfaces = interfaces.clone();
    }

    @SuppressWarnings("unchecked") // Cast to T on return.
    @Override
    public T createProxy(final T pooledObject, final UsageTracking<T> usageTracking) {
        return (T) Proxy.newProxyInstance(classLoader, interfaces, new JdkProxyHandler<>(pooledObject, usageTracking, isUnwrapInvocationTargetException()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public T resolveProxy(final T proxy) {
        return ((JdkProxyHandler<T>) Proxy.getInvocationHandler(proxy)).disableProxy();
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
