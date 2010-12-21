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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.pool2.KeyedObjectPoolFactory;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.TestKeyedObjectPoolFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool2.impl.WhenExhaustedAction;
import org.junit.Test;

/**
 * Tests for {@link GenericKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {
    @Override
    protected KeyedObjectPoolFactory<Object,Object> makeFactory(final KeyedPoolableObjectFactory<Object,Object> objectFactory) {
        return new GenericKeyedObjectPoolFactory<Object,Object>(objectFactory);
    }

    @Test
    public void testConstructors() throws Exception {
        GenericKeyedObjectPoolFactory<Object,Object> factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory());
        factory.createPool().close();
        GenericKeyedObjectPool<Object,Object> pool;


        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxIdlePerKey(2)
            .setMaxWait(3)
            .setMinIdlePerKey(4)
            .setMinEvictableIdleTimeMillis(5)
            .setNumTestsPerEvictionRun(6)
            .setTestOnBorrow(true)
            .setTestOnReturn(false)
            .setTestWhileIdle(true)
            .setTimeBetweenEvictionRunsMillis(8)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .setLifo(false)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxIdlePerKey());
        assertEquals(3, pool.getMaxWait());
        assertEquals(4, pool.getMinIdlePerKey());
        assertEquals(5, pool.getMinEvictableIdleTimeMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertTrue(pool.getTestOnBorrow());
        assertFalse(pool.getTestOnReturn());
        assertTrue(pool.getTestWhileIdle());
        assertFalse(pool.getLifo());
        assertEquals(8, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setWhenExhaustedAction(WhenExhaustedAction.BLOCK)
            .setMaxWait(125)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(WhenExhaustedAction.BLOCK, pool.getWhenExhaustedAction());
        assertEquals(125, pool.getMaxWait());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setTestOnBorrow(true)
            .setTestOnReturn(false)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertTrue(pool.getTestOnBorrow());
        assertFalse(pool.getTestOnReturn());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setMaxIdlePerKey(3)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdlePerKey());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setMaxIdlePerKey(3)
            .setMaxTotal(4)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdlePerKey());
        assertEquals(4, pool.getMaxTotal());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setMaxIdlePerKey(3)
            .setTestOnBorrow(true)
            .setTestOnReturn(false)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdlePerKey());
        assertTrue(pool.getTestOnBorrow());
        assertFalse(pool.getTestOnReturn());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setMaxIdlePerKey(3)
            .setTimeBetweenEvictionRunsMillis(4)
            .setNumTestsPerEvictionRun(5)
            .setMinEvictableIdleTimeMillis(6)
            .setTestOnBorrow(true)
            .setTestOnReturn(false)
            .setTestWhileIdle(false)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdlePerKey());
        assertEquals(4, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(5, pool.getNumTestsPerEvictionRun());
        assertEquals(6, pool.getMinEvictableIdleTimeMillis());
        assertTrue(pool.getTestOnBorrow());
        assertFalse(pool.getTestOnReturn());
        assertFalse(pool.getTestWhileIdle());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();


        config = new GenericKeyedObjectPoolConfig.Builder()
            .setMaxTotalPerKey(1)
            .setMaxWait(2)
            .setMaxIdlePerKey(3)
            .setMaxTotal(4)
            .setTimeBetweenEvictionRunsMillis(5)
            .setNumTestsPerEvictionRun(6)
            .setMinEvictableIdleTimeMillis(7)
            .setTestOnBorrow(true)
            .setTestOnReturn(false)
            .setTestWhileIdle(true)
            .setWhenExhaustedAction(WhenExhaustedAction.GROW)
            .createConfig();
        factory = new GenericKeyedObjectPoolFactory<Object,Object>(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool<Object,Object>)factory.createPool();
        assertEquals(1, pool.getMaxTotalPerKey());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdlePerKey());
        assertEquals(4, pool.getMaxTotal());
        assertEquals(5, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(7, pool.getMinEvictableIdleTimeMillis());
        assertTrue(pool.getTestOnBorrow());
        assertFalse(pool.getTestOnReturn());
        assertTrue(pool.getTestWhileIdle());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.close();
    }
}
