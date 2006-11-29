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

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * A factory for creating {@link GenericObjectPool} instances.
 *
 * @see GenericObjectPool
 * @see ObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericObjectPoolFactory implements ObjectPoolFactory {
    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory) {
        this(factory,GenericObjectPool.DEFAULT_MAX_ACTIVE,GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericObjectPool.DEFAULT_MAX_WAIT,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param config a non-<code>null</code> GenericObjectPool.Config describing the configuration.
     * @throws NullPointerException when config is <code>null</code>.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, GenericObjectPool.Config)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, GenericObjectPool.Config config) throws NullPointerException {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.minIdle,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle,config.softMinEvictableIdleTimeMillis);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericObjectPool.DEFAULT_MAX_WAIT,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param testOnBorrow whether to validate objects before they are returned by the borrowObject.
     * @param testOnReturn whether to validate objects after they are returned to the returnObject.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, boolean, boolean)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param maxIdle the maximum number of idle objects in my pool.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, int)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param maxIdle the maximum number of idle objects in my pool.
     * @param testOnBorrow whether to validate objects before they are returned by the borrowObject.
     * @param testOnReturn whether to validate objects after they are returned to the returnObject.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, int, boolean, boolean)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param maxIdle the maximum number of idle objects in my pool.
     * @param testOnBorrow whether to validate objects before they are returned by the borrowObject.
     * @param testOnReturn whether to validate objects after they are returned to the returnObject.
     * @param timeBetweenEvictionRunsMillis the number of milliseconds to sleep between examining idle objects for eviction.
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread.
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for evcition.
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, int, boolean, boolean, long, int, long, boolean)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,timeBetweenEvictionRunsMillis,numTestsPerEvictionRun,minEvictableIdleTimeMillis,testWhileIdle, GenericObjectPool.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
    }
    
    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param maxIdle the maximum number of idle objects in my pool.
     * @param minIdle the minimum number of idle objects in my pool.
     * @param testOnBorrow whether to validate objects before they are returned by the borrowObject.
     * @param testOnReturn whether to validate objects after they are returned to the returnObject.
     * @param timeBetweenEvictionRunsMillis the number of milliseconds to sleep between examining idle objects for eviction.
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread.
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for evcition.
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread.
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, int, int, boolean, boolean, long, int, long, boolean)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,minIdle,testOnBorrow,testOnReturn,timeBetweenEvictionRunsMillis,numTestsPerEvictionRun,minEvictableIdleTimeMillis,testWhileIdle, GenericObjectPool.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
    }

    /**
     * Create a new GenericObjectPoolFactory.
     *
     * @param factory the PoolableObjectFactory used by created pools.
     * @param maxActive maximum number of objects that can be borrowed from created pools at one time.
     * @param whenExhaustedAction the action to take when the pool is exhausted.
     * @param maxWait the maximum amount of time to wait for an idle object when the pool is exhausted.
     * @param maxIdle the maximum number of idle objects in my pool.
     * @param minIdle the minimum number of idle objects in my pool.
     * @param testOnBorrow whether to validate objects before they are returned by the borrowObject.
     * @param testOnReturn whether to validate objects after they are returned to the returnObject.
     * @param timeBetweenEvictionRunsMillis the number of milliseconds to sleep between examining idle objects for eviction.
     * @param numTestsPerEvictionRun the number of idle objects to examine per run within the idle object eviction thread.
     * @param minEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for evcition.
     * @param testWhileIdle whether or not to validate objects in the idle object eviction thread.
     * @param softMinEvictableIdleTimeMillis the minimum number of milliseconds an object can sit idle in the pool before it is eligible for evcition with the extra condition that at least "minIdle" amount of object remain in the pool.
     * @since Pool 1.3
     * @see GenericObjectPool#GenericObjectPool(PoolableObjectFactory, int, byte, long, int, int, boolean, boolean, long, int, long, boolean, long)
     */
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle, long softMinEvictableIdleTimeMillis) {
        _maxIdle = maxIdle;
        _minIdle = minIdle;
        _maxActive = maxActive;
        _maxWait = maxWait;
        _whenExhaustedAction = whenExhaustedAction;
        _testOnBorrow = testOnBorrow;
        _testOnReturn = testOnReturn;
        _testWhileIdle = testWhileIdle;
        _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        _softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
        _factory = factory;
    }

    public ObjectPool createPool() {
        return new GenericObjectPool(_factory,_maxActive,_whenExhaustedAction,_maxWait,_maxIdle,_minIdle,_testOnBorrow,_testOnReturn,_timeBetweenEvictionRunsMillis,_numTestsPerEvictionRun,_minEvictableIdleTimeMillis,_testWhileIdle,_softMinEvictableIdleTimeMillis);
    }

    protected int _maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    protected int _minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;
    protected int _maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    protected long _maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;
    protected byte _whenExhaustedAction = GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
    protected boolean _testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;
    protected boolean _testOnReturn = GenericObjectPool.DEFAULT_TEST_ON_RETURN;
    protected boolean _testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;
    protected long _timeBetweenEvictionRunsMillis = GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    protected int _numTestsPerEvictionRun =  GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    protected long _minEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    protected long _softMinEvictableIdleTimeMillis = GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    protected PoolableObjectFactory _factory = null;


}
