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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.apache.commons.pool2.MethodCallPoolableObjectFactory;
import org.apache.commons.pool2.ObjectPoolFactory;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.TestObjectPoolFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolFactory;
import org.apache.commons.pool2.impl.WhenExhaustedAction;
import org.junit.Test;

/**
 * Tests for {@link GenericObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestGenericObjectPoolFactory extends TestObjectPoolFactory {
    @Override
    protected ObjectPoolFactory<Object> makeFactory(final PoolableObjectFactory<Object> objectFactory) throws UnsupportedOperationException {
        return new GenericObjectPoolFactory<Object>(objectFactory);
    }

    @Test
    public void testConstructors() throws Exception {
        GenericObjectPoolFactory<Object> factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory());
        GenericObjectPool<Object> pool;
        factory.createPool().close();

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
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
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
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
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setWhenExhaustedAction(WhenExhaustedAction.BLOCK);
        config.setMaxWait(125);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(WhenExhaustedAction.BLOCK, pool.getWhenExhaustedAction());
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


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        config.setMaxWait(2);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        config.setMaxWait(2);
        config.setMaxIdle(3);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        config.setMaxWait(2);
        config.setMaxIdle(3);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        config.setMaxWait(2);
        config.setMaxIdle(3);
        config.setTimeBetweenEvictionRunsMillis(4);
        config.setNumTestsPerEvictionRun(5);
        config.setMinEvictableIdleTimeMillis(6);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(false);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
        assertEquals(1, pool.getMaxActive());
        assertEquals(2, pool.getMaxWait());
        assertEquals(3, pool.getMaxIdle());
        assertEquals(4, pool.getTimeBetweenEvictionRunsMillis());
        assertEquals(5, pool.getNumTestsPerEvictionRun());
        assertEquals(6, pool.getMinEvictableIdleTimeMillis());
        assertEquals(true, pool.getTestOnBorrow());
        assertEquals(false, pool.getTestOnReturn());
        assertEquals(false, pool.getTestWhileIdle());
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setMaxWait(2);
        config.setMaxIdle(3);
        config.setMinIdle(4);
        config.setTimeBetweenEvictionRunsMillis(5);
        config.setNumTestsPerEvictionRun(6);
        config.setMinEvictableIdleTimeMillis(7);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
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
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();


        config = new GenericObjectPoolConfig();
        config.setMaxActive(1);
        config.setMaxWait(2);
        config.setMaxIdle(3);
        config.setMinIdle(4);
        config.setTimeBetweenEvictionRunsMillis(5);
        config.setNumTestsPerEvictionRun(6);
        config.setMinEvictableIdleTimeMillis(7);
        config.setSoftMinEvictableIdleTimeMillis(8);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(true);
        config.setLifo(false);
        config.setWhenExhaustedAction(WhenExhaustedAction.GROW);
        factory = new GenericObjectPoolFactory<Object>(new MethodCallPoolableObjectFactory(), config);
        pool = (GenericObjectPool<Object>)factory.createPool();
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
        assertEquals(WhenExhaustedAction.GROW, pool.getWhenExhaustedAction());
        pool.borrowObject();
        pool.close();
    }
}
