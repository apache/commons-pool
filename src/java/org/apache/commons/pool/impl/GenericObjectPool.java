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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimerTask;

import org.apache.commons.pool.BaseObjectPool;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool.ObjectTimestampPair;

/**
 * A configurable {@link ObjectPool} implementation.
 * <p>
 * When coupled with the appropriate {@link PoolableObjectFactory},
 * <tt>GenericObjectPool</tt> provides robust pooling functionality for
 * arbitrary objects.
 * <p>
 * A <tt>GenericObjectPool</tt> provides a number of configurable parameters:
 * <ul>
 *  <li>
 *    {@link #setMaxActive <i>maxActive</i>} controls the maximum number of
 *    objects that can be borrowed from the pool at one time.  When
 *    non-positive, there is no limit to the number of objects that may be
 *    active at one time. When {@link #setMaxActive <i>maxActive</i>} is
 *    exceeded, the pool is said to be exhausted. The default setting for this
 *    parameter is 8.
 *  </li>
 *  <li>
 *    {@link #setMaxIdle <i>maxIdle</i>} controls the maximum number of objects
 *    that can sit idle in the pool at any time.  When negative, there is no
 *    limit to the number of objects that may be idle at one time. The default
 *    setting for this parameter is 8.
 *  </li>
 *  <li>
 *    {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>} specifies the
 *    behavior of the {@link #borrowObject} method when the pool is exhausted:
 *    <ul>
 *    <li>
 *      When {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>} is
 *      {@link #WHEN_EXHAUSTED_FAIL}, {@link #borrowObject} will throw
 *      a {@link NoSuchElementException}
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>} is
 *      {@link #WHEN_EXHAUSTED_GROW}, {@link #borrowObject} will create a new
 *      object and return it(essentially making {@link #setMaxActive <i>maxActive</i>}
 *      meaningless.)
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>}
 *      is {@link #WHEN_EXHAUSTED_BLOCK}, {@link #borrowObject} will block
 *      (invoke {@link Object#wait()} until a new or idle object is available.
 *      If a positive {@link #setMaxWait <i>maxWait</i>}
 *      value is supplied, the {@link #borrowObject} will block for at
 *      most that many milliseconds, after which a {@link NoSuchElementException}
 *      will be thrown.  If {@link #setMaxWait <i>maxWait</i>} is non-positive,
 *      the {@link #borrowObject} method will block indefinitely.
 *    </li>
 *    </ul>
 *    The default <code>whenExhaustedAction</code> setting is
 *    {@link #WHEN_EXHAUSTED_BLOCK} and the default <code>maxWait</code>
 *    setting is -1. By default, therefore, <code>borrowObject</code> will
 *    block indefinitely until an idle instance becomes available.
 *  </li>
 *  <li>
 *    When {@link #setTestOnBorrow <i>testOnBorrow</i>} is set, the pool will
 *    attempt to validate each object before it is returned from the
 *    {@link #borrowObject} method. (Using the provided factory's
 *    {@link PoolableObjectFactory#validateObject} method.)  Objects that fail
 *    to validate will be dropped from the pool, and a different object will
 *    be borrowed. The default setting for this parameter is
 *    <code>false.</code>
 *  </li>
 *  <li>
 *    When {@link #setTestOnReturn <i>testOnReturn</i>} is set, the pool will
 *    attempt to validate each object before it is returned to the pool in the
 *    {@link #returnObject} method. (Using the provided factory's
 *    {@link PoolableObjectFactory#validateObject}
 *    method.)  Objects that fail to validate will be dropped from the pool.
 *    The default setting for this parameter is <code>false.</code>
 *  </li>
 * </ul>
 * <p>
 * Optionally, one may configure the pool to examine and possibly evict objects
 * as they sit idle in the pool and to ensure that a minimum number of idle
 * objects are available. This is performed by an "idle object eviction"
 * thread, which runs asynchronously. Caution should be used when configuring
 * this optional feature. Eviction runs require an exclusive synchronization
 * lock on the pool, so if they run too frequently and / or incur excessive
 * latency when creating, destroying or validating object instances,
 * performance issues may result.  The idle object eviction thread may be
 * configured using the following attributes:
 * <ul>
 *  <li>
 *   {@link #setTimeBetweenEvictionRunsMillis <i>timeBetweenEvictionRunsMillis</i>}
 *   indicates how long the eviction thread should sleep before "runs" of examining
 *   idle objects.  When non-positive, no eviction thread will be launched. The
 *   default setting for this parameter is -1 (i.e., idle object eviction is
 *   disabled by default).
 *  </li>
 *  <li>
 *   {@link #setMinEvictableIdleTimeMillis <i>minEvictableIdleTimeMillis</i>}
 *   specifies the minimum amount of time that an object may sit idle in the pool
 *   before it is eligible for eviction due to idle time.  When non-positive, no object
 *   will be dropped from the pool due to idle time alone. This setting has no
 *   effect unless <code>timeBetweenEvictionRunsMillis > 0.</code> The default
 *   setting for this parameter is 30 minutes.
 *  </li>
 *  <li>
 *   {@link #setTestWhileIdle <i>testWhileIdle</i>} indicates whether or not idle
 *   objects should be validated using the factory's
 *   {@link PoolableObjectFactory#validateObject} method. Objects that fail to
 *   validate will be dropped from the pool. This setting has no effect unless 
 *   <code>timeBetweenEvictionRunsMillis > 0.</code>  The default setting for
 *   this parameter is <code>false.</code>
 *  </li>
 *  <li>
 *   {@link #setSoftMinEvictableIdleTimeMillis <i>softMinEvictableIdleTimeMillis</i>} 
 *   specifies the minimum amount of time an object may sit idle in the pool
 *   before it is eligible for eviction by the idle object evictor
 *   (if any), with the extra condition that at least "minIdle" amount of object 
 *   remain in the pool.  When non-positive, no objects will be evicted from the pool
 *   due to idle time alone. This setting has no effect unless
 *   <code>timeBetweenEvictionRunsMillis > 0.</code>  The default setting for
 *   this parameter is -1 (disabled).
 *  </li>
 *  <li>
 *   {@link #setNumTestsPerEvictionRun <i>numTestsPerEvictionRun</i>}
 *   determines the number of objects examined in each run of the idle object
 *   evictor. This setting has no effect unless 
 *   <code>timeBetweenEvictionRunsMillis > 0.</code>  The default setting for
 *   this parameter is 3.  
 *  </li>
 * </ul>
 * <p>
 * <p>
 * The pool can be configured to behave as a LIFO queue with respect to idle
 * objects - always returning the most recently used object from the pool,
 * or as a FIFO queue, where borrowObject always returns the oldest object
 * in the idle object pool.
 * <ul>
 *  <li>
 *   {@link #setLifo <i>lifo</i>}
 *   determines whether or not the pool returns idle objects in 
 *   last-in-first-out order. The default setting for this parameter is
 *   <code>true.</code>
 *  </li>
 * </ul>
 * <p>
 * GenericObjectPool is not usable without a {@link PoolableObjectFactory}.  A
 * non-<code>null</code> factory must be provided either as a constructor argument
 * or via a call to {@link #setFactory} before the pool is used.
 * <p>
 * Implementation note: To prevent possible deadlocks, care has been taken to
 * ensure that no call to a factory method will occur within a synchronization
 * block. See POOL-125 and DBCP-44 for more information.
 *
 * @see GenericKeyedObjectPool
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericObjectPool extends BaseObjectPool implements ObjectPool {

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
     * The default cap on the number of "sleeping" instances in the pool.
     * @see #getMaxIdle
     * @see #setMaxIdle
     */
    public static final int DEFAULT_MAX_IDLE  = 8;

    /**
     * The default minimum number of "sleeping" instances in the pool
     * before before the evictor thread (if active) spawns new objects.
     * @see #getMinIdle
     * @see #setMinIdle
     */
    public static final int DEFAULT_MIN_IDLE = 0;

    /**
     * The default cap on the total number of active instances from the pool.
     * @see #getMaxActive
     */
    public static final int DEFAULT_MAX_ACTIVE  = 8;

    /**
     * The default "when exhausted action" for the pool.
     * @see #WHEN_EXHAUSTED_BLOCK
     * @see #WHEN_EXHAUSTED_FAIL
     * @see #WHEN_EXHAUSTED_GROW
     * @see #setWhenExhaustedAction
     */
    public static final byte DEFAULT_WHEN_EXHAUSTED_ACTION = WHEN_EXHAUSTED_BLOCK;
    
    /**
     * The default LIFO status. True means that borrowObject returns the
     * most recently used ("last in") idle object in the pool (if there are
     * idle instances available).  False means that the pool behaves as a FIFO
     * queue - objects are taken from the idle object pool in the order that
     * they are returned to the pool.
     * @see #setLifo
     * @since 1.4
     */
    public static final boolean DEFAULT_LIFO = true;

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
     * The default value for {@link #getSoftMinEvictableIdleTimeMillis}.
     * @see #getSoftMinEvictableIdleTimeMillis
     * @see #setSoftMinEvictableIdleTimeMillis
     */
    public static final long DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS = -1;

    //--- constructors -----------------------------------------------

    /**
     * Create a new <tt>GenericObjectPool</tt>.
     */
    public GenericObjectPool() {
        this(null,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_MIN_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     */
    public GenericObjectPool(PoolableObjectFactory factory) {
        this(factory,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_MIN_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param config a non-<tt>null</tt> {@link GenericObjectPool.Config} describing my configuration
     */
    public GenericObjectPool(PoolableObjectFactory factory, GenericObjectPool.Config config) {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.minIdle,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle,config.softMinEvictableIdleTimeMillis, config.lifo);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_MIN_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #getWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,DEFAULT_MIN_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #getWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #getTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #getTestOnReturn})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #getWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #getMaxIdle})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,DEFAULT_MIN_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #getWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #getMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #getTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #getTestOnReturn})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, DEFAULT_MIN_IDLE, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param minIdle the minimum number of idle objects in my pool (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, minIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle, DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param minIdle the minimum number of idle objects in my pool (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     * @param softMinEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction with the extra condition that at least "minIdle" amount of object remain in the pool. (see {@link #setSoftMinEvictableIdleTimeMillis})
     * @since Pool 1.3
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle, long softMinEvictableIdleTimeMillis) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, minIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle, softMinEvictableIdleTimeMillis, DEFAULT_LIFO);
    }
    
    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param minIdle the minimum number of idle objects in my pool (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     * @param softMinEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for eviction with the extra condition that at least "minIdle" amount of object remain in the pool. (see {@link #setSoftMinEvictableIdleTimeMillis})
     * @param lifo whether or not objects are returned in last-in-first-out order from the idle object pool (see {@link #setLifo})
     * @since Pool 1.4
     */
    public GenericObjectPool(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle, long softMinEvictableIdleTimeMillis, boolean lifo) {
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
        _minIdle = minIdle;
        _testOnBorrow = testOnBorrow;
        _testOnReturn = testOnReturn;
        _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        _softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
        _testWhileIdle = testWhileIdle;

        _pool = new CursorableLinkedList();
        startEvictor(_timeBetweenEvictionRunsMillis);
    }

    //--- public methods ---------------------------------------------

    //--- configuration methods --------------------------------------

    /**
     * Returns the cap on the total number of active instances from the pool.
     * @return the cap on the total number of active instances from the pool.
     * @see #setMaxActive
     */
    public synchronized int getMaxActive() {
        return _maxActive;
    }

    /**
     * Sets the cap on the total number of active instances from the pool.
     * @param maxActive The cap on the total number of active instances from the pool.
     * Use a negative value for no limit.
     * @see #getMaxActive
     */
    public synchronized void setMaxActive(int maxActive) {
        _maxActive = maxActive;
        allocate();
    }

    /**
     * Returns the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @return one of {@link #WHEN_EXHAUSTED_BLOCK}, {@link #WHEN_EXHAUSTED_FAIL} or {@link #WHEN_EXHAUSTED_GROW}
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
                allocate();
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
     * @return maximum number of milliseconds to block when borrowing an object.
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
     * @param maxWait maximum number of milliseconds to block when borrowing an object.
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     * @see #WHEN_EXHAUSTED_BLOCK
     */
    public synchronized void setMaxWait(long maxWait) {
        _maxWait = maxWait;
        allocate();
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool.
     * @return the cap on the number of "idle" instances in the pool.
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
     * @param maxIdle The cap on the number of "idle" instances in the pool.
     * Use a negative value to indicate an unlimited number of idle instances.
     * @see #getMaxIdle
     */
    public synchronized void setMaxIdle(int maxIdle) {
        _maxIdle = maxIdle;
        allocate();
    }

    /**
     * Sets the minimum number of objects allowed in the pool
     * before the evictor thread (if active) spawns new objects.
     * Note that no objects are created when 
     * <code>numActive + numIdle >= maxActive.</code>
     * This setting has no effect if the idle object evictor is disabled
     * (i.e. if <code>timeBetweenEvictionRunsMillis <= 0</code>).
     *
     * @param minIdle The minimum number of objects.
     * @see #getMinIdle
     * @see #getTimeBetweenEvictionRunsMillis()
     */
    public synchronized void setMinIdle(int minIdle) {
        _minIdle = minIdle;
        allocate();
    }

    /**
     * Returns the minimum number of objects allowed in the pool
     * before the evictor thread (if active) spawns new objects.
     * (Note no objects are created when: numActive + numIdle >= maxActive)
     *
     * @return The minimum number of objects.
     * @see #setMinIdle
     */
    public synchronized int getMinIdle() {
        return _minIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
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
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @param testOnBorrow <code>true</code> if objects should be validated before being borrowed.
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        _testOnBorrow = testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @return <code>true</code> when objects will be validated after returned to {@link #returnObject}.
     * @see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return _testOnReturn;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @param testOnReturn <code>true</code> so objects will be validated after returned to {@link #returnObject}.
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
     * @return number of milliseconds to sleep between evictor runs.
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
     * @param timeBetweenEvictionRunsMillis number of milliseconds to sleep between evictor runs.
     * @see #getTimeBetweenEvictionRunsMillis
     */
    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        startEvictor(_timeBetweenEvictionRunsMillis);
    }

    /**
     * Returns the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     *
     * @return max number of objects to examine during each evictor run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized int getNumTestsPerEvictionRun() {
        return _numTestsPerEvictionRun;
    }

    /**
     * Sets the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  I.e., when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run.
     *
     * @param numTestsPerEvictionRun max number of objects to examine during each evictor run.
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
     * @param minEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any), with the extra condition that at least
     * "minIdle" amount of object remain in the pool.
     *
     * @return minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @since Pool 1.3
     * @see #setSoftMinEvictableIdleTimeMillis
     */
    public synchronized long getSoftMinEvictableIdleTimeMillis() {
        return _softMinEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any), with the extra condition that at least
     * "minIdle" amount of object remain in the pool.
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @param softMinEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     * @since Pool 1.3
     * @see #getSoftMinEvictableIdleTimeMillis
     */
    public synchronized void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        _softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @return <code>true</code> when objects will be validated by the evictor.
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized boolean getTestWhileIdle() {
        return _testWhileIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @param testWhileIdle <code>true</code> so objects will be validated by the evictor.
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        _testWhileIdle = testWhileIdle;
    }
    
    /**
     * Whether or not the idle object pool acts as a LIFO queue. True means
     * that borrowObject returns the most recently used ("last in") idle object
     * in the pool (if there are idle instances available).  False means that
     * the pool behaves as a FIFO queue - objects are taken from the idle object
     * pool in the order that they are returned to the pool.
     * 
     * @return <code>true</true> if the pool is configured to act as a LIFO queue
     * @since 1.4
     */
     public synchronized boolean getLifo() {
         return _lifo;
     }

     /**
      * Sets the LIFO property of the pool. True means that borrowObject returns
      * the most recently used ("last in") idle object in the pool (if there are
      * idle instances available).  False means that the pool behaves as a FIFO
      * queue - objects are taken from the idle object pool in the order that
      * they are returned to the pool.
      * 
      * @param lifo the new value for the LIFO property
      * @since 1.4
      */
     public synchronized void setLifo(boolean lifo) {
         this._lifo = lifo;
     }

    /**
     * Sets my configuration.
     *
     * @param conf configuration to use.
     * @see GenericObjectPool.Config
     */
    public synchronized void setConfig(GenericObjectPool.Config conf) {
        setMaxIdle(conf.maxIdle);
        setMinIdle(conf.minIdle);
        setMaxActive(conf.maxActive);
        setMaxWait(conf.maxWait);
        setWhenExhaustedAction(conf.whenExhaustedAction);
        setTestOnBorrow(conf.testOnBorrow);
        setTestOnReturn(conf.testOnReturn);
        setTestWhileIdle(conf.testWhileIdle);
        setNumTestsPerEvictionRun(conf.numTestsPerEvictionRun);
        setMinEvictableIdleTimeMillis(conf.minEvictableIdleTimeMillis);
        setTimeBetweenEvictionRunsMillis(conf.timeBetweenEvictionRunsMillis);
        setSoftMinEvictableIdleTimeMillis(conf.softMinEvictableIdleTimeMillis);
        setLifo(conf.lifo);
        allocate();
    }

    //-- ObjectPool methods ------------------------------------------

    public Object borrowObject() throws Exception {
        long starttime = System.currentTimeMillis();
        Latch latch = new Latch();
        synchronized (this) {
            _allocationQueue.add(latch);
            allocate();
        }

        for(;;) {
            synchronized (this) {
                assertOpen();
            }
                
            // If no object was allocated from the pool above
            if(latch._pair == null) {
                // check if we were allowed to create one
                if(latch._mayCreate) {
                    // allow new object to be created
                } else {
                    // the pool is exhausted
                    switch(_whenExhaustedAction) {
                        case WHEN_EXHAUSTED_GROW:
                            // allow new object to be created
                            break;
                        case WHEN_EXHAUSTED_FAIL:
                            synchronized (this) {
                                _allocationQueue.remove(latch);
                            }
                            throw new NoSuchElementException("Pool exhausted");
                        case WHEN_EXHAUSTED_BLOCK:
                            try {
                                synchronized (latch) {
                                    if(_maxWait <= 0) {
                                        latch.wait();
                                    } else {
                                        // this code may be executed again after a notify then continue cycle
                                        // so, need to calculate the amount of time to wait
                                        final long elapsed = (System.currentTimeMillis() - starttime);
                                        final long waitTime = _maxWait - elapsed;
                                        if (waitTime > 0)
                                        {
                                            latch.wait(waitTime);
                                        }
                                    }
                                }
                            } catch(InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw e; 
                            }
                            if(_maxWait > 0 && ((System.currentTimeMillis() - starttime) >= _maxWait)) {
                                throw new NoSuchElementException("Timeout waiting for idle object");
                            } else {
                                continue; // keep looping
                            }
                        default:
                            throw new IllegalArgumentException("WhenExhaustedAction property " + _whenExhaustedAction + " not recognized.");
                    }
                }
            }

            boolean newlyCreated = false;
            if(null == latch._pair) {
                try {
                    Object obj = _factory.makeObject();
                    latch._pair = new ObjectTimestampPair(obj);
                    newlyCreated = true;
                } finally {
                    if (!newlyCreated) {
                        // object cannot be created
                        synchronized (this) {
                            _numInternalProcessing--;
                            // No need to reset latch - about to throw exception
                            allocate();
                        }
                    }
                }
            }
            // activate & validate the object
            try {
                _factory.activateObject(latch._pair.value);
                if(_testOnBorrow && !_factory.validateObject(latch._pair.value)) {
                    throw new Exception("ValidateObject failed");
                }
                synchronized(this) {
                    _numInternalProcessing--;
                    _numActive++;
                }
                return latch._pair.value;
            }
            catch (Throwable e) {
                // object cannot be activated or is invalid
                try {
                    _factory.destroyObject(latch._pair.value);
                } catch (Throwable e2) {
                    // cannot destroy broken object
                }
                synchronized (this) {
                    _numInternalProcessing--;
                    latch.reset();
                    _allocationQueue.add(0, latch);
                    allocate();
                }
                if(newlyCreated) {
                    throw new NoSuchElementException("Could not create a validated object, cause: " + e.getMessage());
                }
                else {
                    continue; // keep looping
                }
            }
        }
    }

    private synchronized void allocate() {
        if (isClosed()) return;

        // First use any objects in the pool to clear the queue
        for (;;) {
            if (!_pool.isEmpty() && !_allocationQueue.isEmpty()) {
                Latch latch = (Latch) _allocationQueue.removeFirst();
                latch._pair = (ObjectTimestampPair) _pool.removeFirst();
                _numInternalProcessing++;
                synchronized (latch) {
                    latch.notify();
                }
            } else {
                break;
            }
        }

        // Second utilise any spare capacity to create new objects
        for(;;) {
            if((!_allocationQueue.isEmpty()) && (_maxActive < 0 || (_numActive + _numInternalProcessing) < _maxActive)) {
                Latch latch = (Latch) _allocationQueue.removeFirst();
                latch._mayCreate = true;
                _numInternalProcessing++;
                synchronized (latch) {
                    latch.notify();
                }
            } else {
                break;
            }
        }
    }

    public void invalidateObject(Object obj) throws Exception {
        try {
            if (_factory != null) {
                _factory.destroyObject(obj);
            }
        } finally {
            synchronized (this) {
                _numActive--;
                allocate();
            }
        }
    }

    /**
     * Clears any objects sitting idle in the pool.
     */
    public void clear() {
        List toDestroy = new ArrayList();
        
        synchronized(this) {
            toDestroy.addAll(_pool);
            _numInternalProcessing = _numInternalProcessing + _pool._size;
            _pool.clear();
        }
        destroy(toDestroy);
    }

    /*
     * Private method to destroy all the objects in a collection. Assumes
     * objects in the collection are instances of ObjectTimestampPair
     */
    private void destroy(Collection c) {
        for (Iterator it = c.iterator(); it.hasNext();) {
            try {
                _factory.destroyObject(((ObjectTimestampPair)(it.next())).value);
            } catch(Exception e) {
                // ignore error, keep destroying the rest
            } finally {
                synchronized(this) {
                    _numInternalProcessing--;
                    allocate();
                }
            }
        }
    }

    /**
     * Return the number of instances currently borrowed from this pool.
     *
     * @return the number of instances currently borrowed from this pool
     */
    public synchronized int getNumActive() {
        return _numActive;
    }

    /**
     * Return the number of instances currently idle in this pool.
     *
     * @return the number of instances currently idle in this pool
     */
    public synchronized int getNumIdle() {
        return _pool.size();
    }

    /**
     * {@inheritDoc}
     * <p><strong>Note: </strong> There is no guard to prevent an object
     * being returned to the pool multiple times. Clients are expected to
     * discard references to returned objects and ensure that an object is not
     * returned to the pool multiple times in sequence (i.e., without being
     * borrowed again between returns). Violating this contract will result in
     * the same object appearing multiple times in the pool and pool counters 
     * (numActive, numIdle) returning incorrect values.</p>
     */
    public void returnObject(Object obj) throws Exception {
        try {
            addObjectToPool(obj, true);
        } catch (Exception e) {
            if (_factory != null) {
                try {
                    _factory.destroyObject(obj);
                } catch (Exception e2) {
                    // swallowed
                }
                // TODO: Correctness here depends on control in addObjectToPool.
                // These two methods should be refactored, removing the 
                // "behavior flag",decrementNumActive, from addObjectToPool.
                synchronized(this) {
                    _numActive--;
                    allocate();
                }
            }
        }
    }

    private void addObjectToPool(Object obj, boolean decrementNumActive) throws Exception {
        boolean success = true;
        if(_testOnReturn && !(_factory.validateObject(obj))) {
            success = false;
        } else {
            _factory.passivateObject(obj);
        }

        boolean shouldDestroy = !success;

        // Add instance to pool if there is room and it has passed validation
        // (if testOnreturn is set)
        synchronized (this) {
            if (isClosed()) {
                shouldDestroy = true;
            } else {
                if((_maxIdle >= 0) && (_pool.size() >= _maxIdle)) {
                    shouldDestroy = true;
                } else if(success) {
                    // borrowObject always takes the first element from the queue,
                    // so for LIFO, push on top, FIFO add to end
                    if (_lifo) {
                        _pool.addFirst(new ObjectTimestampPair(obj));
                    } else {
                        _pool.addLast(new ObjectTimestampPair(obj));
                    }
                    if (decrementNumActive) {
                        _numActive--;
                    }
                    allocate();
                }
            }
        }

        // Destroy the instance if necessary 
        if(shouldDestroy) {
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
            // Decrement active count *after* destroy if applicable
            if (decrementNumActive) {
                synchronized(this) {
                    _numActive--;
                    allocate();
                }
            }
        }
        
    }

    public void close() throws Exception {
        super.close();
        synchronized (this) {
            clear();
            startEvictor(-1L);
        }
    }

    /**
     * Sets the {@link PoolableObjectFactory factory} this pool uses
     * to create new instances. Trying to change
     * the <code>factory</code> while there are borrowed objects will
     * throw an {@link IllegalStateException}.
     *
     * @param factory the {@link PoolableObjectFactory} used to create new instances.
     * @throws IllegalStateException when the factory cannot be set at this time
     */
    public void setFactory(PoolableObjectFactory factory) throws IllegalStateException {
        List toDestroy = new ArrayList();
        synchronized (this) {
            assertOpen();
            if(0 < getNumActive()) {
                throw new IllegalStateException("Objects are already active");
            } else {
                toDestroy.addAll(_pool);
                _numInternalProcessing = _numInternalProcessing + _pool._size;
                _pool.clear();
            }
            _factory = factory;
        }
        destroy(toDestroy);
    }

    /**
     * <p>Perform <code>numTests</code> idle object eviction tests, evicting
     * examined objects that meet the criteria for eviction. If 
     * <code>testWhileIdle</code> is true, examined objects are validated
     * when visited (and removed if invalid); otherwise only objects that
     * have been idle for more than <code>minEvicableIdletimeMillis</code>
     * are removed.</p>
     * 
     * <p>Successive activations of this method examine objects in 
     * in sequence, cycling through objects in oldest-to-youngest order.</p>
     *
     * @throws Exception if the pool is closed or eviction fails.
     */
    public void evict() throws Exception {
        assertOpen();
        synchronized (this) {
            if(_pool.isEmpty()) {
                return;
            }
            if (null == _evictionCursor) {
                _evictionCursor = (_pool.cursor(_lifo ? _pool.size() : 0));
            }  
        }

        for (int i=0,m=getNumTests();i<m;i++) {
            final ObjectTimestampPair pair;
            synchronized (this) {
                if ((_lifo && !_evictionCursor.hasPrevious()) || 
                        !_lifo && !_evictionCursor.hasNext()) {
                    _evictionCursor.close();
                    _evictionCursor = _pool.cursor(_lifo ? _pool.size() : 0);
                }
                
                pair = _lifo ? 
                        (ObjectTimestampPair) _evictionCursor.previous() : 
                        (ObjectTimestampPair) _evictionCursor.next();
                
                _evictionCursor.remove();
                _numInternalProcessing++;
            }
                        
            boolean removeObject = false;
            final long idleTimeMilis = System.currentTimeMillis() - pair.tstamp;
            if ((getMinEvictableIdleTimeMillis() > 0)
                    && (idleTimeMilis > getMinEvictableIdleTimeMillis())) {
                removeObject = true;
            } else if ((getSoftMinEvictableIdleTimeMillis() > 0)
                    && (idleTimeMilis > getSoftMinEvictableIdleTimeMillis())
                    && ((getNumIdle() + 1)> getMinIdle())) { // +1 accounts for object we are processing
                removeObject = true;
            }
            if(getTestWhileIdle() && !removeObject) {
                boolean active = false;
                try {
                    _factory.activateObject(pair.value);
                    active = true;
                } catch(Exception e) {
                    removeObject=true;
                }
                if(active) {
                    if(!_factory.validateObject(pair.value)) {
                        removeObject=true;
                    } else {
                        try {
                            _factory.passivateObject(pair.value);
                        } catch(Exception e) {
                            removeObject=true;
                        }
                    }
                }
            }
            
            if (removeObject) {
                try {
                    _factory.destroyObject(pair.value);
                } catch(Exception e) {
                    // ignored
                }
            }
            synchronized (this) {
                if(!removeObject) {
                    _evictionCursor.add(pair);
                    if (_lifo) {
                        // Skip over the element we just added back 
                        _evictionCursor.previous();
                    }
                }
                _numInternalProcessing--;
            }
        }
    }

    /**
     * Check to see if we are below our minimum number of objects
     * if so enough to bring us back to our minimum.
     *
     * @throws Exception when {@link #addObject()} fails.
     */
    private void ensureMinIdle() throws Exception {
        // this method isn't synchronized so the
        // calculateDeficit is done at the beginning
        // as a loop limit and a second time inside the loop
        // to stop when another thread already returned the
        // needed objects
        int objectDeficit = calculateDeficit(false);
        for ( int j = 0 ; j < objectDeficit && calculateDeficit(true) > 0 ; j++ ) {
            try {
                addObject();
            } finally {
                synchronized (this) {
                    _numInternalProcessing--;
                    allocate();
                }
            }
        }
    }

    private synchronized int calculateDeficit(boolean incrementInternal) {
        int objectDeficit = getMinIdle() - getNumIdle();
        if (_maxActive > 0) {
            int growLimit = Math.max(0,
                    getMaxActive() - getNumActive() - getNumIdle() - _numInternalProcessing);
            objectDeficit = Math.min(objectDeficit, growLimit);
        }
        if (incrementInternal && objectDeficit >0) {
            _numInternalProcessing++;
        }
        return objectDeficit;
    }

    /**
     * Create an object, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     */
    public void addObject() throws Exception {
        assertOpen();
        if (_factory == null) {
            throw new IllegalStateException("Cannot add objects without a factory.");
        }
        Object obj = _factory.makeObject();
        try {
            assertOpen();
            addObjectToPool(obj, false);
        } catch (IllegalStateException ex) { // Pool closed
            try {
                _factory.destroyObject(obj);
            } catch (Exception ex2) {
                // swallow
            }
            throw ex;
        }
    }

    //--- non-public methods ----------------------------------------

    /**
     * Start the eviction thread or service, or when
     * <i>delay</i> is non-positive, stop it
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
        buf.append("Idle Objects:\n");
        Iterator it = _pool.iterator();
        long time = System.currentTimeMillis();
        while(it.hasNext()) {
            ObjectTimestampPair pair = (ObjectTimestampPair)(it.next());
            buf.append("\t").append(pair.value).append("\t").append(time - pair.tstamp).append("\n");
        }
        return buf.toString();
    }

    private int getNumTests() {
        if(_numTestsPerEvictionRun >= 0) {
            return Math.min(_numTestsPerEvictionRun, _pool.size());
        } else {
            return(int)(Math.ceil(_pool.size()/Math.abs((double)_numTestsPerEvictionRun)));
        }
    }

    //--- inner classes ----------------------------------------------

    /**
     * The idle object evictor {@link TimerTask}.
     * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
     */
    private class Evictor extends TimerTask {
        public void run() {
            try {
                evict();
            } catch(Exception e) {
                // ignored
            } catch(OutOfMemoryError oome) {
                // Log problem but give evictor thread a chance to continue in
                // case error is recoverable
                oome.printStackTrace(System.err);
            }
            try {
                ensureMinIdle();
            } catch(Exception e) {
                // ignored
            }
        }
    }

    /**
     * A simple "struct" encapsulating the
     * configuration information for a {@link GenericObjectPool}.
     * @see GenericObjectPool#GenericObjectPool(org.apache.commons.pool.PoolableObjectFactory,org.apache.commons.pool.impl.GenericObjectPool.Config)
     * @see GenericObjectPool#setConfig
     */
    public static class Config {
        /**
         * @see GenericObjectPool#setMaxIdle
         */
        public int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
        /**
         * @see GenericObjectPool#setMinIdle
         */
        public int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
        /**
         * @see GenericObjectPool#setMaxActive
         */
        public int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
        /**
         * @see GenericObjectPool#setMaxWait
         */
        public long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
        /**
         * @see GenericObjectPool#setWhenExhaustedAction
         */
        public byte whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
        /**
         * @see GenericObjectPool#setTestOnBorrow
         */
        public boolean testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;
        /**
         * @see GenericObjectPool#setTestOnReturn
         */
        public boolean testOnReturn = GenericObjectPool.DEFAULT_TEST_ON_RETURN;
        /**
         * @see GenericObjectPool#setTestWhileIdle
         */
        public boolean testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;
        /**
         * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
         */
        public long timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        /**
         * @see GenericObjectPool#setNumTestsPerEvictionRun
         */
        public int numTestsPerEvictionRun =  GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
        /**
         * @see GenericObjectPool#setMinEvictableIdleTimeMillis
         */
        public long minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
        /**
         * @see GenericObjectPool#setSoftMinEvictableIdleTimeMillis
         */
        public long softMinEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
        /**
         * @see GenericObjectPool#setLifo
         */
        public boolean lifo = GenericObjectPool.DEFAULT_LIFO;
    
    }

    /**
     * Latch used to control allocation order of objects to threads to ensure
     * fairness. ie objects are allocated to threads in the order that threads
     * request objects.
     */
    private static final class Latch {
        ObjectTimestampPair _pair;
        boolean _mayCreate = false;
        
        /**
         * Reset the latch data. Used when an allocation fails and the latch
         * needs to be re-added to the queue. 
         */
        private void reset() {
            _pair = null;
            _mayCreate = false;
        }
    }


    //--- private attributes ---------------------------------------

    /**
     * The cap on the number of idle instances in the pool.
     * @see #setMaxIdle
     * @see #getMaxIdle
     */
    private int _maxIdle = DEFAULT_MAX_IDLE;

    /**
    * The cap on the minimum number of idle instances in the pool.
    * @see #setMinIdle
    * @see #getMinIdle
    */
    private int _minIdle = DEFAULT_MIN_IDLE;

    /**
     * The cap on the total number of active instances from the pool.
     * @see #setMaxActive
     * @see #getMaxActive
     */
    private int _maxActive = DEFAULT_MAX_ACTIVE;

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
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
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
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private volatile boolean _testOnReturn = DEFAULT_TEST_ON_RETURN;

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated}
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

    /**
     * The minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any), with the extra condition that at least
     * "minIdle" amount of object remain in the pool.
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @see #setSoftMinEvictableIdleTimeMillis
     * @see #getSoftMinEvictableIdleTimeMillis
     */
    private long _softMinEvictableIdleTimeMillis = DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /** Whether or not the pool behaves as a LIFO queue (last in first out) */
    private boolean _lifo = DEFAULT_LIFO;
    
    /** My pool. */
    private CursorableLinkedList _pool = null;
    
    /** Eviction cursor - keeps track of idle object evictor position */
    private CursorableLinkedList.Cursor _evictionCursor = null;

    /** My {@link PoolableObjectFactory}. */
    private PoolableObjectFactory _factory = null;

    /**
     * The number of objects {@link #borrowObject} borrowed
     * from the pool, but not yet returned.
     */
    private int _numActive = 0;

    /**
     * My idle object eviction {@link TimerTask}, if any.
     */
    private Evictor _evictor = null;

    /**
     * The number of objects subject to some form of internal processing
     * (usually creation or destruction) that should be included in the total
     * number of objects but are neither active nor idle.
     */
    private int _numInternalProcessing = 0;

    /**
     * Used to track the order in which threads call {@link #borrowObject()} so
     * that objects can be allocated in the order in which the threads requested
     * them.
     */
    private LinkedList _allocationQueue = new LinkedList();

}
