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

import org.apache.commons.pool2.KeyedObjectPoolFactory;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.TestKeyedObjectPoolFactory;
import org.apache.commons.pool2.impl.StackKeyedObjectPool;
import org.apache.commons.pool2.impl.StackKeyedObjectPoolFactory;
import org.junit.Test;

/**
 * Tests for {@link StackKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestStackKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {
    @Override
    protected KeyedObjectPoolFactory<Object,Object> makeFactory(final KeyedPoolableObjectFactory<Object,Object> objectFactory) throws UnsupportedOperationException {
        return new StackKeyedObjectPoolFactory<Object,Object>(objectFactory);
    }

    @Test
    public void testConstructors() throws Exception {
        StackKeyedObjectPoolFactory<Object,Object> factory = new StackKeyedObjectPoolFactory<Object,Object>(createObjectFactory());
        StackKeyedObjectPool<Object,Object> pool = (StackKeyedObjectPool<Object,Object>)factory.createPool();
        pool.close();

        StackObjectPoolConfig config = new StackObjectPoolConfig();
        config.setMaxSleeping(1);
        factory = new StackKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (StackKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1,pool.getMaxSleeping());
        pool.close();

        config.setInitIdleCapacity(2);
        factory = new StackKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (StackKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1,pool.getMaxSleeping());
        assertEquals(2,pool.getInitSleepingCapacity());
        pool.close();

    }
}
