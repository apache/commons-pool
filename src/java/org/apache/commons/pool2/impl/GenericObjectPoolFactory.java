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
 * A factory for creating {@link GenericObjectPool} instances from a provided
 * {@link GenericObjectPoolConfig configuration}.
 * <p>
 * This class is intended to be thread-safe.
 * 
 * @param <T> Type of element pooled in the built pool.
 *
 * @since Pool 1.0
 */

public class GenericObjectPoolFactory<T> implements ObjectPoolFactory<T> {

    private final PoolableObjectFactory<T> factory;
    private final GenericObjectPoolConfig<T> config;


    /**
     * Create a new GenericObjectPoolFactory.
     * 
     * @param config    The configuration that will be used for all pools
     *                  created by this factory. Note that the configuration is
     *                  passed by value. Subsequent changes to config will not
     *                  be reflected in this factory or the pools it creates.
     */
    public GenericObjectPoolFactory(PoolableObjectFactory<T> factory,
            GenericObjectPoolConfig<T> config) {
        this.factory = factory;
        this.config = config.clone();
    }

    
    public PoolableObjectFactory<T> getFactory() {
        return factory;
    }
    
    /**
     * Obtain the configuration currently used by the factory allowing the
     * current settings to be viewed and changed.
     *  
     * @return  The config object currently used by the factory
     */
    public GenericObjectPoolConfig<T> getConfig() {
        return config;
    }


    /**
     * Create a new GenericKeyedObjectPool with the currently configured
     * properties.
     * 
     * @return A pool configured with the current property settings
     */
    public ObjectPool<T> createPool() {
        return new GenericObjectPool<T>(factory, config);
    }
}
