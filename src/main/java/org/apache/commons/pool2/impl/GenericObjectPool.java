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

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
 * {@link Object#wait()}) until a new or idle object is available. If a
 * non-negative {@link #setMaxWaitMillis <i>maxWaitMillis</i>} value is
 * supplied, then {@link #borrowObject} will block for at most that man
 * milliseconds, after which a {@link NoSuchElementException} will be thrown. If
 * {@link #setMaxWaitMillis <i>maxWaitMillis</i>} is negative, the
 * {@link #borrowObject} method will block indefinitely.</li>
 * </ul>
 * The default {@link #getBlockWhenExhausted} is true
 * and the default <code>maxWaitMillis</code> setting is
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
 * @version $Revision$
 *          2011) $
 * @since Pool 1.0
 * This class is intended to be thread-safe.
 */
public class GenericObjectPool<T> extends BaseGenericObjectPool<T>
        implements ObjectPool<T>, GenericObjectPoolMBean {

    /**
     * Create a new <code>GenericObjectPool</code> using default.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory) {
        this(factory, new GenericObjectPoolConfig());
    }

    /**
     * Create a new <code>GenericObjectPool</code> using a specific
     * configuration.
     *
     * @param config    The configuration to use for this pool instance. The
     *                  configuration is used by value. Subsequent changes to
     *                  the configuration object will not be reflected in the
     *                  pool.
     */
    public GenericObjectPool(PoolableObjectFactory<T> factory,
            GenericObjectPoolConfig config) {

        super(config, ONAME_BASE, config.getJmxNamePrefix());

        if (factory == null) {
            throw new IllegalArgumentException("factory may not be null");
        }
        this.factory = factory;

        setConfig(config);

        startEvictor(getTimeBetweenEvictionRunsMillis());
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool.
     *
     * @return the cap on the number of "idle" instances in the pool.
     * @see #setMaxIdle
     */
    @Override
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
     * <p>
     * If the configured value of minIdle is greater than the configured value
     * for maxIdle then the value of maxIdle will be used instead.
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
     * <p>
     * If the configured value of minIdle is greater than the configured value
     * for maxIdle then the value of maxIdle will be used instead.
     *
     * @return The minimum number of objects.
     * @see #setMinIdle
     */
    @Override
    public int getMinIdle() {
        int maxIdle = getMaxIdle();
        if (this.minIdle > maxIdle) {
            return maxIdle;
        } else {
            return minIdle;
        }
    }


    /**
     * Sets my configuration.
     *
     * @param conf
     *            configuration to use.
     * @see GenericObjectPoolConfig
     */
    public void setConfig(GenericObjectPoolConfig conf) {
        setLifo(conf.getLifo());
        setMaxIdle(conf.getMaxIdle());
        setMinIdle(conf.getMinIdle());
        setMaxTotal(conf.getMaxTotal());
        setMaxWaitMillis(conf.getMaxWaitMillis());
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
        setEvictionPolicyClassName(conf.getEvictionPolicyClassName());
    }

    /**
     * Obtain a reference to the factory used to create, destroy and validate
     * the objects used by this pool.
     *
     * @return the factory
     */
    public PoolableObjectFactory<T> getFactory() {
        return factory;
    }


    /**
     * <p>Borrows an object from the pool.</p>
     *
     * <p>If there is one or more idle instance available in the pool, then an
     * idle instance will be selected based on the value of {@link #getLifo()},
     * activated and returned. If activation fails, or {@link #getTestOnBorrow()
     * testOnBorrow} is set to <code>true</code> and validation fails, the
     * instance is destroyed and the next available instance is examined. This
     * continues until either a valid instance is returned or there are no more
     * idle instances available.</p>
     *
     * <p>
     * If there are no idle instances available in the pool, behavior depends on
     * the {@link #getMaxTotal() maxTotal} and (if applicable)
     * {@link #getBlockWhenExhausted()} and
     * {@link #getMaxWaitMillis() maxWait} properties. If the number of instances
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
     * the {@link #getMaxWaitMillis() maxWait} property.
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
        return borrowObject(getMaxWaitMillis());
    }

    /**
     * Borrow an object from the pool using a user specific waiting time which
     * only applies if {@link #getBlockWhenExhausted()} is true.
     *
     * @param borrowMaxWaitMillis   The time to wait in milliseconds for an
     *                              object to become available
     * @return object instance
     * @throws NoSuchElementException
     *             if an instance cannot be returned
     */
    public T borrowObject(long borrowMaxWaitMillis) throws Exception {
        assertOpen();

        PooledObject<T> p = null;

        // Get local copy of current config so it is consistent for entire
        // method execution
        boolean blockWhenExhausted = getBlockWhenExhausted();

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
                    if (borrowMaxWaitMillis < 0) {
                        p = idleObjects.takeFirst();
                    } else {
                        waitTime = System.currentTimeMillis();
                        p = idleObjects.pollFirst(borrowMaxWaitMillis,
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

        updateStatsBorrow(p, waitTime);

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
     * <p>
     * Exceptions encountered destroying objects for any reason are swallowed.
     * </p>
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
                    swallowException(e);
                }
                updateStatsReturn(activeTime);
                return;
            }
        }

        try {
            factory.passivateObject(obj);
        } catch (Exception e1) {
            swallowException(e1);
            try {
                destroy(p);
            } catch (Exception e) {
                swallowException(e);
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
                swallowException(e);
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

    /**
     * {@inheritDoc}
     * <p>
     * Activation of this method decrements the active count and attempts to
     * destroy the instance.
     * </p>
     *
     * @throws Exception if the configured {@link PoolableObjectFactory} throws an
     * exception destroying obj
     * @throws IllegalStateException if obj does not belong to this pool
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
                swallowException(e);
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
     * @throws RuntimeException
     */
    @Override
    public void close() {
        if (isClosed()) {
            return;
        }

        synchronized (closeLock) {
            if (isClosed()) {
                return;
            }

            // Stop the evictor before the pool is closed since evict() calls
            // assertOpen()
            startEvictor(-1L);

            closed = true;
            // This clear removes any idle objects
            clear();

            jmxUnregister();

            // Release any threads that were waiting for an object
            idleObjects.interuptTakeWaiters();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Successive activations of this method examine objects in in sequence,
     * cycling through objects in oldest-to-youngest order.
     */
    @Override
    public void evict() throws Exception {
        assertOpen();

        if (idleObjects.size() == 0) {
            return;
        }

        PooledObject<T> underTest = null;
        EvictionPolicy<T> evictionPolicy = getEvictionPolicy();

        synchronized (evictionLock) {
            EvictionConfig evictionConfig = new EvictionConfig(
                    getMinEvictableIdleTimeMillis(),
                    getSoftMinEvictableIdleTimeMillis(),
                    getMinIdle());

            boolean testWhileIdle = getTestWhileIdle();

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

                if (evictionPolicy.evict(evictionConfig, underTest,
                        idleObjects.size())) {
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
        }
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
        toDestory.invalidate();
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
    @Override
    protected void ensureMinIdle() throws Exception {
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
     * Returns the number of tests to be performed in an Evictor run, based on
     * the current value of <code>numTestsPerEvictionRun</code> and the number
     * of idle instances in the pool.
     *
     * @see #setNumTestsPerEvictionRun
     * @return the number of tests for the Evictor to run
     */
    private int getNumTests() {
        int numTestsPerEvictionRun = getNumTestsPerEvictionRun();
        if (numTestsPerEvictionRun >= 0) {
            return Math.min(numTestsPerEvictionRun, idleObjects.size());
        } else {
            return (int) (Math.ceil(idleObjects.size() /
                    Math.abs((double) numTestsPerEvictionRun)));
        }
    }

    //--- JMX support ----------------------------------------------------------

    /**
     * Return an estimate of the number of threads currently blocked waiting for
     * an object from the pool. This is intended for monitoring only, not for
     * synchronization control.
     *
     * @return  An estimate of the number of threads currently blocked waiting
     *          for an object from the pool
     */
    @Override
    public int getNumWaiters() {
        if (getBlockWhenExhausted()) {
            return idleObjects.getTakeQueueLength();
        } else {
            return 0;
        }
    }


    // --- configuration attributes --------------------------------------------

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

    private final PoolableObjectFactory<T> factory;


    // --- internal attributes -------------------------------------------------

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

    // JMX specific attributes
    private static final String ONAME_BASE =
        "org.apache.commoms.pool2:type=GenericObjectPool,name=";
}
