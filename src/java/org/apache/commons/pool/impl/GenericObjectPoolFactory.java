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
 * @version $Revision: 1.7 $ $Date: 2004/02/28 12:16:21 $ 
 */
public class GenericObjectPoolFactory implements ObjectPoolFactory {
    public GenericObjectPoolFactory(PoolableObjectFactory factory) {
        this(factory,GenericObjectPool.DEFAULT_MAX_ACTIVE,GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericObjectPool.DEFAULT_MAX_WAIT,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, GenericObjectPool.Config config) {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.minIdle,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericObjectPool.DEFAULT_MAX_WAIT,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericObjectPool.DEFAULT_MAX_IDLE,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,GenericObjectPool.DEFAULT_TEST_ON_BORROW,GenericObjectPool.DEFAULT_TEST_ON_RETURN,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle,GenericObjectPool.DEFAULT_MIN_IDLE,testOnBorrow,testOnReturn,GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }
    
    public GenericObjectPoolFactory(PoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int minIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
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
        _factory = factory;
    }

    public ObjectPool createPool() {
        return new GenericObjectPool(_factory,_maxActive,_whenExhaustedAction,_maxWait,_maxIdle,_minIdle,_testOnBorrow,_testOnReturn,_timeBetweenEvictionRunsMillis,_numTestsPerEvictionRun,_minEvictableIdleTimeMillis,_testWhileIdle);
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
    protected PoolableObjectFactory _factory = null;


}
