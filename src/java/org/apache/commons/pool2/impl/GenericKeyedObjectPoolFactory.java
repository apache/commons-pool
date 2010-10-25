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

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedObjectPoolFactory;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;

/**
 * A factory for creating {@link GenericKeyedObjectPool} instances.
 *
 * @see GenericKeyedObjectPool
 * @see KeyedObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericKeyedObjectPoolFactory<K,V> implements KeyedObjectPoolFactory<K,V> {
    /**
     * Create a new GenericKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory to used by created pools.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory)
     */
    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory<K,V> factory) {
        this(factory,new GenericKeyedObjectPoolConfig());
    }

    /**
     * Create a new GenericKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory to used by created pools.
     * @param config a non-null GenericKeyedObjectPool.Config describing the configuration.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory, GenericKeyedObjectPool.Config)
     * @throws NullPointerException when config is <code>null</code>.
     */
    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory<K,V> factory, GenericKeyedObjectPoolConfig config) throws NullPointerException {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        _factory = factory;
        this.config = config;
    }

    /**
     * Create a new GenericKeyedObjectPool with the currently configured properties.
     * 
     * @return GenericKeyedObjectPool with {@link GenericKeyedObjectPool.Config Configuration} determined by
     * current property settings
     */
    public KeyedObjectPool<K,V> createPool() {
        return new GenericKeyedObjectPool<K,V>(_factory,this.config);
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMaxIdle() maxIdle} setting for pools created by this factory.
     * @since 1.5.5
     */
    public int getMaxIdle() {
        return this.config.getMaxIdle();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMaxActive() maxActive} setting for pools created by this factory.
     * @since 1.5.5
     */
    public int getMaxActive() {
        return this.config.getMaxActive();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMaxTotal() maxTotal} setting for pools created by this factory.
     * @since 1.5.5
     */
    public int getMaxTotal() {
        return this.config.getMaxTotal();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMinIdle() minIdle} setting for pools created by this factory.
     * @since 1.5.5
     */
    public int getMinIdle() {
        return this.config.getMinIdle();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMaxWait() maxWait} setting for pools created by this factory.
     * @since 1.5.5
     */
    public long getMaxWait() {
        return this.config.getMaxWait();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getWhenExhaustedAction() whenExhaustedAction} setting for pools created by this factory.
     * @since 1.5.5
     */
    public WhenExhaustedAction getWhenExhaustedAction() {
        return this.config.getWhenExhaustedAction();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getTestOnBorrow() testOnBorrow} setting for pools created by this factory.
     * @since 1.5.5
     */
    public boolean getTestOnBorrow() {
        return this.config.getTestOnBorrow();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getTestOnReturn() testOnReturn} setting for pools created by this factory.
     * @since 1.5.5
     */
    public boolean getTestOnReturn() {
        return this.config.getTestOnReturn();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getTestWhileIdle() testWhileIdle} setting for pools created by this factory.
     * @since 1.5.5
     */
    public boolean getTestWhileIdle() {
        return this.config.getTestWhileIdle();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getTimeBetweenEvictionRunsMillis() timeBetweenEvictionRunsMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public long getTimeBetweenEvictionRunsMillis() {
        return this.config.getTimeBetweenEvictionRunsMillis();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getNumTestsPerEvictionRun() numTestsPerEvictionRun}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public int getNumTestsPerEvictionRun() {
        return this.config.getNumTestsPerEvictionRun();
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getMinEvictableIdleTimeMillis() minEvictableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public long getMinEvictableIdleTimeMillis() {
        return this.config.getMinEvictableIdleTimeMillis();
    }

    /**
     * @return the {@link KeyedPoolableObjectFactory} used by pools created by this factory.
     * @since 1.5.5
     */
    public KeyedPoolableObjectFactory<K,V> getFactory() {
        return _factory;
    }

    /**
     * @return the {@link GenericKeyedObjectPool#getLifo() lifo} setting for pools created by this factory.
     * @since 1.5.5
     */
    public boolean getLifo() {
        return this.config.getLifo();
    }

    //--- private attributes

    private final KeyedPoolableObjectFactory<K,V> _factory;

    private final GenericKeyedObjectPoolConfig config;

}
