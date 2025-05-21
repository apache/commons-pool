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

import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Abstracts a proxy source implementation.
 *
 * @param <T> type of the pooled object to be proxied.
 * @since 3.0.0
 */
public abstract class AbstractProxySource<T> implements ProxySource<T> {

    /**
     * Abstract a builder implementations.
     *
     * @param <T> type of the pooled object to be proxied.
     * @param <P> the AbstractProxySource subclass
     * @param <B> the builder subclass.
     */
    public abstract static class AbstractBuilder<T, P, B extends AbstractBuilder<T, P, B>> implements Supplier<P> {

        /**
         * Whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of {@link InvocationTargetException}.
         */
        protected boolean unwrapInvocationTargetException;

        /**
         * Constructs a new instance for a subclass.
         */
        public AbstractBuilder() {
            // empty
        }

        /**
         * Returns {@code this} instance typed as a subclass.
         *
         * @return {@code this} instance typed as a subclass.
         */
        @SuppressWarnings("unchecked")
        B asThis() {
            return (B) this;
        }

        /**
         * Sets whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of {@link InvocationTargetException}.
         *
         * @param unwrapInvocationTargetException whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of
         *                                        {@link InvocationTargetException}.
         * @return {@code this} instance.
         */
        public B setUnwrapInvocationTargetException(final boolean unwrapInvocationTargetException) {
            this.unwrapInvocationTargetException = unwrapInvocationTargetException;
            return asThis();
        }
    }

    /**
     * Whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of {@link InvocationTargetException}.
     */
    private final boolean unwrapInvocationTargetException;

    /**
     * Constructs a new instance.
     *
     * @param builder Information used to build the new instance.
     */
    protected AbstractProxySource(final AbstractBuilder builder) {
        this.unwrapInvocationTargetException = builder.unwrapInvocationTargetException;
    }

    /**
     * Tests whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of {@link InvocationTargetException}.
     *
     * @return whether the proxy throws {@link InvocationTargetException#getTargetException()} instead of {@link InvocationTargetException}.
     */
    protected boolean isUnwrapInvocationTargetException() {
        return unwrapInvocationTargetException;
    }
}
