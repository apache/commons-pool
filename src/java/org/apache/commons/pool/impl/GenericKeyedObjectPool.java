/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.HashSet;

import org.apache.commons.pool.BaseKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A configurable {@link KeyedObjectPool} implementation.
 * <p>
 * When coupled with the appropriate {@link KeyedPoolableObjectFactory},
 * <tt>GenericKeyedObjectPool</tt> provides robust pooling functionality for
 * arbitrary objects.
 * <p>
 * A <tt>GenericKeyedObjectPool</tt> provides a number of configurable parameters:
 * <ul>
 *  <li>
 *    {@link #setMaxActive <i>maxActive</i>} controls the maximum number of objects (per key)
 *    that can be borrowed from the pool at one time.  When non-positive, there
 *    is no limit to the number of objects that may be active at one time.
 *    When {@link #setMaxActive <i>maxActive</i>} is exceeded, the pool is said to be exhausted.
 *  </li>
 *  <li>
 *    {@link #setMaxIdle <i>maxIdle</i>} controls the maximum number of objects that can
 *    sit idle in the pool (per key) at any time.  When negative, there
 *    is no limit to the number of objects that may be idle at one time.
 *  </li>
 *  <li>
 *    {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>} specifies the
 *    behaviour of the {@link #borrowObject} method when the pool is exhausted:
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
 *      (invoke {@link Object#wait} until a new or idle object is available.
 *      If a positive {@link #setMaxWait <i>maxWait</i>}
 *      value is supplied, the {@link #borrowObject} will block for at
 *      most that many milliseconds, after which a {@link NoSuchElementException}
 *      will be thrown.  If {@link #setMaxWait <i>maxWait</i>} is non-positive,
 *      the {@link #borrowObject} method will block indefinitely.
 *    </li>
 *    </ul>
 *  </li>
 *  <li>
 *    When {@link #setTestOnBorrow <i>testOnBorrow</i>} is set, the pool will
 *    attempt to validate each object before it is returned from the
 *    {@link #borrowObject} method. (Using the provided factory's
 *    {@link org.apache.commons.pool.PoolableObjectFactory#validateObject} method.)  Objects that fail
 *    to validate will be dropped from the pool, and a different object will
 *    be borrowed.
 *  </li>
 *  <li>
 *    When {@link #setTestOnReturn <i>testOnReturn</i>} is set, the pool will
 *    attempt to validate each object before it is returned to the pool in the
 *    {@link #returnObject} method. (Using the provided factory's
 *    {@link org.apache.commons.pool.PoolableObjectFactory#validateObject}
 *    method.)  Objects that fail to validate will be dropped from the pool.
 *  </li>
 * </ul>
 * <p>
 * Optionally, one may configure the pool to examine and possibly evict objects as they
 * sit idle in the pool.  This is performed by an "idle object eviction" thread, which
 * runs asychronously.  The idle object eviction thread may be configured using the
 * following attributes:
 * <ul>
 *  <li>
 *   {@link #setTimeBetweenEvictionRunsMillis <i>timeBetweenEvictionRunsMillis</i>}
 *   indicates how long the eviction thread should sleep before "runs" of examining
 *   idle objects.  When non-positive, no eviction thread will be launched.
 *  </li>
 *  <li>
 *   {@link #setMinEvictableIdleTimeMillis <i>minEvictableIdleTimeMillis</i>}
 *   specifies the minimum amount of time that an object may sit idle in the pool
 *   before it is eligable for eviction due to idle time.  When non-positive, no object
 *   will be dropped from the pool due to idle time alone.
 *  </li>
 *  <li>
 *   {@link #setTestWhileIdle <i>testWhileIdle</i>} indicates whether or not idle
 *   objects should be validated using the factory's
 *   {@link org.apache.commons.pool.PoolableObjectFactory#validateObject} method.  Objects
 *   that fail to validate will be dropped from the pool.
 *  </li>
 * </ul>
 * <p>
 * GenericKeyedObjectPool is not usable without a {@link KeyedPoolableObjectFactory}.  A
 * non-<code>null</code> factory must be provided either as a constructor argument
 * or via a call to {@link #setFactory} before the pool is used.
 * </p>
 * @see GenericObjectPool
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
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
     * The default cap on the number of idle instances in the pool
     * (per key).
     * @see #getMaxIdle
     * @see #setMaxIdle
     */
    public static final int DEFAULT_MAX_IDLE  = 8;

    /**
     * The default cap on the total number of active instances from the pool
     * (per key).
     * @see #getMaxActive
     * @see #setMaxActive
     */
    public static final int DEFAULT_MAX_ACTIVE  = 8;

    /**
     * The default cap on the the maximum number of objects that can exists at one time.
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
     * The default maximum amount of time (in millis) the
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
     * @see #setMinIdle
     * @see #getMinIdle
     */
    public static final int DEFAULT_MIN_IDLE = 0;
    
    //--- constructors -----------------------------------------------

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt>..
     */
    public GenericKeyedObjectPool() {
        this(null,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory) {
        this(factory,DEFAULT_MAX_ACTIVE,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param config a non-<tt>null</tt> {@link GenericKeyedObjectPool.Config} describing my configuration
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, GenericKeyedObjectPool.Config config) {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.maxTotal,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,DEFAULT_WHEN_EXHAUSTED_ACTION,DEFAULT_MAX_WAIT,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,DEFAULT_MAX_IDLE,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (per key) (see {@link #setMaxIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,DEFAULT_TEST_ON_BORROW,DEFAULT_TEST_ON_RETURN,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)KeyedPoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #getMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,testOnBorrow,testOnReturn,DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,DEFAULT_NUM_TESTS_PER_EVICTION_RUN,DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligable for evcition (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param maxTotal the maximum number of objects that can exists at one time (see {@link #setMaxTotal})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligable for evcition (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, GenericKeyedObjectPool.DEFAULT_MIN_IDLE, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }
    
    /**
     * Create a new <tt>GenericKeyedObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param maxActive the maximum number of objects that can be borrowed from me at one time (per key) (see {@link #setMaxActive})
     * @param whenExhaustedAction the action to take when the pool is exhausted (see {@link #setWhenExhaustedAction})
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted an and <i>whenExhaustedAction</i> is {@link #WHEN_EXHAUSTED_BLOCK} (otherwise ignored) (see {@link #setMaxWait})
     * @param maxIdle the maximum number of idle objects in my pool (see {@link #setMaxIdle})
     * @param maxTotal the maximum number of objects that can exists at one time (see {@link #setMaxTotal})
     * @param minIdle the minimum number of idle objects to have in the pool at any one time (see {@link #setMinIdle})
     * @param testOnBorrow whether or not to validate objects before they are returned by the {@link #borrowObject} method (see {@link #setTestOnBorrow})
     * @param testOnReturn whether or not to validate objects after they are returned to the {@link #returnObject} method (see {@link #setTestOnReturn})
     * @param timeBetweenEvictionRunsMillis the amount of time (in milliseconds) to sleep between examining idle objects for eviction (see {@link #setTimeBetweenEvictionRunsMillis})
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread (if any) (see {@link #setNumTestsPerEvictionRun})
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligable for evcition (see {@link #setMinEvictableIdleTimeMillis})
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread, if any (see {@link #setTestWhileIdle})
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        _factory = factory;
        _maxActive = maxActive;
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
        _activeMap = new HashMap();

        startEvictor(_timeBetweenEvictionRunsMillis);
    }

    //--- public methods ---------------------------------------------

    //--- configuration methods --------------------------------------

    /**
     * Returns the cap on the number of active instances from my pool (per key).
     * @return the cap on the number of active instances from my pool (per key).
     * @see #setMaxActive
     */
    public synchronized int getMaxActive() {
        return _maxActive;
    }

    /**
     * Sets the cap on the number of active instances from my pool (per key).
     * @param maxActive The cap on the number of active instances from my pool (per key).
     *                  Use a negative value for an infinite number of instances.
     * @see #getMaxActive
     */
    public synchronized void setMaxActive(int maxActive) {
        _maxActive = maxActive;
        notifyAll();
    }

    /**
     * Returns the cap on the total number of instances from my pool.
     * @return the cap on the total number of instances from my pool.
     * @see #setMaxTotal
     */
    public synchronized int getMaxTotal() {
        return _maxTotal;
    }

    /**
     * Sets the cap on the total number of instances from my pool.
     * @param maxTotal The cap on the total number of instances from my pool.
     *                  Use a negative value for an infinite number of instances.
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
     * When less than 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
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
     * When less than 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     * @see #WHEN_EXHAUSTED_BLOCK
     */
    public synchronized void setMaxWait(long maxWait) {
        _maxWait = maxWait;
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
     * @param maxIdle The cap on the number of "idle" instances in the pool.
     *                Use a negative value to indicate an unlimited number
     *                of idle instances.
     * @see #getMaxIdle
     */
    public synchronized void setMaxIdle(int maxIdle) {
        _maxIdle = maxIdle;
        notifyAll();
    }

    /**
     * Sets the minimum number of idle objects in pool to maintain (per key)
     * @param poolSize - The minimum size of the pool
     * @see #getMinIdle
     */
    public synchronized void setMinIdle(int poolSize) {
        _minIdle = poolSize;
    }
    
    /**
     * Returns the minimum number of idle objects in pool to maintain (per key)
     * @return the minimum number of idle objects in pool to maintain (per key)
     * @see #setMinIdle
     */
    public synchronized int getMinIdle() {
        return _minIdle;
    }
    
    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #setTestOnBorrow
     */
    public synchronized boolean getTestOnBorrow() {
        return _testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #getTestOnBorrow
     */
    public synchronized void setTestOnBorrow(boolean testOnBorrow) {
        _testOnBorrow = testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #setTestOnReturn
     */
    public synchronized boolean getTestOnReturn() {
        return _testOnReturn;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     */
    public synchronized void setTestOnReturn(boolean testOnReturn) {
        _testOnReturn = testOnReturn;
    }

    /**
     * Returns the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
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
     * When a negative value is supplied, <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  I.e., when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run.
     *
     * @see #getNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligable for eviction by the idle object evictor
     * (if any).
     *
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized long getMinEvictableIdleTimeMillis() {
        return _minEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligable for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized boolean getTestWhileIdle() {
        return _testWhileIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        _testWhileIdle = testWhileIdle;
    }

    /**
     * Sets my configuration.
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

    //-- ObjectPool methods ------------------------------------------

    public synchronized Object borrowObject(Object key) throws Exception {
        long starttime = System.currentTimeMillis();
        boolean newlyCreated = false;
        for(;;) {
            LinkedList pool = (LinkedList)(_poolMap.get(key));
            if(null == pool) {
                pool = new LinkedList();
                _poolMap.put(key,pool);
            }
            ObjectTimestampPair pair = null;
            // if there are any sleeping, just grab one of those
            try {
                pair = (ObjectTimestampPair)(pool.removeFirst());
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
                int active = getActiveCount(key);
                if ((_maxActive < 0 || active < _maxActive) &&
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
            _factory.activateObject(key,pair.value);
            if(_testOnBorrow && !_factory.validateObject(key,pair.value)) {
                _factory.destroyObject(key,pair.value);
                if(newlyCreated) {
                    throw new NoSuchElementException("Could not create a validated object");
                } // else keep looping
            } else {
                incrementActiveCount(key);
                return pair.value;
            }
        }
    }

    public synchronized void clear() {
        for(Iterator keyiter = _poolMap.keySet().iterator(); keyiter.hasNext(); ) {
            Object key = keyiter.next();
            final LinkedList list = (LinkedList)(_poolMap.get(key));
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
        if (_recentlyEvictedKeys != null) {
            _recentlyEvictedKeys.clear();
        }
        _totalIdle = 0;
        notifyAll();
    }

    /**
     * Method clears oldest 15% of objects in pool.  The method sorts the 
     * objects into a TreeMap and then iterates the first 15% for removal
     */
    public synchronized void clearOldest() {
        // build sorted map of idle objects
        TreeMap map = new TreeMap();
        for (Iterator keyiter = _poolMap.keySet().iterator(); keyiter.hasNext();) {
            Object key = keyiter.next();
            LinkedList list = (LinkedList) _poolMap.get(key);
            for (Iterator it = list.iterator(); it.hasNext();) {
                // each item into the map uses the objectimestamppair object
                // as the key.  It then gets sorted based on the timstamp field
                // each value in the map is the parent list it belongs in.
                ObjectTimestampPair pair = (ObjectTimestampPair) it.next();
                map.put(pair, key);
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
            LinkedList list = (LinkedList) _poolMap.get(key);
            list.remove(pairTimeStamp);

            try {
                _factory.destroyObject(key, pairTimeStamp.value);
            } catch (Exception e) {
                // ignore error, keep destroying the rest
            }
            // if that was the last object for that key, drop that pool
            if (list.isEmpty()) {
                _poolMap.remove(key);
            }
            _totalIdle--;
            itemsToRemove--;
        }
        notifyAll();
    }
     
    public synchronized void clear(Object key) {
        LinkedList pool = (LinkedList)(_poolMap.remove(key));
        if(null == pool) {
            return;
        } else {
            for(Iterator it = pool.iterator(); it.hasNext(); ) {
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

    public synchronized int getNumActive() {
        return _totalActive;
    }

    public synchronized int getNumIdle() {
        return _totalIdle;
    }

    public synchronized int getNumActive(Object key) {
        return getActiveCount(key);
    }

    public synchronized int getNumIdle(Object key) {
        try {
            return((LinkedList)(_poolMap.get(key))).size();
        } catch(Exception e) {
            return 0;
        }
    }

    public synchronized void returnObject(Object key, Object obj) throws Exception {

        // if we need to validate this object, do so
        boolean success = true; // whether or not this object passed validation
        if(_testOnReturn && !_factory.validateObject(key, obj)) {
            success = false;
            try {
                _factory.destroyObject(key, obj);
            } catch(Exception e) {
                // ignored
            }
        } else {
            try {
                _factory.passivateObject(key, obj);
            } catch(Exception e) {
                success = false;
            }
        }

        boolean shouldDestroy = false;
        // grab the pool (list) of objects associated with the given key
        LinkedList pool = (LinkedList) (_poolMap.get(key));
        // if it doesn't exist, create it
        if(null == pool) {
            pool = new LinkedList();
            _poolMap.put(key, pool);
        }
        decrementActiveCount(key);
        // if there's no space in the pool, flag the object for destruction
        // else if we passivated succesfully, return it to the pool
        if(_maxIdle >= 0 && (pool.size() >= _maxIdle)) {
            shouldDestroy = true;
        } else if(success) {
            pool.addLast(new ObjectTimestampPair(obj));
            _totalIdle++;
        }
        notifyAll();

        if(shouldDestroy) {
            try {
                _factory.destroyObject(key, obj);
            } catch(Exception e) {
                // ignored?
            }
        }
    }

    public synchronized void invalidateObject(Object key, Object obj) throws Exception {
        try {
            _factory.destroyObject(key, obj);
        }
        finally {
            decrementActiveCount(key);
            notifyAll(); // _totalActive has changed
        }
    }

    public synchronized void addObject(Object key) throws Exception {
        Object obj = _factory.makeObject(key);
        incrementActiveCount(key); // returnObject will decrement this
        returnObject(key,obj);
    }

    /**
     * Registers a key for pool control. 
     * 
     * If <i>populateImmediately</i> is <code>true</code>, the pool will immediately commence
     * a sustain cycle. If <i>populateImmediately</i> is <code>false</code>, the pool will be
     * populated when the next schedules sustain task is run.
     * 
     * @param key - The key to register for pool control.
     * @param populateImmediately - If this is <code>true</code>, the pool
     * will start a sustain cycle immediately.
     */
    public synchronized void preparePool(Object key, boolean populateImmediately) {
        LinkedList pool = (LinkedList)(_poolMap.get(key));
        if (null == pool) {
            pool = new LinkedList();
            _poolMap.put(key,pool);
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
    
    public synchronized void close() throws Exception {
        clear();
        _poolMap = null;
        _activeMap = null;
        _recentlyEvictedKeys = null;
        if(null != _evictor) {
            _evictor.cancel();
            _evictor = null;
        }
    }

    public synchronized void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException {
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    public synchronized void evict() throws Exception {
        Object key = null;
        if (_recentlyEvictedKeys == null) {
            _recentlyEvictedKeys = new HashSet(_poolMap.size());
        }
        Set remainingKeys = new HashSet(_poolMap.keySet());
        remainingKeys.removeAll(_recentlyEvictedKeys);
        Iterator keyIter = remainingKeys.iterator();

        ListIterator objIter = null;

        for(int i=0,m=getNumTests();i<m;i++) {
            if(_poolMap.size() > 0) {
                // Find next idle object pool key to work on
                if (key == null) {
                    if (!keyIter.hasNext()) {
                        _recentlyEvictedKeys.clear();
                        remainingKeys = new HashSet(_poolMap.keySet());
                        keyIter = remainingKeys.iterator();
                    }
                    if (!keyIter.hasNext()) {
                        // done, there are no keyed pools
                        return;
                    }
                    key = keyIter.next();
                }

                // if we don't have a keyed object pool iterator
                if (objIter == null) {
                    final LinkedList list = (LinkedList)_poolMap.get(key);
                    if (_evictLastIndex < 0 || _evictLastIndex > list.size()) {
                        _evictLastIndex = list.size();
                    }
                    objIter = list.listIterator(_evictLastIndex);
                }

                // if the _evictionCursor has a previous object, then test it
                if(objIter.hasPrevious()) {
                    ObjectTimestampPair pair = (ObjectTimestampPair)(objIter.previous());
                    boolean removeObject=false;
                    if(_minEvictableIdleTimeMillis > 0 &&
                       System.currentTimeMillis() - pair.tstamp > _minEvictableIdleTimeMillis) {
                       removeObject=true;
                    } else if(_testWhileIdle) {
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
                            objIter.remove();
                            _totalIdle--;
                            _factory.destroyObject(key,pair.value);

                            // Do not remove the key from the _poolList or _poolmap, even if the list
                            // stored in the _poolMap for this key is empty when the 
                            // {@link #getMinIdle <i>minIdle</i>} is > 0.
                            // 
                            // Otherwise if it was the last object for that key, drop that pool
                            if ((_minIdle == 0) && (((LinkedList)(_poolMap.get(key))).isEmpty())) {
                                _poolMap.remove(key);
                            }
                        } catch(Exception e) {
                            ; // ignored
                        }
                    }
                } else {
                    // else done evicting keyed pool
                    _recentlyEvictedKeys.add(key);
                    _evictLastIndex = -1;
                    objIter = null;
                }
            }
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
     * <i>delay</i> is non-positive, stop it
     * if it is already running.
     */
    protected synchronized void startEvictor(long delay) {
        if(null != _evictor) {
            _evictor.cancel();
            _evictor = null;
        }
        if(delay > 0) {
            _evictor = new Evictor(delay);
            Thread t = new Thread(_evictor);
            t.setDaemon(true);
            t.start();
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
            return(int)(Math.ceil((double)_totalIdle/Math.abs((double)_numTestsPerEvictionRun)));
        }
    }

    private void incrementActiveCount(Object key) {
        _totalActive++;
        Integer active = (Integer)(_activeMap.get(key));
        if(null == active) {
            _activeMap.put(key,new Integer(1));
        } else {
            _activeMap.put(key,new Integer(active.intValue() + 1));
        }
    }

    private void decrementActiveCount(Object key) {
        _totalActive--;
        Integer active = (Integer)(_activeMap.get(key));
        if(null == active) {
            // do nothing, either null or zero is OK
        } else if(active.intValue() <= 1) {
            _activeMap.remove(key);
        } else {
            _activeMap.put(key, new Integer(active.intValue() - 1));
        }
    }

    private int getActiveCount(Object key) {
        int active = 0;
        Integer act = (Integer)(_activeMap.get(key));
        if(null != act) {
            active = act.intValue();
        }
        return active;
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
     * A simple "struct" encapsulating an object instance and a timestamp.
     * 
     * Implements Comparable, objects are sorted from old to new.
     */
    class ObjectTimestampPair implements Comparable {
        Object value;
        long tstamp;

        ObjectTimestampPair(Object val) {
            value = val;
            tstamp = System.currentTimeMillis();
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
            return (int) (this.tstamp - other.tstamp);
        }
    }

    /**
     * The idle object evictor thread.
     * @see GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis
     */
    class Evictor implements Runnable {
        private boolean _cancelled = false;
        private long _delay = 0L;

        public Evictor(long delay) {
            _delay = delay;
        }

        void cancel() {
            _cancelled = true;
        }

        public void run() {
            while(!_cancelled) {
                long sleeptime = 0L;
                synchronized(GenericKeyedObjectPool.this) {
                    sleeptime = _timeBetweenEvictionRunsMillis;
                }
                try {
                    Thread.sleep(sleeptime);
                } catch(Exception e) {
                    ; // ignored
                }
                //Evict from the pool
                try {
                    evict();
                } catch(Exception e) {
                    ; // ignored
                }
                //Re-create the connections.
                try {
                    ensureMinIdle();
                } catch (Exception e) { 
                    ; // ignored
                }
            }
        }
    }

    /**
     * A simple "struct" encapsulating the
     * configuration information for a {@link GenericKeyedObjectPool}.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory,GenericKeyedObjectPool.Config)
     * @see GenericKeyedObjectPool#setConfig
     */
    public static class Config {
        public int maxIdle = GenericKeyedObjectPool.DEFAULT_MAX_IDLE;
        public int maxActive = GenericKeyedObjectPool.DEFAULT_MAX_ACTIVE;
        public int maxTotal = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;
        public int minIdle = GenericKeyedObjectPool.DEFAULT_MIN_IDLE;
        public long maxWait = GenericKeyedObjectPool.DEFAULT_MAX_WAIT;
        public byte whenExhaustedAction = GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
        public boolean testOnBorrow = GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW;
        public boolean testOnReturn = GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN;
        public boolean testWhileIdle = GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE;
        public long timeBetweenEvictionRunsMillis = GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        public int numTestsPerEvictionRun =  GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
        public long minEvictableIdleTimeMillis = GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    }

    //--- protected attributes ---------------------------------------

    /**
     * The cap on the number of idle instances in the pool (per key).
     * @see #setMaxIdle
     * @see #getMaxIdle
     */
    private int _maxIdle = DEFAULT_MAX_IDLE;

    /**
     * The minimum no of idle objects to keep in the pool (per key)
     * @see #setMinIdle
     * @see #getMinIdle
     */
    private int _minIdle = DEFAULT_MIN_IDLE;

    /**
     * The cap on the number of active instances from the pool (per key).
     * @see #setMaxActive
     * @see #getMaxActive
     */
    private int _maxActive = DEFAULT_MAX_ACTIVE;

    /**
     * The cap on the total number of instances from the pool.
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
     * When less than 0, the {@link #borrowObject} method
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
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #setTestOnBorrow
     * @see #getTestOnBorrow
     */
    private boolean _testOnBorrow = DEFAULT_TEST_ON_BORROW;

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private boolean _testOnReturn = DEFAULT_TEST_ON_RETURN;

    /**
     * When <tt>true</tt>, objects will be
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
     * before it is eligable for eviction by the idle object evictor
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

    /** My hash of pools (CursorableLinkedLists). */
    private HashMap _poolMap = null;

    /** Count of active objects, per key. */
    private HashMap _activeMap = null;

    /** The total number of active instances. */
    private int _totalActive = 0;

    /** The total number of idle instances. */
    private int _totalIdle = 0;

    /** My {@link KeyedPoolableObjectFactory}. */
    private KeyedPoolableObjectFactory _factory = null;

    /**
     * My idle object eviction thread, if any.
     */
    private Evictor _evictor = null;

    /**
     * Idle object pool keys that have been evicted recently.
     */
    private Set _recentlyEvictedKeys = null;

    /**
     * Position in the _pool where the _evictor last stopped.
     */
    private int _evictLastIndex = -1;
}
