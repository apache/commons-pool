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
 * Tests for {@link GenericKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {
    public TestGenericKeyedObjectPoolFactory(final String name) {
        super(name);
    }

    protected KeyedObjectPoolFactory makeFactory(final KeyedPoolableObjectFactory objectFactory) {
        return new GenericKeyedObjectPoolFactory(objectFactory);
    }

    public void testConstructors() throws Exception {
        GenericKeyedObjectPoolFactory factory = new GenericKeyedObjectPoolFactory(createObjectFactory());
        factory.createPool().close();
        GenericKeyedObjectPool pool;


        final GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        config.maxActive = 1;
        config.maxIdle = 2;
        config.maxWait = 3;
        config.minIdle = 4;
        config.minEvictableIdleTimeMillis = 5;
        config.numTestsPerEvictionRun = 6;
        config.testOnBorrow = true;
        config.testOnReturn = false;
        config.testWhileIdle = true;
        config.timeBetweenEvictionRunsMillis = 8;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        config.lifo = false;
        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), config);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxIdle());
        assertEquals(3, pool.getMaxWait());
        assertEquals(4, pool.getMinIdle());
        assertEquals(5, pool.getMinEvictableIdleTimeMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(false, pool.getLifo());
        assertEquals(8, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, 125);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, pool.getWhenExhaustedAction());
        assertEquals(125, pool.getMaxWait());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, true, false);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, 3);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, 4);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getMaxTotal());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, true, false);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, true, false, 4, 5, 6, false);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(5, pool.getNumTestsPerEvictionRun());
        assertEquals(6, pool.getMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(false, pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();


        factory = new GenericKeyedObjectPoolFactory(createObjectFactory(), 1, GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, 4, true, false, 5, 6, 7, true);
        pool = (GenericKeyedObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getMaxTotal());
        assertEquals(5, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(7, pool.getMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.close();
    }
}
