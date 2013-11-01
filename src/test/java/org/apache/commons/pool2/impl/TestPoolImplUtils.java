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
package org.apache.commons.pool2.impl;

import static org.junit.Assert.assertEquals;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.junit.Test;

public class TestPoolImplUtils {

    @Test
    public void testFactoryTypeSimple() {
        Class<?> result = PoolImplUtils.getFactoryType(SimpleFactory.class);
        assertEquals(String.class, result);
    }

    @Test
    public void testFactoryTypeNotSimple() {
        Class<?> result = PoolImplUtils.getFactoryType(NotSimpleFactory.class);
        assertEquals(Long.class, result);
    }

    private static class SimpleFactory extends BasePooledObjectFactory<String> {
        @Override
        public String create() throws Exception {
            return null;
        }
        @Override
        public PooledObject<String> wrap(String obj) {
            return null;
        }
    }

    private abstract static class FactoryAB<A,B>
            extends BasePooledObjectFactory<B> {
    }

    private abstract static class FactoryBA<A,B> extends FactoryAB<B,A> {
    }

    private abstract static class FactoryC<C> extends FactoryBA<C, String> {
    }

    private abstract static class FactoryDE<D,E> extends FactoryC<D>{
    }

    private abstract static class FactoryF<F> extends FactoryDE<Long,F>{
    }

    private static class NotSimpleFactory extends FactoryF<Integer> {
        @Override
        public Long create() throws Exception {
            return null;
        }
        @Override
        public PooledObject<Long> wrap(Long obj) {
            return null;
        }
    }
}
