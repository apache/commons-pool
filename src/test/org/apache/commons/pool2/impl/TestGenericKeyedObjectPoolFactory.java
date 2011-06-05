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

import static junit.framework.Assert.assertEquals;

import org.apache.commons.pool2.KeyedObjectPoolFactory;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.TestKeyedObjectPoolFactory;
import org.junit.Test;

/**
 * Tests for {@link GenericKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {

    @Override
    protected KeyedObjectPoolFactory<Object,Object> makeFactory(
            final KeyedPoolableObjectFactory<Object,Object> objectFactory) {
        GenericKeyedObjectPoolConfig<Object,Object> config =
            new GenericKeyedObjectPoolConfig<Object,Object>();
        config.setFactory(objectFactory);
        return new GenericKeyedObjectPoolFactory<Object,Object>(config);
    }

    @Test
    public void testConstructors() throws Exception {
        final GenericKeyedObjectPoolConfig<Object,Object> config =
            new GenericKeyedObjectPoolConfig<Object,Object>();
        config.setMaxTotalPerKey(1);
        config.setMaxIdlePerKey(2);
        config.setMaxWait(3);
        config.setMinIdlePerKey(4);
        config.setMinEvictableIdleTimeMillis(5);
        config.setNumTestsPerEvictionRun(6);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRunsMillis(8);
        config.setWhenExhaustedAction(WhenExhaustedAction.FAIL);
        config.setLifo(false);
        config.setFactory(createObjectFactory());
        GenericKeyedObjectPoolFactory<Object,Object> factory =
            new GenericKeyedObjectPoolFactory<Object,Object>(config);
        GenericKeyedObjectPool<Object,Object> pool =
            (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxIdlePerKey());
        assertEquals(3, pool.getMaxWait());
        assertEquals(4, pool.getMinIdlePerKey());
        assertEquals(5, pool.getMinEvictableIdleTimeMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(false, pool.getLifo());
        assertEquals(8, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(WhenExhaustedAction.FAIL, pool.getWhenExhaustedAction());
        pool.close();
    }
}
