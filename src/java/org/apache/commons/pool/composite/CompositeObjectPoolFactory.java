/*
 * Copyright 2006 The Apache Software Foundation.
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

package org.apache.commons.pool.composite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <code>ObjectPoolFactory</code> that builds an optimized custom <code>ObjectPool</code> with the requested feature set.
 *
 * <p>Default values for a newly created factory:
 * <ul>
 *  <li>{@link #setBorrowPolicy(BorrowPolicy) borrowPolicy}:
 *      {@link BorrowPolicy#FIFO FIFO}</li>
 *  <li>{@link #setExhaustionPolicy(ExhaustionPolicy) exhaustionPolicy}:
 *      {@link ExhaustionPolicy#GROW GROW}</li>
 *  <li>{@link #setMaxIdle(int) maxIdle}: a negative value (unlimited)</li>
 *  <li>{@link #setMaxActive(int) maxActive}: a non-positive value (unlimited)</li>
 *  <li>{@link #setLimitPolicy(LimitPolicy) limitPolicy}:
 *      {@link LimitPolicy#FAIL FAIL}
 *      (has no effect unless {@link #setMaxActive(int) maxActive} is positive)</li>
 *  <li>{@link #setMaxWaitMillis(int) maxWaitMillis}: a non-positive value (wait forever)
 *      (has no effect unless {@link #setLimitPolicy(LimitPolicy) limitPolicy} is
 *      {@link LimitPolicy#WAIT WAIT})</li>
 *  <li>{@link #setTrackingPolicy(TrackingPolicy) trackingPolicy}:
 *      {@link TrackingPolicy#SIMPLE SIMPLE}</li>
 *  <li>{@link #setValidateOnReturn(boolean) validateOnReturn}: false (do not validate on return)</li>
 *  <li>{@link #setEvictIdleMillis(long) evictIdleMillis}: non-positive (do not evict objects for being idle)</li>
 *  <li>{@link #setEvictInvalidFrequencyMillis(long) evictInvalidFrequencyMillis}: non-positive (do not check if idle
 *      objects are {@link PoolableObjectFactory#validateObject(Object) invalid} and should be evicted)</li> 
 * </ul>
 * </p>
 *
 * <p>Example usages:</p>
 *
 * <p>To create a "stack" {@link ObjectPool} that keeps at most 5 idle objects, checks idle objects every 5 minutes to
 * see if they are still {@link PoolableObjectFactory#validateObject(Object) valid} and evicts invalid idle objects,
 * evicts idle objects after an hour regardless of wether or not they are
 * {@link PoolableObjectFactory#validateObject(Object) valid}, and throws an {@link IllegalStateException} when you
 * return an object that wasn't originally borrowed from the pool.
 * <p>
 * <pre>
 * PoolableObjectFactory pof = ...;
 * CompositeObjectPoolFactory copf = new CompositeObjectPoolFactory(pof);
 * copf.setBorrowPolicy(BorrowPolicy.LIFO)
 * copf.setMaxIdle(5);
 * copf.setEvictInvalidFrequencyMillis(5 * 60 * 1000);
 * copf.setEvictIdleMillis(60 * 60 * 1000);
 * copf.setTrackingPolicy(TrackingPolicy.REFERENCE);
 * ObjectPool pool = copf.createPool();
 * </pre>
 *
 * <p>To create a fifo {@link ObjectPool} that does not automatically create new objects as needed, allows
 * only 2 objects to be borrowed at a time, waits up to 10 seconds for an idle object to become available,
 * and populates a created pool with 3 objects.
 * <p>
 * <pre>
 * PoolableObjectFactory pof = ...;
 * CompositeObjectPoolFactory copf = new CompositeObjectPoolFactory(pof);
 * copf.setsetExhaustionPolicy(ExhaustionPolicy.FAIL);
 * copf.setMaxActive(2);
 * copf.setLimitPolicy(LimitPolicy.WAIT);
 * copf.setMaxWaitMillis(10 * 1000);
 * ObjectPool pool = copf.createPool();
 * pool.addObject(); pool.addObject(); pool.addObject();
 * </pre>
 *
 * <p>To create a fifo {@link ObjectPool} that doesn't prevent idle objects from being garbage collected, detects when
 * borrowed objects are not returned to the pool and prints a stack trace from where they were borrowed.
 * </p>
 * <pre>
 * PoolableObjectFactory pof = ...;
 * CompositeObjectPoolFactory copf = new CompositeObjectPoolFactory(pof);
 * copf.setBorrowPolicy(BorrowPolicy.SOFT_FIFO);
 * copf.setTrackingPolicy(TrackingPolicy.DEBUG);
 * ObjectPool pool = copf.createPool();
 * </pre>
 *
 * <p>{@link ObjectPool}s created by this factory have the following properties:
 * <ul>
 *  <li>The must have a {@link ObjectPoolFactory}</li>
 *  <li>They are thread-safe.</li>
 *  <li>{@link ObjectPool#borrowObject() Borrowed objects} will either be
 *      {@link PoolableObjectFactory#makeObject() newly created} or have been
 *      {@link PoolableObjectFactory#activateObject(Object) activated} and
 *      {@link PoolableObjectFactory#validateObject(Object) validated}.</li>
 *  <li>Objects that fail {@link PoolableObjectFactory#validateObject(Object) validation} will be
 *      {@link ObjectPool#invalidateObject(Object) invalidated}.</li>
 *  <li>Objects that cause {@link PoolableObjectFactory object factories} to throw an exception during
 *      {@link PoolableObjectFactory#activateObject(Object) activation} or
 *      {@link PoolableObjectFactory#validateObject(Object) validation} will be
 *      {@link PoolableObjectFactory#destroyObject(Object) destroyed} and removed from the pool. The pool will then try
 *      again to return an object.</li>
 *  <li>Exceptions thrown by {@link PoolableObjectFactory} methods except for {@link PoolableObjectFactory#makeObject()}
 *      will be contained and dealt with.</li>
 *  <li>The factory cannot be updated. {@link ObjectPool#setFactory(PoolableObjectFactory)} always throws an
 *      {@link UnsupportedOperationException}.</li>
 *  <li>Calling {@link ObjectPool#borrowObject()} or {@link ObjectPool#addObject()} after calling
 *      {@link ObjectPool#close()} will throw an {@link IllegalStateException}.</li>
 *  <li>The {@link ObjectPool#close()} method returned from this factory makes a good effort to free any resources
 *      associated with the pool but it's possible for any threads currently executing in a pool method to leave behind
 *      some non-freed resources. If you demand perfect behavior of the {@link ObjectPool#close()} method the wrap each
 *      access to the pool in a synchronized block.</li>
 *  <li>Deserialized {@link ObjectPool}s produced by this factory will not retain their idle objects. Active objects
 *      borrowed from the serialized {@link ObjectPool} must not be returned to the deserialized {@link ObjectPool}.
 *      All other behavior and settings of the {@link ObjectPool} will be maintained.</li>
 * </ul>
 * </p>
 *
 * @see BorrowPolicy
 * @see ExhaustionPolicy
 * @see LimitPolicy
 * @see TrackingPolicy
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
public final class CompositeObjectPoolFactory implements ObjectPoolFactory, Cloneable, Serializable {

    private static final long serialVersionUID = 2675590130354850408L;

    /**
     * Serialize access to {@link #config}.
     * Once JDK 1.5 is required by pool this should be changed to a ReadWriteLock.
     */
    private final transient Object lock = new Object();

    /**
     * A cached struct of these values. Note: if any setter is called this must be set to null.
     * @see #getConfig()
     */
    private transient FactoryConfig config = null;

    /**
     * The object factory to be used by pools created from this pool factory.
     */
    // XXX: Add better handling of when this instance is not Serializable
    private PoolableObjectFactory factory;

    /**
     * Configured {@link Lender} type.
     */
    private BorrowPolicy borrowPolicy = BorrowPolicy.FIFO;

    /**
     * Configured {@link Manager} type.
     */
    private ExhaustionPolicy exhaustionPolicy = ExhaustionPolicy.GROW;

    /**
     * Maximum number of idle objects in the pool.
     * A negative value means unlimited.
     * Zero means the pool will behave like a factory.
     * A positive value limits the number of idle objects.
     */
    private int maxIdle = -1;

    /**
     * Maximum number of active objects from the pool. A non-positive value means unlimited.
     *
     * @see ActiveLimitManager
     */
    private int maxActive = -1;

    /**
     * Configured {@link ActiveLimitManager} type. Not used if {@link #maxActive} is non-positive.
     */
    private LimitPolicy limitPolicy = LimitPolicy.FAIL;

    /**
     * Configured max wait time for an available object. Non-positive means wait forever.
     *
     * @see WaitLimitManager
     */
    private int maxWaitMillis = -1;

    /**
     * Configured {@link Tracker} type.
     */
    private TrackingPolicy trackingPolicy = TrackingPolicy.SIMPLE;

    /**
     * Should the object pool validate borrowed objects when they are returned.
     */
    private boolean validateOnReturn = false;

    /**
     * Idle timeout for idle objects to be evicted.
     * A non-positive value means do not evict objects just because they are idle.
     */
    private long evictIdleMillis = -1;

    /**
     * Frequency idle objects should be checked to be still valid.
     * A non-positive value means do not evict objects just because they fail to validate.
     */
    private long evictInvalidFrequencyMillis = -1;

    /**
     * Create a new object pool factory with the specified object factory.
     *
     * @param factory the object factory for this pool, must not be null.
     */
    public CompositeObjectPoolFactory(final PoolableObjectFactory factory) {
        setFactory(factory);
    }

    /**
     * Create and return a new <code>ObjectPool</code>.
     *
     * @return a new {@link ObjectPool}
     * @throws IllegalStateException when this pool factory is not configured properly
     */
    public ObjectPool createPool() throws IllegalStateException {
        return createPool(getConfig());
    }

    /**
     * Create and return a new {@link ObjectPool} based on the settings stored in <code>config</code>.
     *
     * @param config the settings to use to construct said pool.
     * @return a new {@link ObjectPool}
     * @throws IllegalStateException when this pool factory is not configured properly
     */
    static ObjectPool createPool(final FactoryConfig config) throws IllegalStateException {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null.");
        }
        return new CompositeObjectPool(config.factory, getList(config), getManager(config), getLender(config),
                getTracker(config), config.validateOnReturn, config);
    }

    /**
     * Choose a {@link List} implementation optimized for this pool's behavior.
     *
     * @param config
     * @return a {@link List} implementation optimized for this pool's behavior.
     */
    private static List getList(final FactoryConfig config) {
        final List list; // LIFO is more suited to an ArrayList, FIFO is more suited to a LinkedList
        if (BorrowPolicy.NULL.equals(config.borrowPolicy) || config.maxIdle == 0) {
            // an empty pool can use an empty list.
            list = Collections.EMPTY_LIST;

        } else if (BorrowPolicy.LIFO.equals(config.borrowPolicy) || BorrowPolicy.SOFT_LIFO.equals(config.borrowPolicy)) {
            // pre-allocate the backing array if the max size is known.
            if (config.maxIdle >= 0) {
                list = new ArrayList(config.maxIdle);
            } else {
                list = new ArrayList();
            }

        } else {
            // For small list sizes the cost of shuffling the items in an array down one spot
            // is cheaper than for LinkedList to manage it's internal structures.
            // The threshold (10) was based on some benchmarks on some 1.4 and 1.5 JVMs was between 10 to 25
            if (0 <= config.maxIdle && config.maxIdle <= 10) {
                list = new ArrayList(config.maxIdle);
            } else {
                list = new LinkedList();
            }
        }
        return list;
    }

    /**
     * Choose a {@link Lender} based on this factory's settings.
     *
     * @return a new lender for an object pool.
     * @param config
     */
    private static Lender getLender(final FactoryConfig config) {
        final BorrowPolicy borrowPolicy = config.borrowPolicy;
        Lender lender;
        if (config.maxIdle != 0) {
            if (BorrowPolicy.FIFO.equals(borrowPolicy)) {
                lender = new FifoLender();
            } else if (BorrowPolicy.LIFO.equals(borrowPolicy)) {
                lender = new LifoLender();
            } else if (BorrowPolicy.SOFT_FIFO.equals(borrowPolicy)) {
                lender = new SoftLender(new FifoLender());
            } else if (BorrowPolicy.SOFT_LIFO.equals(borrowPolicy)) {
                lender = new SoftLender(new LifoLender());
            } else if (BorrowPolicy.NULL.equals(borrowPolicy)) {
                lender = new NullLender();
            } else {
                throw new IllegalStateException("No clue what this borrow type is: " + borrowPolicy);
            }
        } else {
            lender = new NullLender();
        }

        // If the lender is a NullLender then there is no point to evicting idle objects that aren't there.
        if (!(lender instanceof NullLender)) {
            // If the evictIdleMillis were less than evictInvalidFrequencyMillis
            // then the InvalidEvictorLender would never run.
            if (config.evictInvalidFrequencyMillis > 0 && config.evictIdleMillis < config.evictInvalidFrequencyMillis) {
                lender = new InvalidEvictorLender(lender);
                ((InvalidEvictorLender)lender).setValidationFrequencyMillis(config.evictInvalidFrequencyMillis);
            }

            if (config.evictIdleMillis > 0) {
                lender = new IdleEvictorLender(lender);
                ((IdleEvictorLender)lender).setIdleTimeoutMillis(config.evictIdleMillis);
            }
        }
        return lender;
    }

    /**
     * Compose a {@link Manager} based on this factory's settings.
     *
     * @param config
     * @return a new manager for an object pool.
     */
    private static Manager getManager(final FactoryConfig config) {
        Manager manager;
        final ExhaustionPolicy exhaustionPolicy = config.exhaustionPolicy;
        if (ExhaustionPolicy.GROW.equals(exhaustionPolicy)) {
            manager = new GrowManager();
        } else if (ExhaustionPolicy.FAIL.equals(exhaustionPolicy)) {
            if (BorrowPolicy.NULL.equals(config.borrowPolicy)) {
                throw new IllegalStateException("Using the NULL borrow type with the FAIL exhaustion behavior is pointless.");
            } else if (config.maxIdle == 0) {
                throw new IllegalStateException("Using the FAIL exhaustion behavior with a max of zero idle objects is pointless.");
            }
            manager = new FailManager();
        } else {
            throw new IllegalStateException("No clue what this exhaustion behavior is: " + exhaustionPolicy);
        }

        final int maxActive = config.maxActive;
        if (maxActive > 0) {
            if (TrackingPolicy.NULL.equals(config.trackingPolicy)) {
                throw new IllegalStateException("Using the NULL tracker and limiting pool size is not valid.");
            }
            final LimitPolicy limitPolicy = config.limitPolicy;
            if (LimitPolicy.FAIL.equals(limitPolicy)) {
                manager = new FailLimitManager(manager);
            } else if (LimitPolicy.WAIT.equals(limitPolicy)) {
                manager = new WaitLimitManager(manager);
                ((WaitLimitManager)manager).setMaxWaitMillis(config.maxWaitMillis);
            } else {
                throw new IllegalStateException("No clue what this wait behavior is: " + limitPolicy);
            }
            ((ActiveLimitManager)manager).setMaxActive(maxActive);
        }

        if (config.maxIdle > 0) {
            manager = new IdleLimitManager(manager);
            ((IdleLimitManager)manager).setMaxIdle(config.maxIdle);
        }
        return manager;
    }

    /**
     * Choose a {@link Tracker} based on this factory's settings.
     *
     * @param config
     * @return a new tracker for an object pool.
     */
    private static Tracker getTracker(final FactoryConfig config) {
        final Tracker tracker;
        final TrackingPolicy trackingPolicy = config.trackingPolicy;
        if (TrackingPolicy.SIMPLE.equals(trackingPolicy)) {
            tracker = new SimpleTracker();
        } else if (TrackingPolicy.NULL.equals(trackingPolicy)) {
            tracker = new NullTracker();
        } else if (TrackingPolicy.REFERENCE.equals(trackingPolicy)) {
            tracker = new ReferenceTracker();
        } else if (TrackingPolicy.DEBUG.equals(trackingPolicy)) {
            tracker = new DebugTracker();
        } else {
            throw new IllegalStateException("No clue what this tracking type is: " + trackingPolicy);
        }
        return tracker;
    }

    /**
     * Create or use a cached {@link FactoryConfig} with the factory's current settings.
     *
     * @return this factory's current settings in a "struct".
     */
    private FactoryConfig getConfig() {
        synchronized (lock) {
            if (config == null) {
                config = new FactoryConfig(this);
            }
            return config;
        }
    }

    /**
     * Object factory used by pools created from this factory.
     *
     * @return object factory used by pools created from this factory.
     */
    public PoolableObjectFactory getFactory() {
        return factory;
    }

    /**
     * Set the object factory used by pools created from this factory.
     *
     * @param factory the object factory to be used by pools.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     */
    public void setFactory(final PoolableObjectFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("object factory must not be null.");
        }
        synchronized (lock){
            config = null;
            this.factory = factory;
        }
    }

    /**
     * Order in which objects are borrowed from the pool.
     *
     * @return the order in which objects are pooled.
     */
    public BorrowPolicy getBorrowPolicy() {
        return borrowPolicy;
    }

    /**
     * Set the order in which objects are borrowed from the pool.
     *
     * <p>Note: this doesn't mean much if {@link #setMaxIdle(int) maxIdle} is set to zero.</p>
     *
     * @param borrowPolicy the order in which objects are pooled.
     * @throws IllegalArgumentException when <code>borrowPolicy</code> is <code>null</code>.
     */
    public void setBorrowPolicy(final BorrowPolicy borrowPolicy) throws IllegalArgumentException {
        if (borrowPolicy == null) {
            throw new IllegalArgumentException("borrow type must not be null.");
        }
        synchronized (lock){
            config = null;
            this.borrowPolicy = borrowPolicy;
        }
    }

    /**
     * Behavior of the pool when all idle objects have been exhausted.
     *
     * @return behavior of the pool when all idle objects have been exhausted.
     */
    public ExhaustionPolicy getExhaustionPolicy() {
        return exhaustionPolicy;
    }

    /**
     * Set the behavior for when the pool is exhausted of any idle objects.
     *
     * @param exhaustionPolicy the desired exhausted behavior of the pool.
     * @throws IllegalArgumentException when <code>exhaustionPolicy</code> is <code>null</code>.
     */
    public void setExhaustionPolicy(final ExhaustionPolicy exhaustionPolicy) throws IllegalArgumentException {
        if (exhaustionPolicy == null) {
            throw new IllegalArgumentException("exhaustion behavior must not be null.");
        }
        synchronized (lock){
            config = null;
            this.exhaustionPolicy = exhaustionPolicy;
        }
    }

    /**
     * Maximum number of idle objects in the pool.
     * A negative value means unlimited.
     * Zero means the pool will behave like a factory.
     * A positive value limits the number of idle objects.
     *
     * @return a non-negative value is the maximum number of idle objects in the pool, else unlimited.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Set the maximum number of idle objects in the pool.
     * A negative value means unlimited.
     * Zero means the pool will behave like a factory.
     * A positive value limits the number of idle objects.
     *
     * @param maxIdle a non-negative value is the maximum number of idle objects in the pool, else unlimited.
     */
    public void setMaxIdle(final int maxIdle) {
        synchronized (lock){
            config = null;
            this.maxIdle = maxIdle;
        }
    }

    /**
     * Maximum number of objects associated with this pool. A non-positive value means there is no limit.
     *
     * @return if &gt; 0 the the maximum number of objects else no size limit.
     */
    public int getMaxActive() {
        return maxActive;
    }

    /**
     * Set the maximum number of objects associated with this pool. Any non-positive value means there is no limit.
     *
     * @param maxActive the limit of active and idle objects in the pool or &lt;= 0 for no limit.
     */
    public void setMaxActive(final int maxActive) {
        synchronized (lock){
            config = null;
            this.maxActive = maxActive;
        }
    }

    /**
     * Behavior of the pool when the limit of active objects has been reached.
     *
     * @return the behavior of the pool when the limit of active objects has been reached.
     * @see #getMaxWaitMillis()
     */
    public LimitPolicy getLimitPolicy() {
        return limitPolicy;
    }

    /**
     * Set the behavior of when the pool's limit of active objects has been reached.
     *
     * @param limitPolicy action to take if the pool has an object limit and is exhausted.
     * @throws IllegalArgumentException when <code>limitPolicy</code> is <code>null</code>.
     * @see #setMaxWaitMillis(int)
     */
    public void setLimitPolicy(final LimitPolicy limitPolicy) throws IllegalArgumentException {
        if (limitPolicy == null) {
            throw new IllegalArgumentException("limit behavior must not be null.");
        }
        synchronized (lock){
            config = null;
            this.limitPolicy = limitPolicy;
        }
    }

    /**
     * Wait time in milliseconds for an object to become available to the pool when the {@link LimitPolicy#WAIT WAIT}
     * <code>LimitPolicy</code> is used.
     *
     * @return the wait time for an object to become available to the pool.
     * @see #getLimitPolicy()
     * @see LimitPolicy#WAIT
     */
    public int getMaxWaitMillis() {
        return maxWaitMillis;
    }

    /**
     * Set the wait time in milliseconds for an object to become available to the pool when it was exhausted.
     * This has no effect unless the {@link #setLimitPolicy(LimitPolicy) limit behavior}
     * is set to {@link LimitPolicy#WAIT}.
     *
     * @param maxWaitMillis the milliseconds to wait for an available object in the pool or &lt;= 0 for no limit.
     * @see #setLimitPolicy(LimitPolicy)
     */
    public void setMaxWaitMillis(final int maxWaitMillis) {
        synchronized (lock){
            config = null;
            this.maxWaitMillis = maxWaitMillis;
        }
    }

    /**
     * Type of tracking for active objects while they are borrowed from the pool.
     *
     * @return Type of tracking for active objects while they are borrowed from the pool.
     */
    public TrackingPolicy getTrackingPolicy() {
        return trackingPolicy;
    }

    /**
     * Set the type of tracking for active objects while they are borrowed from the pool.
     *
     * @param trackingPolicy type of tracking for active objects.
     * @throws IllegalArgumentException when <code>trackingPolicy</code> is <code>null</code>.
     */
    public void setTrackingPolicy(final TrackingPolicy trackingPolicy) throws IllegalArgumentException {
        if (trackingPolicy == null) {
            throw new IllegalArgumentException("tracker type must not be null.");
        }
        synchronized (lock){
            config = null;
            this.trackingPolicy = trackingPolicy;
        }
    }

    /**
     * Are objects validated when they are returned to the pool.
     *
     * @return are objects validated when they are returned to the pool.
     */
    public boolean isValidateOnReturn() {
        return validateOnReturn;
    }

    /**
     * Set if objects should be validated when returned to the pool.
     *
     * @param validateOnReturn <code>true</code> to validate objects when they are added to the pool.
     */
    public void setValidateOnReturn(final boolean validateOnReturn) {
        synchronized (lock){
            config = null;
            this.validateOnReturn = validateOnReturn;
        }
    }

    /**
     * Idle timeout for idle objects to be evicted.
     * A non-positive value means do not evict objects just because they are idle.
     *
     * @return if positive the time in milliseconds to evict idle objects.
     */
    public long getEvictIdleMillis() {
        synchronized (lock) {
            return evictIdleMillis;
        }
    }

    /**
     * Set the idle timeout for idle objects to be evicted.
     * A non-positive value means do not evict objects just because they are idle.
     *
     * @param evictIdleMillis if positive the time in milliseconds to evict idle objects.
     */
    public void setEvictIdleMillis(final long evictIdleMillis) {
        synchronized (lock){
            config = null;
            this.evictIdleMillis = evictIdleMillis;
        }
    }

    /**
     * Frequency idle objects should be checked to be still valid.
     * A non-positive value means do not evict objects just because they fail to validate.
     *
     * @return if positive the frequency in milliseconds to check that idle objects are still valid.
     */
    public long getEvictInvalidFrequencyMillis() {
        synchronized (lock) {
            return evictInvalidFrequencyMillis;
        }
    }

    /**
     * Set the frequency idle objects should be checked to be still valid.
     * A non-positive value means do not evict objects just because they fail to validate.
     *
     * @param evictInvalidFrequencyMillis if positive the frequency in milliseconds to check that
     * idle objects are still valid.
     */
    public void setEvictInvalidFrequencyMillis(final long evictInvalidFrequencyMillis) {
        synchronized (lock){
            config = null;
            this.evictInvalidFrequencyMillis = evictInvalidFrequencyMillis;
        }
    }

    /**
     * Create a copy of this factory with the existing values.
     * @return a copy of this {@link ObjectPoolFactory}.
     * @throws CloneNotSupportedException if {@link Object#clone()} does.
     */
    public Object clone() throws CloneNotSupportedException {
        final CompositeObjectPoolFactory copf = (CompositeObjectPoolFactory)super.clone();
        // Cannot share KeyedPoolableObjectFactoryAdapter between instances because of ThreadLocal usage
        if (copf.factory instanceof KeyedPoolableObjectFactoryAdapter) {
            KeyedPoolableObjectFactoryAdapter kpofa = (KeyedPoolableObjectFactoryAdapter)copf.factory;
            copf.factory = new KeyedPoolableObjectFactoryAdapter(kpofa.getDelegate());
        }
        return copf;
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("CompositeObjectPoolFactory{");
        sb.append(getConfig());
        sb.append('}');
        return sb.toString();
    }

    /**
     * A "struct" to capture this factory's settings so {@link CompositeObjectPool}s can print friendly
     * {@link Object#toString()} messages and be {@link Object#clone()}. For the meaning of each field see
     * the field of {@link CompositeObjectPoolFactory} with the same name.
     */
    static final class FactoryConfig implements Serializable {
        private static final long serialVersionUID = 8055395905602482612L;

        /** @see CompositeObjectPoolFactory#factory */
        // XXX: Add better handling of when this instance is not Serializable
        private final PoolableObjectFactory factory;

        /** @see CompositeObjectPoolFactory#borrowPolicy */
        private final BorrowPolicy borrowPolicy;

        /** @see CompositeObjectPoolFactory#exhaustionPolicy */
        private final ExhaustionPolicy exhaustionPolicy;

        /** @see CompositeObjectPoolFactory#maxIdle */
        private final int maxIdle;

        /** @see CompositeObjectPoolFactory#maxActive */
        private final int maxActive;

        /** @see CompositeObjectPoolFactory#limitPolicy */
        private final LimitPolicy limitPolicy;

        /** @see CompositeObjectPoolFactory#maxWaitMillis */
        private final int maxWaitMillis;

        /** @see CompositeObjectPoolFactory#trackingPolicy */
        private final TrackingPolicy trackingPolicy;

        /** @see CompositeObjectPoolFactory#validateOnReturn */
        private final boolean validateOnReturn;

        /** @see CompositeObjectPoolFactory#evictIdleMillis */
        private final long evictIdleMillis;

        /** @see CompositeObjectPoolFactory#evictInvalidFrequencyMillis */
        private final long evictInvalidFrequencyMillis;

        /**
         * Convenience constructor. This <b>must</b> be called from a synchronized context to be thread-safe.
         */
        FactoryConfig(final CompositeObjectPoolFactory copf) {
            this(copf.getFactory(), copf.getBorrowPolicy(), copf.getExhaustionPolicy(), copf.getMaxIdle(),
                    copf.getMaxActive(), copf.getLimitPolicy(), copf.getMaxWaitMillis(), copf.getTrackingPolicy(),
                    copf.isValidateOnReturn(), copf.getEvictIdleMillis(), copf.getEvictInvalidFrequencyMillis());
        }

        FactoryConfig(final PoolableObjectFactory factory, final BorrowPolicy borrowPolicy,
                      final ExhaustionPolicy exhaustionPolicy, final int maxIdle, final int maxActive,
                      final LimitPolicy limitPolicy, final int maxWaitMillis, final TrackingPolicy trackingPolicy,
                      final boolean validateOnReturn, final long evictIdleMillis,
                      final long evictInvalidFrequencyMillis) {
            this.factory = factory;
            this.borrowPolicy = borrowPolicy;
            this.exhaustionPolicy = exhaustionPolicy;
            this.maxIdle = maxIdle;
            this.maxActive = maxActive;
            this.limitPolicy = limitPolicy;
            this.maxWaitMillis = maxWaitMillis;
            this.trackingPolicy = trackingPolicy;
            this.validateOnReturn = validateOnReturn;
            this.evictIdleMillis = evictIdleMillis;
            this.evictInvalidFrequencyMillis = evictInvalidFrequencyMillis;
        }


        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append("factory=").append(factory);
            sb.append(", borrowPolicy=").append(borrowPolicy);
            sb.append(", exhaustionPolicy=").append(exhaustionPolicy);
            sb.append(", maxIdle=").append(maxIdle);
            sb.append(", maxActive=").append(maxActive);
            if (maxActive > 0) {
                sb.append(", limitPolicy=").append(limitPolicy);
                if (LimitPolicy.WAIT.equals(limitPolicy)) {
                    sb.append(", maxWaitMillis=").append(maxWaitMillis);
                }
            }
            sb.append(", trackingPolicy=").append(trackingPolicy);
            sb.append(", validateOnReturn=").append(validateOnReturn);
            if (evictIdleMillis > 0) {
                sb.append(", evictIdleMillis=").append(evictIdleMillis);
            }
            if (evictInvalidFrequencyMillis > 0) {
                sb.append(", evictInvalidFrequencyMillis=").append(evictInvalidFrequencyMillis);
            }
            return sb.toString();
        }
    }
}