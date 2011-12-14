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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.PoolUtils;

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
 *    {@link #setMaxTotalPerKey maxTotalPerKey} controls the maximum number of objects
 *    (per key) that can allocated by the pool (checked out to client threads,
 *    or idle in the pool) at one time.  When non-positive, there is no limit
 *    to the number of objects per key. When {@link #setMaxTotalPerKey maxTotalPerKey} is
 *    reached, the keyed pool is said to be exhausted.  The default setting for
 *    this parameter is 8.
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
 *    {@link #setMaxIdlePerKey maxIdlePerKey} controls the maximum number of objects that can
 *    sit idle in the pool (per key) at any time.  When negative, there
 *    is no limit to the number of objects that may be idle per key. The
 *    default setting for this parameter is 8.
 *  </li>
 *  <li>
 *    {@link #getBlockWhenExhausted} specifies the
 *    behavior of the {@link #borrowObject borrowObject} method when a keyed
 *    pool is exhausted:
 *    <ul>
 *    <li>
 *      When {@link #getBlockWhenExhausted} is false,
 *     {@link #borrowObject borrowObject} will throw
 *      a {@link NoSuchElementException}
 *    </li>
 *    <li>
 *      When {@link #getBlockWhenExhausted} is true,
 *      {@link #borrowObject borrowObject} will block
 *      (invoke {@link Object#wait() wait} until a new or idle object is available.
 *      If a non-negative {@link #setMaxWait maxWait}
 *      value is supplied, the {@link #borrowObject borrowObject} will block for at
 *      most that many milliseconds, after which a {@link NoSuchElementException}
 *      will be thrown.  If {@link #setMaxWait maxWait} is negative,
 *      the {@link #borrowObject borrowObject} method will block indefinitely.
 *    </li>
 *    </ul>
 *    The default {@link #getBlockWhenExhausted()} setting is true.
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
 * used when configuring this optional feature. Eviction runs contend with client
 * threads for access to objects in the pool, so if they run too frequently
 * performance issues may result.  The idle object eviction thread may be
 * configured using the following attributes:
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
 *    {@link #setMinIdlePerKey minIdlePerKey} sets a target value for the minimum number of
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
 * non-<code>null</code> factory must be provided as a constructor argument
 * before the pool is used.
 * </p>
 * <p>
 * Implementation note: To prevent possible deadlocks, care has been taken to
 * ensure that no call to a factory method will occur within a synchronization
 * block. See POOL-125 and DBCP-44 for more information.
 * </p>
 * @see GenericObjectPool
 *
 * @param <K> The type of keys maintained by this pool.
 * @param <T> Type of element pooled in this pool.
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class GenericKeyedObjectPool<K,T> implements KeyedObjectPool<K,T>,
    GenericKeyedObjectPoolMBean<K> {

    //--- constructors -----------------------------------------------

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using defaults and no
     * factory.
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory<K,T> factory) {
        this(factory, new GenericKeyedObjectPoolConfig());
    }

    /**
     * Create a new <code>GenericKeyedObjectPool</code> using a specific
     * configuration.
     * 
     * @param config    The configuration to use for this pool instance. The
     *                  configuration is used by value. Subsequent changes to
     *                  the configuration object will not be reflected in the
     *                  pool.
     */
    public GenericKeyedObjectPool(KeyedPoolableObjectFactory<K,T> factory,
            GenericKeyedObjectPoolConfig config) {
        // Copy the settings from the config
        this.factory = factory;
        setConfig(config);

        startEvictor(getMinEvictableIdleTimeMillis());

        initStats();

        ObjectName onameTemp = null;
        // JMX Registration
        if (config.isJmxEnabled()) {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            String jmxNamePrefix = config.getJmxNamePrefix();
            int i = 1;
            boolean registered = false;
            while (!registered) {
                try {
                    ObjectName oname =
                        new ObjectName(ONAME_BASE + jmxNamePrefix + i);
                    mbs.registerMBean(this, oname);
                    onameTemp = oname;
                    registered = true;
                } catch (MalformedObjectNameException e) {
                    if (GenericObjectPoolConfig.DEFAULT_JMX_NAME_PREFIX.equals(
                            jmxNamePrefix)) {
                        // Shouldn't happen. Skip registration if it does.
                        registered = true;
                    } else {
                        // Must be an invalid name prefix. Use the default
                        // instead.
                        jmxNamePrefix =
                            GenericObjectPoolConfig.DEFAULT_JMX_NAME_PREFIX;
                    }
                } catch (InstanceAlreadyExistsException e) {
                    // Increment the index and try again
                    i++;
                } catch (MBeanRegistrationException e) {
                    // Shouldn't happen. Skip registration if it does.
                    registered = true;
                } catch (NotCompliantMBeanException e) {
                    // Shouldn't happen. Skip registration if it does.
                    registered = true;
                }
            }
        }
        this.oname = onameTemp;
    }

    //--- configuration methods --------------------------------------

    /**
     * Returns the cap on the number of object instances allocated by the pool
     * (checked out or idle),  per key.
     * A negative value indicates no limit.
     *
     * @return the cap on the number of active instances per key.
     * @see #setMaxTotalPerKey
     */
    public int getMaxTotalPerKey() {
        return maxTotalPerKey;
    }

    /**
     * Sets the cap on the number of object instances managed by the pool per key.
     * @param maxTotalPerKey The cap on the number of object instances per key.
     * Use a negative value for no limit.
     *
     * @see #getMaxTotalPerKey
     */
    public void setMaxTotalPerKey(int maxTotalPerKey) {
        this.maxTotalPerKey = maxTotalPerKey;
    }

    /**
     * Returns the overall maximum number of objects (across pools) that can
     * exist at one time. A negative value indicates no limit.
     * @return the maximum number of instances in circulation at one time.
     * @see #setMaxTotal
     */
    public int getMaxTotal() {
        return maxTotal;
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
    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    /**
     * Returns whether to block when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @return true if the pool should block
     * @see #setBlockWhenExhausted
     */
    public boolean getBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    /**
     * Sets whether to block when the {@link #borrowObject} method
     * is invoked when the pool is exhausted (the maximum number
     * of "active" objects has been reached).
     *
     * @param shouldBlock true if the pool should block
     * @see #getBlockWhenExhausted
     */
    public void setBlockWhenExhausted(boolean shouldBlock) {
        blockWhenExhausted = shouldBlock;
    }


    /**
     * Returns the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getBlockWhenExhausted} is true.
     *
     * When less than 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @return the maximum number of milliseconds borrowObject will block.
     * @see #setMaxWait
     * @see #setBlockWhenExhausted
     */
    public long getMaxWait() {
        return maxWait;
    }

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getBlockWhenExhausted} is true.
     *
     * When less than 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @param maxWait the maximum number of milliseconds borrowObject will block or negative for indefinitely.
     * @see #getMaxWait
     * @see #setBlockWhenExhausted
     */
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * Returns the cap on the number of "idle" instances per key.
     * @return the maximum number of "idle" instances that can be held
     * in a given keyed pool.
     * @see #setMaxIdlePerKey
     */
    public int getMaxIdlePerKey() {
        return maxIdlePerKey;
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
     * @see #getMaxIdlePerKey
     */
    public void setMaxIdlePerKey(int maxIdle) {
        maxIdlePerKey = maxIdle;
    }

    /**
     * Sets the minimum number of idle objects to maintain in each of the keyed
     * pools. This setting has no effect unless
     * <code>timeBetweenEvictionRunsMillis > 0</code> and attempts to ensure
     * that each pool has the required minimum number of instances are only
     * made during idle object eviction runs.
     * @param poolSize - The minimum size of the each keyed pool
     * @since Pool 1.3
     * @see #getMinIdlePerKey
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setMinIdlePerKey(int poolSize) {
        minIdlePerKey = poolSize;
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
    public int getMinIdlePerKey() {
        return minIdlePerKey;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @return <code>true</code> if objects are validated before being borrowed.
     * @see #setTestOnBorrow
     */
    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @param testOnBorrow whether object should be validated before being returned by borrowObject.
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @return <code>true</code> when objects will be validated before being returned.
     * @see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @param testOnReturn <code>true</code> so objects will be validated before being returned.
     * @see #getTestOnReturn
     */
    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
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
    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
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
    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        startEvictor(timeBetweenEvictionRunsMillis);
    }

    /**
     * Returns the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     *
     * @return number of objects to examine each eviction run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    /**
     * Sets the max number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, 
     * <code>ceil({@link #getNumIdle()})/abs({@link #getNumTestsPerEvictionRun})</code>
     * tests will be run.  I.e., when the value is <code>-n</code>, roughly one <code>n</code>th of the
     * idle objects will be tested per run.  When the value is positive, the number of tests
     * actually performed in each run will be the minimum of this value and the number of instances
     * idle in the pools.
     *
     * @param numTestsPerEvictionRun number of objects to examine each eviction run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
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
    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * @param minEvictableIdleTimeMillis minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @return <code>true</code> when objects are validated when borrowed.
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @param testWhileIdle <code>true</code> so objects are validated when borrowed.
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    /**
     * Sets the configuration.
     * @param conf the new configuration to use.
     * @see GenericKeyedObjectPoolConfig
     */
    public void setConfig(GenericKeyedObjectPoolConfig conf) {
        setLifo(conf.getLifo());
        setMaxIdlePerKey(conf.getMaxIdlePerKey());
        setMaxTotalPerKey(conf.getMaxTotalPerKey());
        setMaxTotal(conf.getMaxTotal());
        setMinIdlePerKey(conf.getMinIdlePerKey());
        setMaxWait(conf.getMaxWait());
        setBlockWhenExhausted(conf.getBlockWhenExhausted());
        setTestOnBorrow(conf.getTestOnBorrow());
        setTestOnReturn(conf.getTestOnReturn());
        setTestWhileIdle(conf.getTestWhileIdle());
        setNumTestsPerEvictionRun(conf.getNumTestsPerEvictionRun());
        setMinEvictableIdleTimeMillis(conf.getMinEvictableIdleTimeMillis());
        setTimeBetweenEvictionRunsMillis(conf.getTimeBetweenEvictionRunsMillis());
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
     public boolean getLifo() {
         return lifo;
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
     public void setLifo(boolean lifo) {
         this.lifo = lifo;
     }

     /**
      * Obtain a reference to the factory used to create, destroy and validate
      * the objects used by this pool.
      *  
      * @return the factory
      */
     public KeyedPoolableObjectFactory<K, T> getFactory() {
         return factory;
     }
     
     
    //-- ObjectPool methods ------------------------------------------

    /**
     * <p>Borrows an object from the keyed pool associated with the given key.</p>
     * 
     * <p>If there is an idle instance available in the pool associated with the given key, then
     * either the most-recently returned (if {@link #getLifo() lifo} == true) or "oldest" (lifo == false)
     * instance sitting idle in the pool will be activated and returned.  If activation fails, or
     * {@link #getTestOnBorrow() testOnBorrow} is set to true and validation fails, the instance is destroyed and the
     * next available instance is examined.  This continues until either a valid instance is returned or there
     * are no more idle instances available.</p>
     * 
     * <p>If there are no idle instances available in the pool associated with the given key, behavior
     * depends on the {@link #getMaxTotalPerKey() maxTotalPerKey}, {@link #getMaxTotal() maxTotal}, and (if applicable)
     * {@link #getBlockWhenExhausted()} and {@link #getMaxWait() maxWait} settings. If the
     * number of instances checked out from the pool under the given key is less than <code>maxTotalPerKey</code> and
     * the total number of instances in circulation (under all keys) is less than <code>maxTotal</code>, a new instance
     * is created, activated and (if applicable) validated and returned to the caller.</p>
     * 
     * <p>If the associated keyed pool is exhausted (no available idle instances and no capacity to create new ones),
     * this method will either block ({@link #getBlockWhenExhausted()} is true) or throw a <code>NoSuchElementException</code>
     * ({@link #getBlockWhenExhausted()} is false).
     * The length of time that this method will block when {@link #getBlockWhenExhausted()} is true
     * is determined by the {@link #getMaxWait() maxWait} property.</p>
     * 
     * <p>When the pool is exhausted, multiple calling threads may be simultaneously blocked waiting for instances
     * to become available.  As of pool 1.5, a "fairness" algorithm has been implemented to ensure that threads receive
     * available instances in request arrival order.</p>
     * 
     * @param key pool key
     * @return object instance from the keyed pool
     * @throws NoSuchElementException if a keyed object instance cannot be returned.
     */
    public T borrowObject(K key) throws Exception {
        return borrowObject(key, getMaxWait());
    }
     
    /**
     * <p>Borrows an object from the keyed pool associated with the given key
     * using a user specific waiting time which only applies if
     * {@link #getBlockWhenExhausted()} is true.</p>
     * 
     * @param key pool key
     * @param borrowMaxWait
     * @return object instance from the keyed pool
     * @throws NoSuchElementException if a keyed object instance cannot be returned.
     */
    public T borrowObject(K key, long borrowMaxWait) throws Exception {

        assertOpen();

        PooledObject<T> p = null;

        // Get local copy of current config so it is consistent for entire
        // method execution
        boolean blockWhenExhausted = getBlockWhenExhausted();

        boolean create;
        long waitTime = 0;
        ObjectDeque<T> objectDeque = register(key);
        
        try {
            while (p == null) {
                create = false;
                if (blockWhenExhausted) {
                    if (objectDeque != null) {
                        p = objectDeque.getIdleObjects().pollFirst();
                    }
                    if (p == null) {
                        create = true;
                        p = create(key);
                    }
                    if (p == null && objectDeque != null) {
                        if (borrowMaxWait < 0) {
                            p = objectDeque.getIdleObjects().takeFirst();
                        } else {
                            waitTime = System.currentTimeMillis();
                            p = objectDeque.getIdleObjects().pollFirst(
                                    borrowMaxWait, TimeUnit.MILLISECONDS);
                            waitTime = System.currentTimeMillis() - waitTime;
                        }
                    }
                    if (p == null) {
                        throw new NoSuchElementException(
                                "Timeout waiting for idle object");
                    }
                    if (!p.allocate()) {
                        p = null;
                    }
                } else {
                    if (objectDeque != null) {
                        p = objectDeque.getIdleObjects().pollFirst();
                    }
                    if (p == null) {
                        create = true;
                        p = create(key);
                    }
                    if (p == null) {
                        throw new NoSuchElementException("Pool exhausted");
                    }
                    if (!p.allocate()) {
                        p = null;
                    }
                }
    
                if (p != null) {
                    try {
                        factory.activateObject(key, p.getObject());
                    } catch (Exception e) {
                        try {
                            destroy(key, p, true);
                        } catch (Exception e1) {
                            // Ignore - activation failure is more important
                        }
                        p = null;
                        if (create) {
                            NoSuchElementException nsee = new NoSuchElementException(
                                    "Unable to activate object");
                            nsee.initCause(e);
                            throw nsee;
                        }
                    }
                    if (p != null && getTestOnBorrow()) {
                        boolean validate = false;
                        Throwable validationThrowable = null;
                        try {
                            validate = factory.validateObject(key, p.getObject());
                        } catch (Throwable t) {
                            PoolUtils.checkRethrow(t);
                        }
                        if (!validate) {
                            try {
                                destroy(key, p, true);
                                destroyedByBorrowValidationCount.incrementAndGet();
                            } catch (Exception e) {
                                // Ignore - validation failure is more important
                            }
                            p = null;
                            if (create) {
                                NoSuchElementException nsee = new NoSuchElementException(
                                        "Unable to validate object");
                                nsee.initCause(validationThrowable);
                                throw nsee;
                            }
                        }
                    }
                }
            }
        } finally {
            deregister(key);
        }
        
        borrowedCount.incrementAndGet();
        synchronized (idleTimes) {
            idleTimes.add(Long.valueOf(p.getIdleTimeMillis()));
            idleTimes.poll();
        }
        synchronized (waitTimes) {
            waitTimes.add(Long.valueOf(waitTime));
            waitTimes.poll();
        }
        synchronized (maxBorrowWaitTimeMillisLock) {
            if (waitTime > maxBorrowWaitTimeMillis) {
                maxBorrowWaitTimeMillis = waitTime;
            }
        }
        return p.getObject();
    }


     /**
      * <p>Returns an object to a keyed pool.</p>
      * 
      * <p>For the pool to function correctly, the object instance <strong>must</strong> have been borrowed
      * from the pool (under the same key) and not yet returned. Repeated <code>returnObject</code> calls on
      * the same object/key pair (with no <code>borrowObject</code> calls in between) will result in multiple
      * references to the object in the idle instance pool.</p>
      * 
      * <p>If {@link #getMaxIdlePerKey() maxIdle} is set to a positive value and the number of idle instances under the given
      * key has reached this value, the returning instance is destroyed.</p>
      * 
      * <p>If {@link #getTestOnReturn() testOnReturn} == true, the returning instance is validated before being returned
      * to the idle instance pool under the given key.  In this case, if validation fails, the instance is destroyed.</p>
      * 
      * @param key pool key
      * @param t instance to return to the keyed pool
      * @throws Exception
      */
     public void returnObject(K key, T t) throws Exception {
         
         ObjectDeque<T> objectDeque = poolMap.get(key);
         
         PooledObject<T> p = objectDeque.getAllObjects().get(t);
         
         if (p == null) {
             throw new IllegalStateException(
                     "Returned object not currently part of this pool");
         }

         long activeTime = p.getActiveTimeMillis();

         if (getTestOnReturn()) {
             if (!factory.validateObject(key, t)) {
                 try {
                     destroy(key, p, true);
                 } catch (Exception e) {
                     // TODO - Ignore?
                 }
                 updateStatsReturn(activeTime);
                 return;
             }
         }

         try {
             factory.passivateObject(key, t);
         } catch (Exception e1) {
             try {
                 destroy(key, p, true);
             } catch (Exception e) {
                 // TODO - Ignore?
             }
             updateStatsReturn(activeTime);
             return;
         }

         if (!p.deallocate()) {
             throw new IllegalStateException(
                     "Object has already been retured to this pool");
         }

         int maxIdle = getMaxIdlePerKey();
         LinkedBlockingDeque<PooledObject<T>> idleObjects =
             objectDeque.getIdleObjects();

         if (isClosed() || maxIdle > -1 && maxIdle <= idleObjects.size()) {
             try {
                 destroy(key, p, true);
             } catch (Exception e) {
                 // TODO - Ignore?
             }
         } else {
             if (getLifo()) {
                 idleObjects.addFirst(p);
             } else {
                 idleObjects.addLast(p);
             }
         }
 
         if (hasBorrowWaiters()) {
             reuseCapacity();
         }

         updateStatsReturn(activeTime);
     }


     private void updateStatsReturn(long activeTime) {
         returnedCount.incrementAndGet();
         synchronized (activeTimes) {
             activeTimes.add(Long.valueOf(activeTime));
             activeTimes.poll();
         }
     }

     
     /**
      * {@inheritDoc}
      * <p>Activation of this method decrements the active count associated with
      * the given keyed pool  and attempts to destroy <code>obj.</code></p>
      * 
      * @param key pool key
      * @param obj instance to invalidate
      * @throws Exception if an exception occurs destroying the object
      */
     public void invalidateObject(K key, T obj) throws Exception {
         
         ObjectDeque<T> objectDeque = poolMap.get(key);
         
         PooledObject<T> p = objectDeque.getAllObjects().get(obj);
         if (p == null) {
             throw new IllegalStateException(
                     "Object not currently part of this pool");
         }
         destroy(key, p, true);
     }


     /**
      * Clears any objects sitting idle in the pool by removing them from the
      * idle instance pool and then invoking the configured PoolableObjectFactory's
      * {@link KeyedPoolableObjectFactory#destroyObject(Object, Object)} method on
      * each idle instance.
      *  
      * <p> Implementation notes:
      * <ul><li>This method does not destroy or effect in any way instances that are
      * checked out when it is invoked.</li>
      * <li>Invoking this method does not prevent objects being
      * returned to the idle instance pool, even during its execution. It locks
      * the pool only during instance removal. Additional instances may be returned
      * while removed items are being destroyed.</li>
      * <li>Exceptions encountered destroying idle instances are swallowed.</li></ul></p>
      */
     public void clear() {
         Iterator<K> iter = poolMap.keySet().iterator();
         
         while (iter.hasNext()) {
             clear(iter.next());
         }
     }


     /**
      * Clears the specified pool, removing all pooled instances corresponding
      * to the given <code>key</code>.
      *
      * @param key the key to clear
      */
     public void clear(K key) {
         
         register(key);
         
         try {
             ObjectDeque<T> objectDeque = poolMap.get(key);
             if (objectDeque == null) {
                 return;
             }
             LinkedBlockingDeque<PooledObject<T>> idleObjects =
                     objectDeque.getIdleObjects();
             
             PooledObject<T> p = idleObjects.poll();
    
             while (p != null) {
                 try {
                     destroy(key, p, true);
                 } catch (Exception e) {
                     // TODO - Ignore?
                 }
                 p = idleObjects.poll();
             }
         } finally {
             deregister(key);
         }
     }


     /**
      * Returns the total number of instances current borrowed from this pool but not yet returned.
      *
      * @return the total number of instances currently borrowed from this pool
      */
     public int getNumActive() {
         return numTotal.get() - getNumIdle();
     }

     /**
      * Returns the total number of instances currently idle in this pool.
      *
      * @return the total number of instances currently idle in this pool
      */
     public int getNumIdle() {
         Iterator<ObjectDeque<T>> iter = poolMap.values().iterator();
         int result = 0;
         
         while (iter.hasNext()) {
             result += iter.next().getIdleObjects().size();
         }

         return result;
     }

     /**
      * Returns the number of instances currently borrowed from but not yet returned
      * to the pool corresponding to the given <code>key</code>.
      *
      * @param key the key to query
      * @return the number of instances corresponding to the given <code>key</code> currently borrowed in this pool
      */
     public int getNumActive(K key) {
         final ObjectDeque<T> objectDeque = poolMap.get(key);
         if (objectDeque != null) {
             return objectDeque.getAllObjects().size() -
                     objectDeque.getIdleObjects().size();
         } else {
             return 0;
         }
     }

     /**
      * Returns the number of instances corresponding to the given <code>key</code> currently idle in this pool.
      *
      * @param key the key to query
      * @return the number of instances corresponding to the given <code>key</code> currently idle in this pool
      */
     public int getNumIdle(K key) {
         final ObjectDeque<T> objectDeque = poolMap.get(key);
         return objectDeque != null ? objectDeque.getIdleObjects().size() : 0;
     }


     /**
      * <p>Closes the keyed object pool.  Once the pool is closed, {@link #borrowObject(Object)}
      * will fail with IllegalStateException, but {@link #returnObject(Object, Object)} and
      * {@link #invalidateObject(Object, Object)} will continue to work, with returned objects
      * destroyed on return.</p>
      * 
      * <p>Destroys idle instances in the pool by invoking {@link #clear()}.</p> 
      * 
      * @throws Exception
      */
     public void close() throws Exception {
         if (isClosed()) {
             return;
         }

         synchronized (closeLock) {
             if (isClosed()) {
                 return;
             }

             closed = true;
             clear();
             evictionIterator = null;
             evictionKeyIterator = null;
             startEvictor(-1L);
             if (oname != null) {
                 ManagementFactory.getPlatformMBeanServer().unregisterMBean(
                         oname);
             }
         }

     }

     /**
      * Has this pool instance been closed.
      * @return <code>true</code> when this pool has been closed.
      * @since Pool 1.4
      */
     public boolean isClosed() {
         return closed;
     }

     /**
      * Throws an <code>IllegalStateException</code> when this pool has been closed.
      * @throws IllegalStateException when this pool has been closed.
      * @see #isClosed()
      * @since Pool 1.4
      */
     protected void assertOpen() throws IllegalStateException {
         if(isClosed()) {
             throw new IllegalStateException("Pool not open");
         }
     }

     /** Whether or not the pool is closed */
     private volatile boolean closed = false;

     
     /**
      * Clears oldest 15% of objects in pool.  The method sorts the
      * objects into a TreeMap and then iterates the first 15% for removal.
      * 
      * @since Pool 1.3
      */
     public void clearOldest() {

         // build sorted map of idle objects
         final Map<PooledObject<T>, K> map = new TreeMap<PooledObject<T>, K>();

         for (K k : poolMap.keySet()) {
             final LinkedBlockingDeque<PooledObject<T>> idleObjects =
                 poolMap.get(k).getIdleObjects();
             for (PooledObject<T> p : idleObjects) {
                 // each item into the map using the PooledObject object as the
                 // key. It then gets sorted based on the idle time
                 map.put(p, k);
             }
         }

         // Now iterate created map and kill the first 15% plus one to account
         // for zero
         int itemsToRemove = ((int) (map.size() * 0.15)) + 1;
         Iterator<Map.Entry<PooledObject<T>, K>> iter =
             map.entrySet().iterator();

         while (iter.hasNext() && itemsToRemove > 0) {
             Map.Entry<PooledObject<T>, K> entry = iter.next();
             // kind of backwards on naming.  In the map, each key is the
             // PooledObject because it has the ordering with the timestamp
             // value.  Each value that the key references is the key of the
             // list it belongs to.
             K key = entry.getValue();
             PooledObject<T> p = entry.getKey();
             // Assume the destruction succeeds
             boolean destroyed = true;
             try {
                 destroyed = destroy(key, p, false);
            } catch (Exception e) {
                // TODO - Ignore?
            }
            if (destroyed) {
                itemsToRemove--;
            }
        }
    }
    
    /**
     * Attempt to create one new instance to serve from the most heavily
     * loaded pool that can add a new instance.
     * 
     * This method exists to ensure liveness in the pool when threads are
     * parked waiting and capacity to create instances under the requested keys
     * subsequently becomes available.
     * 
     * This method is not guaranteed to create an instance and its selection
     * of the most loaded pool that can create an instance may not always be
     * correct, since it does not lock the pool and instances may be created,
     * borrowed, returned or destroyed by other threads while it is executing.
     * 
     * @return true if an instance is created and added to a pool
     */
    private boolean reuseCapacity() {
        final int maxTotalPerKey = getMaxTotalPerKey();
   
        // Find the most loaded pool that could take a new instance
        int maxQueueLength = 0;
        LinkedBlockingDeque<PooledObject<T>> mostLoaded = null;
        K loadedKey = null;
        for (K k : poolMap.keySet()) {
            final ObjectDeque<T> deque = poolMap.get(k);
            if (deque != null) {
                final LinkedBlockingDeque<PooledObject<T>> pool = deque.getIdleObjects();
                final int queueLength = pool.getTakeQueueLength();
                if (getNumActive(k) < maxTotalPerKey && queueLength > maxQueueLength) {
                    maxQueueLength = queueLength;
                    mostLoaded = pool; 
                    loadedKey = k;
                }
            }
        }
        
        // Attempt to add an instance to the most loaded pool
        boolean success = false;
        if (mostLoaded != null) {
            register(loadedKey);
            try {
                PooledObject<T> p = create(loadedKey);
                if (p != null) {
                    addIdleObject(loadedKey, p);
                    success = true;
                }
            } catch (Exception ex) {
                // Swallow and return false
            } finally {
                deregister(loadedKey);
            }
        }
        return success;   
    }
    
    /**
     * Returns true if there are threads parked waiting to borrow instances
     * from at least one of the keyed pools.
     * 
     * @return true if {@link #reuseCapacity()} would be useful
     */
    private boolean hasBorrowWaiters() {
        for (K k : poolMap.keySet()) {
            final ObjectDeque<T> deque = poolMap.get(k);
            if (deque != null) {
                final LinkedBlockingDeque<PooledObject<T>> pool =
                    deque.getIdleObjects();
                if(pool.hasTakeWaiters()) {
                    return true;
                }
            }
        }
        return false;
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
    public void evict() throws Exception {
        assertOpen();

        if (getNumIdle() == 0) {
            return;
        }

        boolean testWhileIdle = getTestWhileIdle();
        long idleEvictTime = Long.MAX_VALUE;
         
        if (getMinEvictableIdleTimeMillis() > 0) {
            idleEvictTime = getMinEvictableIdleTimeMillis();
        }

        PooledObject<T> underTest = null;
        LinkedBlockingDeque<PooledObject<T>> idleObjects = null;
         
        for (int i = 0, m = getNumTests(); i < m; i++) {
            if(evictionIterator == null || !evictionIterator.hasNext()) {
                if (evictionKeyIterator == null ||
                        !evictionKeyIterator.hasNext()) {
                    List<K> keyCopy = new ArrayList<K>();
                    keyCopy.addAll(poolKeyList);
                    evictionKeyIterator = keyCopy.iterator();
                }
                while (evictionKeyIterator.hasNext()) {
                    evictionKey = evictionKeyIterator.next();
                    ObjectDeque<T> objectDeque = poolMap.get(evictionKey);
                    if (objectDeque == null) {
                        continue;
                    }
                    idleObjects = objectDeque.getIdleObjects();
                    
                    if (getLifo()) {
                        evictionIterator = idleObjects.descendingIterator();
                    } else {
                        evictionIterator = idleObjects.iterator();
                    }
                    if (evictionIterator.hasNext()) {
                        break;
                    }
                    evictionIterator = null;
                }
            }
            if (evictionIterator == null) {
                // Pools exhausted
                return;
            }
            try {
                underTest = evictionIterator.next();
            } catch (NoSuchElementException nsee) {
                // Object was borrowed in another thread
                // Don't count this as an eviction test so reduce i;
                i--;
                evictionIterator = null;
                continue;
            }

            if (!underTest.startEvictionTest()) {
                // Object was borrowed in another thread
                // Don't count this as an eviction test so reduce i;
                i--;
                continue;
            }

            if (idleEvictTime < underTest.getIdleTimeMillis()) {
                destroy(evictionKey, underTest, true);
                destroyedByEvictorCount.incrementAndGet();
            } else {
                if (testWhileIdle) {
                    boolean active = false;
                    try {
                        factory.activateObject(evictionKey, 
                                underTest.getObject());
                        active = true;
                    } catch (Exception e) {
                        destroy(evictionKey, underTest, true);
                        destroyedByEvictorCount.incrementAndGet();
                    }
                    if (active) {
                        if (!factory.validateObject(evictionKey,
                                underTest.getObject())) {
                            destroy(evictionKey, underTest, true);
                            destroyedByEvictorCount.incrementAndGet();
                        } else {
                            try {
                                factory.passivateObject(evictionKey,
                                        underTest.getObject());
                            } catch (Exception e) {
                                destroy(evictionKey, underTest, true);
                                destroyedByEvictorCount.incrementAndGet();
                            }
                        }
                    }
                }
                if (!underTest.endEvictionTest(idleObjects)) {
                    // TODO - May need to add code here once additional states
                    // are used
                }
            }
        }
    }

     
    private PooledObject<T> create(K key) throws Exception {
        int maxTotalPerKey = getMaxTotalPerKey(); // Per key
        int maxTotal = getMaxTotal();   // All keys

        // Check against the overall limit
        boolean loop = true;
        
        while (loop) {
            int newNumTotal = numTotal.incrementAndGet();
            if (maxTotal > -1 && newNumTotal > maxTotal) {
                numTotal.decrementAndGet();
                if (getNumIdle() == 0) {
                    return null;
                } else {
                    clearOldest();
                }
            } else {
                loop = false;
            }
        }
         
        ObjectDeque<T> objectDeque = poolMap.get(key);
        long newCreateCount = objectDeque.getCreateCount().incrementAndGet();

        // Check against the per key limit
        if (maxTotalPerKey > -1 && newCreateCount > maxTotalPerKey ||
                newCreateCount > Integer.MAX_VALUE) {
            numTotal.decrementAndGet();
            objectDeque.getCreateCount().decrementAndGet();
            return null;
        }
         

        T t = null;
        try {
            t = factory.makeObject(key);
        } catch (Exception e) {
            numTotal.decrementAndGet();
            throw e;
        }

        PooledObject<T> p = new PooledObject<T>(t);
        createdCount.incrementAndGet();
        objectDeque.getAllObjects().put(t, p);
        return p;
    }

    /**
     * Invalidate toDestroy and if it is idle under key or always is true, destroy it.
     * Return true if toDestroy is destroyed.
     * 
     * @param key pool key
     * @param toDestroy instance to invalidate and destroy if conditions are met
     * @param always true means instance will be destroyed regardless of idle pool membership
     * @return true iff toDestroy is destroyed
     * @throws Exception
     */
    private boolean destroy(K key, PooledObject<T> toDestroy, boolean always)
            throws Exception {
        
        register(key);

        try {
            ObjectDeque<T> objectDeque = poolMap.get(key);
            boolean isIdle = objectDeque.getIdleObjects().remove(toDestroy);
            
            if (isIdle || always) {
                objectDeque.getAllObjects().remove(toDestroy.getObject());
                toDestroy.invalidate();
        
                try {
                    factory.destroyObject(key, toDestroy.getObject());
                } finally {
                    objectDeque.getCreateCount().decrementAndGet();
                    destroyedCount.incrementAndGet();
                    numTotal.decrementAndGet();
                }
                return true;
            } else {
                return false;
            }
        } finally {
            deregister(key);
        }
    }

    private ObjectDeque<T> register(K k) {
        Lock lock = keyLock.readLock();
        ObjectDeque<T> objectDeque = null;
        try {
            lock.lock();
            objectDeque = poolMap.get(k);
            if (objectDeque == null) {
                // Upgrade to write lock
                lock.unlock();
                lock = keyLock.writeLock();
                lock.lock();
                objectDeque = poolMap.get(k);
                if (objectDeque == null) {
                    objectDeque = new ObjectDeque<T>();
                    objectDeque.getNumInterested().incrementAndGet();
                    poolMap.put(k, objectDeque);
                    poolKeyList.add(k);
                } else {
                    objectDeque.getNumInterested().incrementAndGet();
                }
            } else {
                objectDeque.getNumInterested().incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
        return objectDeque;
    }
    
    private void deregister(K k) {
        ObjectDeque<T> objectDeque;

        // TODO Think carefully about when a read lock is required
        objectDeque = poolMap.get(k);
        long numInterested = objectDeque.getNumInterested().decrementAndGet();
        if (numInterested == 0 && objectDeque.getCreateCount().get() == 0) {
            // Potential to remove key
            Lock lock = keyLock.writeLock();
            lock.lock();
            try {
                if (objectDeque.getCreateCount().get() == 0 &&
                        objectDeque.getNumInterested().get() == 0) {
                    poolMap.remove(k);
                    poolKeyList.remove(k);
                }
            } finally {
                lock.unlock();
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
    private void ensureMinIdle() throws Exception {
        int minIdle = getMinIdlePerKey();
        if (minIdle < 1) {
            return;
        }

        for (K k : poolMap.keySet()) {
            ensureMinIdle(k);
        }
    }


    /**
     * Re-creates any needed objects to maintain the minimum levels of
     * pooled objects for the specified key.
     *
     * This method uses {@link #calculateDeficit} to calculate the number
     * of objects to be created. {@link #calculateDeficit} can be overridden to
     * provide a different method of calculating the number of objects to be
     * created.
     * @param key The key to process
     * @throws Exception If there was an error whilst creating the pooled objects
     */
    private void ensureMinIdle(K key) throws Exception {
        int minIdle = getMinIdlePerKey();
        if (minIdle < 1) {
            return;
        }

        // Calculate current pool objects
        ObjectDeque<T> objectDeque = poolMap.get(key);

        // this method isn't synchronized so the
        // calculateDeficit is done at the beginning
        // as a loop limit and a second time inside the loop
        // to stop when another thread already returned the
        // needed objects
        int deficit = calculateDeficit(objectDeque);

        for (int i = 0; i < deficit && calculateDeficit(objectDeque) > 0; i++) {
            addObject(key);
        }
    }

    
    /**
     * <p>Adds an object to the keyed pool.</p>
     * 
     * <p>Validates the object if testOnReturn == true and passivates it before returning it to the pool.
     * if validation or passivation fails, or maxIdle is set and there is no room in the pool, the instance
     * is destroyed.</p>
     * 
     * <p>Calls {@link #allocate()} on successful completion</p>
     * 
     * @param key pool key
     * @param p instance to add to the keyed pool
     * @throws Exception
     */
    private void addIdleObject(K key, PooledObject<T> p) throws Exception {

        if (p != null) {
            factory.passivateObject(key, p.getObject());
            LinkedBlockingDeque<PooledObject<T>> idleObjects =
                    poolMap.get(key).getIdleObjects();
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
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
     * @throws IllegalStateException when no factory has been set or after {@link #close} has been
     * called on this pool.
     */
    public void addObject(K key) throws Exception {
        assertOpen();
        if (factory == null) {
            throw new IllegalStateException("Cannot add objects without a factory.");
        }
        register(key);
        try {
            PooledObject<T> p = create(key);
            addIdleObject(key, p);
        } finally {
            deregister(key);
        }
    }

    /**
     * Registers a key for pool control and ensures that {@link #getMinIdlePerKey()}
     * idle instances are created.
     *
     * @param key - The key to register for pool control.
     * @since Pool 1.3
     */
    public void preparePool(K key) throws Exception {
        ensureMinIdle(key);
    }

    //--- non-public methods ----------------------------------------

    /**
     * Start the eviction thread or service, or when
     * <code>delay</code> is non-positive, stop it
     * if it is already running.
     *
     * @param delay milliseconds between evictor runs.
     */
    // Needs to be final; see POOL-195. Make protected method final as it is called from constructor.
    protected final synchronized void startEvictor(long delay) {
        if (null != evictor) {
            EvictionTimer.cancel(evictor);
            evictor = null;
        }
        if (delay > 0) {
            evictor = new Evictor();
            EvictionTimer.schedule(evictor, delay, delay);
        }
    }

    /**
     * Returns pool info including {@link #getNumActive()}, {@link #getNumIdle()}
     * and currently defined keys.
     * 
     * @return string containing debug information
     */
    String debugInfo() {
        StringBuilder buf = new StringBuilder();
        buf.append("Active: ").append(getNumActive()).append("\n");
        buf.append("Idle: ").append(getNumIdle()).append("\n");
        for (Entry<K,ObjectDeque<T>> entry : poolMap.entrySet()) {
            buf.append(entry.getKey());
            buf.append(": ");
            buf.append(entry.getValue());
            buf.append("\n");
        }
        return buf.toString();
    }

    /** 
     * Returns the number of tests to be performed in an Evictor run,
     * based on the current values of <code>_numTestsPerEvictionRun</code>
     * and <code>_totalIdle</code>.
     * 
     * @see #setNumTestsPerEvictionRun
     * @return the number of tests for the Evictor to run
     */
    private int getNumTests() {
        int totalIdle = getNumIdle();
        int numTests = getNumTestsPerEvictionRun();
        if (numTests >= 0) {
            return Math.min(numTests, totalIdle);
        }
        return(int)(Math.ceil(totalIdle/Math.abs((double)numTests)));
    }

    /**
     * This returns the number of objects to create during the pool
     * sustain cycle. This will ensure that the minimum number of idle
     * instances is maintained without going past the maxTotalPerKey value.
     * 
     * @param pool the ObjectPool to calculate the deficit for
     * @return The number of objects to be created
     */
    private int calculateDeficit(ObjectDeque<T> objectDeque) {
        
        if (objectDeque == null) {
            return getMinIdlePerKey();
        }

        // Used more than once so keep a local copy so the value is consistent
        int maxTotal = getMaxTotal();
        int maxTotalPerKey = getMaxTotalPerKey();

        int objectDefecit = 0;

        // Calculate no of objects needed to be created, in order to have
        // the number of pooled objects < maxTotalPerKey();
        objectDefecit = getMinIdlePerKey() - objectDeque.getIdleObjects().size();
        if (maxTotalPerKey > 0) {
            int growLimit = Math.max(0,
                    maxTotalPerKey - objectDeque.getIdleObjects().size());
            objectDefecit = Math.min(objectDefecit, growLimit);
        }

        // Take the maxTotal limit into account
        if (maxTotal > 0) {
            int growLimit = Math.max(0, maxTotal - getNumActive() - getNumIdle());
            objectDefecit = Math.min(objectDefecit, growLimit);
        }

        return objectDefecit;
    }

    
    //--- JMX specific attributes ----------------------------------------------

    private void initStats() {
        for (int i = 0; i < AVERAGE_TIMING_STATS_CACHE_SIZE; i++) {
            activeTimes.add(null);
            idleTimes.add(null);
            waitTimes.add(null);
        }
    }

    private long getMeanFromStatsCache(LinkedList<Long> cache) {
        List<Long> times = new ArrayList<Long>(AVERAGE_TIMING_STATS_CACHE_SIZE);
        synchronized (cache) {
            times.addAll(cache);
        }
        double result = 0;
        int counter = 0;
        Iterator<Long> iter = times.iterator();
        while (iter.hasNext()) {
            Long time = iter.next();
            if (time != null) {
                counter++;
                result = result * ((counter - 1) / (double) counter) +
                        time.longValue()/(double) counter;
            }
        }
        return (long) result;
    }

    public Map<String,Integer> getNumActivePerKey() {
        HashMap<String,Integer> result = new HashMap<String,Integer>();

        Iterator<Entry<K,ObjectDeque<T>>> iter = poolMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<K,ObjectDeque<T>> entry = iter.next();
            if (entry != null) {
                K key = entry.getKey();
                ObjectDeque<T> objectDequeue = entry.getValue();
                if (key != null && objectDequeue != null) {
                    result.put(key.toString(), Integer.valueOf(
                            objectDequeue.getAllObjects().size() -
                            objectDequeue.getIdleObjects().size()));
                }
            }
        }
        return result;
    }

    public long getBorrowedCount() {
        return borrowedCount.get();
    }

    public long getReturnedCount() {
        return returnedCount.get();
    }

    public long getCreatedCount() {
        return createdCount.get();
    }

    public long getDestroyedCount() {
        return destroyedCount.get();
    }

    public long getDestroyedByEvictorCount() {
        return destroyedByEvictorCount.get();
    }

    public long getDestroyedByBorrowValidationCount() {
        return destroyedByBorrowValidationCount.get();
    }

    public long getMeanActiveTimeMillis() {
        return getMeanFromStatsCache(activeTimes);
    }

    public long getMeanIdleTimeMillis() {
        return getMeanFromStatsCache(idleTimes);
    }

    public long getMeanBorrowWaitTimeMillis() {
        return getMeanFromStatsCache(waitTimes);
    }

    public long getMaxBorrowWaitTimeMillis() {
        return maxBorrowWaitTimeMillis;
    }

    //--- inner classes ----------------------------------------------

    /**
     * Maintains information on the per key queue for a given key.
     */
    private class ObjectDeque<S> {
        /** Idle instances */
        private final LinkedBlockingDeque<PooledObject<S>> idleObjects =
                new LinkedBlockingDeque<PooledObject<S>>();

        /** 
         * Number of instances created - number destroyed.
         * Invariant: createCount <= maxTotalPerKey
         */
        private AtomicInteger createCount = new AtomicInteger(0);

        /** All instances under management - checked out our idle in the pool. */
        private Map<S, PooledObject<S>> allObjects =
                new ConcurrentHashMap<S, PooledObject<S>>();

        /** 
         * Number of threads with registered interest in this key. 
         * register(K) increments this counter and deRegister(K) decrements it.
         * Invariant: empty keyed pool will not be dropped unless numInterested is 0.
         */
        private AtomicLong numInterested = new AtomicLong(0);
        
        /**
         * Returns the idle instance pool.
         * 
         * @return deque of idle instances
         */
        public LinkedBlockingDeque<PooledObject<S>> getIdleObjects() {
            return idleObjects;
        }
        
        /**
         * Returns the number of instances that have been created under this
         * this key minus the number that have been destroyed.
         * 
         * @return the number of instances (active or idle) currently being
         * managed by the pool under this key
         */
        public AtomicInteger getCreateCount() {
            return createCount;
        }
        
        /**
         * Returns the number of threads with registered interest in this key.
         * This keyed pool will not be dropped if empty unless this method returns 0.
         * 
         * @return number of threads that have registered, but not deregistered this key
         */
        public AtomicLong getNumInterested() {
            return numInterested;
        }
        
        /**
         * The full set of objects under management by this keyed pool.
         * 
         * Includes both idle instances and those checked out to clients.
         * The map is keyed on pooled instances.  Note: pooled instances
         * <em>must</em> be distinguishable by equals for this structure to
         * work properly.
         * 
         * @return map of pooled instances
         */
        public Map<S, PooledObject<S>> getAllObjects() {
            return allObjects;
        }
    }

    /**
     * The idle object evictor {@link TimerTask}.
     * @see GenericKeyedObjectPool#setTimeBetweenEvictionRunsMillis
     */
    private class Evictor extends TimerTask {
        /**
         * Run pool maintenance.  Evict objects qualifying for eviction and then
         * invoke {@link GenericKeyedObjectPool#ensureMinIdle()}.
         */
        @Override
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
            //Re-create idle instances.
            try {
                ensureMinIdle();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    //--- attributes -----------------------------------------------------------

    /**
     * The cap on the number of idle instances per key.
     * @see #setMaxIdle
     * @see #getMaxIdle
     */
    private int maxIdlePerKey = GenericKeyedObjectPoolConfig.DEFAULT_MAX_IDLE_PER_KEY;

    /**
     * The minimum no of idle objects per key.
     * @see #setMinIdle
     * @see #getMinIdle
     */
    private volatile int minIdlePerKey =
        GenericKeyedObjectPoolConfig.DEFAULT_MIN_IDLE_PER_KEY;

    /**
     * The cap on the number of active instances from the pool.
     * @see #setMaxTotalPerKey
     * @see #getMaxTotalPerKey
     */
    private int maxTotalPerKey =
        GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL_PER_KEY;

    /**
     * The cap on the total number of instances from the pool if non-positive.
     * @see #setMaxTotal
     * @see #getMaxTotal
     */
    private int maxTotal = GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL;

    /**
     * The maximum amount of time (in millis) the
     * {@link #borrowObject} method should block before throwing
     * an exception when the pool is exhausted and the
     * {@link #getBlockWhenExhausted} is true.
     *
     * When less than 0, the {@link #borrowObject} method
     * may block indefinitely.
     *
     * @see #setMaxWait
     * @see #getMaxWait
     * @see #setBlockWhenExhausted
     * @see #getBlockWhenExhausted
     */
    private long maxWait = GenericKeyedObjectPoolConfig.DEFAULT_MAX_WAIT;

    /**
     * When the {@link #borrowObject} method is invoked when the pool is
     * exhausted (the maximum number of "active" objects has been reached)
     * should the {@link #borrowObject} method block or not?
     *
     * @see #setBlockWhenExhausted
     * @see #getBlockWhenExhausted
     */
    private boolean blockWhenExhausted =
        GenericKeyedObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned by the {@link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * @see #setTestOnBorrow
     * @see #getTestOnBorrow
     */
    private volatile boolean testOnBorrow =
        GenericKeyedObjectPoolConfig.DEFAULT_TEST_ON_BORROW;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {@link #returnObject}.
     *
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private volatile boolean testOnReturn =
        GenericKeyedObjectPoolConfig.DEFAULT_TEST_ON_RETURN;

    /**
     * When <code>true</code>, objects will be
     * {@link org.apache.commons.pool2.PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #getTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private boolean testWhileIdle =
        GenericKeyedObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE;

    /**
     * The number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * @see #setTimeBetweenEvictionRunsMillis
     * @see #getTimeBetweenEvictionRunsMillis
     */
    private long timeBetweenEvictionRunsMillis =
        GenericKeyedObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

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
    private int numTestsPerEvictionRun =
        GenericKeyedObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

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
    private long minEvictableIdleTimeMillis =
        GenericKeyedObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /** Whether or not the pools behave as LIFO queues (last in first out) */
    private boolean lifo = GenericKeyedObjectPoolConfig.DEFAULT_LIFO;

    /** My {@link KeyedPoolableObjectFactory}. */
    final private KeyedPoolableObjectFactory<K,T> factory;

    /**
     * My idle object eviction {@link TimerTask}, if any.
     */
    private Evictor evictor = null; // @GuardedBy("this")

    /** My hash of pools (ObjectQueue). */
    private final Map<K,ObjectDeque<T>> poolMap =
            new ConcurrentHashMap<K,ObjectDeque<T>>();
    
    /** List of pool keys - used to control eviction order */
    private final List<K> poolKeyList = new ArrayList<K>();

    /** Lock used to manage adding/removing of keys */
    private final ReadWriteLock keyLock = new ReentrantReadWriteLock(true);

    /**
     * The combined count of the currently active objects for all keys and those
     * in the process of being created. Under load, it may exceed
     * {@link #maxTotal} but there will never be more than {@link #maxTotal}
     * created at any one time.
     */
    private final AtomicInteger numTotal = new AtomicInteger(0);
    
    /**
     * An iterator for {@link ObjectDeque#getIdleObjects()} that is used by the
     * evictor.
     */
    private Iterator<PooledObject<T>> evictionIterator = null;

    /**
     * An iterator for {@link #poolMap} entries.
     */
    private Iterator<K> evictionKeyIterator = null;
    
    /**
     * The key associated with the {@link ObjectDeque#getIdleObjects()}
     * currently being evicted.
     */
    private K evictionKey = null;

    /** Object used to ensure closed() is only called once. */
    private final Object closeLock = new Object();

    // JMX specific attributes
    private static final int AVERAGE_TIMING_STATS_CACHE_SIZE = 100;
    private final AtomicLong borrowedCount = new AtomicLong(0);
    private final AtomicLong returnedCount = new AtomicLong(0);
    private final AtomicLong createdCount = new AtomicLong(0);
    private final AtomicLong destroyedCount = new AtomicLong(0);
    private final AtomicLong destroyedByEvictorCount = new AtomicLong(0);
    private final AtomicLong destroyedByBorrowValidationCount = new AtomicLong(0);
    private final LinkedList<Long> activeTimes = new LinkedList<Long>(); // @GuardedBy("activeTimes") - except in initStats()
    private final LinkedList<Long> idleTimes = new LinkedList<Long>(); // @GuardedBy("activeTimes") - except in initStats()
    private final LinkedList<Long> waitTimes = new LinkedList<Long>(); // @GuardedBy("activeTimes") - except in initStats()

    private final Object maxBorrowWaitTimeMillisLock = new Object();
    private volatile long maxBorrowWaitTimeMillis = 0; // @GuardedBy("maxBorrowWaitTimeMillisLock")

    private final ObjectName oname;

    private static final String ONAME_BASE =
        "org.apache.commoms.pool2:type=GenericKeyedObjectPool,name=";
}
