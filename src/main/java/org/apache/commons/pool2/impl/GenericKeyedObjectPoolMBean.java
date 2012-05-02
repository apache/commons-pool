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

import java.util.List;
import java.util.Map;

/**
 * Defines the methods that will be made available via JMX.
 */
public interface GenericKeyedObjectPoolMBean<K> {
    // Expose getters for configuration settings
    /**
     * See {@link GenericKeyedObjectPool#getBlockWhenExhausted()}
     */
    boolean getBlockWhenExhausted();
    /**
     * See {@link GenericKeyedObjectPool#getLifo()}
     */
    boolean getLifo();
    /**
     * See {@link GenericKeyedObjectPool#getMaxIdlePerKey()}
     */
    int getMaxIdlePerKey();
    /**
     * See {@link GenericKeyedObjectPool#getMaxTotal()}
     */
    int getMaxTotal();
    /**
     * See {@link GenericKeyedObjectPool#getMaxTotalPerKey()}
     */
    int getMaxTotalPerKey();
    /**
     * See {@link GenericKeyedObjectPool#getMaxWaitMillis()}
     */
    long getMaxWaitMillis();
    /**
     * See {@link GenericKeyedObjectPool#getMinEvictableIdleTimeMillis()}
     */
    long getMinEvictableIdleTimeMillis();
    /**
     * See {@link GenericKeyedObjectPool#getMinIdlePerKey()}
     */
    int getMinIdlePerKey();
    /**
     * See {@link GenericKeyedObjectPool#getNumActive()}
     */
    int getNumActive();
    /**
     * See {@link GenericKeyedObjectPool#getNumIdle()}
     */
    int getNumIdle();
    /**
     * See {@link GenericKeyedObjectPool#getNumTestsPerEvictionRun()}
     */
    int getNumTestsPerEvictionRun();
    /**
     * See {@link GenericKeyedObjectPool#getTestOnBorrow()}
     */
    boolean getTestOnBorrow();
    /**
     * See {@link GenericKeyedObjectPool#getTestOnReturn()}
     */
    boolean getTestOnReturn();
    /**
     * See {@link GenericKeyedObjectPool#getTestWhileIdle()}
     */
    boolean getTestWhileIdle();
    /**
     * See {@link GenericKeyedObjectPool#getTimeBetweenEvictionRunsMillis()}
     */
    long getTimeBetweenEvictionRunsMillis();
    /**
     * See {@link GenericKeyedObjectPool#isClosed()}
     */
    boolean isClosed();
    // Expose getters for monitoring attributes
    /**
     * See {@link GenericKeyedObjectPool#getNumActivePerKey()}
     */
    Map<String,Integer> getNumActivePerKey();
    /**
     * See {@link GenericKeyedObjectPool#getBorrowedCount()}
     */
    long getBorrowedCount();
    /**
     * See {@link GenericKeyedObjectPool#getReturnedCount()}
     */
    long getReturnedCount();
    /**
     * See {@link GenericKeyedObjectPool#getCreatedCount()}
     */
    long getCreatedCount();
    /**
     * See {@link GenericKeyedObjectPool#getDestroyedCount()}
     */
    long getDestroyedCount();
    /**
     * See {@link GenericKeyedObjectPool#getDestroyedByEvictorCount()}
     */
    long getDestroyedByEvictorCount();
    /**
     * See {@link GenericKeyedObjectPool#getDestroyedByBorrowValidationCount()}
     */
    long getDestroyedByBorrowValidationCount();
    /**
     * See {@link GenericKeyedObjectPool#getMeanActiveTimeMillis()}
     */
    long getMeanActiveTimeMillis();
    /**
     * See {@link GenericKeyedObjectPool#getMeanIdleTimeMillis()}
     */
    long getMeanIdleTimeMillis();
    /**
     * See {@link GenericKeyedObjectPool#getMaxBorrowWaitTimeMillis()}
     */
    long getMeanBorrowWaitTimeMillis();
    /**
     * See {@link GenericKeyedObjectPool#getMaxBorrowWaitTimeMillis()}
     */
    long getMaxBorrowWaitTimeMillis();
    /**
     * See {@link GenericKeyedObjectPool#getSwallowedExceptions()}
     */
    String[] getSwallowedExceptions();
    /**
     * See {@link GenericKeyedObjectPool#getCreationStackTrace()}
     */
    String getCreationStackTrace();
    /**
     * See {@link GenericKeyedObjectPool#getNumWaiters()}
     */
    int getNumWaiters();
    /**
     * See {@link GenericKeyedObjectPool#getNumWaiters(Object)}
     */
    int getNumWaiters(K key);
    /**
     * See {@link GenericKeyedObjectPool#getKeys()}
     */
    List<K> getKeys();
}
