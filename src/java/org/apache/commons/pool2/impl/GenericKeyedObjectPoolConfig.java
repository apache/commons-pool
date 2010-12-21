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
 * Configuration class for {@link GenericKeyedObjectPool} and {@link GenericKeyedObjectPoolFactory} instances.
 *
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
public class GenericKeyedObjectPoolConfig {

    /**
     * The cap on the number of idle instances in the pool.
     */
    private final int maxIdlePerKey;

    /**
     * The cap on the minimum number of idle instances in the pool.
     */
    private final int minIdlePerKey;

    /**
     * The cap on the total number of active instances from the pool per key.
     */
    private final int maxTotalPerKey;

    /**
     * The cap on the total number of active instances from the pool.
     */
    private final int maxTotal;

    /**
     * The maximum amount of time (in millis) the
     * {@link org.apache.commons.pool2.ObjectPool#borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getWhenExhaustedAction "when exhausted" action} is
     * {@link WhenExhaustedAction#BLOCK}.
     *
     * When less than or equal to 0, the {@link org.apache.commons.pool2.ObjectPool#borrowObject} method
     * may block indefinitely.
     */
    private final long maxWait;

    /**
     * The action to take when the {@link org.apache.commons.pool2.ObjectPool#borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     */
    private final WhenExhaustedAction whenExhaustedAction;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link org.apache.commons.pool2.ObjectPool#borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     */
    private final boolean testOnBorrow;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     */
    private final boolean testOnReturn;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     */
    private final boolean testWhileIdle;

    /**
     * The number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     */
    private final long timeBetweenEvictionRunsMillis;

    /**
     * The max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  I.e., when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run.
     */
    private final int numTestsPerEvictionRun;

    /**
     * The minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     */
    private final long minEvictableIdleTimeMillis;

    /**
     * Whether or not the pool behaves as a LIFO queue (last in first out).
     */
    private final boolean lifo;

    /**
     * TODO please fill me
     *
     * @param maxIdlePerKey
     * @param minIdlePerKey
     * @param maxTotalPerKey
     * @param maxWait
     * @param whenExhaustedAction
     * @param testOnBorrow
     * @param testOnReturn
     * @param testWhileIdle
     * @param timeBetweenEvictionRunsMillis
     * @param numTestsPerEvictionRun
     * @param minEvictableIdleTimeMillis
     * @param lifo
     */
    private GenericKeyedObjectPoolConfig(final int maxIdlePerKey,
            final int minIdlePerKey,
            final int maxTotalPerKey,
            final int maxTotal,
            final long maxWait,
            final WhenExhaustedAction whenExhaustedAction,
            final boolean testOnBorrow,
            final boolean testOnReturn,
            final boolean testWhileIdle,
            final long timeBetweenEvictionRunsMillis,
            final int numTestsPerEvictionRun,
            final long minEvictableIdleTimeMillis,
            final boolean lifo) {
        this.maxIdlePerKey = maxIdlePerKey;
        this.minIdlePerKey = minIdlePerKey;
        this.maxTotalPerKey = maxTotalPerKey;
        this.maxTotal = maxTotal;
        this.maxWait = maxWait;
        this.whenExhaustedAction = whenExhaustedAction;
        this.testOnBorrow = testOnBorrow;
        this.testOnReturn = testOnReturn;
        this.testWhileIdle = testWhileIdle;
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        this.lifo = lifo;
    }

    public int getMaxIdlePerKey() {
        return maxIdlePerKey;
    }

    public int getMinIdlePerKey() {
        return minIdlePerKey;
    }

    public int getMaxTotalPerKey() {
        return maxTotalPerKey;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public WhenExhaustedAction getWhenExhaustedAction() {
        return whenExhaustedAction;
    }

    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public boolean getLifo() {
        return lifo;
    }

    public static final class Builder {

        /**
         * The default cap on the number of "sleeping" instances in the pool.
         */
        public static final int DEFAULT_MAX_IDLE_PER_KEY = 8;

        /**
         * The default minimum number of "sleeping" instances in the pool
         * before before the evictor thread (if active) spawns new objects.
         */
        public static final int DEFAULT_MIN_IDLE_PER_KEY = 0;

        /**
         * The default cap on the total number of active instances from the pool per key.
         */
        public static final int DEFAULT_MAX_TOTAL_PER_KEY = 8;

        /**
         * The default cap on the total number of active instances from the pool.
         */
        public static final int DEFAULT_MAX_TOTAL = -1;

        /**
         * The default "when exhausted action" for the pool.
         */
        public static final WhenExhaustedAction DEFAULT_WHEN_EXHAUSTED_ACTION = WhenExhaustedAction.BLOCK;

        /**
         * The default LIFO status. True means that borrowObject returns the
         * most recently used ("last in") idle object in the pool (if there are
         * idle instances available).  False means that the pool behaves as a FIFO
         * queue - objects are taken from the idle object pool in the order that
         * they are returned to the pool.
         */
        public static final boolean DEFAULT_LIFO = true;

        /**
         * The default maximum amount of time (in milliseconds) the
         * {@link #borrowObject} method should block before throwing
         * an exception when the pool is exhausted and the
         * {@link #getWhenExhaustedAction "when exhausted" action} is
         * {@link WhenExhaustedAction#BLOCK}.
         */
        public static final long DEFAULT_MAX_WAIT = -1L;

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
         * The default number of objects to examine per run in the
         * idle object evictor.
         */
        public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3;

        /**
         * The default value for {@link #getMinEvictableIdleTimeMillis}.
         */
        public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 30L;

        /**
         * The cap on the number of idle instances in the pool.
         */
        private int maxIdlePerKey = DEFAULT_MAX_IDLE_PER_KEY;

        /**
         * The cap on the minimum number of idle instances in the pool.
         *
         * @see #setMinIdle
         */
        private int minIdlePerKey = DEFAULT_MIN_IDLE_PER_KEY;

        /**
         * The cap on the total number of active instances from the pool per key.
         *
         * @see #setMaxTotalPerKey(int)
         */
        private int maxTotalPerKey = DEFAULT_MAX_TOTAL_PER_KEY;

        /**
         * The cap on the total number of active instances from the pool per key.
         *
         * @see #setMaxTotal(int)
         */
        private int maxTotal = DEFAULT_MAX_TOTAL;

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
         * @see #setMaxWait
         * @see WhenExhaustedAction#BLOCK
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
         * @see #setTestOnBorrow
         */
        private boolean testOnBorrow = DEFAULT_TEST_ON_BORROW;

        /**
         * When <tt>true</tt>, objects will be
         * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
         * before being returned to the pool within the
         * {@link #returnObject}.
         *
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
         * @see #setTimeBetweenEvictionRunsMillis
         */
        private long minEvictableIdleTimeMillis = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

        /**
         * Whether or not the pool behaves as a LIFO queue (last in first out)
         *
         * @see #setLifo
         */
        private boolean lifo = DEFAULT_LIFO;

        public Builder setMaxIdlePerKey(int maxIdlePerKey) {
            this.maxIdlePerKey = maxIdlePerKey;
            return this;
        }

        public Builder setMinIdlePerKey(int minIdlePerKey) {
            this.minIdlePerKey = minIdlePerKey;
            return this;
        }

        public Builder setMaxTotalPerKey(int maxTotalPerKey) {
            this.maxTotalPerKey = maxTotalPerKey;
            return this;
        }

        public Builder setMaxTotal(int maxTotal) {
            this.maxTotal = maxTotal;
            return this;
        }

        public Builder setMaxWait(long maxWait) {
            this.maxWait = maxWait;
            return this;
        }

        public Builder setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction) {
            this.whenExhaustedAction = whenExhaustedAction;
            return this;
        }

        public Builder setTestOnBorrow(boolean testOnBorrow) {
            this.testOnBorrow = testOnBorrow;
            return this;
        }

        public Builder setTestOnReturn(boolean testOnReturn) {
            this.testOnReturn = testOnReturn;
            return this;
        }

        public Builder setTestWhileIdle(boolean testWhileIdle) {
            this.testWhileIdle = testWhileIdle;
            return this;
        }

        public Builder setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
            this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
            return this;
        }

        public Builder setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
            this.numTestsPerEvictionRun = numTestsPerEvictionRun;
            return this;
        }

        public Builder setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
            this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
            return this;
        }

        public Builder setLifo(boolean lifo) {
            this.lifo = lifo;
            return this;
        }

        /**
         * Creates a {@link GenericObjectPoolConfig} instance.
         *
         * @return the created {@link GenericObjectPoolConfig} instance.
         */
        public GenericKeyedObjectPoolConfig createConfig() {
            return new GenericKeyedObjectPoolConfig(maxIdlePerKey,
                    minIdlePerKey,
                    maxTotalPerKey,
                    maxTotal,
                    maxWait,
                    whenExhaustedAction,
                    testOnBorrow,
                    testOnReturn,
                    testWhileIdle,
                    timeBetweenEvictionRunsMillis,
                    numTestsPerEvictionRun,
                    minEvictableIdleTimeMillis,
                    lifo);
        }

    }

}
