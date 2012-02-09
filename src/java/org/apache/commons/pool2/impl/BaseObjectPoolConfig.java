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
 * Provides the common configuration attributes used by object pool
 * configurations.
 * <p>
 * This class is not thread-safe; it is only intended to be used to provide
 * attributes used when creating a pool.
 */
public abstract class BaseObjectPoolConfig implements Cloneable {

    /**
     * The default LIFO status. True means that borrowObject returns the most
     * recently used ("last in") idle object in the pool (if there are idle
     * instances available). False means that the pool behaves as a FIFO queue -
     * objects are taken from the idle object pool in the order that they are
     * returned to the pool.
     */
    public static final boolean DEFAULT_LIFO = true;

    /**
     * The default maximum amount of time (in milliseconds) the
     * {@code borrowObject} method should block before throwing an exception
     * when the pool is exhausted and {@link #getBlockWhenExhausted} is true.
     */
    public static final long DEFAULT_MAX_WAIT = -1L;


    /**
     * The default value for {@link #getMinEvictableIdleTimeMillis}.
     */
    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS =
            1000L * 60L * 30L;

    /**
     * The default value for {@link #getSoftMinEvictableIdleTimeMillis}.
     */
    public static final long DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS = -1;

    /**
     * The default number of objects to examine per run in the idle object
     * evictor.
     */
    public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3;

    /**
     * The default "test on borrow" value.
     */
    public static final boolean DEFAULT_TEST_ON_BORROW = false;

    /**
     * The default "test on return" value.
     */
    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    /**
     * The default "test while idle" value.
     */
    public static final boolean DEFAULT_TEST_WHILE_IDLE = false;

    /**
     * The default "time between eviction runs" value.
     */
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1L;

    /**
     * The default "block when exhausted" value for the pool.
     */
    public static final boolean DEFAULT_BLOCK_WHEN_EXHAUSTED = true;

    public static final boolean DEFAULT_JMX_ENABLE = true;

    /**
     * The default prefix to use for the name component of the JMX object name
     * under which the pool will be registered.
     */
    public static final String DEFAULT_JMX_NAME_PREFIX = "pool";

    /**
     * The default policy that will be used to evict objects from the pool.
     */
    public static final String DEFAULT_EVICTION_POLICY_CLASS_NAME =
            DefaultEvictionPolicy.class.getName();

    private boolean lifo = DEFAULT_LIFO;

    private long maxWait = DEFAULT_MAX_WAIT;

    private long minEvictableIdleTimeMillis =
        DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    private long softMinEvictableIdleTimeMillis =
            DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    private int numTestsPerEvictionRun =
        DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    private String evictionPolicyClassName = DEFAULT_EVICTION_POLICY_CLASS_NAME;
    
    private boolean testOnBorrow = DEFAULT_TEST_ON_BORROW;

    private boolean testOnReturn = DEFAULT_TEST_ON_RETURN;

    private boolean testWhileIdle = DEFAULT_TEST_WHILE_IDLE;

    private long timeBetweenEvictionRunsMillis =
        DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    private boolean blockWhenExhausted = DEFAULT_BLOCK_WHEN_EXHAUSTED;

    private boolean jmxEnabled = DEFAULT_JMX_ENABLE;

    private String jmxNamePrefix = DEFAULT_JMX_NAME_PREFIX;

    public boolean getLifo() {
        return lifo;
    }

    public void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public long getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    public void setSoftMinEvictableIdleTimeMillis(
            long softMinEvictableIdleTimeMillis) {
        this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public String getEvictionPolicyClassName() {
        return evictionPolicyClassName;
    }

    public void setEvictionPolicyClassName(String evictionPolicyClassName) {
        this.evictionPolicyClassName = evictionPolicyClassName;
    }

    public boolean getBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    public void setBlockWhenExhausted(boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public String getJmxNamePrefix() {
        return jmxNamePrefix;
    }

    public void setJmxNamePrefix(String jmxNamePrefix) {
        this.jmxNamePrefix = jmxNamePrefix;
    }
}
