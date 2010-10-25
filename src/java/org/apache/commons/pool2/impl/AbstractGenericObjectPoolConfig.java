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
 * Abstract Configuration class.
 *
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
abstract class AbstractGenericObjectPoolConfig {

    /**
     * The default cap on the number of "sleeping" instances in the pool.
     *
     * @see #getMaxIdle
     * @see #setMaxIdle
     */
    public static final int DEFAULT_MAX_IDLE  = 8;

    /**
     * The default minimum number of "sleeping" instances in the pool
     * before before the evictor thread (if active) spawns new objects.
     *
     * @see #getMinIdle
     * @see #setMinIdle
     */
    public static final int DEFAULT_MIN_IDLE = 0;

    /**
     * The default cap on the total number of active instances from the pool.
     *
     * @see #getMaxActive
     * @see #setMaxActive
     */
    public static final int DEFAULT_MAX_ACTIVE  = 8;

    /**
     * The default "when exhausted action" for the pool.
     *
     * @see WhenExhaustedAction#BLOCK
     * @see WhenExhaustedAction#FAIL
     * @see WhenExhaustedAction#GROW
     * @see #setWhenExhaustedAction
     */
    public static final WhenExhaustedAction DEFAULT_WHEN_EXHAUSTED_ACTION = WhenExhaustedAction.BLOCK;

    /**
     * The default LIFO status. True means that borrowObject returns the
     * most recently used ("last in") idle object in the pool (if there are
     * idle instances available).  False means that the pool behaves as a FIFO
     * queue - objects are taken from the idle object pool in the order that
     * they are returned to the pool.
     *
     * @see #getLifo
     * @see #setLifo
     */
    public static final boolean DEFAULT_LIFO = true;

    /**
     * The default maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getWhenExhaustedAction "when exhausted" action} is
     * {@link WhenExhaustedAction#BLOCK}.
     *
     * @see #getMaxWait
     * @see #setMaxWait
     */
    public static final long DEFAULT_MAX_WAIT = -1L;

    /**
     * The default "test on borrow" value.
     *
     * @see #getTestOnBorrow
     * @see #setTestOnBorrow
     */
    public static final boolean DEFAULT_TEST_ON_BORROW = false;

    /**
     * The default "test on return" value.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    /**
     * The default "test while idle" value.
     *
     * @see #getTestWhileIdle
     * @see #setTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final boolean DEFAULT_TEST_WHILE_IDLE = false;

    /**
     * The default "time between eviction runs" value.
     *
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1L;

    /**
     * The default number of objects to examine per run in the
     * idle object evictor.
     *
     * @see #getNumTestsPerEvictionRun
     * @see #setNumTestsPerEvictionRun
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3;

    /**
     * The default value for {@link #getMinEvictableIdleTimeMillis}.
     *
     * @see #getMinEvictableIdleTimeMillis
     * @see #setMinEvictableIdleTimeMillis
     */
    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 30L;

    /**
     * The cap on the number of idle instances in the pool.
     *
     * @see #getMaxIdle
     * @see #setMaxIdle
     */
    private int maxIdle = DEFAULT_MAX_IDLE;

    /**
     * The cap on the minimum number of idle instances in the pool.
     *
     * @see #getMinIdle
     * @see #setMinIdle
     */
    private int minIdle = DEFAULT_MIN_IDLE;

    /**
     * The cap on the total number of active instances from the pool.
     *
     * @see #getMaxActive
     * @see #setMaxActive
     */
    private int maxActive = DEFAULT_MAX_ACTIVE;

    /**
     * The maximum amount of time (in millis) the
     * {@link org.apache.commons.pool2.ObjectPool#borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getWhenExhaustedAction "when exhausted" action} is
     * {@link WhenExhaustedAction#BLOCK}.
     *
     * When less than or equal to 0, the {@link org.apache.commons.pool2.ObjectPool#borrowObject} method
     * may block indefinitely.
     *
     * @see #getMaxWait
     * @see #setMaxWait
     * @see WhenExhaustedAction#BLOCK
     * @see #getWhenExhaustedAction
     * @see #setWhenExhaustedAction
     */
    private long maxWait = DEFAULT_MAX_WAIT;

    /**
     * The action to take when the {@link org.apache.commons.pool2.ObjectPool#borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @see WHEN_EXHAUSTED_ACTION#BLOCK
     * @see WHEN_EXHAUSTED_ACTION#FAIL
     * @see WHEN_EXHAUSTED_ACTION#GROW
     * @see DEFAULT_WHEN_EXHAUSTED_ACTION
     * @see #getWhenExhaustedAction
     * @see #setWhenExhaustedAction
     */
    private WhenExhaustedAction whenExhaustedAction = DEFAULT_WHEN_EXHAUSTED_ACTION;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link org.apache.commons.pool2.ObjectPool#borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #getTestOnBorrow
     * @see #setTestOnBorrow
     */
    private boolean testOnBorrow = DEFAULT_TEST_ON_BORROW;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private boolean testOnReturn = DEFAULT_TEST_ON_RETURN;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #getTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private boolean testWhileIdle = DEFAULT_TEST_WHILE_IDLE;

    /**
     * The number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @see #setTimeBetweenEvictionRunsMillis
     * @see #getTimeBetweenEvictionRunsMillis
     */
    private long timeBetweenEvictionRunsMillis = DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    /**
     * The max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  I.e., when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run.
     *
     * @see #setNumTestsPerEvictionRun
     * @see #getNumTestsPerEvictionRun
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private int numTestsPerEvictionRun =  DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    /**
     * The minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @see #setMinEvictableIdleTimeMillis
     * @see #getMinEvictableIdleTimeMillis
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private long minEvictableIdleTimeMillis = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /**
     * Whether or not the pool behaves as a LIFO queue (last in first out)
     *
     * @see #getLifo
     * @see #setLifo
     */
    private boolean lifo = DEFAULT_LIFO;

    public final int getMaxIdle() {
        return maxIdle;
    }

    public final void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public final int getMinIdle() {
        return minIdle;
    }

    public final void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public final int getMaxActive() {
        return maxActive;
    }

    public final void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public final long getMaxWait() {
        return maxWait;
    }

    public final void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public final WhenExhaustedAction getWhenExhaustedAction() {
        return whenExhaustedAction;
    }

    public final void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction) {
        this.whenExhaustedAction = whenExhaustedAction;
    }

    public final boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public final void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public final boolean getTestOnReturn() {
        return testOnReturn;
    }

    public final void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public final boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public final void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public final long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public final void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public final int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public final void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    public final long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public final void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public final boolean getLifo() {
        return lifo;
    }

    public final void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

}
