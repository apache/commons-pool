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

package org.apache.commons.pool3;

import java.util.function.Supplier;

import org.apache.commons.pool3.impl.DefaultPooledObject;

/**
 * A poolable object factory for testing.
 *
 */
public class MethodCallPoolableObjectFactory <K> implements PooledObjectFactory<K, PrivateException> {
    private final Supplier<K> supplier;

    public MethodCallPoolableObjectFactory(final Supplier<K> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void activateObject(final PooledObject<K> obj) {
       //do nothing
    }

    @Override
    public void destroyObject(final PooledObject<K> obj) {
        //do nothing
    }

    @Override
    public PooledObject<K> makeObject() {
        // Generate new object, don't use cache via Integer.valueOf(...)
        final K obj = supplier.get();
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void passivateObject(final PooledObject<K> obj) {
        //do nothing
    }

    @Override
    public boolean validateObject(final PooledObject<K> obj) {
        return true;
    }
}
