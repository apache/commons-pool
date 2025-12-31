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

import org.apache.commons.pool3.UsageTracking;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

/**
 * cglib is unmaintained and does not work well (or possibly at all?) in newer JDKs, particularly JDK17+; see https://github.com/cglib/cglib
 * <p>
 * Provides proxy objects using CGLib.
 * </p>
 *
 * @param <T> type of the pooled object to be proxied.
 * @since 2.0
 */
public class CglibProxySource<T> extends AbstractProxySource<T> {

    /**
     * Builds instances of {@link CglibProxySource}.
     *
     * @param <T> type of the pooled object to be proxied.
     * @since 3.0.0
     */
    public static class Builder<T> extends AbstractBuilder<T, CglibProxySource<T>, Builder<T>> {

        private Class<? extends T> superclass;

        /**
         * Constructs a new instance.
         */
        public Builder() {
            // empty
        }

        @Override
        public CglibProxySource<T> get() {
            return new CglibProxySource<>(this);
        }

        /**
         * Sets the superclass.
         *
         * @param superclass the superclass.
         * @return {@code this} instance.
         */
        public Builder<T> setSuperclass(final Class<? extends T> superclass) {
            this.superclass = superclass;
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

    private final Class<? extends T> superclass;

    private CglibProxySource(final Builder<T> builder) {
        super(builder);
        this.superclass = builder.superclass;
    }

    /**
     * Constructs a new proxy source for the given class.
     * <p>
     * For additional features, use a {@link #builder()}.
     * </p>
     *
     * @param superclass The class to proxy
     */
    public CglibProxySource(final Class<? extends T> superclass) {
        super(builder());
        this.superclass = superclass;
    }

    @SuppressWarnings("unchecked") // Case to T on return
    @Override
    public T createProxy(final T pooledObject, final UsageTracking<T> usageTracking) {
        final Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superclass);
        final CglibProxyHandler<T> proxyInterceptor = new CglibProxyHandler<>(pooledObject, usageTracking, isUnwrapInvocationTargetException());
        enhancer.setCallback(proxyInterceptor);
        return (T) enhancer.create();
    }

    @Override
    public T resolveProxy(final T proxy) {
        @SuppressWarnings("unchecked")
        final CglibProxyHandler<T> cglibProxyHandler = (CglibProxyHandler<T>) ((Factory) proxy).getCallback(0);
        return cglibProxyHandler.disableProxy();
    }

    /**
     * Converts this instance to a string suitable for debugging.
     *
     * @since 2.4.3
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CglibProxySource [superclass=");
        builder.append(superclass);
        builder.append("]");
        return builder.toString();
    }
}
