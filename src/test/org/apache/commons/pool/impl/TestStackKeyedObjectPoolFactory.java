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

package org.apache.commons.pool.impl;

import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestKeyedObjectPoolFactory;

/**
 * Tests for {@link StackKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Id$
 */
public class TestStackKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {
    public TestStackKeyedObjectPoolFactory(final String name) {
        super(name);
    }

    @Override
    protected KeyedObjectPoolFactory<Object, Integer> makeFactory(final KeyedPoolableObjectFactory<Object, Integer> objectFactory) throws UnsupportedOperationException {
        return new StackKeyedObjectPoolFactory<Object, Integer>(objectFactory);
    }

    public void testConstructors() throws Exception {
        StackKeyedObjectPoolFactory<Object, Integer> factory = new StackKeyedObjectPoolFactory<Object, Integer>();
        factory.createPool().close();

        factory = new StackKeyedObjectPoolFactory<Object, Integer>(1);
        StackKeyedObjectPool<Object, Integer> pool = (StackKeyedObjectPool<Object, Integer>)factory.createPool();
        assertEquals(1,pool._maxSleeping);
        pool.close();

        factory = new StackKeyedObjectPoolFactory<Object, Integer>(1, 2);
        pool = (StackKeyedObjectPool<Object, Integer>)factory.createPool();
        assertEquals(1,pool._maxSleeping);
        assertEquals(2,pool._initSleepingCapacity);
        pool.close();

        factory = new StackKeyedObjectPoolFactory<Object, Integer>(createObjectFactory());
        pool = (StackKeyedObjectPool<Object, Integer>)factory.createPool();
        pool.close();

        factory = new StackKeyedObjectPoolFactory<Object, Integer>(createObjectFactory(),  1);
        pool = (StackKeyedObjectPool<Object, Integer>)factory.createPool();
        assertEquals(1,pool._maxSleeping);
        pool.close();

        factory = new StackKeyedObjectPoolFactory<Object, Integer>(createObjectFactory(),  1, 2);
        pool = (StackKeyedObjectPool<Object, Integer>)factory.createPool();
        assertEquals(1,pool._maxSleeping);
        assertEquals(2,pool._initSleepingCapacity);
        pool.close();

    }
}
