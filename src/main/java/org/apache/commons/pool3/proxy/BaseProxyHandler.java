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

import java.lang.reflect.Method;

import org.apache.commons.pool3.UsageTracking;

/**
 * Base implementation for object wrappers when using a
 * {@link ProxiedObjectPool}.
 *
 * @param <T> type of the wrapped pooled object
 *
 * @since 2.0
 */
class BaseProxyHandler<T> {

    private volatile T pooledObject;
    private final UsageTracking<T> usageTracking;

    /**
     * Constructs a new wrapper for the given pooled object.
     *
     * @param pooledObject  The object to wrap
     * @param usageTracking The instance, if any (usually the object pool) to
     *                      be provided with usage tracking information for this
     *                      wrapped object
     */
    BaseProxyHandler(final T pooledObject, final UsageTracking<T> usageTracking) {
        this.pooledObject = pooledObject;
        this.usageTracking = usageTracking;
    }

    /**
     * Disable the proxy wrapper. Called when the object has been returned to
     * the pool. Further use of the wrapper should result in an
     * {@link IllegalStateException}.
     *
     * @return the object that this proxy was wrapping
     */
    T disableProxy() {
        final T result = pooledObject;
        pooledObject = null;
        return result;
    }

    /**
     * Invoke the given method on the wrapped object.
     *
     * @param method    The method to invoke
     * @param args      The arguments to the method
     * @return          The result of the method call
     * @throws Throwable    If the method invocation fails
     */
    Object doInvoke(final Method method, final Object[] args) throws Throwable {
        validateProxiedObject();
        final T object = getPooledObject();
        if (usageTracking != null) {
            usageTracking.use(object);
        }
        return method.invoke(object, args);
    }

    /**
     * Gets the wrapped, pooled object.
     *
     * @return the underlying pooled object
     */
    T getPooledObject() {
        return pooledObject;
    }

    /**
     * @since 2.4.3
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" [pooledObject=");
        builder.append(pooledObject);
        builder.append(", usageTracking=");
        builder.append(usageTracking);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Check that the proxy is still valid (i.e. that {@link #disableProxy()}
     * has not been called).
     *
     * @throws IllegalStateException if {@link #disableProxy()} has been called
     */
    void validateProxiedObject() {
        if (pooledObject == null) {
            throw new IllegalStateException("This object may no longer be " +
                    "used as it has been returned to the Object Pool.");
        }
    }
}
