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

public interface GenericObjectPoolMBean {
    // Expose standard attributes via JMX
    boolean getBlockWhenExhausted();
    boolean getLifo();
    int getMaxIdle();
    int getMaxTotal();
    long getMaxWait();
    long getMinEvictableIdleTimeMillis();
    int getMinIdle();
    int getNumActive();
    int getNumIdle();
    int getNumTestsPerEvictionRun();
    boolean getTestOnBorrow();
    boolean getTestOnReturn();
    boolean getTestWhileIdle();
    long getTimeBetweenEvictionRunsMillis();
    boolean isClosed();
    // JMX specific attributes
    long getBorrowedCount();
    long getReturnedCount();
    long getCreatedCount();
    long getDestroyedCount();
    long getDestroyedByEvictorCount();
    long getDestroyedByBorrowValidationCount();
    long getMeanActiveTimeMillis();
    long getMeanIdleTimeMillis();
    long getMeanBorrowWaitTimeMillis();
    long getMaxBorrowWaitTimeMillis();
    String[] getSwallowedExceptions();
}
