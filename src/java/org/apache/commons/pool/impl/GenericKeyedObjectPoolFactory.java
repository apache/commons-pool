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

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A factory for creating {@link GenericKeyedObjectPool} instances.
 *
 * @see GenericKeyedObjectPool
 * @see KeyedObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.7 $ $Date: 2004/02/28 12:16:21 $ 
 */
public class GenericKeyedObjectPoolFactory implements KeyedObjectPoolFactory {
    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory) {
        this(factory,GenericKeyedObjectPool.DEFAULT_MAX_ACTIVE,GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericKeyedObjectPool.DEFAULT_MAX_WAIT,GenericKeyedObjectPool.DEFAULT_MAX_IDLE,GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, GenericKeyedObjectPool.Config config) {
        this(factory,config.maxActive,config.whenExhaustedAction,config.maxWait,config.maxIdle,config.testOnBorrow,config.testOnReturn,config.timeBetweenEvictionRunsMillis,config.numTestsPerEvictionRun,config.minEvictableIdleTimeMillis,config.testWhileIdle);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive) {
        this(factory,maxActive,GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION,GenericKeyedObjectPool.DEFAULT_MAX_WAIT,GenericKeyedObjectPool.DEFAULT_MAX_IDLE, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL,GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericKeyedObjectPool.DEFAULT_MAX_IDLE, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL,GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,GenericKeyedObjectPool.DEFAULT_MAX_IDLE, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL,testOnBorrow,testOnReturn,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL,GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle, maxTotal, GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW,GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn) {
        this(factory,maxActive,whenExhaustedAction,maxWait,maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL,testOnBorrow,testOnReturn,GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN,GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        this(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, GenericKeyedObjectPool.DEFAULT_MAX_TOTAL, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
    }

    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, int maxTotal, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
        _maxIdle = maxIdle;
        _maxActive = maxActive;
        _maxTotal = maxTotal;
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

    public KeyedObjectPool createPool() {
        return new GenericKeyedObjectPool(_factory,_maxActive,_whenExhaustedAction,_maxWait,_maxIdle,_maxTotal,_testOnBorrow,_testOnReturn,_timeBetweenEvictionRunsMillis,_numTestsPerEvictionRun,_minEvictableIdleTimeMillis,_testWhileIdle);
    }

    //--- protected attributes ---------------------------------------

    protected int _maxIdle = GenericKeyedObjectPool.DEFAULT_MAX_IDLE;
    protected int _maxActive = GenericKeyedObjectPool.DEFAULT_MAX_ACTIVE;
    protected int _maxTotal = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;
    protected long _maxWait = GenericKeyedObjectPool.DEFAULT_MAX_WAIT;
    protected byte _whenExhaustedAction = GenericKeyedObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION;
    protected boolean _testOnBorrow = GenericKeyedObjectPool.DEFAULT_TEST_ON_BORROW;
    protected boolean _testOnReturn = GenericKeyedObjectPool.DEFAULT_TEST_ON_RETURN;
    protected boolean _testWhileIdle = GenericKeyedObjectPool.DEFAULT_TEST_WHILE_IDLE;
    protected long _timeBetweenEvictionRunsMillis = GenericKeyedObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
    protected int _numTestsPerEvictionRun =  GenericKeyedObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    protected long _minEvictableIdleTimeMillis = GenericKeyedObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;
    protected KeyedPoolableObjectFactory _factory = null;

}
