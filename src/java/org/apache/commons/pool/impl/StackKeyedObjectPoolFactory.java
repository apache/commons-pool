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

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A factory for creating {@link StackKeyedObjectPool} instances.
 *
 * @see StackKeyedObjectPool
 * @see KeyedObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class StackKeyedObjectPoolFactory implements KeyedObjectPoolFactory {
    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @see StackKeyedObjectPool#StackKeyedObjectPool()
     */
    public StackKeyedObjectPoolFactory() {
        this((KeyedPoolableObjectFactory)null,StackKeyedObjectPool.DEFAULT_MAX_SLEEPING,StackKeyedObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @param max cap on the number of "sleeping" instances in the pool.
     * @see StackKeyedObjectPool#StackKeyedObjectPool(int)
     */
    public StackKeyedObjectPoolFactory(int max) {
        this((KeyedPoolableObjectFactory)null,max,StackKeyedObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @param max cap on the number of "sleeping" instances in the pool.
     * @param init initial size of the pool (this specifies the size of the container, it does not cause the pool to be pre-populated.)
     * @see StackKeyedObjectPool#StackKeyedObjectPool(int, int)
     */
    public StackKeyedObjectPoolFactory(int max, int init) {
        this((KeyedPoolableObjectFactory)null,max,init);
    }

    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory used by created pools.
     * @see StackKeyedObjectPool#StackKeyedObjectPool(KeyedPoolableObjectFactory)
     */
    public StackKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory) {
        this(factory,StackKeyedObjectPool.DEFAULT_MAX_SLEEPING,StackKeyedObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory used by created pools.
     * @param max cap on the number of "sleeping" instances in the pool.
     * @see StackKeyedObjectPool#StackKeyedObjectPool(KeyedPoolableObjectFactory, int)
     */
    public StackKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int max) {
        this(factory,max,StackKeyedObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new StackKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory used by created pools.
     * @param max cap on the number of "sleeping" instances in the pool.
     * @param init initial size of the pool (this specifies the size of the container, it does not cause the pool to be pre-populated.)
     * @see StackKeyedObjectPool#StackKeyedObjectPool(KeyedPoolableObjectFactory, int, int)
     */
    public StackKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int max, int init) {
        _factory = factory;
        _maxSleeping = max;
        _initCapacity = init;
    }

    public KeyedObjectPool createPool() {
        return new StackKeyedObjectPool(_factory,_maxSleeping,_initCapacity);
    }

    protected KeyedPoolableObjectFactory _factory = null;
    protected int _maxSleeping = StackKeyedObjectPool.DEFAULT_MAX_SLEEPING;
    protected int _initCapacity = StackKeyedObjectPool.DEFAULT_INIT_SLEEPING_CAPACITY;

}
