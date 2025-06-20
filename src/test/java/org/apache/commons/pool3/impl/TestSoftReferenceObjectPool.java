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
package org.apache.commons.pool3.impl;

import org.apache.commons.pool3.BasePooledObjectFactory;
import org.apache.commons.pool3.ObjectPool;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;
import org.apache.commons.pool3.TestBaseObjectPool;

/**
 */
class TestSoftReferenceObjectPool extends TestBaseObjectPool {

    private static final class SimpleFactory extends BasePooledObjectFactory<String, RuntimeException> {

        int counter;

        @Override
        public String create() {
            return String.valueOf(counter++);
        }

        @Override
        public PooledObject<String> wrap(final String value) {
            return new DefaultPooledObject<>(value);
        }
    }

    @Override
    protected Object getNthObject(final int n) {
        return String.valueOf(n);
    }

    @Override
    protected boolean isFifo() {
        return false;
    }

    @Override
    protected boolean isLifo() {
        return false;
    }

    @Override
    protected <E extends Exception> ObjectPool<String, E> makeEmptyPool(final int cap) {
        return (ObjectPool<String, E>) new SoftReferenceObjectPool<>(new SimpleFactory());
    }

    @Override
    protected <E extends Exception> ObjectPool<Object, E> makeEmptyPool(final PooledObjectFactory<Object, E> factory) {
        return new SoftReferenceObjectPool<>(factory);
    }
}
