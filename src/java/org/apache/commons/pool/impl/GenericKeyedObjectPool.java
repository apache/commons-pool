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

package org.apache.commons.pool.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TimerTask;

import org.apache.commons.pool.BaseKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A configurable <code>KeyedObjectPool</code> implementation.
 * <p>
 * When coupled with the appropriate {@link KeyedPoolableObjectFactory},
 * <code>GenericKeyedObjectPool</code> provides robust pooling functionality for
 * keyed objects. A <code>GenericKeyedObjectPool</code> can be viewed as a map
 * of pools, keyed on the (unique) key values provided to the 
 * {@link #preparePool preparePool}, {@link #addObject addObject} or
 * {@link #borrowObject borrowObject} methods. Each time a new key value is
 * provided to one of these methods, a new pool is created under the given key
 * to be managed by the containing <code>GenericKeyedObjectPool.</code>
 * </p>
 * <p>A <code>GenericKeyedObjectPool</code> provides a number of configurable
 * parameters:</p>
 * <ul>
 *  <li>
 *    {@link #setMaxActive maxActive} controls the maximum number of objects
 *    (per key) that can be borrowed from the pool at one time.  When
 *    non-positive, there is no limit to the number of objects per key.
 *    When {@link #setMaxActive maxActive} is exceeded, the keyed pool is said
 *    to be exhausted.  The default setting for this parameter is 8.
 *  </li>
 *  <li>
 *    {@link #setMaxTotal maxTotal} sets a global limit on the number of objects
 *    that can be in circulation (active or idle) within the combined set of
 *    pools.  When non-positive, there is no limit to the total number of
 *    objects in circulation. When {@link #setMaxTotal maxTotal} is exceeded,
 *    all keyed pools are exhausted. When <code>maxTotal</code> is set to a
 *    positive value and {@link #borrowObject borrowObject} is invoked
 *    when at the limit with no idle instances available, an attempt is made to
 *    create room by clearing the oldest 15% of the elements from the keyed
 *    pools. The default setting for this parameter is -1 (no limit).
 *  </li>
 *  <li>
 *    {@link #setMaxIdle maxIdle} controls the maximum number of objects that can
 *    sit idle in the pool (per key) at any time.  When negative, there
 *    is no limit to the number of objects that may be idle per key. The
 *    default setting for this parameter is 8.
 *  </li>
 *  <li>
 *    {@link #setWhenExhaustedAction whenExhaustedAction} specifies the
 *    behavior of the {@link #borrowObject borrowObject} method when a keyed
 *    pool is exhausted:
 *    <ul>
 *    <li>
 *      When {@link #setWhenExhaustedAction whenExhaustedAction} is
 *      {@link #WHEN_EXHAUSTED_FAIL}, {@link #borrowObject borrowObject} will throw
 *      a {@link NoSuchElementException}
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction whenExhaustedAction} is
 *      {@link #WHEN_EXHAUSTED_GROW}, {@link #borrowObject borrowObject} will create a new
 *      object and return it (essentially making {@link #setMaxActive maxActive}
 *      meaningless.)
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction whenExhaustedAction}
 *      is {@link #WHEN_EXHAUSTED_BLOCK}, {@link #borrowObject borrowObject} will block
 *      (invoke {@link Object#wait() wait} until a new or idle object is available.
 *      If a positive {@link #setMaxWait maxWait}
 *      value is supplied, the {@link #borrowObject borrowObject} will block for at
 *      most that many milliseconds, after which a {@link NoSuchElementException}
 *      will be thrown.  If {@link #setMaxWait maxWait} is non-positive,
 *      the {@link #borrowObject borrowObject} method will block indefinitely.
 *    </li>
 *    </ul>
 *    The default <code>whenExhaustedAction</code> setting is
 *    {@link #WHEN_EXHAUSTED_BLOCK}.
 *  </li>
 *  <li>
 *    When {@link #setTestOnBorrow testOnBorrow} is set, the pool will
 *    attempt to validate each object before it is returned from the
 *    {@link #borrowObject borrowObject} method. (Using the provided factory's
 *    {@link KeyedPoolableObjectFactory#validateObject validateObject} method.)
 *    Objects that fail to validate will be dropped from the pool, and a
 *    different object will be borrowed. The default setting for this parameter
 *    is <code>false.</code>
 *  </li>
 *  <li>
 *    When {@link #setTestOnReturn testOnReturn} is set, the pool will
 *    attempt to validate each object before it is returned to the pool in the
 *    {@link #returnObject returnObject} method. (Using the provided factory's
 *    {@link KeyedPoolableObjectFactory#validateObject validateObject}
 *    method.)  Objects that fail to validate will be dropped from the pool.
 *    The default setting for this parameter is <code>false.</code>
 *  </li>
 * </ul>
 * <p>
 * Optionally, one may configure the pool to examine and possibly evict objects
 * as they sit idle in the pool and to ensure that a minimum number of idle
 * objects is maintained for each key. This is performed by an
 * "idle object eviction" thread, which runs asynchronously. Caution should be
 * used when configuring this optional feature. Eviction runs require an
 * exclusive synchronization lock on the pool, so if they run too frequently
 * and / or incur excessive latency when creating, destroying or validating
 * object instances, performance issues may result.  The idle object eviction
 * thread may be configured using the following attributes:
 * <ul>
 *  <li>
 *   {@link #setTimeBetweenEvictionRunsMillis timeBetweenEvictionRunsMillis}
 *   indicates how long the eviction thread should sleep before "runs" of examining
 *   idle objects.  When non-positive, no eviction thread will be launched. The
 *   default setting for this parameter is -1 (i.e., by default, idle object
 *   eviction is disabled).
 *  </li>
 *  <li>
 *   {@link #setMinEvictableIdleTimeMillis minEvictableIdleTimeMillis}
 *   specifies the minimum amount of time that an object may sit idle in the
 *   pool before it is eligible for eviction due to idle time.  When
 *   non-positive, no object will be dropped from the pool due to idle time
 *   alone.  This setting has no effect unless 
 *   <code>timeBetweenEvictionRunsMillis > 0.</code>  The default setting
 *   for this parameter is 30 minutes.
 *  </li>
 *  <li>
 *   {@link #setTestWhileIdle testWhileIdle} indicates whether or not idle
 *   objects should be validated using the factory's
 *   {@link KeyedPoolableObjectFactory#validateObject validateObject} method
 *   during idle object eviction runs.  Objects that fail to validate will be
 *   dropped from the pool. This setting has no effect unless 
 *   <code>timeBetweenEvictionRunsMillis > 0.</code>  The default setting
 *   for this parameter is <code>false.</code>
 *  </li>
 *  <li>
 *    {@link #setMinIdle minIdle} sets a target value for the minimum number of
 *    idle objects (per key) that should always be available. If this parameter
 *    is set to a positive number and 
 *    <code>timeBetweenEvictionRunsMillis > 0,</code> each time the idle object
 *    eviction thread runs, it will try to create enough idle instances so that
 *    there will be <code>minIdle</code> idle instances available under each
 *    key. This parameter is also used by {@link #preparePool preparePool}
 *    if <code>true</code> is provided as that method's
 *    <code>populateImmediately</code> parameter. The default setting for this
 *    parameter is 0.
 *  </li>
 * </ul>
 * <p>
 * The pools can be configured to behave as LIFO queues with respect to idle
 * objects - always returning the most recently used object from the pool,
 * or as FIFO queues, where borrowObject always returns the oldest object
 * in the idle object pool.
 * <ul>
 *  <li>
 *   {@link #setLifo <i>Lifo</i>}
 *   determines whether or not the pools return idle objects in 
 *   last-in-first-out order. The default setting for this parameter is 
 *   <code>true.</code>
 *  </li>
 * </ul>
 * <p>
 * GenericKeyedObjectPool is not usable without a {@link KeyedPoolableObjectFactory}.  A
 * non-<code>null</code> factory must be provided either as a constructor argument
 * or via a call to {@link #setFactory setFactory} before the pool is used.
 * </p>
 * @see GenericObjectPool
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericKeyedObjectPool extends BaseKeyedObjectPool implements KeyedObjectPool {

    //--- public constants -------------------------------------------

    /**
     * A "when exhausted action" type indicating that when the pool is
     * exhausted (i.e., the maximum number of active objects has
     * been reached), the {@link #borrowObject}
     * method should fail, throwing a {@link NoSuchElementException}.
     * @see #WHEN_EXHAUSTED_BLOCK
     * @see #WHEN_EXHAUSTED_GROW
     * @see #setWhenExhaustedAction
     */
    public static final byte WHEN_EXHAUSTED_FAIL   = 0;

    /**
     * A "when exhausted action" type indicating that when the pool
     * is exhausted (i.e., the maximum number
     * of active objects has been reached), the {@link #borrowObject}
     * method should block until a new object is available, or the
     * {@link #getMaxWait maximum wait time} has been reached.
     * @see #WHEN_EXHAUSTED_FAIL
     * @see #WHEN_EXHAUSTED_GROW
     * @see #setMaxWait
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     */
    public static final byte WHEN_EXHAUSTED_BLOCK  = 1;

    /**
     * A "when exhausted action" type indicating that when the pool is
     * exhausted (i.e., the maximum number
     * of active objects has been reached), the {@link #borrowObject}
     * method should simply create a new object anyway.
     * @see #WHEN_EXHAUSTED_FAIL
     * @see #WHEN_EXHAUSTED_GROW
     * @see #setWhenExhaustedAction
     */
    public static final byte WHEN_EXHAUSTED_GROW   = 2;

    /**
     * The default cap on the number of idle instances (per key) in the pool.
     * @see #getMaxIdle
     * @see #setMaxIdle
     */
    public static final int DEFAULT_MAX_IDLE  = 8;

    /**
     * The default cap on the total number of active instances (per key)
     * from the pool.
     * @see #getMaxActive
     * @see #setMaxActive
     */
    public static final int DEFAULT_MAX_ACTIVE  = 8;

    /**
     * The default cap on the the overall maximum number of objects that can
     * exist at one time.
     * @see #getMaxTotal
     * @see #setMaxTotal
     */
    public static final int DEFAULT_MAX_TOTAL  = -1;

    /**
     * The default "when exhausted action" for the pool.
     * @see #WHEN_EXHAUSTED_BLOCK
     * @see #WHEN_EXHAUSTED_FAIL
     * @see #WHEN_EXHAUSTED_GROW
     * @see #setWhenExhaustedAction
     */
    public static final byte DEFAULT_WHEN_EXHAUSTED_ACTION = WHEN_EXHAUSTED_BLOCK;

    /**
     * The default maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     * @see #getMaxWait
     * @see #setMaxWait
     */
    public static final long DEFAULT_MAX_WAIT = -1L;

    /**
     * The default "test on borrow" value.
     * @see #getTestOnBorrow
     * @see #setTestOnBorrow
     */
    public static final boolean DEFAULT_TEST_ON_BORROW = false;

    /**
     * The default "test on return" value.
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    /**
     * The default "test while idle" value.
     * @see #getTestWhileIdle
     * @see #setTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final boolean DEFAULT_TEST_WHILE_IDLE = false;

    /**
     * The default "time between eviction runs" value.
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1L;

    /**
     * The default number of objects to examine per run in the
     * idle object evictor.
     * @see #getNumTestsPerEvictionRun
     * @see #setNumTestsPerEvictionRun
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 3;

    /**
     * The default value for {@link #getMinEvictableIdleTimeMillis}.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setMinEvictableIdleTimeMillis
     */
    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000L * 60L * 30L;

    /**
     * The default minimum level of idle objects in the pool.
     * @since Pool 1.3
     * @see #setMinIdle
     * @see #getMinIdle
     */
    public static final int DEFAULT_MIN_IDLE = 0;
    
    /**
     * The default LIFO status. True means that borrowObject returns the
     * most recently used ("last in") idle object in a pool (if there are
     * idle instances available).  False means that pools behave as FIFO
     * queues - objects are taken from idle object pools in the order that
     * they are returned.
     * @see #setLifo
     */
    public static final boolean DEFAULT_LIFO = true;
    
    //--- constructors -----------------------------------------------

    /**
     * Create a new <code>GenericKeyedObjectPool</code> with no factory.
     *
     * @see #GenericKeyedObjectPool(KeyedPoolableObjectFactory)
     * @see #setFactory(KeyedPoolableObjectFactory)
     */
    public GenericKeyedObjectPool() {
        this(null,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory) {
        this(factory,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param config a non-<code>null</code> {@link GenericKeyedObjectPool.Config} describing the configuration
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, GenericKeyedObjectPool.Config config) {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.maxTotal, config.minIdle,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle,config.lifo);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param maxTotal the maximum number of objects that can exists at one time (see {@link #setMaxTotal})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, GenericKeyedObjectPool.DEFAULT_MIN_IDLE, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }
    
    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param maxTotal the maximum number of objects that can exists at one time (see {@link #setMaxTotal})
     * @param minIdle the minimum number of idle objects to have in the pool at any one time (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     * @since Pool 1.3
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, maxTotal, minIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle, DEFAULT_LIFO);
    }
    
    /**
     * Create a new <code>GenericKeyedObjectPool</code> using the specified values.
     * @param factory the <code>KeyedPoolableObjectFactory</code> to use to create, validate, and destroy objects if not <code>null</code>
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted and <code>whenExhaustedAction</code> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param maxTotal the maximum number of objects that can exists at one time (see {@link #setMaxTotal})
     * @param minIdle the minimum number of idle objects to have in the pool at any one time (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     * @param lifo whether or not the pools behave as LIFO (last in first out) queues (see {@link #setLifo}) 
     * @since Pool 1.4
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle, boolean lifo) {
        _factory = factory;
        _maxActive = maxActive;
        _lifo = lifo;
        switch(whenExhaustedAction) {
            case WHEN_EXHAUSTED_BLOCK:
            case WHEN_EXHAUSTED_FAIL:
            case WHEN_EXHAUSTED_GROW:
                _whenExhaustedAction = whenExhaustedAction;
                break;
            default:
                throw new IllegalArgumentException("whenExhaustedAction " + whenExhaustedAction + " not recognized.");
        }
        _maxWait = maxWait;
        _maxIdle = maxIdle;
        _maxTotal = maxTotal;
        _minIdle = minIdle;
        _testOnBorrow = testOnBorrow;
        _testOnReturn = testOnReturn;
        _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        _testWhileIdle = testWhileIdle;

        _poolMap = new HashMap();
        _poolList = new CursorableLinkedList();

        startEvictor(_timeBetweenEvictionRunsMillis);
    }

    //--- public methods ---------------------------------------------

    //--- configuration methods --------------------------------------

    /**
     * Returns the cap on the number of active instances per key.
     * A negative value indicates no limit.
     * @return the cap on the number of active instances per key.
     * @see #setMaxActive
     */
    public synchronized int getMaxActive() {
        return _maxActive;
    }

    /**
     * Sets the cap on the number of active instances per key.
     * @param maxActive The cap on the number of active instances per key.
     * Use a negative value for no limit.
     * @see #getMaxActive
     */
    public synchronized void setMaxActive(int maxActive) {
        _maxActive = maxActive;
        notifyAll();
    }

    /**
     * Returns the overall maximum number of objects (across pools) that can
     * exist at one time. A negative value indicates no limit.
     * @return the maximum number of instances in circulation at one time.
     * @see #setMaxTotal
     */
    public synchronized int getMaxTotal() {
        return _maxTotal;
    }

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
    public synchronized void setMaxTotal(int maxTotal) {
        _maxTotal = maxTotal;
        notifyAll();
    }

    /**
     * Returns the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @return one of {@link #WHEN_EXHAUSTED_BLOCK}, 
     * {@link #WHEN_EXHAUSTED_FAIL} or {@link #WHEN_EXHAUSTED_GROW}
     * @see #setWhenExhaustedAction
     */
    public synchronized byte getWhenExhaustedAction() {
        return _whenExhaustedAction;
    }

    /**
     * Sets the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @param whenExhaustedAction the action code, which must be one of
     *        {@link #WHEN_EXHAUSTED_BLOCK}, {@link #WHEN_EXHAUSTED_FAIL},
     *        or {@link #WHEN_EXHAUSTED_GROW}
     * @see #getWhenExhaustedAction
     */
    public synchronized void setWhenExhaustedAction(byte whenExhaustedAction) {
        switch(whenExhaustedAction) {
            case WHEN_EXHAUSTED_BLOCK:
            case WHEN_EXHAUSTED_FAIL:
            case WHEN_EXHAUSTED_GROW:
                _whenExhaustedAction = whenExhaustedAction;
                notifyAll();
                break;
            default:
                throw new IllegalArgumentException("whenExhaustedAction " + whenExhaustedAction + " not recognized.");
        }
    }


    /**
     * Returns the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     *
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @return the maximum number of milliseconds borrowObject will block.
     * @see #setMaxWait
     * @see #setWhenExhaustedAction
     * @see #WHEN_EXHAUSTED_BLOCK
     */
    public synchronized long getMaxWait() {
        return _maxWait;
    }

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #setWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     *
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @param maxWait the maximum number of milliseconds borrowObject will block or negative for indefinitely.
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     * @see #WHEN_EXHAUSTED_BLOCK
     */
    public synchronized void setMaxWait(long maxWait) {
        _maxWait = maxWait;
    }

    /**
     * Returns the cap on the number of "idle" instances per key.
     * @return the maximum number of "idle" instances that can be held
     * in a given keyed pool.
     * @see #setMaxIdle
     */
    public synchronized int getMaxIdle() {
        return _maxIdle;
    }

    /**
     * Sets the cap on the number of "idle" instances in the pool.
     * If maxIdle is set too low on heavily loaded systems it is possible you
     * will see objects being destroyed and almost immediately new objects
     * being created. This is a result of the active threads momentarily
     * returning objects faster than they are requesting them them, causing the
     * number of idle objects to rise above maxIdle. The best value for maxIdle
     * for heavily loaded system will vary but the default is a good starting
     * point.
     * @param maxIdle the maximum number of "idle" instances that can be held
     * in a given keyed pool. Use a negative value for no limit.
     * @see #getMaxIdle
     * @see #DEFAULT_MAX_IDLE
     */
    public synchronized void setMaxIdle(int maxIdle) {
        _maxIdle = maxIdle;
        notifyAll();
    }

    /**
     * Sets the minimum number of idle objects to maintain in each of the keyed
     * pools. This setting has no effect unless 
     * <code>timeBetweenEvictionRunsMillis > 0</code> and attempts to ensure
     * that each pool has the required minimum number of instances are only
     * made during idle object eviction runs.
     * @param poolSize - The minimum size of the each keyed pool
     * @since Pool 1.3
     * @see #getMinIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setMinIdle(int poolSize) {
        _minIdle = poolSize;
    }

    /**
     * Returns the minimum number of idle objects to maintain in each of the keyed
     * pools. This setting has no effect unless 
     * <code>timeBetweenEvictionRunsMillis > 0</code> and attempts to ensure
     * that each pool has the required minimum number of instances are only
     * made during idle object eviction runs.
     * @return minimum size of the each keyed pool
     * @since Pool 1.3
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized int getMinIdle() {
        return _minIdle;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @return <code>true</code> if objects are validated before being borrowed.
     * @see #setTestOnBorrow
     */
    public boolean getTestOnBorrow() {
        return _testOnBorrow;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @param testOnBorrow whether object should be validated before being returned by borrowObject.
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        _testOnBorrow = testOnBorrow;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @return <code>true</code> when objects will be validated before being returned.
     * @see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return _testOnReturn;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @param testOnReturn <code>true</code> so objects will be validated before being returned.
     * @see #getTestOnReturn
     */
    public void setTestOnReturn(boolean testOnReturn) {
        _testOnReturn = testOnReturn;
    }

    /**
     * Returns the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @return milliseconds to sleep between evictor runs.
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized long getTimeBetweenEvictionRunsMillis() {
        return _timeBetweenEvictionRunsMillis;
    }

    /**
     * Sets the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @param timeBetweenEvictionRunsMillis milliseconds to sleep between evictor runs.
     * @see #getTimeBetweenEvictionRunsMillis
     */
    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        startEvictor(_timeBetweenEvictionRunsMillis);
    }

    /**
     * Returns the number of objects to examine during each run of the
     * idle object evictor thread (if any).
     *
     * @return number of objects to examine each eviction run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized int getNumTestsPerEvictionRun() {
        return _numTestsPerEvictionRun;
    }

    /**
     * Sets the number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <code>ceil({@link #getNumIdle()})/abs({@link #getNumTestsPerEvictionRun})</code>
     * tests will be run.  I.e., when the value is <code>-n</code>, roughly one <code>n</code>th of the
     * idle objects will be tested per run.
     *
     * @param numTestsPerEvictionRun number of objects to examine each eviction run.
     * @see #getNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     *
     * @return minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized long getMinEvictableIdleTimeMillis() {
        return _minEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @param minEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @return <code>true</code> when objects are validated when borrowed.
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized boolean getTestWhileIdle() {
        return _testWhileIdle;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @param testWhileIdle <code>true</code> so objects are validated when borrowed.
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        _testWhileIdle = testWhileIdle;
    }

    /**
     * Sets the configuration.
     * @param conf the new configuration to use.
     * @see GenericKeyedObjectPool.Config
     */
    public synchronized void setConfig(GenericKeyedObjectPool.Config conf) {
        setMaxIdle(conf.maxIdle);
        setMaxActive(conf.maxActive);
        setMaxTotal(conf.maxTotal);
        setMinIdle(conf.minIdle);
        setMaxWait(conf.maxWait);
        setWhenExhaustedAction(conf.whenExhaustedAction);
        setTestOnBorrow(conf.testOnBorrow);
        setTestOnReturn(conf.testOnReturn);
        setTestWhileIdle(conf.testWhileIdle);
        setNumTestsPerEvictionRun(conf.numTestsPerEvictionRun);
        setMinEvictableIdleTimeMillis(conf.minEvictableIdleTimeMillis);
        setTimeBetweenEvictionRunsMillis(conf.timeBetweenEvictionRunsMillis);
    }
    
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
     public synchronized boolean getLifo() {
         return _lifo;
     }

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
     public synchronized void setLifo(boolean lifo) {
         this._lifo = lifo;
     }

    //-- ObjectPool methods ------------------------------------------

    public Object borrowObject(Object key) throws Exception {
        long starttime = System.currentTimeMillis();
        boolean newlyCreated = false;
        for(;;) {
            ObjectTimestampPair pair = null;
            ObjectQueue pool = null;
            synchronized (this) {
                assertOpen();
                pool = (ObjectQueue)(_poolMap.get(key));
                if(null == pool) {
                    pool = new ObjectQueue();
                    _poolMap.put(key,pool);
                    _poolList.add(key);
                }
                // if there are any sleeping, just grab one of those
                try {
                    pair = (ObjectTimestampPair)(pool.queue.removeFirst());
                    if(null != pair) {
                        _totalIdle--;
                    }
                } catch(NoSuchElementException e) { /* ignored */
                }
                // otherwise
                if(null == pair) {
                    // if there is a totalMaxActive and we are at the limit then
                    // we have to make room
                    if ((_maxTotal > 0) && (_totalActive + _totalIdle >= _maxTotal)) {
                        clearOldest();
                    }
    
                    // check if we can create one
                    // (note we know that the num sleeping is 0, else we wouldn't be here)
                    if ((_maxActive < 0 || pool.activeCount < _maxActive) &&
                        (_maxTotal < 0 || _totalActive + _totalIdle < _maxTotal)) {
                        Object obj = _factory.makeObject(key);
                        pair = new ObjectTimestampPair(obj);
                        newlyCreated = true;
                    } else {
                        // the pool is exhausted
                        switch(_whenExhaustedAction) {
                            case WHEN_EXHAUSTED_GROW:
                                Object obj = _factory.makeObject(key);
                                pair = new ObjectTimestampPair(obj);
                                break;
                            case WHEN_EXHAUSTED_FAIL:
                                throw new NoSuchElementException();
                            case WHEN_EXHAUSTED_BLOCK:
                                try {
                                    if(_maxWait <= 0) {
                                        wait();
                                    } else {
                                        // this code may be executed again after a notify then continue cycle
                                        // so, need to calculate the amount of time to wait
                                        final long elapsed = (System.currentTimeMillis() - starttime);
                                        final long waitTime = _maxWait - elapsed;
                                        if (waitTime > 0)
                                        {
                                            wait(waitTime);
                                        }
                                    }
                                } catch(InterruptedException e) {
                                    // ignored
                                }
                                if(_maxWait > 0 && ((System.currentTimeMillis() - starttime) >= _maxWait)) {
                                    throw new NoSuchElementException("Timeout waiting for idle object");
                                } else {
                                    continue; // keep looping
                                }
                            default:
                                throw new IllegalArgumentException("whenExhaustedAction " + _whenExhaustedAction + " not recognized.");
                        }
                    }
                }
                pool.incrementActiveCount();
            }
            
            // Activate.  If activate fails, decrement active count and destroy.
            // If instance failing activation is new, throw NoSuchElementException;
            // otherwise keep looping
            try {
                _factory.activateObject(key, pair.value);
            } catch (Exception e) {
                try {
                    _factory.destroyObject(key,pair.value);
                } catch (Exception e2) {
                    // swallowed
                } finally {
                    synchronized (this) {
                        pool.decrementActiveCount();
                    }
                }
                if(newlyCreated) {
                    throw new NoSuchElementException(
                       "Could not create a validated object, cause: "
                            + e.getMessage());
                }
                else {
                    continue; // keep looping
                }
            }

            // Validate.  If validation fails, decrement active count and
            // destroy. If instance failing validation is new, throw
            // NoSuchElementException; otherwise keep looping
            boolean invalid = true;
            try {
                invalid = _testOnBorrow && !_factory.validateObject(key, pair.value);
            } catch (Exception e) {
                // swallowed
            }
            if (invalid) {
                try {
                    _factory.destroyObject(key,pair.value);
                } catch (Exception e) {
                    // swallowed
                } finally {
                    synchronized (this) {
                        pool.decrementActiveCount();
                    }
                }
                if(newlyCreated) {
                    throw new NoSuchElementException("Could not create a validated object");
                } // else keep looping
            } else {
                return pair.value;
            }
        }
    }

    /**
     * Clears the pool, removing all pooled instances.
     */
    public synchronized void clear() {
        for(Iterator entries = _poolMap.entrySet().iterator(); entries.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)entries.next();
            final Object key = entry.getKey();
            final CursorableLinkedList list = ((ObjectQueue)(entry.getValue())).queue;
            for(Iterator it = list.iterator(); it.hasNext(); ) {
                try {
                    _factory.destroyObject(key,((ObjectTimestampPair)(it.next())).value);
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
                it.remove();
            }
        }
        _poolMap.clear();
        _poolList.clear();
        _totalIdle = 0;
        notifyAll();
    }

    /**
     * Method clears oldest 15% of objects in pool.  The method sorts the
     * objects into a TreeMap and then iterates the first 15% for removal
     * @since Pool 1.3
     */
    public synchronized void clearOldest() {
        // build sorted map of idle objects
        final Map map = new TreeMap();
        for (Iterator keyiter = _poolMap.keySet().iterator(); keyiter.hasNext();) {
            final Object key = keyiter.next();
            final CursorableLinkedList list = ((ObjectQueue)_poolMap.get(key)).queue;
            for (Iterator it = list.iterator(); it.hasNext();) {
                // each item into the map uses the objectimestamppair object
                // as the key.  It then gets sorted based on the timstamp field
                // each value in the map is the parent list it belongs in.
                map.put(it.next(), key);
            }
        }

        // Now iterate created map and kill the first 15% plus one to account for zero
        Set setPairKeys = map.entrySet();
        int itemsToRemove = ((int) (map.size() * 0.15)) + 1;

        Iterator iter = setPairKeys.iterator();
        while (iter.hasNext() && itemsToRemove > 0) {
            Map.Entry entry = (Map.Entry) iter.next();
            // kind of backwards on naming.  In the map, each key is the objecttimestamppair
            // because it has the ordering with the timestamp value.  Each value that the
            // key references is the key of the list it belongs to.
            Object key = entry.getValue();
            ObjectTimestampPair pairTimeStamp = (ObjectTimestampPair) entry.getKey();
            final CursorableLinkedList list = 
                ((ObjectQueue)(_poolMap.get(key))).queue;
            list.remove(pairTimeStamp);

            try {
                _factory.destroyObject(key, pairTimeStamp.value);
            } catch (Exception e) {
                // ignore error, keep destroying the rest
            }
            // if that was the last object for that key, drop that pool
            if (list.isEmpty()) {
                _poolMap.remove(key);
                _poolList.remove(key);
            }
            _totalIdle--;
            itemsToRemove--;
        }
        notifyAll();
    }

    /**
     * Clears the specified pool, removing all pooled instances corresponding to the given <code>key</code>.
     *
     * @param key the key to clear
     */
    public synchronized void clear(Object key) {
        final ObjectQueue pool = (ObjectQueue)(_poolMap.remove(key));
        if(null == pool) {
            return;
        } else {
            _poolList.remove(key);
            for(Iterator it = pool.queue.iterator(); it.hasNext(); ) {
                try {
                    _factory.destroyObject(key,((ObjectTimestampPair)(it.next())).value);
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
                it.remove();
                _totalIdle--;
            }
        }
        
        notifyAll();
    }

    /**
     * Returns the total number of instances current borrowed from this pool but not yet returned.
     *
     * @return the total number of instances currently borrowed from this pool
     */
    public synchronized int getNumActive() {
        return _totalActive;
    }

    /**
     * Returns the total number of instances currently idle in this pool.
     *
     * @return the total number of instances currently idle in this pool
     */
    public synchronized int getNumIdle() {
        return _totalIdle;
    }

    /**
     * Returns the number of instances currently borrowed from but not yet returned
     * to the pool corresponding to the given <code>key</code>.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently borrowed in this pool
     */
    public synchronized int getNumActive(Object key) {
        final ObjectQueue pool = (ObjectQueue)(_poolMap.get(key));
        return pool != null ? pool.activeCount : 0;
    }

    /**
     * Returns the number of instances corresponding to the given <code>key</code> currently idle in this pool.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently idle in this pool
     */
    public synchronized int getNumIdle(Object key) {
        final ObjectQueue pool = (ObjectQueue)(_poolMap.get(key));
        return pool != null ? pool.queue.size() : 0;
    }

    public void returnObject(Object key, Object obj) throws Exception {
        try {
            addObjectToPool(key, obj, true);
        } catch (Exception e) {
            if (_factory != null) {
                try {
                    _factory.destroyObject(key, obj);
                } catch (Exception e2) {
                    // swallowed
                }
                // TODO: Correctness here depends on control in addObjectToPool.
                // These two methods should be refactored, removing the 
                // "behavior flag",decrementNumActive, from addObjectToPool.
                ObjectQueue pool = (ObjectQueue) (_poolMap.get(key));
                if (pool != null) {
                    synchronized(this) {
                        pool.decrementActiveCount();
                        notifyAll();
                    }  
                }
            }
        }
    }

    private void addObjectToPool(Object key, Object obj,
            boolean decrementNumActive) throws Exception {

        // if we need to validate this object, do so
        boolean success = true; // whether or not this object passed validation
        if(_testOnReturn && !_factory.validateObject(key, obj)) {
            success = false;
        } else {
            _factory.passivateObject(key, obj);
        }

        boolean shouldDestroy = !success;
        ObjectQueue pool;
        
        // Add instance to pool if there is room and it has passed validation
        // (if testOnreturn is set)
        synchronized (this) {
            // grab the pool (list) of objects associated with the given key
            pool = (ObjectQueue) (_poolMap.get(key));
            // if it doesn't exist, create it
            if(null == pool) {
                pool = new ObjectQueue();
                _poolMap.put(key, pool);
                _poolList.add(key);
            }
            if (isClosed()) {
                shouldDestroy = true;
            } else {
                // if there's no space in the pool, flag the object for destruction
                // else if we passivated successfully, return it to the pool
                if(_maxIdle >= 0 && (pool.queue.size() >= _maxIdle)) {
                    shouldDestroy = true;
                } else if(success) {
                    // borrowObject always takes the first element from the queue,
                    // so for LIFO, push on top, FIFO add to end
                    if (_lifo) {
                        pool.queue.addFirst(new ObjectTimestampPair(obj)); 
                    } else {
                        pool.queue.addLast(new ObjectTimestampPair(obj));
                    }
                    _totalIdle++;
                }
            }
        }

        // Destroy the instance if necessary 
        if(shouldDestroy) {
            try {
                _factory.destroyObject(key, obj);
            } catch(Exception e) {
                // ignored?
            }
        }
        
        // Decrement active count *after* destroy if applicable
        if (decrementNumActive) {
            synchronized(this) {
                pool.decrementActiveCount();
                notifyAll();
            }
        }
    }

    public void invalidateObject(Object key, Object obj) throws Exception {
        try {
            _factory.destroyObject(key, obj);
        } finally {
            synchronized (this) {
                ObjectQueue pool = (ObjectQueue) (_poolMap.get(key));
                if(null == pool) {
                    pool = new ObjectQueue();
                    _poolMap.put(key, pool);
                    _poolList.add(key);
                }
                pool.decrementActiveCount();
                notifyAll(); // _totalActive has changed
            }
        }
    }

    /**
     * Create an object using the {@link KeyedPoolableObjectFactory#makeObject factory},
     * passivate it, and then place it in the idle object pool.
     * <code>addObject</code> is useful for "pre-loading" a pool with idle objects.
     *
     * @param key the key a new instance should be added to
     * @throws Exception when {@link KeyedPoolableObjectFactory#makeObject} fails.
     * @throws IllegalStateException when no {@link #setFactory factory} has been set or after {@link #close} has been called on this pool.
     */
    public void addObject(Object key) throws Exception {
        assertOpen();
        if (_factory == null) {
            throw new IllegalStateException("Cannot add objects without a factory.");
        }
        Object obj = _factory.makeObject(key);
        synchronized (this) {
            try {
                assertOpen();
                addObjectToPool(key, obj, false);
            } catch (IllegalStateException ex) { // Pool closed
                try {
                    _factory.destroyObject(key, obj);
                } catch (Exception ex2) {
                    // swallow
                }
                throw ex;
            }
        }
    }

    /**
     * Registers a key for pool control.
     *
     * If <code>populateImmediately</code> is <code>true</code> and
     * <code>minIdle > 0,</code> the pool under the given key will be
     * populated immediately with <code>minIdle</code> idle instances.
     *
     * @param key - The key to register for pool control.
     * @param populateImmediately - If this is <code>true</code>, the pool
     * will be populated immediately.
     * @since Pool 1.3
     */
    public synchronized void preparePool(Object key, boolean populateImmediately) {
        ObjectQueue pool = (ObjectQueue)(_poolMap.get(key));
        if (null == pool) {
            pool = new ObjectQueue();
            _poolMap.put(key,pool);
            _poolList.add(key);
        }

        if (populateImmediately) {
            try {
                // Create the pooled objects
                ensureMinIdle(key);
            }
            catch (Exception e) {
                //Do nothing
            }
        }
    }

    public void close() throws Exception {
        super.close();
        synchronized (this) {
            clear();
            if(null != _evictionCursor) {
                _evictionCursor.close();
                _evictionCursor = null;
            }
            if(null != _evictionKeyCursor) {
                _evictionKeyCursor.close();
                _evictionKeyCursor = null;
            }
            startEvictor(-1L);
        }
    }

    public synchronized void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException {
        assertOpen();
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    /**
     * <p>Perform <code>numTests</code> idle object eviction tests, evicting
     * examined objects that meet the criteria for eviction. If 
     * <code>testWhileIdle</code> is true, examined objects are validated
     * when visited (and removed if invalid); otherwise only objects that
     * have been idle for more than <code>minEvicableIdletimeMillis</code>
     * are removed.</p>
     * 
     * <p>Successive activations of this method examine objects in keyed pools
     * in sequence, cycling through the keys and examining objects in
     * oldest-to-youngest order within the keyed pools.</p>
     *
     * @throws Exception when there is a problem evicting idle objects.
     */
    public synchronized void evict() throws Exception {
        // Initialize key to last key value
        Object key = null;
        if (_evictionKeyCursor != null && 
                _evictionKeyCursor._lastReturned != null) {
            key = _evictionKeyCursor._lastReturned.value();
        }
        
        for (int i=0,m=getNumTests(); i<m; i++) {
            // make sure pool map is not empty; otherwise do nothing
            if (_poolMap == null || _poolMap.size() == 0) {
                continue;
            }

            // if we don't have a key cursor, then create one
            if (null == _evictionKeyCursor) {
                resetEvictionKeyCursor();
                key = null;
            }

            // if we don't have an object cursor, create one
            if (null == _evictionCursor) {
                // if the _evictionKeyCursor has a next value, use this key
                if (_evictionKeyCursor.hasNext()) {
                    key = _evictionKeyCursor.next();
                    resetEvictionObjectCursor(key);
                } else {
                    // Reset the key cursor and try again
                    resetEvictionKeyCursor();
                    if (_evictionKeyCursor != null) {
                        if (_evictionKeyCursor.hasNext()) {
                            key = _evictionKeyCursor.next();
                            resetEvictionObjectCursor(key);
                        }
                    }
                }
            }  

            if (_evictionCursor == null) {
                continue; // should never happen; do nothing
            }

            // If eviction cursor is exhausted, try to move
            // to the next key and reset
            if((_lifo && !_evictionCursor.hasPrevious()) ||
                    (!_lifo && !_evictionCursor.hasNext())) {
                if (_evictionKeyCursor != null) {
                    if (_evictionKeyCursor.hasNext()) {
                        key = _evictionKeyCursor.next();
                        resetEvictionObjectCursor(key);
                    } else { // Need to reset Key cursor
                        resetEvictionKeyCursor();
                        if (_evictionKeyCursor != null) {
                            if (_evictionKeyCursor.hasNext()) {
                                key = _evictionKeyCursor.next();
                                resetEvictionObjectCursor(key);
                            }
                        }
                    }
                }
            }

            if((_lifo && !_evictionCursor.hasPrevious()) ||
                    (!_lifo && !_evictionCursor.hasNext())) {
                continue; // reset failed, do nothing
            }

            // if LIFO and the _evictionCursor has a previous object, 
            // or FIFO and _evictionCursor has a next object, test it
            ObjectTimestampPair pair = _lifo ? 
                    (ObjectTimestampPair) _evictionCursor.previous() : 
                    (ObjectTimestampPair) _evictionCursor.next();
            boolean removeObject=false;
            if((_minEvictableIdleTimeMillis > 0) &&
               (System.currentTimeMillis() - pair.tstamp > 
               _minEvictableIdleTimeMillis)) {
                removeObject=true;
            }
            if(_testWhileIdle && removeObject == false) {
                boolean active = false;
                try {
                    _factory.activateObject(key,pair.value);
                    active = true;
                } catch(Exception e) {
                    removeObject=true;
                }
                if(active) {
                    if(!_factory.validateObject(key,pair.value)) {
                        removeObject=true;
                    } else {
                        try {
                            _factory.passivateObject(key,pair.value);
                        } catch(Exception e) {
                            removeObject=true;
                        }
                    }
                }
            }
            if(removeObject) {
                try {
                    _evictionCursor.remove();
                    _totalIdle--;
                    _factory.destroyObject(key, pair.value);
                    // Do not remove the key from the _poolList or _poolmap,
                    // even if the list stored in the _poolMap for this key is
                    // empty when minIdle > 0.
                    //
                    // Otherwise if it was the last object for that key,
                    // drop that pool
                    if (_minIdle == 0) {
                        ObjectQueue objectQueue = 
                            (ObjectQueue)_poolMap.get(key);
                        if (objectQueue != null && 
                                objectQueue.queue.isEmpty()) {
                            _poolMap.remove(key);
                            _poolList.remove(key);  
                        }
                    }
                } catch(Exception e) {
                    // ignored
                }
            }
        }
    }
    
    /**
     * Resets the eviction key cursor and closes any
     * associated eviction object cursor
     */
    private void resetEvictionKeyCursor() {
        if (_evictionKeyCursor != null) {
            _evictionKeyCursor.close();
        }
        _evictionKeyCursor = _poolList.cursor();
        if (null != _evictionCursor) {
            _evictionCursor.close();
            _evictionCursor = null;
        }  
    }
    
    /**
     * Resets the eviction object cursor for the given key
     * 
     * @param key eviction key
     */
    private void resetEvictionObjectCursor(Object key) {
        if (_evictionCursor != null) {
            _evictionCursor.close();
        }
        if (_poolMap == null) { 
            return;
        }
        ObjectQueue pool = (ObjectQueue) (_poolMap.get(key));
        if (pool != null) {
            CursorableLinkedList queue = pool.queue;
            _evictionCursor = queue.cursor(_lifo ? queue.size() : 0);   
        }
    }

    /**
     * Iterates through all the known keys and creates any necessary objects to maintain
     * the minimum level of pooled objects.
     * @see #getMinIdle
     * @see #setMinIdle
     * @throws Exception If there was an error whilst creating the pooled objects.
     */
    private synchronized void ensureMinIdle() throws Exception {
        Iterator iterator = _poolMap.keySet().iterator();

        //Check if should sustain the pool
        if (_minIdle > 0) {
            // Loop through all elements in _poolList
            // Find out the total number of max active and max idle for that class
            // If the number is less than the minIdle, do creation loop to boost numbers
            // Increment idle count + 1
            while (iterator.hasNext()) {
                //Get the next key to process
                Object key = iterator.next();
                ensureMinIdle(key);
            }
        }
    }

    /**
     * Re-creates any needed objects to maintain the minimum levels of
     * pooled objects for the specified key.
     *
     * This method uses {@link #calculateDefecit} to calculate the number
     * of objects to be created. {@link #calculateDefecit} can be overridden to
     * provide a different method of calculating the number of objects to be
     * created.
     * @param key The key to process
     * @throws Exception If there was an error whilst creating the pooled objects
     */
    private synchronized void ensureMinIdle(Object key) throws Exception {
        // Calculate current pool objects
        int numberToCreate = calculateDefecit(key);

        //Create required pool objects, if none to create, this loop will not be run.
        for (int i = 0; i < numberToCreate; i++) {
            addObject(key);
        }
    }

    //--- non-public methods ----------------------------------------

    /**
     * Start the eviction thread or service, or when
     * <code>delay</code> is non-positive, stop it
     * if it is already running.
     *
     * @param delay milliseconds between evictor runs.
     */
    protected synchronized void startEvictor(long delay) {
        if(null != _evictor) {
            EvictionTimer.cancel(_evictor);
            _evictor = null;
        }
        if(delay > 0) {
            _evictor = new Evictor();
            EvictionTimer.schedule(_evictor, delay, delay);
        }
    }

    synchronized String debugInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append("Active: ").append(getNumActive()).append("\n");
        buf.append("Idle: ").append(getNumIdle()).append("\n");
        Iterator it = _poolMap.keySet().iterator();
        while(it.hasNext()) {
            buf.append("\t").append(_poolMap.get(it.next())).append("\n");
        }
        return buf.toString();
    }

    private int getNumTests() {
        if(_numTestsPerEvictionRun >= 0) {
            return _numTestsPerEvictionRun;
        } else {
            return(int)(Math.ceil(_totalIdle/Math.abs((double)_numTestsPerEvictionRun)));
        }
    }

    /**
     * This returns the number of objects to create during the pool
     * sustain cycle. This will ensure that the minimum number of idle
     * connections is maintained without going past the maxPool value.
     * <p>
     * This method has been left public so derived classes can override
     * the way the defecit is calculated. ie... Increase/decrease the pool
     * size at certain times of day to accomodate for usage patterns.
     *
     * @param key - The key of the pool to calculate the number of
     *              objects to be re-created
     * @return The number of objects to be created
     */
    private int calculateDefecit(Object key) {
        int objectDefecit = 0;

        //Calculate no of objects needed to be created, in order to have
        //the number of pooled objects < maxActive();
        objectDefecit = getMinIdle() - getNumIdle(key);
        if (getMaxActive() > 0) {
            int growLimit = Math.max(0, getMaxActive() - getNumActive(key) - getNumIdle(key));
            objectDefecit = Math.min(objectDefecit, growLimit);
        }

        // Take the maxTotal limit into account
        if (getMaxTotal() > 0) {
            int growLimit = Math.max(0, getMaxTotal() - getNumActive() - getNumIdle());
            objectDefecit = Math.min(objectDefecit, growLimit);
        }

        return objectDefecit;
    }

    //--- inner classes ----------------------------------------------

    /**
     * A "struct" that keeps additional information about the actual queue of pooled objects.
     */
    private class ObjectQueue {
        private int activeCount = 0;
        private final CursorableLinkedList queue = new CursorableLinkedList();

        void incrementActiveCount() {
            _totalActive++;
            activeCount++;
        }

        void decrementActiveCount() {
            _totalActive--;
            if (activeCount > 0) {
                activeCount--;
            }
        }
    }
    
    /**
     * A simple "struct" encapsulating an object instance and a timestamp.
     *
     * Implements Comparable, objects are sorted from old to new.
     *
     * This is also used by {@link GenericObjectPool}.
     */
    static class ObjectTimestampPair implements Comparable {
        Object value;
        long tstamp;

        ObjectTimestampPair(Object val) {
            this(val, System.currentTimeMillis());
        }

        ObjectTimestampPair(Object val, long time) {
            value = val;
            tstamp = time;
        }

        public String toString() {
            return value + ";" + tstamp;
        }

        public int compareTo(Object obj) {
            return compareTo((ObjectTimestampPair) obj);
        }

        public int compareTo(ObjectTimestampPair other) {
            final long tstampdiff = this.tstamp - other.tstamp;
            if (tstampdiff == 0) {
                // make sure the natural ordering is consistent with equals
                // see java.lang.Comparable Javadocs
                return System.identityHashCode(this) - System.identityHashCode(other);
            } else {
                // handle int overflow
                return (int)Math.min(Math.max(tstampdiff, Integer.MIN_VALUE), Integer.MAX_VALUE);
            }
        }
    }

    /**
     * The idle object evictor {@link TimerTask}.
     * @see GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis
     */
    private class Evictor extends TimerTask {
        public void run() {
            //Evict from the pool
            try {
                evict();
            } catch(Exception e) {
                // ignored
            } catch(OutOfMemoryError oome) {
                // Log problem but give evictor thread a chance to continue in
                // case error is recoverable
                oome.printStackTrace(System.err);
            }
            //Re-create the connections.
            try {
                ensureMinIdle();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    /**
     * A simple "struct" encapsulating the
     * configuration information for a <code>GenericKeyedObjectPool</code>.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory,GenericKeyedObjectPool.Config)
     * @see GenericKeyedObjectPool#setConfig
     */
    public static class Config {
        /**
         * @see GenericKeyedObjectPool#setMaxIdle
         */
        public int maxIdle = GenericKeyedObjectPool.DEFAULT_MAX_IDLE;
        /**
         * @see GenericKeyedObjectPool#setMaxActive
         */
        public int maxActive = GenericKeyedObjectPool.DEFAULT_MAX_ACTIVE;
        /**
         * @see GenericKeyedObjectPool#setMaxTotal
         */
        public int maxTotal = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;
        /**
         * @see GenericKeyedObjectPool#setMinIdle
         */
        public int minIdle = GenericKeyedObjectPool.DEFAULT_MIN_IDLE;
        /**
         * @see GenericKeyedObjectPool#setMaxWait
         */
        public long maxWait = GenericKeyedObjectPool.DEFAULT_MAX_WAIT;
        /**
         * @see GenericKeyedObjectPool#setWhenExhaustedAction
         */
        public byte whenExhaustedAction = GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
        /**
         * @see GenericKeyedObjectPool#setTestOnBorrow
         */
        public boolean testOnBorrow = GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW;
        /**
         * @see GenericKeyedObjectPool#setTestOnReturn
         */
        public boolean testOnReturn = GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN;
        /**
         * @see GenericKeyedObjectPool#setTestWhileIdle
         */
        public boolean testWhileIdle = GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE;
        /**
         * @see GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis
         */
        public long timeBetweenEvictionRunsMillis = GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        /**
         * @see GenericKeyedObjectPool#setNumTestsPerEvictionRun
         */
        public int numTestsPerEvictionRun =  GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
        /**
         * @see GenericKeyedObjectPool#setMinEvictableIdleTimeMillis
         */
        public long minEvictableIdleTimeMillis = GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
        /**
         * @see GenericKeyedObjectPool#setLifo
         */
        public boolean lifo = GenericKeyedObjectPool.DEFAULT_LIFO;
    }

    //--- protected attributes ---------------------------------------

    /**
     * The cap on the number of idle instances in the pool.
     * @see #setMaxIdle
     * @see #getMaxIdle
     */
    private int _maxIdle = DEFAULT_MAX_IDLE;

    /**
     * The minimum no of idle objects to keep in the pool.
     * @see #setMinIdle
     * @see #getMinIdle
     */
    private int _minIdle = DEFAULT_MIN_IDLE;

    /**
     * The cap on the number of active instances from the pool.
     * @see #setMaxActive
     * @see #getMaxActive
     */
    private int _maxActive = DEFAULT_MAX_ACTIVE;

    /**
     * The cap on the total number of instances from the pool if non-positive.
     * @see #setMaxTotal
     * @see #getMaxTotal
     */
    private int _maxTotal = DEFAULT_MAX_TOTAL;
    
    /**
     * The maximum amount of time (in millis) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getWhenExhaustedAction "when exhausted" action} is
     * {@link #WHEN_EXHAUSTED_BLOCK}.
     *
     * When less than or equal to 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @see #setMaxWait
     * @see #getMaxWait
     * @see #WHEN_EXHAUSTED_BLOCK
     * @see #setWhenExhaustedAction
     * @see #getWhenExhaustedAction
     */
    private long _maxWait = DEFAULT_MAX_WAIT;

    /**
     * The action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @see #WHEN_EXHAUSTED_BLOCK
     * @see #WHEN_EXHAUSTED_FAIL
     * @see #WHEN_EXHAUSTED_GROW
     * @see #DEFAULT_WHEN_EXHAUSTED_ACTION
     * @see #setWhenExhaustedAction
     * @see #getWhenExhaustedAction
     */
    private byte _whenExhaustedAction = DEFAULT_WHEN_EXHAUSTED_ACTION;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #setTestOnBorrow
     * @see #getTestOnBorrow
     */
    private volatile boolean _testOnBorrow = DEFAULT_TEST_ON_BORROW;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private volatile boolean _testOnReturn = DEFAULT_TEST_ON_RETURN;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #getTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private boolean _testWhileIdle = DEFAULT_TEST_WHILE_IDLE;

    /**
     * The number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @see #setTimeBetweenEvictionRunsMillis
     * @see #getTimeBetweenEvictionRunsMillis
     */
    private long _timeBetweenEvictionRunsMillis = DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    /**
     * The number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <code>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</code>
     * tests will be run.  I.e., when the value is <code>-n</code>, roughly one <code>n</code>th of the
     * idle objects will be tested per run.
     *
     * @see #setNumTestsPerEvictionRun
     * @see #getNumTestsPerEvictionRun
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private int _numTestsPerEvictionRun =  DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

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
    private long _minEvictableIdleTimeMillis = DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /** My hash of pools (ObjectQueue). */
    private Map _poolMap = null;

    /** The total number of active instances. */
    private int _totalActive = 0;

    /** The total number of idle instances. */
    private int _totalIdle = 0;

    /** My {@link KeyedPoolableObjectFactory}. */
    private KeyedPoolableObjectFactory _factory = null;

    /**
     * My idle object eviction {@link TimerTask}, if any.
     */
    private Evictor _evictor = null;

    /**
     * A cursorable list of my pools.
     * @see GenericKeyedObjectPool.Evictor#run
     */
    private CursorableLinkedList _poolList = null;
    
    private CursorableLinkedList.Cursor _evictionCursor = null;
    private CursorableLinkedList.Cursor _evictionKeyCursor = null;
    
    /** Whether or not the pools behave as LIFO queues (last in first out) */
    private boolean _lifo = DEFAULT_LIFO;
}
