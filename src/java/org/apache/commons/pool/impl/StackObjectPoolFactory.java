/*
 * Copyright 1999-2004,2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;

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
public class StackObjectPoolFactory implements ObjectPoolFactory {
    /**
     * Create a new StackObjectPoolFactory.
     * 
     * @see StackObjectPool#StackObjectPool()
     */
    public StackObjectPoolFactory() {
        this((PoolableObjectFactory)null,StackObjectPool.DEFAULT_MAX_SLEEPING,StackObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool.
     * @see StackObjectPool#StackObjectPool(int)
     */
    public StackObjectPoolFactory(int maxIdle) {
        this((PoolableObjectFactory)null,maxIdle,StackObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool.
     * @param initIdleCapacity - initial size of the pool (this specifies the size of the container, it does not cause the pool to be pre-populated.)
     * @see StackObjectPool#StackObjectPool(int, int)
     */
    public StackObjectPoolFactory(int maxIdle, int initIdleCapacity) {
        this((PoolableObjectFactory)null,maxIdle,initIdleCapacity);
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @see StackObjectPool#StackObjectPool(PoolableObjectFactory)
     */
    public StackObjectPoolFactory(PoolableObjectFactory factory) {
        this(factory,StackObjectPool.DEFAULT_MAX_SLEEPING,StackObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxIdle cap on the number of "sleeping" instances in the pool.
     */
    public StackObjectPoolFactory(PoolableObjectFactory factory, int maxIdle) {
        this(factory,maxIdle,StackObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxIdle cap on the number of "sleeping" instances in the pool.
     * @param initIdleCapacity - initial size of the pool (this specifies the size of the container, it does not cause the pool to be pre-populated.)
     */
    public StackObjectPoolFactory(PoolableObjectFactory factory, int maxIdle, int initIdleCapacity) {
        _factory = factory;
        _maxSleeping = maxIdle;
        _initCapacity = initIdleCapacity;
    }

    public ObjectPool createPool() {
        return new StackObjectPool(_factory,_maxSleeping,_initCapacity);
    }

    protected PoolableObjectFactory _factory = null;
    protected int _maxSleeping = StackObjectPool.DEFAULT_MAX_SLEEPING;
    protected int _initCapacity = StackObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY;

}
