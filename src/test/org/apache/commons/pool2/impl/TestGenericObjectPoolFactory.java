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

import static junit.framework.Assert.*;

import org.apache.commons.pool2.MethodCallPoolableObjectFactory;
import org.apache.commons.pool2.ObjectPoolFactory;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.TestObjectPoolFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolFactory;
import org.junit.Test;

/**
 * Tests for {@link GenericObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericObjectPoolFactory extends TestObjectPoolFactory {

    @Override
    protected ObjectPoolFactory<Object> makeFactory(
            final PoolableObjectFactory<Object> objectFactory)
            throws UnsupportedOperationException {
        
        GenericObjectPoolConfig<Object> config =
            new GenericObjectPoolConfig<Object>();
        return new GenericObjectPoolFactory<Object>(objectFactory, config);
    }

    @Test
    public void testConstructors() throws Exception {
        final GenericObjectPoolConfig<Object> config =
            new GenericObjectPoolConfig<Object>();
        config.setMaxTotal(1);
        config.setMaxIdle(2);
        config.setMaxWait(3);
        config.setMinIdle(4);
        config.setMinEvictableIdleTimeMillis(5);
        config.setNumTestsPerEvictionRun(6);
        config.setSoftMinEvictableIdleTimeMillis(7);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setLifo(false);
        config.setTimeBetweenEvictionRunsMillis(8);
        config.setBlockWhenExhausted(false);

        GenericObjectPoolFactory<Object> factory =
            new GenericObjectPoolFactory<Object>(
                    new MethodCallPoolableObjectFactory(), config);
        GenericObjectPool<Object> pool =
            (GenericObjectPool<Object>) factory.createPool();

        assertEquals(1, pool.getMaxTotal());
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
        assertEquals(false, pool.getBlockWhenExhausted());
        pool.borrowObject();
        pool.close();
    }
}
