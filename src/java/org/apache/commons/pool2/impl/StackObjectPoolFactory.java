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
        this(factory,StackObjectPoolConfig.Builder.createDefaultConfig());
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
        this.reconfigure(config);
        _factory = factory;
    }

    /**
     * Allows reconfiguring the current StackObjectPoolFactory instance
     * without setting the parameters one by one.
     *
     * @param config the {@link StackObjectPoolConfig} used to configure the pool.
     */
    public synchronized final void reconfigure(StackObjectPoolConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.maxSleeping = config.getMaxSleeping();
        this.initIdleCapacity = config.getInitIdleCapacity();
    }

    /**
     * Create a StackObjectPool.
     * 
     * @return a new StackObjectPool with the configured factory, maxIdle and initial capacity settings
     */
    public ObjectPool<T> createPool() {
        return new StackObjectPool<T>(_factory,new StackObjectPoolConfig.Builder()
                .setMaxSleeping(this.maxSleeping)
                .setInitIdleCapacity(this.initIdleCapacity)
                .createConfig());
    }

    /**
     * The PoolableObjectFactory used by created pools.
     */
    private final PoolableObjectFactory<T> _factory;

    /**
     * cap on the number of "sleeping" instances in the pool
     */
    private int maxSleeping; // @GuardedBy("this")

    /**
     * initial size of the pool (this specifies the size of the container,
     * it does not cause the pool to be pre-populated.)
     */
    private int initIdleCapacity; // @GuardedBy("this")

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
        return this.maxSleeping;
    }

    /**
     * Sets the maxIdle setting for created pools.
     *
     * @param maxSleeping
     * @since 2.0
     */
    public synchronized void setMaxSleeping(int maxSleeping) {
        this.maxSleeping = maxSleeping;
    }

    /**
     * Returns the initial capacity of created pools.
     * 
     * @return size of created containers (created pools are not pre-populated)
     * @since 1.5.5
     */
    public synchronized int getInitCapacity() {
        return this.initIdleCapacity;
    }

    /**
     * Set the size of created containers (created pools are not pre-populated).
     *
     * @param initIdleCapacity size of created containers (created pools are not pre-populated)
     */
    public synchronized void setInitCapacity(int initIdleCapacity) {
        this.initIdleCapacity = initIdleCapacity;
    }

}
