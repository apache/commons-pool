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

import org.apache.commons.pool2.UsageTracking;

/**
 * The interface that any provide of proxy instances must implement to allow the
 * {@link ProxiedObjectPool} to create proxies as required.
 *
 * @param <T> type of the pooled object to be proxied
 *
 * @since 2.0
 */
interface ProxySource<T> {

    /**
     * Create a new proxy object, wrapping the given pooled object.
     *
     * @param pooledObject  The object to wrap
     * @param usageTracking The instance, if any (usually the object pool) to
     *                      be provided with usage tracking information for this
     *                      wrapped object
     *
     * @return the new proxy object
     */
    T createProxy(T pooledObject, UsageTracking<T> usageTracking);

    /**
     * Obtain the wrapped object from the given proxy.
     *
     * @param proxy The proxy object
     *
     * @return The pooled objetc wrapped by the given proxy
     */
    T resolveProxy(T proxy);
}
