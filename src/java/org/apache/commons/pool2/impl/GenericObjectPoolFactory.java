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

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.ObjectPoolFactory;
import org.apache.commons.pool2.PoolableObjectFactory;

/**
 * A factory for creating {@link GenericObjectPool} instances.
 *
 * @see GenericObjectPool
 * @see ObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericObjectPoolFactory<T> implements ObjectPoolFactory<T> {
    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @see GenericObjectPoolConfig#GenericObjectPool(PoolableObjectFactory)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory<T> factory) {
        this(factory,new GenericObjectPoolConfig());
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param config a non-<code>null</code> GenericObjectPoolConfig describing the configuration.
     * @throws IllegalArgumentException when factory or config is <code>null</code>.
     * @see GenericObjectPoolConfig#GenericObjectPool(PoolableObjectFactory, GenericObjectPoolConfig)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory<T> factory, GenericObjectPoolConfig config) throws NullPointerException {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this._factory = factory;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    public ObjectPool<T> createPool() {
        return new GenericObjectPool<T>(_factory,this.config);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getMaxIdle() maxIdle} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getMaxIdle() {
        return this.config.getMaxIdle();
    }

    /**
     * @param maxIdle the {@link GenericObjectPoolConfig#getMaxIdle() maxIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxIdle(int maxIdle) {
        this.config.setMaxIdle(maxIdle);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getMinIdle() minIdle} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getMinIdle() {
        return this.config.getMinIdle();
    }

    /**
     * @param minIdle the {@link GenericObjectPoolConfig#getMinIdle() minIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMinIdle(int minIdle) {
        this.config.setMinIdle(minIdle);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getMaxActive() maxActive} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getMaxActive() {
        return this.config.getMaxActive();
    }

    /**
     * @param maxActive the {@link GenericObjectPoolConfig#getMaxActive() maxActive} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxActive(int maxActive) {
        this.config.setMaxActive(maxActive);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getMaxWait() maxWait} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getMaxWait() {
        return this.config.getMaxWait();
    }

    /**
     * @param maxWait the {@link GenericObjectPoolConfig#getMaxWait() maxWait} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxWait(long maxWait) {
        this.config.setMaxWait(maxWait);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getWhenExhaustedAction() whenExhaustedAction} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized WhenExhaustedAction getWhenExhaustedAction() {
        return this.config.getWhenExhaustedAction();
    }

    /**
     * @param whenExhaustedAction the {@link GenericObjectPoolConfig#getWhenExhaustedAction() whenExhaustedAction} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction) {
        this.config.setWhenExhaustedAction(whenExhaustedAction);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getTestOnBorrow() testOnBorrow} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestOnBorrow() {
        return this.config.getTestOnBorrow();
    }

    /**
     * @param testOnBorrow the {@link GenericObjectPoolConfig#getTestOnBorrow() testOnBorrow} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestOnBorrow(boolean testOnBorrow) {
        this.config.setTestOnBorrow(testOnBorrow);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getTestOnReturn() testOnReturn} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestOnReturn() {
        return this.config.getTestOnReturn();
    }

    /**
     * @param testOnReturn the {@link GenericObjectPoolConfig#getTestOnReturn() testOnReturn} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestOnReturn(boolean testOnReturn) {
        this.config.setTestOnReturn(testOnReturn);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getTestWhileIdle() testWhileIdle} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestWhileIdle() {
        return this.config.getTestWhileIdle();
    }

    /**
     * @param testWhileIdle the {@link GenericObjectPoolConfig#getTestWhileIdle() testWhileIdle} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        this.config.setTestWhileIdle(testWhileIdle);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getTimeBetweenEvictionRunsMillis() timeBetweenEvictionRunsMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getTimeBetweenEvictionRunsMillis() {
        return this.config.getTimeBetweenEvictionRunsMillis();
    }

    /**
     * @param timeBetweenEvictionRunsMillis the {@link GenericObjectPoolConfig#getTimeBetweenEvictionRunsMillis() timeBetweenEvictionRunsMillis}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getNumTestsPerEvictionRun() numTestsPerEvictionRun}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getNumTestsPerEvictionRun() {
        return this.config.getNumTestsPerEvictionRun();
    }

    /**
     * @param numTestsPerEvictionRun the {@link GenericObjectPoolConfig#getNumTestsPerEvictionRun() numTestsPerEvictionRun}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getMinEvictableIdleTimeMillis() minEvictableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getMinEvictableIdleTimeMillis() {
        return this.config.getMinEvictableIdleTimeMillis();
    }

    /**
     * @param minEvictableIdleTimeMillis the {@link GenericObjectPoolConfig#getMinEvictableIdleTimeMillis() minEvictableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getSoftMinEvictableIdleTimeMillis() softMinEvicatableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getSoftMinEvictableIdleTimeMillis() {
        return this.config.getSoftMinEvictableIdleTimeMillis();
    }

    /**
     * @param softMinEvictableIdleTimeMillis the {@link GenericObjectPoolConfig#getSoftMinEvictableIdleTimeMillis() softMinEvicatableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        this.config.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }

    /**
     * @return the {@link GenericObjectPoolConfig#getLifo() lifo} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getLifo() {
        return this.config.getLifo();
    }

    /**
     * @param lifo the {@link GenericObjectPoolConfig#getLifo() lifo} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setLifo(boolean lifo) {
        this.config.setLifo(lifo);
    }

    /**
     * @return the {@link PoolableObjectFactory} used by pools created by this factory
     */
    public PoolableObjectFactory<T> getFactory() {
        return _factory;
    }

    /**
     * The {@link PoolableObjectFactory} used by pools created by this factory.
     */
    private final PoolableObjectFactory<T> _factory;

    /**
     * The {@link GenericObjectPoolConfig} used by pools created by this factory.
     */
    private final GenericObjectPoolConfig config;

}
