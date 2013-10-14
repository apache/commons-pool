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

import java.lang.reflect.Method;

import org.apache.commons.pool2.UsageTracking;

/**
 * @since 2.0
 */
class BaseProxyHandler<T> {

    private T pooledObject;
    private final UsageTracking<T> usageTracking;


    BaseProxyHandler(T pooledObject, UsageTracking<T> usageTracking) {
        this.pooledObject = pooledObject;
        this.usageTracking = usageTracking;
    }


    T getPooledObject() {
        return pooledObject;
    }


    T disableProxy() {
        T result = pooledObject;
        pooledObject = null;
        return result;
    }


    void validateProxiedObject() {
        if (pooledObject == null) {
            throw new IllegalStateException("This object may no longer be " +
                    "used as it has been returned to the Object Pool.");
        }
    }


    Object doInvoke(Method method, Object[] args) throws Throwable {
        validateProxiedObject();
        T pooledObject = getPooledObject();
        if (usageTracking != null) {
            usageTracking.use(pooledObject);
        }
        return method.invoke(pooledObject, args);
    }
}
