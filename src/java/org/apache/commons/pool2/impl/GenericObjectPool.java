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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PoolableObjectFactory;

/**
 * A configurable {@link ObjectPool} implementation.
 * <p>
 * When coupled with the appropriate {@link PoolableObjectFactory},
 * <tt>GenericObjectPool</tt> provides robust pooling functionality for
 * arbitrary objects.
 * <p>
 * A <tt>GenericObjectPool</tt> provides a number of configurable parameters:
 * <ul>
 * <li>
 *    {@link #setMaxTotal <i>maxTotal</i>} controls the maximum number of
 * objects that can be allocated by the pool (checked out to clients, or idle
 * awaiting checkout) at a given time. When non-positive, there is no limit to
 * the number of objects that can be managed by the pool at one time. When
 * {@link #setMaxTotal <i>maxTotal</i>} is reached, the pool is said to be
 * exhausted. The default setting for this parameter is 8.</li>
 * <li>
 *    {@link #setMaxIdle <i>maxIdle</i>} controls the maximum number of objects
 * that can sit idle in the pool at any time. When negative, there is no limit
 * to the number of objects that may be idle at one time. The default setting
 * for this parameter is 8.</li>
 * <li>
 *    {@link #getBlockWhenExhausted} specifies the
 * behavior of the {@link #borrowObject} method when the pool is exhausted:
 * <ul>
 * <li>When {@link #getBlockWhenExhausted} is false,
 * {@link #borrowObject} will throw a {@link NoSuchElementException}</li>
 * <li>When {@link #getBlockWhenExhausted} is true,
 * {@link #borrowObject} will block (invoke
 * {@link Object#wait()}) until a new or idle object is available. If a positive
 * {@link #setMaxWait <i>maxWait</i>} value is supplied, then
 * {@link #borrowObject} will block for at most that many milliseconds, after
 * which a {@link NoSuchElementException} will be thrown. If {@link #setMaxWait
 * <i>maxWait</i>} is non-positive, the {@link #borrowObject} method will block
 * indefinitely.</li>
 * </ul>
 * The default {@link #getBlockWhenExhausted} is true
 * and the default <code>maxWait</code> setting is
 * -1. By default, therefore, <code>borrowObject</code> will block indefinitely
 * until an idle instance becomes available.</li>
 * <li>When {@link #setTestOnBorrow <i>testOnBorrow</i>} is set, the pool will
 * attempt to validate each object before it is returned from the
 * {@link #borrowObject} method. (Using the provided factory's
 * {@link PoolableObjectFactory#validateObject} method.) Objects that fail to
 * validate will be dropped from the pool, and a different object will be
 * borrowed. The default setting for this parameter is <code>false.</code></li>
 * <li>When {@link #setTestOnReturn <i>testOnReturn</i>} is set, the pool will
 * attempt to validate each object before it is returned to the pool in the
 * {@link #returnObject} method. (Using the provided factory's
 * {@link PoolableObjectFactory#validateObject} method.) Objects that fail to
 * validate will be dropped from the pool. The default setting for this
 * parameter is <code>false.</code></li>
 * </ul>
 * <p>
 * Optionally, one may configure the pool to examine and possibly evict objects
 * as they sit idle in the pool and to ensure that a minimum number of idle
 * objects are available. This is performed by an "idle object eviction" thread,
 * which runs asynchronously. Caution should be used when configuring this
 * optional feature. Eviction runs contend with client threads for access to
 * objects in the pool, so if they run too frequently performance issues may
 * result. The idle object eviction thread may be configured using the following
 * attributes:
 * <ul>
 * <li>
 *   {@link #setTimeBetweenEvictionRunsMillis
 * <i>timeBetweenEvictionRunsMillis</i>} indicates how long the eviction thread
 * should sleep before "runs" of examining idle objects. When non-positive, no
 * eviction thread will be launched. The default setting for this parameter is
 * -1 (i.e., idle object eviction is disabled by default).</li>
 * <li>
 *   {@link #setMinEvictableIdleTimeMillis <i>minEvictableIdleTimeMillis</i>}
 * specifies the minimum amount of time that an object may sit idle in the pool
 * before it is eligible for eviction due to idle time. When non-positive, no
 * object will be dropped from the pool due to idle time alone. This setting has
 * no effect unless <code>timeBetweenEvictionRunsMillis > 0.</code> The default
 * setting for this parameter is 30 minutes.</li>
 * <li>
 *   {@link #setTestWhileIdle <i>testWhileIdle</i>} indicates whether or not
 * idle objects should be validated using the factory's
 * {@link PoolableObjectFactory#validateObject} method. Objects that fail to
 * validate will be dropped from the pool. This setting has no effect unless
 * <code>timeBetweenEvictionRunsMillis > 0.</code> The default setting for this
 * parameter is <code>false.</code></li>
 * <li>
 *   {@link #setSoftMinEvictableIdleTimeMillis
 * <i>softMinEvictableIdleTimeMillis</i>} specifies the minimum amount of time
 * an object may sit idle in the pool before it is eligible for eviction by the
 * idle object evictor (if any), with the extra condition that at least
 * "minIdle" object instances remain in the pool. This setting has no
 * effect unless <code>timeBetweenEvictionRunsMillis > 0.</code> and it is
 * superseded by {@link #setMinEvictableIdleTimeMillis
 * <i>minEvictableIdleTimeMillis</i>} (that is, if
 * <code>minEvictableIdleTimeMillis</code> is positive, then
 * <code>softMinEvictableIdleTimeMillis</code> is ignored). The default setting
 * for this parameter is -1 (disabled).</li>
 * <li>
 *   {@link #setNumTestsPerEvictionRun <i>numTestsPerEvictionRun</i>}
 * determines the number of objects examined in each run of the idle object
 * evictor. This setting has no effect unless
 * <code>timeBetweenEvictionRunsMillis > 0.</code> The default setting for this
 * parameter is 3.</li>
 * </ul>
 * <p>
 * <p>
 * The pool can be configured to behave as a LIFO queue with respect to idle
 * objects - always returning the most recently used object from the pool, or as
 * a FIFO queue, where borrowObject always returns the oldest object in the idle
 * object pool.
 * <ul>
 * <li>
 *   {@link #setLifo <i>lifo</i>} determines whether or not the pool returns
 * idle objects in last-in-first-out order. The default setting for this
 * parameter is <code>true.</code></li>
 * </ul>
 * <p>
 * GenericObjectPool is not usable without a {@link PoolableObjectFactory}. A
 * non-<code>null</code> factory must be provided as a constructor
 * argument before the pool is used.
 * <p>
 * Implementation note: To prevent possible deadlocks, care has been taken to
 * ensure that no call to a factory method will occur within a synchronization
 * block. See POOL-125 and DBCP-44 for more information.
 * 
 * @see GenericKeyedObjectPool
 * @param <T>
 *            Type of element pooled in this pool.
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date: 2011-05-11 13:50:33 +0100 (Wed, 11 May
 *          2011) $
 * @since Pool 1.0
 */
public class GenericObjectPool<T> extends BaseObjectPool<T>
        implements GenericObjectPoolMBean {

    // --- constructors -----------------------------------------------

    /**
     * Create a new <tt>GenericObjectPool</tt> with default properties.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory) {
        this(factory, new GenericObjectPoolConfig<T>());
    }

    public GenericObjectPool(PoolableObjectFactory<T> factory,
            GenericObjectPoolConfig<T> config) {
        this.factory = factory;
        this.lifo = config.getLifo();
        this.maxTotal = config.getMaxTotal();
        this.maxIdle = config.getMaxIdle();
        this.maxWait = config.getMaxWait();
        this.minEvictableIdleTimeMillis =
                config.getMinEvictableIdleTimeMillis();
        this.minIdle = config.getMinIdle();
        this.numTestsPerEvictionRun = config.getNumTestsPerEvictionRun();
        this.softMinEvictableIdleTimeMillis =
                config.getSoftMinEvictableIdleTimeMillis();
        this.testOnBorrow = config.getTestOnBorrow();
        this.testOnReturn = config.getTestOnReturn();
        this.testWhileIdle = config.getTestWhileIdle();
        this.timeBetweenEvictionRunsMillis =
                config.getTimeBetweenEvictionRunsMillis();
        this.blockWhenExhausted = config.getBlockWhenExhausted();

        startEvictor(timeBetweenEvictionRunsMillis);

        initStats();

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
    }


    // --- public methods ---------------------------------------------

    // --- configuration methods --------------------------------------

    /**
     * Returns the maximum number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time. When
     * non-positive, there is no limit to the number of objects that can be
     * managed by the pool at one time.
     * 
     * @return the cap on the total number of object instances managed by the
     *         pool.
     * @see #setMaxTotal
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Sets the cap on the number of objects that can be allocated by the pool
     * (checked out to clients, or idle awaiting checkout) at a given time. Use
     * a negative value for no limit.
     * 
     * @param maxTotal
     *            The cap on the total number of object instances managed by the
     *            pool. Negative values mean that there is no limit to the
     *            number of objects allocated by the pool.
     * @see #getMaxTotal
     */
    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    /**
     * Returns whether to block when the {@link #borrowObject} method is
     * invoked when the pool is exhausted (the maximum number of "active"
     * objects has been reached).
     * 
     * @return true if should block when the pool is exhuasted
     * @see #setBlockWhenExhausted
     */
    public boolean getBlockWhenExhausted() {
        return blockWhenExhausted;
    }

    /**
     * Sets whether to block when the {@link #borrowObject} method is invoked
     * when the pool is exhausted (the maximum number of "active" objects has
     * been reached).
     * 
     * @param blockWhenExhausted   true if should block when the pool is exhausted
     * @see #getBlockWhenExhausted
     */
    public void setBlockWhenExhausted(boolean blockWhenExhausted) {
        this.blockWhenExhausted = blockWhenExhausted;
    }

    /**
     * Returns the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing an exception
     * when the pool is exhausted and the {@link #getBlockWhenExhausted} is true.
     * When less than or equal to 0, the {@link #borrowObject} method may block indefinitely.
     * 
     * @return maximum number of milliseconds to block when borrowing an object.
     * @see #setMaxWait
     * @see #setBlockWhenExhausted
     */
    public long getMaxWait() {
        return maxWait;
    }

    /**
     * Sets the maximum amount of time (in milliseconds) the
     * {@link #borrowObject} method should block before throwing an exception
     * when the pool is exhausted and the {@link #getBlockWhenExhausted} is true.
     * When less than or equal to 0, the {@link #borrowObject} method may block indefinitely.
     * 
     * @param maxWait
     *            maximum number of milliseconds to block when borrowing an
     *            object.
     * @see #getMaxWait
     * @see #getBlockWhenExhausted
     */
    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool.
     * 
     * @return the cap on the number of "idle" instances in the pool.
     * @see #setMaxIdle
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Sets the cap on the number of "idle" instances in the pool. If maxIdle is
     * set too low on heavily loaded systems it is possible you will see objects
     * being destroyed and almost immediately new objects being created. This is
     * a result of the active threads momentarily returning objects faster than
     * they are requesting them them, causing the number of idle objects to rise
     * above maxIdle. The best value for maxIdle for heavily loaded system will
     * vary but the default is a good starting point.
     * 
     * @param maxIdle
     *            The cap on the number of "idle" instances in the pool. Use a
     *            negative value to indicate an unlimited number of idle
     *            instances.
     * @see #getMaxIdle
     */
    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * Sets the minimum number of objects allowed in the pool before the evictor
     * thread (if active) spawns new objects. Note that no objects are created
     * when <code>numActive + numIdle >= maxActive.</code> This setting has no
     * effect if the idle object evictor is disabled (i.e. if
     * <code>timeBetweenEvictionRunsMillis <= 0</code>).
     * 
     * @param minIdle
     *            The minimum number of objects.
     * @see #getMinIdle
     * @see #getTimeBetweenEvictionRunsMillis()
     */
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    /**
     * Returns the minimum number of objects allowed in the pool before the
     * evictor thread (if active) spawns new objects. (Note no objects are
     * created when: numActive + numIdle >= maxActive)
     * 
     * @return The minimum number of objects.
     * @see #setMinIdle
     */
    public int getMinIdle() {
        return minIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned by the {@link #borrowObject} method. If the object fails to
     * validate, it will be dropped from the pool, and we will attempt to borrow
     * another.
     * 
     * @return <code>true</code> if objects are validated before being borrowed.
     * @see #setTestOnBorrow
     */
    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned by the {@link #borrowObject} method. If the object fails to
     * validate, it will be dropped from the pool, and we will attempt to borrow
     * another.
     * 
     * @param testOnBorrow
     *            <code>true</code> if objects should be validated before being
     *            borrowed.
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned to the pool within the {@link #returnObject}.
     * 
     * @return <code>true</code> when objects will be validated after returned
     *         to {@link #returnObject}.
     * @see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned to the pool within the {@link #returnObject}.
     * 
     * @param testOnReturn
     *            <code>true</code> so objects will be validated after returned
     *            to {@link #returnObject}.
     * @see #getTestOnReturn
     */
    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    /**
     * Returns the number of milliseconds to sleep between runs of the idle
     * object evictor thread. When non-positive, no idle object evictor thread
     * will be run.
     * 
     * @return number of milliseconds to sleep between evictor runs.
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    /**
     * Sets the number of milliseconds to sleep between runs of the idle object
     * evictor thread. When non-positive, no idle object evictor thread will be
     * run.
     * 
     * @param timeBetweenEvictionRunsMillis
     *            number of milliseconds to sleep between evictor runs.
     * @see #getTimeBetweenEvictionRunsMillis
     */
    public void setTimeBetweenEvictionRunsMillis(
            long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        startEvictor(timeBetweenEvictionRunsMillis);
    }

    /**
     * Returns the max number of objects to examine during each run of the idle
     * object evictor thread (if any).
     * 
     * @return max number of objects to examine during each evictor run.
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    /**
     * Sets the max number of objects to examine during each run of the idle
     * object evictor thread (if any).
     * <p>
     * When a negative value is supplied,
     * <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run. That is, when the value is <i>-n</i>, roughly one
     * <i>n</i>th of the idle objects will be tested per run. When the value is
     * positive, the number of tests actually performed in each run will be the
     * minimum of this value and the number of instances idle in the pool.
     * 
     * @param numTestsPerEvictionRun
     *            max number of objects to examine during each evictor run.
     * @see #getNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligible for eviction by the idle object evictor (if any).
     * 
     * @return minimum amount of time an object may sit idle in the pool before
     *         it is eligible for eviction.
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction by the idle object evictor (if any). When
     * non-positive, no objects will be evicted from the pool due to idle time
     * alone.
     * 
     * @param minEvictableIdleTimeMillis
     *            minimum amount of time an object may sit idle in the pool
     *            before it is eligible for eviction.
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * Returns the minimum amount of time
     * an object may sit idle in the pool before it is eligible for eviction by the
     * idle object evictor (if any), with the extra condition that at least
     * "minIdle" object instances remain in the pool. This setting has no
     * effect unless {@code timeBetweenEvictionRunsMillis > 0.} and it is
     * superseded by {@link #setMinEvictableIdleTimeMillis
     * <i>minEvictableIdleTimeMillis</i>} (that is, if
     * {@code minEvictableIdleTimeMillis} is positive, then
     * {@code softMinEvictableIdleTimeMillis} is ignored). The default setting
     * for this parameter is -1 (disabled).
     * 
     * @return minimum amount of time an object may sit idle in the pool before
     *         it is eligible for eviction if minIdle instances are available
     * @since Pool 1.3
     */
    public long getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool before
     * it is eligible for eviction by the idle object evictor (if any), with the
     * extra condition that at least "minIdle" object instances remain in the
     * pool. When non-positive, no objects will be evicted from the pool due to
     * idle time alone.
     * 
     * @param softMinEvictableIdleTimeMillis
     *            minimum amount of time an object may sit idle in the pool
     *            before it is eligible for eviction.
     * @since Pool 1.3
     * @see #getSoftMinEvictableIdleTimeMillis
     */
    public void setSoftMinEvictableIdleTimeMillis(
            long softMinEvictableIdleTimeMillis) {
        this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} by the idle object
     * evictor (if any). If an object fails to validate, it will be dropped from
     * the pool.
     * 
     * @return <code>true</code> when objects will be validated by the evictor.
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} by the idle object
     * evictor (if any). If an object fails to validate, it will be dropped from
     * the pool.
     * 
     * @param testWhileIdle
     *            <code>true</code> so objects will be validated by the evictor.
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    /**
     * Whether or not the idle object pool acts as a LIFO queue. True means that
     * borrowObject returns the most recently used ("last in") idle object in
     * the pool (if there are idle instances available). False means that the
     * pool behaves as a FIFO queue - objects are taken from the idle object
     * pool in the order that they are returned to the pool.
     * 
     * @return <code>true</true> if the pool is configured to act as a LIFO queue
     * @since 1.4
     */
    public boolean getLifo() {
        return lifo;
    }

    /**
     * Sets the LIFO property of the pool. True means that borrowObject returns
     * the most recently used ("last in") idle object in the pool (if there are
     * idle instances available). False means that the pool behaves as a FIFO
     * queue - objects are taken from the idle object pool in the order that
     * they are returned to the pool.
     * 
     * @param lifo
     *            the new value for the LIFO property
     * @since 1.4
     */
    public void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    /**
     * Sets my configuration.
     * 
     * @param conf
     *            configuration to use.
     * @see GenericObjectPoolConfig
     */
    public void setConfig(GenericObjectPoolConfig<T> conf) {
        setMaxIdle(conf.getMaxIdle());
        setMinIdle(conf.getMinIdle());
        setMaxTotal(conf.getMaxTotal());
        setMaxWait(conf.getMaxWait());
        setBlockWhenExhausted(conf.getBlockWhenExhausted());
        setTestOnBorrow(conf.getTestOnBorrow());
        setTestOnReturn(conf.getTestOnReturn());
        setTestWhileIdle(conf.getTestWhileIdle());
        setNumTestsPerEvictionRun(conf.getNumTestsPerEvictionRun());
        setMinEvictableIdleTimeMillis(conf.getMinEvictableIdleTimeMillis());
        setTimeBetweenEvictionRunsMillis(
                conf.getTimeBetweenEvictionRunsMillis());
        setSoftMinEvictableIdleTimeMillis(
                conf.getSoftMinEvictableIdleTimeMillis());
        setLifo(conf.getLifo());
    }

    // -- ObjectPool methods ------------------------------------------

    /**
     * <p>
     * Borrows an object from the pool.
     * </p>
     * <p>
     * If there is an idle instance available in the pool, then either the
     * most-recently returned (if {@link #getLifo() lifo} == true) or "oldest"
     * (lifo == false) instance sitting idle in the pool will be activated and
     * returned. If activation fails, or {@link #getTestOnBorrow() testOnBorrow}
     * is set to true and validation fails, the instance is destroyed and the
     * next available instance is examined. This continues until either a valid
     * instance is returned or there are no more idle instances available.
     * </p>
     * <p>
     * If there are no idle instances available in the pool, behavior depends on
     * the {@link #getMaxTotal() maxTotal} and (if applicable)
     * {@link #getBlockWhenExhausted()} and
     * {@link #getMaxWait() maxWait} properties. If the number of instances
     * checked out from the pool is less than <code>maxActive,</code> a new
     * instance is created, activated and (if applicable) validated and returned
     * to the caller.
     * </p>
     * <p>
     * If the pool is exhausted (no available idle instances and no capacity to
     * create new ones), this method will either block (
     * {@link #getBlockWhenExhausted()} is true) or throw a
     * <code>NoSuchElementException</code> ({@link #getBlockWhenExhausted()} is false). The
     * length of time that this method will block when
     * {@link #getBlockWhenExhausted()} is true is determined by
     * the {@link #getMaxWait() maxWait} property.
     * </p>
     * <p>
     * When the pool is exhausted, multiple calling threads may be
     * simultaneously blocked waiting for instances to become available. As of
     * pool 1.5, a "fairness" algorithm has been implemented to ensure that
     * threads receive available instances in request arrival order.
     * </p>
     * 
     * @return object instance
     * @throws NoSuchElementException
     *             if an instance cannot be returned
     */
    @Override
    public T borrowObject() throws Exception {
        return borrowObject(maxWait);
    }
    
    /**
     * Borrow an object from the pool using a user specific waiting time which
     * only applies if {@link #getBlockWhenExhausted()} is true.
     * 
     * @param borrowMaxWait The time to wait in milliseconds for an object to
     *                      become available
     * @return object instance
     * @throws NoSuchElementException
     *             if an instance cannot be returned
     */
    public T borrowObject(long borrowMaxWait) throws Exception {
        assertOpen();

        PooledObject<T> p = null;

        // Get local copy of current config so it is consistent for entire
        // method execution
        boolean blockWhenExhausted = this.blockWhenExhausted;

        boolean create;
        long waitTime = 0;

        while (p == null) {
            create = false;
            if (blockWhenExhausted) {
                p = idleObjects.pollFirst();
                if (p == null) {
                    create = true;
                    p = create();
                }
                if (p == null) {
                    if (borrowMaxWait < 1) {
                        p = idleObjects.takeFirst();
                    } else {
                        waitTime = System.currentTimeMillis();
                        p = idleObjects.pollFirst(borrowMaxWait,
                                TimeUnit.MILLISECONDS);
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
                p = idleObjects.pollFirst();
                if (p == null) {
                    create = true;
                    p = create();
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
                    factory.activateObject(p.getObject());
                } catch (Exception e) {
                    try {
                        destroy(p);
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
                        validate = factory.validateObject(p.getObject());
                    } catch (Throwable t) {
                        PoolUtils.checkRethrow(t);
                    }
                    if (!validate) {
                        try {
                            destroy(p);
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
     * <p>
     * Returns an object instance to the pool.
     * </p>
     * <p>
     * If {@link #getMaxIdle() maxIdle} is set to a positive value and the
     * number of idle instances has reached this value, the returning instance
     * is destroyed.
     * </p>
     * <p>
     * If {@link #getTestOnReturn() testOnReturn} == true, the returning
     * instance is validated before being returned to the idle instance pool. In
     * this case, if validation fails, the instance is destroyed.
     * </p>
     * 
     * @param obj
     *            instance to return to the pool
     */
    @Override
    public void returnObject(T obj) {

        PooledObject<T> p = allObjects.get(obj);

        if (p == null) {
            throw new IllegalStateException(
                    "Returned object not currently part of this pool");
        }

        long activeTime = p.getActiveTimeMillis();

        if (getTestOnReturn()) {
            if (!factory.validateObject(obj)) {
                try {
                    destroy(p);
                } catch (Exception e) {
                    // TODO - Ignore?
                }
                updateStatsReturn(activeTime);
                return;
            }
        }

        try {
            factory.passivateObject(obj);
        } catch (Exception e1) {
            try {
                destroy(p);
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

        int maxIdle = getMaxIdle();
        if (isClosed() || maxIdle > -1 && maxIdle <= idleObjects.size()) {
            try {
                destroy(p);
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
     * <p>
     * Activation of this method decrements the active count and attempts to
     * destroy the instance.
     * </p>
     * 
     * @throws Exception
     *             if the configured {@link PoolableObjectFactory} throws an
     *             exception destroying obj
     */
    @Override
    public void invalidateObject(T obj) throws Exception {
        PooledObject<T> p = allObjects.get(obj);
        if (p == null) {
            throw new IllegalStateException(
                    "Object not currently part of this pool");
        }
        destroy(p);
    }

    /**
     * Clears any objects sitting idle in the pool by removing them from the
     * idle instance pool and then invoking the configured
     * {@link PoolableObjectFactory#destroyObject(Object)} method on each idle
     * instance.
     * <p>
     * Implementation notes:
     * <ul>
     * <li>This method does not destroy or effect in any way instances that are
     * checked out of the pool when it is invoked.</li>
     * <li>Invoking this method does not prevent objects being returned to the
     * idle instance pool, even during its execution. It locks the pool only
     * during instance removal. Additional instances may be returned while
     * removed items are being destroyed.</li>
     * <li>Exceptions encountered destroying idle instances are swallowed.</li>
     * </ul>
     * </p>
     */
    @Override
    public void clear() {
        PooledObject<T> p = idleObjects.poll();

        while (p != null) {
            try {
                destroy(p);
            } catch (Exception e) {
                // TODO - Ignore?
            }
            p = idleObjects.poll();
        }
    }

    /**
     * Return the number of instances currently borrowed from this pool.
     * 
     * @return the number of instances currently borrowed from this pool
     */
    @Override
    public int getNumActive() {
        return allObjects.size() - idleObjects.size();
    }

    /**
     * Return the number of instances currently idle in this pool.
     * 
     * @return the number of instances currently idle in this pool
     */
    @Override
    public int getNumIdle() {
        return idleObjects.size();
    }

    /**
     * <p>
     * Closes the pool. Once the pool is closed, {@link #borrowObject()} will
     * fail with IllegalStateException, but {@link #returnObject(Object)} and
     * {@link #invalidateObject(Object)} will continue to work, with returned
     * objects destroyed on return.
     * </p>
     * <p>
     * Destroys idle instances in the pool by invoking {@link #clear()}.
     * </p>
     * 
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        clear();
        startEvictor(-1L);
    }

    /**
     * <p>
     * Perform <code>numTests</code> idle object eviction tests, evicting
     * examined objects that meet the criteria for eviction. If
     * <code>testWhileIdle</code> is true, examined objects are validated when
     * visited (and removed if invalid); otherwise only objects that have been
     * idle for more than <code>minEvicableIdletimeMillis</code> are removed.
     * </p>
     * <p>
     * Successive activations of this method examine objects in in sequence,
     * cycling through objects in oldest-to-youngest order.
     * </p>
     * 
     * @throws Exception
     *             if the pool is closed or eviction fails.
     */
    public void evict() throws Exception {
        assertOpen();

        if (idleObjects.size() == 0) {
            return;
        }

        PooledObject<T> underTest = null;

        boolean testWhileIdle = getTestWhileIdle();
        long idleEvictTime = Long.MAX_VALUE;
        long idleSoftEvictTime = Long.MAX_VALUE;
        
        if (getMinEvictableIdleTimeMillis() > 0) {
            idleEvictTime = getMinEvictableIdleTimeMillis();
        }
        if (getSoftMinEvictableIdleTimeMillis() > 0) {
            idleSoftEvictTime = getSoftMinEvictableIdleTimeMillis();
        }
                
        for (int i = 0, m = getNumTests(); i < m; i++) {
            if (evictionIterator == null || !evictionIterator.hasNext()) {
                if (getLifo()) {
                    evictionIterator = idleObjects.descendingIterator();
                } else {
                    evictionIterator = idleObjects.iterator();
                }
            }
            if (!evictionIterator.hasNext()) {
                // Pool exhausted, nothing to do here
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

            if (idleEvictTime < underTest.getIdleTimeMillis() ||
                    (idleSoftEvictTime < underTest.getIdleTimeMillis() &&
                            getMinIdle() < idleObjects.size())) {
                destroy(underTest);
                destroyedByEvictorCount.incrementAndGet();
            } else {
                if (testWhileIdle) {
                    boolean active = false;
                    try {
                        factory.activateObject(underTest.getObject());
                        active = true;
                    } catch (Exception e) {
                        destroy(underTest);
                        destroyedByEvictorCount.incrementAndGet();
                    }
                    if (active) {
                        if (!factory.validateObject(underTest.getObject())) {
                            destroy(underTest);
                            destroyedByEvictorCount.incrementAndGet();
                        } else {
                            try {
                                factory.passivateObject(underTest.getObject());
                            } catch (Exception e) {
                                destroy(underTest);
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

        return;
    }

    private PooledObject<T> create() throws Exception {
        int localMaxTotal = getMaxTotal();
        long newCreateCount = createCount.incrementAndGet();
        if (localMaxTotal > -1 && newCreateCount > localMaxTotal ||
                newCreateCount > Integer.MAX_VALUE) {
            createCount.decrementAndGet();
            return null;
        }

        T t = null;
        try {
            t = factory.makeObject();
        } catch (Exception e) {
            createCount.decrementAndGet();
            throw e;
        }

        PooledObject<T> p = new PooledObject<T>(t);
        createdCount.incrementAndGet();
        allObjects.put(t, p);
        return p;
    }

    private void destroy(PooledObject<T> toDestory) throws Exception {
        idleObjects.remove(toDestory);
        allObjects.remove(toDestory.getObject());
        try {
            factory.destroyObject(toDestory.getObject());
        } finally {
            destroyedCount.incrementAndGet();
            createCount.decrementAndGet();
        }
    }

    /**
     * Check to see if we are below our minimum number of objects if so enough
     * to bring us back to our minimum.
     * 
     * @throws Exception
     *             when {@link #addObject()} fails.
     */
    private void ensureMinIdle() throws Exception {
        int minIdle = getMinIdle();
        if (minIdle < 1) {
            return;
        }

        while (idleObjects.size() < minIdle) {
            PooledObject<T> p = create();
            if (p == null) {
                // Can't create objects, no reason to think another call to
                // create will work. Give up.
                break;
            }
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
            }
        }
    }

    /**
     * Create an object, and place it into the pool. addObject() is useful for
     * "pre-loading" a pool with idle objects.
     */
    @Override
    public void addObject() throws Exception {
        assertOpen();
        if (factory == null) {
            throw new IllegalStateException(
                    "Cannot add objects without a factory.");
        }
        PooledObject<T> p = create();
        addIdleObject(p);
    }

    // --- non-public methods ----------------------------------------

    private void addIdleObject(PooledObject<T> p) throws Exception {
        if (p != null) {
            factory.passivateObject(p.getObject());
            if (getLifo()) {
                idleObjects.addFirst(p);
            } else {
                idleObjects.addLast(p);
            }
        }
    }

    /**
     * Start the eviction thread or service, or when <i>delay</i> is
     * non-positive, stop it if it is already running.
     * 
     * @param delay
     *            milliseconds between evictor runs.
     */
    protected void startEvictor(long delay) {
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
     * Returns pool info including {@link #getNumActive()},
     * {@link #getNumIdle()} and a list of objects idle in the pool with their
     * idle times.
     * 
     * @return string containing debug information
     */
    synchronized String debugInfo() {
        StringBuilder buf = new StringBuilder();
        buf.append("Active: ").append(getNumActive()).append("\n");
        buf.append("Idle: ").append(getNumIdle()).append("\n");
        buf.append("Idle Objects:\n");
        for (PooledObject<T> pair : idleObjects) {
            buf.append("\t").append(pair.toString());
        }
        return buf.toString();
    }

    /**
     * Returns the number of tests to be performed in an Evictor run, based on
     * the current value of <code>numTestsPerEvictionRun</code> and the number
     * of idle instances in the pool.
     * 
     * @see #setNumTestsPerEvictionRun
     * @return the number of tests for the Evictor to run
     */
    private int getNumTests() {
        if (numTestsPerEvictionRun >= 0) {
            return Math.min(numTestsPerEvictionRun, idleObjects.size());
        } else {
            return (int) (Math.ceil(idleObjects.size() /
                    Math.abs((double) numTestsPerEvictionRun)));
        }
    }

    //--- JMX specific attributes ----------------------------------------------

    private void initStats() {
        for (int i = 0; i < AVERAGE_TIMING_STATS_CACHE_SIZE; i++) {
            activeTimes.add(null);
            idleTimes.add(null);
            waitTimes.add(null);
        }
    }

    private long getMeanFromStatsCache(Deque<Long> cache) {
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
                result = result * ((counter - 1) / counter) +
                        time.longValue()/counter;
            }
        }
        return (long) result;
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

    // --- inner classes ----------------------------------------------

    /**
     * The idle object evictor {@link TimerTask}.
     * 
     * @see GenericObjectPool#setTimeBetweenEvictionRunsMillis
     */
    private class Evictor extends TimerTask {
        /**
         * Run pool maintenance. Evict objects qualifying for eviction and then
         * invoke {@link GenericObjectPool#ensureMinIdle()}.
         */
        @Override
        public void run() {
            try {
                evict();
            } catch (Exception e) {
                // ignored
            } catch (OutOfMemoryError oome) {
                // Log problem but give evictor thread a chance to continue in
                // case error is recoverable
                oome.printStackTrace(System.err);
            }
            try {
                ensureMinIdle();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    // --- private attributes ---------------------------------------

    /**
     * The cap on the number of idle instances in the pool.
     * 
     * @see #setMaxIdle
     * @see #getMaxIdle
     */
    private volatile int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

    /**
     * The cap on the minimum number of idle instances in the pool.
     * 
     * @see #setMinIdle
     * @see #getMinIdle
     */
    private volatile int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

    /**
     * The cap on the total number of active instances from the pool.
     * 
     * @see #setMaxTotal
     * @see #getMaxTotal
     */
    private volatile int maxTotal =
        GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

    /**
     * The maximum amount of time (in millis) the {@link #borrowObject} method
     * should block before throwing an exception when the pool is exhausted and
     * {@link #getBlockWhenExhausted()} is true.
     * When less than or equal to 0, the
     * {@link #borrowObject} method may block indefinitely.
     * 
     * @see #setMaxWait
     * @see #getMaxWait
     * @see #setBlockWhenExhausted
     * @see #getBlockWhenExhausted
     */
    private volatile long maxWait = GenericObjectPoolConfig.DEFAULT_MAX_WAIT;

    /**
     * When the {@link #borrowObject} method is invoked when the pool is
     * exhausted (the maximum number of "active" objects has been reached)
     * should the {@link #borrowObject} method block or not?
     * 
     * @see #setBlockWhenExhausted
     * @see #getBlockWhenExhausted
     */
    private volatile boolean blockWhenExhausted =
        GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned by the {@link #borrowObject} method. If the object fails to
     * validate, it will be dropped from the pool, and we will attempt to borrow
     * another.
     * 
     * @see #setTestOnBorrow
     * @see #getTestOnBorrow
     */
    private volatile boolean testOnBorrow =
        GenericObjectPoolConfig.DEFAULT_TEST_ON_BORROW;

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} before being
     * returned to the pool within the {@link #returnObject}.
     * 
     * @see #getTestOnReturn
     * @see #setTestOnReturn
     */
    private volatile boolean testOnReturn =
        GenericObjectPoolConfig.DEFAULT_TEST_ON_RETURN;

    /**
     * When <tt>true</tt>, objects will be
     * {@link PoolableObjectFactory#validateObject validated} by the idle object
     * evictor (if any). If an object fails to validate, it will be dropped from
     * the pool.
     * 
     * @see #setTestWhileIdle
     * @see #getTestWhileIdle
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private volatile boolean testWhileIdle =
        GenericObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE;

    /**
     * The number of milliseconds to sleep between runs of the idle object
     * evictor thread. When non-positive, no idle object evictor thread will be
     * run.
     * 
     * @see #setTimeBetweenEvictionRunsMillis
     * @see #getTimeBetweenEvictionRunsMillis
     */
    private volatile long timeBetweenEvictionRunsMillis =
        GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;

    /**
     * The max number of objects to examine during each run of the idle object
     * evictor thread (if any).
     * <p>
     * When a negative value is supplied,
     * <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
     * tests will be run. I.e., when the value is <i>-n</i>, roughly one
     * <i>n</i>th of the idle objects will be tested per run.
     * 
     * @see #setNumTestsPerEvictionRun
     * @see #getNumTestsPerEvictionRun
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private volatile int numTestsPerEvictionRun =
        GenericObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligible for eviction by the idle object evictor (if any). When
     * non-positive, no objects will be evicted from the pool due to idle time
     * alone.
     * 
     * @see #setMinEvictableIdleTimeMillis
     * @see #getMinEvictableIdleTimeMillis
     * @see #getTimeBetweenEvictionRunsMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    private volatile long minEvictableIdleTimeMillis =
        GenericObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligible for eviction by the idle object evictor (if any), with the
     * extra condition that at least "minIdle" amount of object remain in the
     * pool. When non-positive, no objects will be evicted from the pool due to
     * idle time alone.
     * 
     * @see #setSoftMinEvictableIdleTimeMillis
     * @see #getSoftMinEvictableIdleTimeMillis
     */
    private volatile long softMinEvictableIdleTimeMillis =
        GenericObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    /** Whether or not the pool behaves as a LIFO queue (last in first out) */
    private volatile boolean lifo = GenericObjectPoolConfig.DEFAULT_LIFO;

    /** My {@link PoolableObjectFactory}. */
    final private PoolableObjectFactory<T> factory;

    /**
     * My idle object eviction {@link TimerTask}, if any.
     */
    private Evictor evictor = null;

    /**
     * All of the objects currently associated with this pool in any state. It
     * excludes objects that have been destroyed. The size of
     * {@link #allObjects} will always be less than or equal to {@link
     * #_maxActive}.
     */
    private final Map<T, PooledObject<T>> allObjects =
        new ConcurrentHashMap<T, PooledObject<T>>();

    /**
     * The combined count of the currently created objects and those in the
     * process of being created. Under load, it may exceed {@link #_maxActive}
     * if multiple threads try and create a new object at the same time but
     * {@link #create(boolean)} will ensure that there are never more than
     * {@link #_maxActive} objects created at any one time.
     */
    private final AtomicLong createCount = new AtomicLong(0);

    /** The queue of idle objects */
    private final LinkedBlockingDeque<PooledObject<T>> idleObjects =
        new LinkedBlockingDeque<PooledObject<T>>();

    /** An iterator for {@link #idleObjects} that is used by the evictor. */
    private Iterator<PooledObject<T>> evictionIterator = null;

    // JMX specific attributes
    private static final int AVERAGE_TIMING_STATS_CACHE_SIZE = 100;
    private AtomicLong borrowedCount = new AtomicLong(0);
    private AtomicLong returnedCount = new AtomicLong(0);
    private AtomicLong createdCount = new AtomicLong(0);
    private AtomicLong destroyedCount = new AtomicLong(0);
    private AtomicLong destroyedByEvictorCount = new AtomicLong(0);
    private AtomicLong destroyedByBorrowValidationCount = new AtomicLong(0);
    private final Deque<Long> activeTimes = new LinkedList<Long>();
    private final Deque<Long> idleTimes = new LinkedList<Long>();
    private final Deque<Long> waitTimes = new LinkedList<Long>();
    private Object maxBorrowWaitTimeMillisLock = new Object();
    private volatile long maxBorrowWaitTimeMillis = 0;

    private static final String ONAME_BASE =
        "org.apache.commoms.pool2:type=GenericObjectPool,name=";
}
