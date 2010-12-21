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

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedObjectPoolFactory;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;

/**
 * A factory for creating {@link GenericKeyedObjectPool} instances.
 *
 * @see GenericKeyedObjectPool
 * @see KeyedObjectPoolFactory
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericKeyedObjectPoolFactory<K,V> implements KeyedObjectPoolFactory<K,V> {
    /**
     * Create a new GenericKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory to used by created pools.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory)
     */
    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory<K,V> factory) {
        this(factory,new GenericKeyedObjectPoolConfig.Builder().createConfig());
    }

    /**
     * Create a new GenericKeyedObjectPoolFactory.
     *
     * @param factory the KeyedPoolableObjectFactory to used by created pools.
     * @param config a non-null GenericKeyedObjectPool.Config describing the configuration.
     * @see GenericKeyedObjectPool#GenericKeyedObjectPool(KeyedPoolableObjectFactory, GenericKeyedObjectPoolConfig)
     * @throws NullPointerException when config is <code>null</code>.
     */
    public GenericKeyedObjectPoolFactory(KeyedPoolableObjectFactory<K,V> factory, GenericKeyedObjectPoolConfig config) throws NullPointerException {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        _factory = factory;
        this.maxIdlePerKey = config.getMaxIdlePerKey();
        this.minIdlePerKey = config.getMinIdlePerKey();
        this.maxTotalPerKey = config.getMaxTotalPerKey();
        this.maxTotal = config.getMaxTotal();
        this.maxWait = config.getMaxWait();
        this.whenExhaustedAction = config.getWhenExhaustedAction();
        this.testOnBorrow = config.getTestOnBorrow();
        this.testOnReturn = config.getTestOnReturn();
        this.testWhileIdle = config.getTestWhileIdle();
        this.timeBetweenEvictionRunsMillis = config.getTimeBetweenEvictionRunsMillis();
        this.numTestsPerEvictionRun = config.getNumTestsPerEvictionRun();
        this.minEvictableIdleTimeMillis = config.getMinEvictableIdleTimeMillis();
        this.lifo = config.getLifo();
    }

    /**
     * Create a new GenericKeyedObjectPool with the currently configured properties.
     * 
     * @return GenericKeyedObjectPool with {@link GenericKeyedObjectPoolConfig Configuration} determined by
     * current property settings
     */
    public KeyedObjectPool<K,V> createPool() {
        return new GenericKeyedObjectPool<K,V>(_factory,new GenericKeyedObjectPoolConfig.Builder()
                .setMaxIdlePerKey(maxIdlePerKey)
                .setMinIdlePerKey(minIdlePerKey)
                .setMaxTotalPerKey(maxTotalPerKey)
                .setMaxTotal(maxTotal)
                .setMaxWait(maxWait)
                .setWhenExhaustedAction(whenExhaustedAction)
                .setTestOnBorrow(testOnBorrow)
                .setTestOnReturn(testOnReturn)
                .setTestWhileIdle(testWhileIdle)
                .setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis)
                .setNumTestsPerEvictionRun(numTestsPerEvictionRun)
                .setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis)
                .setLifo(lifo)
                .createConfig());
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMaxIdle() maxIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized int getMaxIdlePerKey() {
        return this.maxIdlePerKey;
    }

    /**
     * @param maxIdle the {@link GenericKeyedObjectPoolConfig#getMaxIdle() maxIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxIdlePerKey(int maxIdlePerKey) {
        this.maxIdlePerKey = maxIdlePerKey;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMinIdle() minIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized int getMinIdlePerKey() {
        return this.minIdlePerKey;
    }

    /**
     * @param minIdle the {@link GenericKeyedObjectPoolConfig#getMinIdle() minIdle} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMinIdlePerKey(int minIdlePerKey) {
        this.minIdlePerKey = minIdlePerKey;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMaxTotalPerKey()} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized int getMaxTotalPerKey() {
        return this.maxTotalPerKey;
    }

    /**
     * @param maxActive the {@link GenericKeyedObjectPoolConfig#getMaxTotalPerKey()} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxTotalPerKey(int maxTotalPerKey) {
        this.maxTotalPerKey = maxTotalPerKey;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMaxTotal() maxTotal} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getMaxTotal() {
        return this.maxTotal;
    }

    /**
     * @param maxActive the {@link GenericKeyedObjectPoolConfig#getMaxTotal() maxTotal} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMaxWait() maxWait} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getMaxWait() {
        return this.maxWait;
    }

    /**
     * @param maxWait the {@link GenericKeyedObjectPoolConfig#getMaxWait() maxWait} setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getWhenExhaustedAction() whenExhaustedAction} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized WhenExhaustedAction getWhenExhaustedAction() {
        return this.whenExhaustedAction;
    }

    /**
     * @param whenExhaustedAction the {@link GenericKeyedObjectPoolConfig#getWhenExhaustedAction() whenExhaustedAction} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction) {
        this.whenExhaustedAction = whenExhaustedAction;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getTestOnBorrow() testOnBorrow} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestOnBorrow() {
        return this.testOnBorrow;
    }

    /**
     * @param testOnBorrow the {@link GenericKeyedObjectPoolConfig#getTestOnBorrow() testOnBorrow} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getTestOnReturn() testOnReturn} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestOnReturn() {
        return this.testOnReturn;
    }

    /**
     * @param testOnReturn the {@link GenericKeyedObjectPoolConfig#getTestOnReturn() testOnReturn} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getTestWhileIdle() testWhileIdle} setting for pools
     * created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getTestWhileIdle() {
        return this.testWhileIdle;
    }

    /**
     * @param testWhileIdle the {@link GenericKeyedObjectPoolConfig#getTestWhileIdle() testWhileIdle} setting for pools
     * created by this factory.
     * @since 2.0
     */
    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getTimeBetweenEvictionRunsMillis() timeBetweenEvictionRunsMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    /**
     * @param timeBetweenEvictionRunsMillis the {@link GenericKeyedObjectPoolConfig#getTimeBetweenEvictionRunsMillis() timeBetweenEvictionRunsMillis}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getNumTestsPerEvictionRun() numTestsPerEvictionRun}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized int getNumTestsPerEvictionRun() {
        return this.numTestsPerEvictionRun;
    }

    /**
     * @param numTestsPerEvictionRun the {@link GenericKeyedObjectPoolConfig#getNumTestsPerEvictionRun() numTestsPerEvictionRun}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getMinEvictableIdleTimeMillis() minEvictableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized long getMinEvictableIdleTimeMillis() {
        return this.minEvictableIdleTimeMillis;
    }

    /**
     * @param minEvictableIdleTimeMillis the {@link GenericKeyedObjectPoolConfig#getMinEvictableIdleTimeMillis() minEvictableIdleTimeMillis}
     * setting for pools created by this factory.
     * @since 2.0
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * @return the {@link GenericKeyedObjectPoolConfig#getLifo() lifo} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized boolean getLifo() {
        return this.lifo;
    }

    /**
     * @param lifo the {@link GenericKeyedObjectPoolConfig#getLifo() lifo} setting for pools created by this factory.
     * @since 1.5.5
     */
    public synchronized void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    /**
     * @return the {@link KeyedPoolableObjectFactory} used by pools created by this factory.
     * @since 1.5.5
     */
    public KeyedPoolableObjectFactory<K,V> getFactory() {
        return _factory;
    }

    //--- private attributes

    private final KeyedPoolableObjectFactory<K,V> _factory;

    /**
     * The cap on the number of idle instances in the pool.
     */
    private int maxIdlePerKey; // @GuardedBy("this")

    /**
     * The cap on the minimum number of idle instances in the pool.
     *
     * @see #setMinIdle
     */
    private int minIdlePerKey; // @GuardedBy("this")

    /**
     * The cap on the total number of active instances from the pool per key.
     *
     * @see #setMaxActive
     */
    private int maxTotalPerKey; // @GuardedBy("this")

    /**
     * The cap on the total number of active instances from the pool.
     *
     * @see #setMaxActive
     */
    private int maxTotal; // @GuardedBy("this")

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
    private long maxWait; // @GuardedBy("this")

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
    private WhenExhaustedAction whenExhaustedAction; // @GuardedBy("this")

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
    private boolean testOnBorrow; // @GuardedBy("this")

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #setTestOnReturn
     */
    private boolean testOnReturn; // @GuardedBy("this")

    /**
     * When <tt>true</tt>, objects will be
     * {@link org.apache.commons.pool2.ObjectPool#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private boolean testWhileIdle; // @GuardedBy("this")

    /**
     * The number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private long timeBetweenEvictionRunsMillis; // @GuardedBy("this")

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
    private int numTestsPerEvictionRun; // @GuardedBy("this")

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
    private long minEvictableIdleTimeMillis; // @GuardedBy("this")

    /**
     * Whether or not the pool behaves as a LIFO queue (last in first out)
     *
     * @see #setLifo
     */
    private boolean lifo; // @GuardedBy("this")

}
