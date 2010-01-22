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

import java.util.NoSuchElementException;

import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPoolFactory;

/**
 * Tests for {@link GenericObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericObjectPoolFactory extends TestObjectPoolFactory {
    public TestGenericObjectPoolFactory(final String name) {
        super(name);
    }

    protected ObjectPoolFactory makeFactory(final PoolableObjectFactory objectFactory) throws UnsupportedOperationException {
        return new GenericObjectPoolFactory(objectFactory);
    }

    public void testConstructors() throws Exception {
        GenericObjectPoolFactory factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory());
        GenericObjectPool pool;
        factory.createPool().close();

        final GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.maxActive = 1;
        config.maxIdle = 2;
        config.maxWait = 3;
        config.minIdle = 4;
        config.minEvictableIdleTimeMillis = 5;
        config.numTestsPerEvictionRun = 6;
        config.softMinEvictableIdleTimeMillis = 7;
        config.testOnBorrow = true;
        config.testOnReturn = false;
        config.testWhileIdle = true;
        config.lifo = false;
        config.timeBetweenEvictionRunsMillis = 8;
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxIdle());
        assertEquals(3, pool.getMaxWait());
        assertEquals(4, pool.getMinIdle());
        assertEquals(5, pool.getMinEvictableIdleTimeMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(7, pool.getSoftMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(false, pool.getLifo());
        assertEquals(8, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, 125);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_BLOCK, pool.getWhenExhaustedAction());
        assertEquals(125, pool.getMaxWait());
        pool.borrowObject();
        long startTime = System.currentTimeMillis();
        try {
            pool.borrowObject();
            fail();
        } catch (NoSuchElementException nsee) {
            // expected
        }
        long delay = System.currentTimeMillis() - startTime;
        assertTrue("delay: " + delay, delay > 100);
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, true, false);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, 3);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, true, false);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, true, false, 4, 5, 6, false);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(5, pool.getNumTestsPerEvictionRun());
        assertEquals(6, pool.getMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(false, pool.getTestWhileIdle());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, 4, true, false, 5, 6, 7, true);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getMinIdle());
        assertEquals(5, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(7, pool.getMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        factory = new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1, GenericObjectPool.WHEN_EXHAUSTED_GROW, 2, 3, 4, true, false, 5, 6, 7, true, 8, false);
        pool = (GenericObjectPool)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getMinIdle());
        assertEquals(5, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(6, pool.getNumTestsPerEvictionRun());
        assertEquals(7, pool.getMinEvictableIdleTimeMillis());
        assertEquals(8, pool.getSoftMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(true, pool.getTestWhileIdle());
        assertEquals(false, pool.getLifo());
        assertEquals(GenericObjectPool.WHEN_EXHAUSTED_GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();
    }
}
