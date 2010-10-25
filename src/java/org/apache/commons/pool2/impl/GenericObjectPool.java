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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimerTask;

import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool.ObjectTimestampPair;

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
 *    objects that can be allocated by the pool (checked out to clients, or
 *    idle awaiting checkout) at a given time.  When non-positive, there is no
 *    limit to the number of objects that can be managed by the pool at one time.
 *    When {@link #setMaxActive <i>maxActive</i>} is reached, the pool is said
 *    to be exhausted. The default setting for this parameter is 8.
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
 *      {@link WhenExhaustedAction#FAIL}, {@link #borrowObject} will throw
 *      a {@link NoSuchElementException}
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>} is
 *      {@link WhenExhaustedAction#GROW}, {@link #borrowObject} will create a new
 *      object and return it (essentially making {@link #setMaxActive <i>maxActive</i>}
 *      meaningless.)
 *    </li>
 *    <li>
 *      When {@link #setWhenExhaustedAction <i>whenExhaustedAction</i>}
 *      is {@link WhenExhaustedAction#BLOCK}, {@link #borrowObject} will block
 *      (invoke {@link Object#wait()}) until a new or idle object is available.
 *      If a positive {@link #setMaxWait <i>maxWait</i>}
 *      value is supplied, then {@link #borrowObject} will block for at
 *      most that many milliseconds, after which a {@link NoSuchElementException}
 *      will be thrown.  If {@link #setMaxWait <i>maxWait</i>} is non-positive,
 *      the {@link #borrowObject} method will block indefinitely.
 *    </li>
 *    </ul>
 *    The default <code>whenExhaustedAction</code> setting is
 *    {@link WhenExhaustedAction#BLOCK} and the default <code>maxWait</code>
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
 * this optional feature. Eviction runs contend with client threads for access
 * to objects in the pool, so if they run too frequently performance issues may
 * result. The idle object eviction thread may be configured using the following
 * attributes:
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
 *   (if any), with the extra condition that at least "minIdle" object instances
 *   remain in the pool.  When non-positive, no objects will be evicted from the pool
 *   due to idle time alone. This setting has no effect unless
 *   <code>timeBetweenEvictionRunsMillis > 0.</code> and it is superceded by
 *   {@link #setMinEvictableIdleTimeMillis <i>minEvictableIdleTimeMillis</i>}
 *   (that is, if <code>minEvictableIdleTimeMillis</code> is positive, then
 *   <code>softMinEvictableIdleTimeMillis</code> is ignored). The default setting for
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
 * non-<code>null</code> factory must be provided as a constructor argument
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
public class GenericObjectPool<T> extends BaseObjectPool<T> implements ObjectPool<T> {

    //--- constructors -----------------------------------------------

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified factory and a default {@link GenericObjectPoolConfig}.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory) {
        this(factory, new GenericObjectPoolConfig());
    }

    /**
     * Create a new <tt>GenericObjectPool</tt> using the specified values.
     * @param factory the (possibly <tt>null</tt>)PoolableObjectFactory to use to create, validate and destroy objects
     * @param config a non-<tt>null</tt> {@link GenericObjectPoolConfig} describing my configuration
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory, GenericObjectPoolConfig config) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this._factory = factory;
        this.config = config;

        _pool = new CursorableLinkedList<ObjectTimestampPair<T>>();
        startEvictor(config.getTimeBetweenEvictionRunsMillis());
    }

    //--- public methods ---------------------------------------------

    //--- configuration methods --------------------------------------

    /**
     * Returns the maximum number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time.
     * When non-positive, there is no limit to the number of objects that can
     * be managed by the pool at one time.
     *
     * @return the cap on the total number of object instances managed by the pool.
     * @see #setMaxActive
     */
    public synchronized int getMaxActive() {
        return this.config.getMaxActive();
    }

    /**
     * Sets the cap on the number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time. Use
     * a negative value for no limit.
     *
     * @param maxActive The cap on the total number of object instances managed by the pool.
     * Negative values mean that there is no limit to the number of objects allocated
     * by the pool.
     * @see #getMaxActive
     */
    public synchronized void setMaxActive(int maxActive) {
        this.config.setMaxActive(maxActive);
        allocate();
    }

    /**
     * Returns the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @return one of {@link WhenExhaustedAction#BLOCK}, {@link WhenExhaustedAction#FAIL} or {@link WhenExhaustedAction#GROW}
     * @see #setWhenExhaustedAction
     */
    public synchronized WhenExhaustedAction getWhenExhaustedAction() {
        return this.config.getWhenExhaustedAction();
    }

    /**
     * Sets the action to take when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @param whenExhaustedAction the action code, which must be one of
     *        {@link WhenExhaustedAction#BLOCK}, {@link WhenExhaustedAction#FAIL},
     *        or {@link WhenExhaustedAction#GROW}
     * @see #getWhenExhaustedAction
     */
    public synchronized void setWhenExhaustedAction(WhenExhaustedAction whenExhaustedAction) {
        this.config.setWhenExhaustedAction(whenExhaustedAction);
        allocate();
    }


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
     * @return maximum number of milliseconds to block when borrowing an object.
     * @see #setMaxWait
     * @see #setWhenExhaustedAction
     * @see WhenExhaustedAction#BLOCK
     */
    public synchronized long getMaxWait() {
        return this.config.getMaxWait();
    }

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
     * @param maxWait maximum number of milliseconds to block when borrowing an object.
     * @see #getMaxWait
     * @see #setWhenExhaustedAction
     * @see WhenExhaustedAction#BLOCK
     */
    public synchronized void setMaxWait(long maxWait) {
        this.config.setMaxWait(maxWait);
        allocate();
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool.
     * @return the cap on the number of "idle" instances in the pool.
     * @see #setMaxIdle
     */
    public synchronized int getMaxIdle() {
        return this.config.getMaxIdle();
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
        this.config.setMaxIdle(maxIdle);
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
        this.config.setMinIdle(minIdle);
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
        return this.config.getMinIdle();
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
        return this.config.getTestOnBorrow();
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
        this.config.setTestOnBorrow(testOnBorrow);
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
        return this.config.getTestOnReturn();
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
        this.config.setTestOnReturn(testOnReturn);
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
        return this.config.getTimeBetweenEvictionRunsMillis();
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
        this.config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        startEvictor(this.config.getTimeBetweenEvictionRunsMillis());
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
        return this.config.getNumTestsPerEvictionRun();
    }

    /**
     * Sets the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  That is, when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run. When the value is positive, the number of tests
     * actually performed in each run will be the minimum of this value and the number of instances
     * idle in the pool.
     *
     * @param numTestsPerEvictionRun max number of objects to examine during each evictor run.
     * @see #getNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.config.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
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
        return this.config.getMinEvictableIdleTimeMillis();
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     * @param minEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
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
        return this.config.getSoftMinEvictableIdleTimeMillis();
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any), with the extra condition that at least
     * "minIdle" object instances remain in the pool.
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @param softMinEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction.
     * @since Pool 1.3
     * @see #getSoftMinEvictableIdleTimeMillis
     */
    public synchronized void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        this.config.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
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
        return this.config.getTestWhileIdle();
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
        this.config.setTestWhileIdle(testWhileIdle);
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
         return this.config.getLifo();
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
         this.config.setLifo(lifo);
     }

    /**
     * Sets my configuration.
     *
     * @param conf configuration to use.
     * @see GenericObjectPool.Config
     */
    public synchronized void setConfig(GenericObjectPoolConfig conf) {
        this.config = conf;
        allocate();
    }

    //-- ObjectPool methods ------------------------------------------

    /**
     * <p>Borrows an object from the pool.</p>
     * 
     * <p>If there is an idle instance available in the pool, then either the most-recently returned
     * (if {@link #getLifo() lifo} == true) or "oldest" (lifo == false) instance sitting idle in the pool
     * will be activated and returned.  If activation fails, or {@link #getTestOnBorrow() testOnBorrow} is set
     * to true and validation fails, the instance is destroyed and the next available instance is examined.
     * This continues until either a valid instance is returned or there are no more idle instances available.</p>
     * 
     * <p>If there are no idle instances available in the pool, behavior depends on the {@link #getMaxActive() maxActive}
     * and (if applicable) {@link #getWhenExhaustedAction() whenExhaustedAction} and {@link #getMaxWait() maxWait}
     * properties. If the number of instances checked out from the pool is less than <code>maxActive,</code> a new
     * instance is created, activated and (if applicable) validated and returned to the caller.</p>
     * 
     * <p>If the pool is exhausted (no available idle instances and no capacity to create new ones),
     * this method will either block ({@link WhenExhaustedAction#BLOCK}), throw a <code>NoSuchElementException</code>
     * ({@link WhenExhaustedAction#FAIL}), or grow ({@link WhenExhaustedAction#GROW} - ignoring maxActive).
     * The length of time that this method will block when <code>whenExhaustedAction == WHEN_EXHAUSTED_BLOCK</code>
     * is determined by the {@link #getMaxWait() maxWait} property.</p>
     * 
     * <p>When the pool is exhausted, multiple calling threads may be simultaneously blocked waiting for instances
     * to become available.  As of pool 1.5, a "fairness" algorithm has been implemented to ensure that threads receive
     * available instances in request arrival order.</p>
     * 
     * @return object instance
     * @throws NoSuchElementException if an instance cannot be returned
     */
    @Override
    public T borrowObject() throws Exception {
        long starttime = System.currentTimeMillis();
        Latch latch = new Latch();
        WhenExhaustedAction whenExhaustedAction;
        long maxWait;
        synchronized (this) {
            // Get local copy of current config. Can't sync when used later as
            // it can result in a deadlock. Has the added advantage that config
            // is consistent for entire method execution
            whenExhaustedAction = this.config.getWhenExhaustedAction();
            maxWait = this.config.getMaxWait();

            // Add this request to the queue
            _allocationQueue.add(latch);

            // Work the allocation queue, allocating idle instances and
            // instance creation permits in request arrival order
            allocate();
        }

        for(;;) {
            synchronized (this) {
                assertOpen();
            }

            // If no object was allocated from the pool above
            if(latch.getPair() == null) {
                // check if we were allowed to create one
                if(latch.mayCreate()) {
                    // allow new object to be created
                } else {
                    // the pool is exhausted
                    switch(whenExhaustedAction) {
                        case GROW:
                            // allow new object to be created
                            synchronized (this) {
                                // Make sure another thread didn't allocate us an object
                                // or permit a new object to be created
                                if (latch.getPair() == null && !latch.mayCreate()) {
                                    _allocationQueue.remove(latch);
                                    _numInternalProcessing++;
                                }
                            }
                            break;
                        case FAIL:
                            synchronized (this) {
                                // Make sure allocate hasn't already assigned an object
                                // in a different thread or permitted a new object to be created
                                if (latch.getPair() != null || latch.mayCreate()) {
                                    break;
                                }
                                _allocationQueue.remove(latch);
                            }
                            throw new NoSuchElementException("Pool exhausted");
                        case BLOCK:
                            try {
                                synchronized (latch) {
                                    // Before we wait, make sure another thread didn't allocate us an object
                                    // or permit a new object to be created
                                    if (latch.getPair() == null && !latch.mayCreate()) {
                                        if(maxWait <= 0) {
                                            latch.wait();
                                        } else {
                                            // this code may be executed again after a notify then continue cycle
                                            // so, need to calculate the amount of time to wait
                                            final long elapsed = (System.currentTimeMillis() - starttime);
                                            final long waitTime = maxWait - elapsed;
                                            if (waitTime > 0)
                                            {
                                                latch.wait(waitTime);
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            } catch(InterruptedException e) {
                                synchronized(this) {
                                    // Make sure allocate hasn't already assigned an object
                                    // in a different thread or permitted a new object to be created
                                    if (latch.getPair() == null && !latch.mayCreate()) {
                                        // Remove latch from the allocation queue
                                        _allocationQueue.remove(latch);
                                    } else {
                                        break;
                                    }
                                }
                                Thread.currentThread().interrupt();
                                throw e;
                            }
                            if(maxWait > 0 && ((System.currentTimeMillis() - starttime) >= maxWait)) {
                                synchronized(this) {
                                    // Make sure allocate hasn't already assigned an object
                                    // in a different thread or permitted a new object to be created
                                    if (latch.getPair() == null && !latch.mayCreate()) {
                                        // Remove latch from the allocation queue
                                        _allocationQueue.remove(latch);
                                    } else {
                                        break;
                                    }
                                }
                                throw new NoSuchElementException("Timeout waiting for idle object");
                            } else {
                                continue; // keep looping
                            }
                        default:
                            throw new IllegalArgumentException("WhenExhaustedAction property " + whenExhaustedAction +
                                    " not recognized.");
                    }
                }
            }

            boolean newlyCreated = false;
            if(null == latch.getPair()) {
                try {
                    T obj = _factory.makeObject();
                    latch.setPair(new ObjectTimestampPair<T>(obj));
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
                _factory.activateObject(latch.getPair().getValue());
                if(this.config.getTestOnBorrow() &&
                        !_factory.validateObject(latch.getPair().getValue())) {
                    throw new Exception("ValidateObject failed");
                }
                synchronized(this) {
                    _numInternalProcessing--;
                    _numActive++;
                }
                return latch.getPair().getValue();
            }
            catch (Throwable e) {
                PoolUtils.checkRethrow(e);
                // object cannot be activated or is invalid
                try {
                    _factory.destroyObject(latch.getPair().getValue());
                } catch (Throwable e2) {
                    PoolUtils.checkRethrow(e2);
                    // cannot destroy broken object
                }
                synchronized (this) {
                    _numInternalProcessing--;
                    if (!newlyCreated) {
                        latch.reset();
                        _allocationQueue.add(0, latch);
                    }
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

    /**
     * Allocate available instances to latches in the allocation queue.  Then
     * set _mayCreate to true for as many additional latches remaining in queue
     * as _maxActive allows.
     */
    private synchronized void allocate() {
        if (isClosed()) return;

        // First use any objects in the pool to clear the queue
        for (;;) {
            if (!_pool.isEmpty() && !_allocationQueue.isEmpty()) {
                Latch latch = _allocationQueue.removeFirst();
                latch.setPair(_pool.removeFirst());
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
            if((!_allocationQueue.isEmpty()) && (this.config.getMaxActive() < 0 || (_numActive + _numInternalProcessing) < this.config.getMaxActive())) {
                Latch latch = _allocationQueue.removeFirst();
                latch.setMayCreate(true);
                _numInternalProcessing++;
                synchronized (latch) {
                    latch.notify();
                }
            } else {
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>Activation of this method decrements the active count and attempts to destroy the instance.</p>
     * 
     * @throws Exception if the configured {@link PoolableObjectFactory} throws an exception destroying obj
     */
    @Override
    public void invalidateObject(T obj) throws Exception {
        try {
            _factory.destroyObject(obj);
        } finally {
            synchronized (this) {
                _numActive--;
                allocate();
            }
        }
    }

    /**
     * Clears any objects sitting idle in the pool by removing them from the
     * idle instance pool and then invoking the configured 
     * {@link PoolableObjectFactory#destroyObject(Object)} method on each idle
     * instance. 
     * 
     * <p> Implementation notes:
     * <ul><li>This method does not destroy or effect in any way instances that are
     * checked out of the pool when it is invoked.</li>
     * <li>Invoking this method does not prevent objects being
     * returned to the idle instance pool, even during its execution. It locks
     * the pool only during instance removal. Additional instances may be returned
     * while removed items are being destroyed.</li>
     * <li>Exceptions encountered destroying idle instances are swallowed.</li></ul></p>
     */
    @Override
    public void clear() {
        List<ObjectTimestampPair<T>> toDestroy = new ArrayList<ObjectTimestampPair<T>>();

        synchronized(this) {
            toDestroy.addAll(_pool);
            _numInternalProcessing = _numInternalProcessing + _pool._size;
            _pool.clear();
        }
        destroy(toDestroy, _factory);
    }

    /**
     * Private method to destroy all the objects in a collection using the 
     * supplied object factory.  Assumes that objects in the collection are
     * instances of ObjectTimestampPair and that the object instances that
     * they wrap were created by the factory.
     * 
     * @param c Collection of objects to destroy
     * @param factory PoolableConnectionFactory used to destroy the objects
     */
    private void destroy(Collection<ObjectTimestampPair<T>> c, PoolableObjectFactory<T> factory) {
        for (ObjectTimestampPair<T> pair : c) {
            try {
                factory.destroyObject(pair.getValue());
            } catch (Exception e) {
                // ignore error, keep destroying the rest
            } finally {
                synchronized (this) {
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
    @Override
    public synchronized int getNumActive() {
        return _numActive;
    }

    /**
     * Return the number of instances currently idle in this pool.
     *
     * @return the number of instances currently idle in this pool
     */
    @Override
    public synchronized int getNumIdle() {
        return _pool.size();
    }

    /**
     * <p>Returns an object instance to the pool.</p>
     * 
     * <p>If {@link #getMaxIdle() maxIdle} is set to a positive value and the number of idle instances
     * has reached this value, the returning instance is destroyed.</p>
     * 
     * <p>If {@link #getTestOnReturn() testOnReturn} == true, the returning instance is validated before being returned
     * to the idle instance pool.  In this case, if validation fails, the instance is destroyed.</p>
     * 
     * <p><strong>Note: </strong> There is no guard to prevent an object
     * being returned to the pool multiple times. Clients are expected to
     * discard references to returned objects and ensure that an object is not
     * returned to the pool multiple times in sequence (i.e., without being
     * borrowed again between returns). Violating this contract will result in
     * the same object appearing multiple times in the pool and pool counters
     * (numActive, numIdle) returning incorrect values.</p>
     * 
     * @param obj instance to return to the pool
     */
    @Override
    public void returnObject(T obj) throws Exception {
        try {
            addObjectToPool(obj, true);
        } catch (Exception e) {
            try {
                _factory.destroyObject(obj);
            } catch (Exception e2) {
                // swallowed
            }
            // TODO: Correctness here depends on control in addObjectToPool.
            // These two methods should be refactored, removing the
            // "behavior flag", decrementNumActive, from addObjectToPool.
            synchronized(this) {
                _numActive--;
                allocate();
            }
        }
    }

    /**
     * <p>Adds an object to the pool.</p>
     * 
     * <p>Validates the object if testOnReturn == true and passivates it before returning it to the pool.
     * if validation or passivation fails, or maxIdle is set and there is no room in the pool, the instance
     * is destroyed.</p>
     * 
     * <p>Calls {@link #allocate()} on successful completion</p>
     * 
     * @param obj instance to add to the pool
     * @param decrementNumActive whether or not to decrement the active count
     * @throws Exception
     */
    private void addObjectToPool(T obj, boolean decrementNumActive) throws Exception {
        boolean success = true;
        if(this.config.getTestOnReturn() && !(_factory.validateObject(obj))) {
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
                if((this.config.getMaxIdle() >= 0) && (_pool.size() >= this.config.getMaxIdle())) {
                    shouldDestroy = true;
                } else if(success) {
                    // borrowObject always takes the first element from the queue,
                    // so for LIFO, push on top, FIFO add to end
                    if (this.getLifo()) {
                        _pool.addFirst(new ObjectTimestampPair<T>(obj));
                    } else {
                        _pool.addLast(new ObjectTimestampPair<T>(obj));
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

    /**
     * <p>Closes the pool.  Once the pool is closed, {@link #borrowObject()}
     * will fail with IllegalStateException, but {@link #returnObject(Object)} and
     * {@link #invalidateObject(Object)} will continue to work, with returned objects
     * destroyed on return.</p>
     * 
     * <p>Destroys idle instances in the pool by invoking {@link #clear()}.</p> 
     * 
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        synchronized (this) {
            clear();
            startEvictor(-1L);
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
                _evictionCursor = (_pool.cursor(this.config.getLifo() ? _pool.size() : 0));
            }
        }

        for (int i=0,m=getNumTests();i<m;i++) {
            final ObjectTimestampPair<T> pair;
            synchronized (this) {
                if ((this.config.getLifo() && !_evictionCursor.hasPrevious()) ||
                        !this.config.getLifo() && !_evictionCursor.hasNext()) {
                    _evictionCursor.close();
                    _evictionCursor = _pool.cursor(this.config.getLifo() ? _pool.size() : 0);
                }

                pair = this.config.getLifo() ? _evictionCursor.previous() : _evictionCursor.next();

                _evictionCursor.remove();
                _numInternalProcessing++;
            }

            boolean removeObject = false;
            final long idleTimeMilis = System.currentTimeMillis() - pair.getTstamp();
            if ((getMinEvictableIdleTimeMillis() > 0) &&
                    (idleTimeMilis > getMinEvictableIdleTimeMillis())) {
                removeObject = true;
            } else if ((getSoftMinEvictableIdleTimeMillis() > 0) &&
                    (idleTimeMilis > getSoftMinEvictableIdleTimeMillis()) &&
                    ((getNumIdle() + 1)> getMinIdle())) { // +1 accounts for object we are processing
                removeObject = true;
            }
            if(getTestWhileIdle() && !removeObject) {
                boolean active = false;
                try {
                    _factory.activateObject(pair.getValue());
                    active = true;
                } catch(Exception e) {
                    removeObject=true;
                }
                if(active) {
                    if(!_factory.validateObject(pair.getValue())) {
                        removeObject=true;
                    } else {
                        try {
                            _factory.passivateObject(pair.getValue());
                        } catch(Exception e) {
                            removeObject=true;
                        }
                    }
                }
            }

            if (removeObject) {
                try {
                    _factory.destroyObject(pair.getValue());
                } catch(Exception e) {
                    // ignored
                }
            }
            synchronized (this) {
                if(!removeObject) {
                    _evictionCursor.add(pair);
                    if (this.config.getLifo()) {
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

    /**
     * This returns the number of objects to create during the pool
     * sustain cycle. This will ensure that the minimum number of idle
     * instances is maintained without going past the maxActive value.
     *
     * @param incrementInternal - Should the count of objects currently under
     *                            some form of internal processing be
     *                            incremented?
     * @return The number of objects to be created
     */
    private synchronized int calculateDeficit(boolean incrementInternal) {
        int objectDeficit = getMinIdle() - getNumIdle();
        if (this.config.getMaxActive() > 0) {
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
    @Override
    public void addObject() throws Exception {
        assertOpen();
        T obj = _factory.makeObject();
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

    /**
     * Returns pool info including {@link #getNumActive()}, {@link #getNumIdle()}
     * and a list of objects idle in the pool with their idle times.
     * 
     * @return string containing debug information
     */
    synchronized String debugInfo() {
        StringBuffer buf = new StringBuffer();
        buf.append("Active: ").append(getNumActive()).append("\n");
        buf.append("Idle: ").append(getNumIdle()).append("\n");
        buf.append("Idle Objects:\n");
        long time = System.currentTimeMillis();
        for (ObjectTimestampPair<T> pair  : _pool) {
            buf.append("\t").append(pair.getValue()).append("\t").append(time - pair.getTstamp()).append("\n");            
        }
        return buf.toString();
    }

    /** 
     * Returns the number of tests to be performed in an Evictor run,
     * based on the current value of <code>numTestsPerEvictionRun</code>
     * and the number of idle instances in the pool.
     * 
     * @see #setNumTestsPerEvictionRun
     * @return the number of tests for the Evictor to run
     */
    private int getNumTests() {
        int numTestsPerEvictionRun = this.config.getNumTestsPerEvictionRun();
        if(numTestsPerEvictionRun >= 0) {
            return Math.min(numTestsPerEvictionRun, _pool.size());
        } else {
            return(int)(Math.ceil(_pool.size()/Math.abs((double)numTestsPerEvictionRun)));
        }
    }

    //--- inner classes ----------------------------------------------

    /**
     * The idle object evictor {@link TimerTask}.
     * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
     */
    private class Evictor extends TimerTask {
        /**
         * Run pool maintenance.  Evict objects qualifying for eviction and then
         * invoke {@link GenericObjectPool#ensureMinIdle()}.
         */
        @Override
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
     * Latch used to control allocation order of objects to threads to ensure
     * fairness. That is, objects are allocated to threads in the order that
     * threads request objects.
     */
    private final class Latch {

        /** object timestamp pair allocated to this latch */
        private ObjectTimestampPair<T> _pair;

        /** Whether or not this latch may create an object instance */
        private boolean _mayCreate = false;

        /**
         * Returns ObjectTimestampPair allocated to this latch
         * @return ObjectTimestampPair allocated to this latch
         */
        private synchronized ObjectTimestampPair<T> getPair() {
            return _pair;
        }

        /**
         * Sets ObjectTimestampPair on this latch
         * @param pair ObjectTimestampPair allocated to this latch
         */
        private synchronized void setPair(ObjectTimestampPair<T> pair) {
            _pair = pair;
        }

        /**
         * Whether or not this latch may create an object instance 
         * @return true if this latch has an instance creation permit
         */
        private synchronized boolean mayCreate() {
            return _mayCreate;
        }

        /**
         * Sets the mayCreate property
         * @param mayCreate new value for mayCreate
         */
        private synchronized void setMayCreate(boolean mayCreate) {
            _mayCreate = mayCreate;
        }

        /**
         * Reset the latch data. Used when an allocation fails and the latch
         * needs to be re-added to the queue.
         */
        private synchronized void reset() {
            _pair = null;
            _mayCreate = false;
        }
    }


    //--- private attributes ---------------------------------------

    /** Pool configuration */
    private GenericObjectPoolConfig config;

    /** My pool. */
    private CursorableLinkedList<ObjectTimestampPair<T>> _pool = null;

    /** Eviction cursor - keeps track of idle object evictor position */
    private CursorableLinkedList<ObjectTimestampPair<T>>.Cursor _evictionCursor = null;

    /** My {@link PoolableObjectFactory}. */
    private final PoolableObjectFactory<T> _factory;

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
    private final LinkedList<Latch> _allocationQueue = new LinkedList<Latch>();

}
