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

/**
 * Defines the methods that will be made available via JMX.
 *
 * @version $Revision: $
 *
 * @since 2.0
 */
public interface GenericObjectPoolMBean {
    // Getters for basic configuration settings
    /**
     * See {@link GenericObjectPool#getBlockWhenExhausted()}
     */
    boolean getBlockWhenExhausted();
    /**
     * See {@link GenericObjectPool#getLifo()}
     */
    boolean getLifo();
    /**
     * See {@link GenericObjectPool#getMaxIdle()}
     */
    int getMaxIdle();
    /**
     * See {@link GenericObjectPool#getMaxTotal()}
     */
    int getMaxTotal();
    /**
     * See {@link GenericObjectPool#getMaxWaitMillis()}
     */
    long getMaxWaitMillis();
    /**
     * See {@link GenericObjectPool#getMinEvictableIdleTimeMillis()}
     */
    long getMinEvictableIdleTimeMillis();
    /**
     * See {@link GenericObjectPool#getMinIdle()}
     */
    int getMinIdle();
    /**
     * See {@link GenericObjectPool#getNumActive()}
     */
    int getNumActive();
    /**
     * See {@link GenericObjectPool#getNumIdle()}
     */
    int getNumIdle();
    /**
     * See {@link GenericObjectPool#getNumTestsPerEvictionRun()}
     */
    int getNumTestsPerEvictionRun();
    /**
     * See {@link GenericObjectPool#getTestOnBorrow()}
     */
    boolean getTestOnBorrow();
    /**
     * See {@link GenericObjectPool#getTestOnReturn()}
     */
    boolean getTestOnReturn();
    /**
     * See {@link GenericObjectPool#getTestWhileIdle()}
     */
    boolean getTestWhileIdle();
    /**
     * See {@link GenericObjectPool#getTimeBetweenEvictionRunsMillis()}
     */
    long getTimeBetweenEvictionRunsMillis();
    /**
     * See {@link GenericObjectPool#isClosed()}
     */
    boolean isClosed();
    // Getters for monitoring attributes
    /**
     * See {@link GenericObjectPool#getBorrowedCount()}
     */
    long getBorrowedCount();
    /**
     * See {@link GenericObjectPool#getReturnedCount()}
     */
    long getReturnedCount();
    /**
     * See {@link GenericObjectPool#getCreatedCount()}
     */
    long getCreatedCount();
    /**
     * See {@link GenericObjectPool#getDestroyedCount()}
     */
    long getDestroyedCount();
    /**
     * See {@link GenericObjectPool#getDestroyedByEvictorCount()}
     */
    long getDestroyedByEvictorCount();
    /**
     * See {@link GenericObjectPool#getDestroyedByBorrowValidationCount()}
     */
    long getDestroyedByBorrowValidationCount();
    /**
     * See {@link GenericObjectPool#getMeanActiveTimeMillis()}
     */
    long getMeanActiveTimeMillis();
    /**
     * See {@link GenericObjectPool#getMeanIdleTimeMillis()}
     */
    long getMeanIdleTimeMillis();
    /**
     * See {@link GenericObjectPool#getMeanBorrowWaitTimeMillis()}
     */
    long getMeanBorrowWaitTimeMillis();
    /**
     * See {@link GenericObjectPool#getMaxBorrowWaitTimeMillis()}
     */
    long getMaxBorrowWaitTimeMillis();
    /**
     * See {@link GenericObjectPool#getSwallowedExceptions()}
     */
    String[] getSwallowedExceptions();
    /**
     * See {@link GenericObjectPool#getCreationStackTrace()}
     */
    String getCreationStackTrace();
    /**
     * See {@link GenericObjectPool#getNumWaiters()}
     */
    int getNumWaiters();
    
    // Getters for abandoned object removal configuration
    /**
     * See {@link GenericObjectPool#isAbandonedConfig()}
     */
    boolean isAbandonedConfig();  
    /**
     * See {@link GenericObjectPool#getLogAbandoned()}
     */
    boolean getLogAbandoned();
    /**
     * See {@link GenericObjectPool#getRemoveAbandonedOnBorrow()}
     */
    boolean getRemoveAbandonedOnBorrow();
    /**
     * See {@link GenericObjectPool#getRemoveAbandonedOnMaintenance()}
     */
    boolean getRemoveAbandonedOnMaintenance();
    /**
     * See {@link GenericObjectPool#getRemoveAbandonedTimeout()}
     */
    int getRemoveAbandonedTimeout();
}
