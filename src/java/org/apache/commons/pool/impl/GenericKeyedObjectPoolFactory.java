/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/impl/GenericKeyedObjectPoolFactory.java,v $
 * $Revision: 1.4 $
 * $Date: 2003/08/26 14:14:15 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
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
 * http://www.apache.org/
 *
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
 * @version $Id: GenericKeyedObjectPoolFactory.java,v 1.4 2003/08/26 14:14:15 dirkv Exp $
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
