/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/impl/GenericObjectPoolFactory.java,v 1.3 2003/08/21 18:17:35 dirkv Exp $
 * $Revision: 1.3 $
 * $Date: 2003/08/21 18:17:35 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
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
 * @version $Id: GenericObjectPoolFactory.java,v 1.3 2003/08/21 18:17:35 dirkv Exp $
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
