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
 * The <code>GenericKeyedObjectPool</code> JMX interface.
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface GenericKeyedObjectPoolMBean<K> {

    /**
     * Returns the cap on the number of object instances allocated by the pool
     * (checked out or idle),  per key.
     * A negative value indicates no limit.
     *
     * @return the cap on the number of active instances per key.
     * @see #setMaxTotalPerKey
     * @since 2.0
     */
    int getMaxTotalPerKey();

    /**
     * Sets the cap on the total number of instances from all pools combined.
     * When <code>maxTotal</code> is set to a
     * positive value and {@link #borrowObject borrowObject} is invoked
     * when at the limit with no idle instances available, an attempt is made to
     * create room by clearing the oldest 15% of the elements from the keyed
     * pools.
     *
     * @param maxTotal The cap on the total number of instances across pools.
     * Use a negative value for no limit.
     * @see #getMaxTotal
     */
    void setMaxTotalPerKey(int maxTotalPerKey);

    /**
     * Returns the overall maximum number of objects (across pools) that can
     * exist at one time. A negative value indicates no limit.
     * @return the maximum number of instances in circulation at one time.
     * @see #setMaxTotal
     */
    int getMaxTotal();

    /**
     * Sets the cap on the total number of instances from all pools combined.
     * When <code>maxTotal</code> is set to a
     * positive value and {@link #borrowObject borrowObject} is invoked
     * when at the limit with no idle instances available, an attempt is made to
     * create room by clearing the oldest 15% of the elements from the keyed
     * pools.
     *
     * @param maxTotal The cap on the total number of instances across pools.
     * Use a negative value for no limit.
     * @see #getMaxTotal
     */
    void setMaxTotal(int maxTotal);

    /**
     * Returns the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @return one of {@link WhenExhaustedAction#BLOCK},
     * {@link WhenExhaustedAction#FAIL} or {@link WhenExhaustedAction#GROW}
     * @see #setWhenExhaustedAction
     */
    WhenExhaustedAction getWhenExhaustedAction();

    /**
     * Sets the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @param whenExhaustedAction the action code
     * @see #getWhenExhaustedAction
     */
    void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction);

    /**
     * Returns the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link WhenExhaustedAction#BLOCK}.
     *
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @return the maximum number of milliseconds borrowObject will block.
     * @see #setMaxWait
     * @see #setWhenExhaustedAction
     * @see WhenExhaustedAction#BLOCK
     */
    long getMaxWait();

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link WhenExhaustedAction#BLOCK}.
     *
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @param maxWait the maximum number of milliseconds borrowObject will block or negative for indefinitely.
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     * @see WhenExhaustedAction#BLOCK
     */
    void setMaxWait(long maxWait);

    /**
     * Returns the cap on the number of "idle" instances per key.
     * @return the maximum number of "idle" instances that can be held
     * in a given keyed pool.
     * @see #setMaxIdle
     *
     * @since 2.0
     */
    int getMaxIdlePerKey();

    /**
     * Sets the cap on the number of "idle" instances in the pool.
     * If maxIdle is set too low on heavily loaded systems it is possible you
     * will see objects being destroyed and almost immediately new objects
     * being created. This is a result of the active threads momentarily
     * returning objects faster than they are requesting them them, causing the
     * number of idle objects to rise above maxIdle. The best value for maxIdle
     * for heavily loaded system will vary but the default is a good starting
     * point.
     * @param maxIdlePerKey the maximum number of "idle" instances that can be held
     * in a given keyed pool. Use a negative value for no limit.
     * @see #getMaxIdle
     * @see #DEFAULT_MAX_IDLE_PER_KEY
     *
     * @since 2.0
     */
    void setMaxIdlePerKey(int maxIdlePerKey);

    /**
     * Sets the minimum number of idle objects to maintain in each of the keyed
     * pools. This setting has no effect unless
     * <code>timeBetweenEvictionRunsMillis > 0</code> and attempts to ensure
     * that each pool has the required minimum number of instances are only
     * made during idle object eviction runs.
     * @param minIdlePerKey - The minimum size of the each keyed pool
     * @since Pool 1.3
     * @see #getMinIdle
     * @see #setTimeBetweenEvictionRunsMillis
     *
     * @since 2.0
     */
    void setMinIdle(int minIdlePerKey);

    /**
     * Returns the minimum number of idle objects to maintain in each of the keyed
     * pools. This setting has no effect unless
     * <code>timeBetweenEvictionRunsMillis > 0</code> and attempts to ensure
     * that each pool has the required minimum number of instances are only
     * made during idle object eviction runs.
     * @return minimum size of the each keyed pool
     * @since Pool 1.3
     * @see #setTimeBetweenEvictionRunsMillis
     *
     * @since 2.0
     */
    int getMinIdlePerKey();

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @return <code>true</code> if objects are validated before being borrowed.
     * @see #setTestOnBorrow
     */
    boolean getTestOnBorrow();

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @param testOnBorrow whether object should be validated before being returned by borrowObject.
     * @see #getTestOnBorrow
     */
    void setTestOnBorrow(boolean testOnBorrow);

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @return <code>true</code> when objects will be validated before being returned.
     * @see #setTestOnReturn
     */
    boolean getTestOnReturn();

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @param testOnReturn <code>true</code> so objects will be validated before being returned.
     * @see #getTestOnReturn
     */
    void setTestOnReturn(boolean testOnReturn);

    /**
     * Returns the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @return milliseconds to sleep between evictor runs.
     * @see #setTimeBetweenEvictionRunsMillis
     */
    long getTimeBetweenEvictionRunsMillis();

    /**
     * Sets the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @param timeBetweenEvictionRunsMillis milliseconds to sleep between evictor runs.
     * @see #getTimeBetweenEvictionRunsMillis
     */
    void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);

    /**
     * Returns the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     *
     * @return number of objects to examine each eviction run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    int getNumTestsPerEvictionRun();

    /**
     * Sets the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, 
     * <code>ceil({@link #getNumIdle()})/abs({@link #getNumTestsPerEvictionRun})</code>
     * tests will be run.  I.e., when the value is <code>-n</code>, roughly one <code>n</code>th of the
     * idle objects will be tested per run.  When the value is positive, the number of tests
     * actually performed in each run will be the minimum of this value and the number of instances
     * idle in the pools.
     *
     * @param numTestsPerEvictionRun number of objects to examine each eviction run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     *
     * @return minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    long getMinEvictableIdleTimeMillis();

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @param minEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis);

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @return <code>true</code> when objects are validated when borrowed.
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    boolean getTestWhileIdle();

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @param testWhileIdle <code>true</code> so objects are validated when borrowed.
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    void setTestWhileIdle(boolean testWhileIdle);

    /**
     * Whether or not the idle object pools act as LIFO queues. True means
     * that borrowObject returns the most recently used ("last in") idle object
     * in a pool (if there are idle instances available).  False means that
     * the pools behave as FIFO queues - objects are taken from idle object
     * pools in the order that they are returned.
     *
     * @return <code>true</code> if the pools are configured to act as LIFO queues
     * @since 1.4
     */
    boolean getLifo();

    /**
     * Sets the LIFO property of the pools. True means that borrowObject returns
     * the most recently used ("last in") idle object in a pool (if there are
     * idle instances available).  False means that the pools behave as FIFO
     * queues - objects are taken from idle object pools in the order that
     * they are returned.
     *
     * @param lifo the new value for the lifo property
     * @since 1.4
     */
    void setLifo(boolean lifo);

    /**
     * Returns the total number of instances current borrowed from this pool but not yet returned.
     *
     * @return the total number of instances currently borrowed from this pool
     */
    int getNumActive();

    /**
     * Returns the total number of instances currently idle in this pool.
     *
     * @return the total number of instances currently idle in this pool
     */
    int getNumIdle();

    /**
     * Returns the number of instances currently borrowed from but not yet returned
     * to the pool corresponding to the given <code>key</code>.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently borrowed in this pool
     */
    int getNumActive(K key);

    /**
     * Returns the number of instances corresponding to the given <code>key</code> currently idle in this pool.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently idle in this pool
     */
    int getNumIdle(K key);

}
