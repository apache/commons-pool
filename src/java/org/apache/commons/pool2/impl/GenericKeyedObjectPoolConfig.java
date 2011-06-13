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

import org.apache.commons.pool2.KeyedPoolableObjectFactory;

/**
 * A simple "struct" encapsulating the configuration for a
 * {@link GenericKeyedObjectPool}.
 * 
 * @since Pool 2.0
 */
public class GenericKeyedObjectPoolConfig<K,T> extends BaseObjectPoolConfig {

    public static final int DEFAULT_MAX_TOTAL_PER_KEY = 8;

    /**
     * The default maximum number of instances under management
     * (idle or checked out) across all keyed pools.
     */
    public static final int DEFAULT_MAX_TOTAL = -1;
    
    /**
     * The default minimum number of idle instances that the maintenance
     * thread (if enabled) will try to maintain per key.
     */
    public static final int DEFAULT_MIN_IDLE_PER_KEY = 0;
    
    /**
     * The default maximum number of idle instances per key.
     */
    public static final int DEFAULT_MAX_IDLE_PER_KEY = 8;
    
    
    private int minIdlePerKey = DEFAULT_MIN_IDLE_PER_KEY;
    
    private int maxIdlePerKey = DEFAULT_MAX_IDLE_PER_KEY;
    
    private int maxTotalPerKey = DEFAULT_MAX_TOTAL_PER_KEY;
    
    private int maxTotal = DEFAULT_MAX_TOTAL;
    
    private KeyedPoolableObjectFactory<K,T> factory = null;
    
    public GenericKeyedObjectPoolConfig() {
    }

    public KeyedPoolableObjectFactory<K,T> getFactory() {
        return factory;
    }

    public void setFactory(KeyedPoolableObjectFactory<K,T> factory) {
        this.factory = factory;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxTotalPerKey() {
        return maxTotalPerKey;
    }

    public void setMaxTotalPerKey(int maxTotalPerKey) {
        this.maxTotalPerKey = maxTotalPerKey;
    }
    
    public int getMinIdlePerKey() {
        return minIdlePerKey;
    }

    public void setMinIdlePerKey(int minIdlePerKey) {
        this.minIdlePerKey = minIdlePerKey;
    }

    public int getMaxIdlePerKey() {
        return maxIdlePerKey;
    }

    public void setMaxIdlePerKey(int maxIdlePerKey) {
        this.maxIdlePerKey = maxIdlePerKey;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GenericKeyedObjectPoolConfig<K, T> clone() {
        try {
            return (GenericKeyedObjectPoolConfig<K, T>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen
        }
    }
}
