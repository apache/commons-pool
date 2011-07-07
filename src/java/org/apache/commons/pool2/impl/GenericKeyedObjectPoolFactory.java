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
 * A factory for creating {@link GenericKeyedObjectPool} instances from a
 * provided {@link GenericKeyedObjectPoolConfig configuration}.
 *
 * @param <K> The type of keys maintained by the built pool.
 * @param <T> Type of element pooled in the built pool.
 *
 * @since Pool 1.0
 */
public class GenericKeyedObjectPoolFactory<K,T>
        implements KeyedObjectPoolFactory<K,T> {

    private KeyedPoolableObjectFactory<K,T> factory;
    private GenericKeyedObjectPoolConfig<K, T> config;

    /**
     * Create a new GenericKeyedObjectPoolFactory.
     *
     * @param config    The configuration that will be used for all pools
     *                  created by this factory. Note that the configuration is
     *                  passed by value. Subsequent changes to config will not
     *                  be reflected in this factory or the pools it creates.
     */
    public GenericKeyedObjectPoolFactory(
            KeyedPoolableObjectFactory<K,T> factory,
            GenericKeyedObjectPoolConfig<K,T> config) {
        this.factory = factory;
        this.config = config.clone();
    }

    
    public KeyedPoolableObjectFactory<K,T> getFactory() {
        return factory;
    }
    
    public void setFactory(KeyedPoolableObjectFactory<K,T> factory) {
        this.factory = factory;
    }

    /**
     * Obtain the configuration currently used by the factory allowing the
     * current settings to be viewed and changed.
     *  
     * @return  The config object currently used by the factory
     */
    public GenericKeyedObjectPoolConfig<K, T> getConfig() {
        return config;
    }


    /**
     * Replace the configuration settings used by this factory.
     *  
     * @param config    The new settings which will be passed by value
     */
    public void setConfig(GenericKeyedObjectPoolConfig<K, T> config) {
        this.config = config.clone();
    }


    /**
     * Create a new GenericKeyedObjectPool with the currently configured
     * properties.
     * 
     * @return A pool configured with the current property settings
     */
    public KeyedObjectPool<K,T> createPool() {
        return new GenericKeyedObjectPool<K,T>(factory, config);
    }
}
