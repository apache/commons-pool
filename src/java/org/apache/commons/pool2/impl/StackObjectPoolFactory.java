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
 * A factory for creating {@link StackObjectPool} instances.
 *
 * @see StackObjectPool
 * @see StackKeyedObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class StackObjectPoolFactory<T> implements ObjectPoolFactory<T> {
    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @see StackObjectPool#StackObjectPool(PoolableObjectFactory)
     */
    public StackObjectPoolFactory(PoolableObjectFactory<T> factory) {
        this(factory,new StackObjectPoolConfig());
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param config the {@link StackObjectPoolConfig} used to configure the pool.
     */
    public StackObjectPoolFactory(PoolableObjectFactory<T> factory, StackObjectPoolConfig config) {
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
     * Create a StackObjectPool.
     * 
     * @return a new StackObjectPool with the configured factory, maxIdle and initial capacity settings
     */
    public ObjectPool<T> createPool() {
        return new StackObjectPool<T>(_factory,this.config);
    }

    /**
     * The PoolableObjectFactory used by created pools.
     */
    private final PoolableObjectFactory<T> _factory;

    /**
     * The {@link StackObjectPoolConfig} used to configure the pool.
     */
    private final StackObjectPoolConfig config;

    /**
     * Returns the factory used by created pools.
     * 
     * @return the PoolableObjectFactory used by created pools
     * @since 1.5.5
     */
    public PoolableObjectFactory<T> getFactory() {
        return _factory;
    }

    /**
     * Returns the maxIdle setting for created pools.
     * 
     * @return the maximum number of idle instances in created pools
     * @since 1.5.5
     */
    public synchronized int getMaxSleeping() {
        return this.config.getMaxSleeping();
    }

    /**
     * Sets the maxIdle setting for created pools.
     *
     * @param maxSleeping
     * @since 2.0
     */
    public synchronized void setMaxSleeping(int maxSleeping) {
        this.config.setMaxSleeping(maxSleeping);
    }

    /**
     * Returns the initial capacity of created pools.
     * 
     * @return size of created containers (created pools are not pre-populated)
     * @since 1.5.5
     */
    public synchronized int getInitCapacity() {
        return this.config.getInitIdleCapacity();
    }

}
