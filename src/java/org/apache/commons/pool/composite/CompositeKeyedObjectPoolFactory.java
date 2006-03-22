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

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;

/**
 * {@link KeyedObjectPoolFactory} that builds a custom {@link KeyedObjectPool} via composition of custom
 * {@link ObjectPool}s.
 *
 * <p>Note: Currently the default values and behavior is effectivly inherited from {@link CompositeObjectPoolFactory},
 * review it if you are uncertian about the behavior of this factory. Future verions of this factory may not inherit
 * behavior from the {@link CompositeObjectPoolFactory}.
 * </p>
 *
 * @see CompositeObjectPoolFactory
 * @see BorrowType
 * @see ExhaustionBehavior
 * @see LimitBehavior
 * @see TrackingType
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
public final class CompositeKeyedObjectPoolFactory implements KeyedObjectPoolFactory, Cloneable, Serializable {

    private static final long serialVersionUID = 4099516083825584165L;

    /**
     * Factory to create {@link ObjectPool}s to back key.
     */
    private final CompositeObjectPoolFactory factory;

    /**
     * Create a new keyed object pool factory witht he specific keyed object factory.
     *
     * @param factory the keyed object factory for this pool, must not be null.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     * @see #setKeyedFactory(KeyedPoolableObjectFactory)
     */
    public CompositeKeyedObjectPoolFactory(final KeyedPoolableObjectFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("keyed poolable object factory must not be null.");
        }
        this.factory = new CompositeObjectPoolFactory(new KeyedPoolableObjectFactoryAdapter(factory));
    }

    /**
     * Create a new keyed object pool factory with the specific object factory. This is a convenience constructor for
     * when you have a {@link PoolableObjectFactory} but want a {@link KeyedObjectPool} and the
     * {@link PoolableObjectFactory object factory} doesn't care about the key.
     *
     * @param factory the object factory for this pool, must not be null.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     * @see #setFactory(PoolableObjectFactory)
     */
    public CompositeKeyedObjectPoolFactory(final PoolableObjectFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("poolable object factory must not be null.");
        }
        this.factory = new CompositeObjectPoolFactory(factory);
    }

    /**
     * Create a new keyed object pool factory that uses a {@link CompositeObjectPoolFactory} to create
     * {@link ObjectPool}s for each key.
     *
     * @param factory the object factory to back this keyed object factory.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     */
    public CompositeKeyedObjectPoolFactory(final CompositeObjectPoolFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("composite object pool factory must not be null.");
        }
        try {
            this.factory = (CompositeObjectPoolFactory)factory.clone();
        } catch (CloneNotSupportedException cnse) {
            // should never happen
            final IllegalArgumentException iae = new IllegalArgumentException("factory must support cloning.");
            iae.initCause(cnse);
            throw iae;
        }
    }

    /**
     * Create a new {@link KeyedObjectPool}.
     *
     * @return a new {@link KeyedObjectPool}
     */
    public KeyedObjectPool createPool() {
        try {
            // backing factory must be cloned else changing this factory's
            // settings would affect previously created keyed object pools
            return new CompositeKeyedObjectPool((ObjectPoolFactory)factory.clone());
        } catch (CloneNotSupportedException cnse) {
            final IllegalStateException ise = new IllegalStateException("backing object pool factory doesn't support cloning.");
            ise.initCause(cnse);
            throw ise;
        }
    }

    /**
     * Create a new {@link KeyedObjectPool} that uses an {@link ObjectPoolFactory} to create an internal
     * {@link ObjectPool} to back each key. Use of this method is generally discouraged but it could be used to do some
     * generally funky and intersting things.
     *
     * <p><b>There are no guarentees the {@link KeyedObjectPool} returned by this method will behave in previously
     * guarenteed way. Use at your own risk.</b></p>
     *
     * @param factory the object pool factory that creates object pools to back each key.
     * @return a keyed object pool that uses an object pool factory to create object pools to back each key.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     */
    public static KeyedObjectPool createPool(final ObjectPoolFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("object pool factory must not be null.");
        }
        return new CompositeKeyedObjectPool(factory);
    }

    /**
     * Keyed object factory or null. This returns null in the case that this pool factory is configured to use a
     * {@link PoolableObjectFactory} in which case you should use {@link #getFactory()}.
     *
     * @return keyed object factory or null if this factory is using a {@link PoolableObjectFactory}.
     */
    public KeyedPoolableObjectFactory getKeyedFactory() {
        final PoolableObjectFactory pof = factory.getFactory();
        if (pof instanceof KeyedPoolableObjectFactoryAdapter) {
            return ((KeyedPoolableObjectFactoryAdapter)pof).getDelegate();
        }
        return null;
    }

    /**
     * The keyed object factory used by keyed object pools crated by this factory. Calling this clears any value
     * previously set via {@link #setFactory(PoolableObjectFactory)}.
     *
     * @param factory the keyed object factory used by created keyed object pools.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     * @see #CompositeKeyedObjectPoolFactory(KeyedPoolableObjectFactory)
     */
    public void setKeyedFactory(final KeyedPoolableObjectFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("keyed object factory must not be null.");
        }
        setFactory(new KeyedPoolableObjectFactoryAdapter(factory));
    }

    /**
     * Object factory or null. This returns null in the case that this pool factory is configured to use a
     * {@link KeyedPoolableObjectFactory} in which case you should use {@link #getKeyedFactory()}.
     *
     * @return object factory or null if this factory is using a {@link KeyedPoolableObjectFactory}.
     */
    public PoolableObjectFactory getFactory() {
        final PoolableObjectFactory pof = factory.getFactory();
        if (pof instanceof KeyedPoolableObjectFactoryAdapter) {
            return null;
        }
        return pof;
    }

    /**
     * The object factory used by keyed object pools created by this factory. The key will be ignored. Calling this
     * clears any value previously  set via {@link #setKeyedFactory(KeyedPoolableObjectFactory)}.
     *
     * @param factory the object factory used by created keyed object pools.
     * @throws IllegalArgumentException if <code>factory</code> is <code>null</code>.
     * @see #CompositeKeyedObjectPoolFactory(PoolableObjectFactory)
     */
    public void setFactory(final PoolableObjectFactory factory) throws IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("poolable object factory must not be null.");
        }
        this.factory.setFactory(factory);
    }

    /**
     * Order in which objects are borrowed from the pool.
     *
     * @return the order in which objects are pooled.
     */
    public BorrowType getBorrowType() {
        return factory.getBorrowType();
    }

    /**
     * Set the order in which objects are borrowed from the pool.
     *
     * @param borrowType the order in which objects are pooled.
     * @throws IllegalArgumentException when <code>borrowType</code> is <code>null</code>.
     */
    public void setBorrowType(final BorrowType borrowType) throws IllegalArgumentException {
        factory.setBorrowType(borrowType);
    }

    /**
     * Behavior of the pool when all idle objects have been exhasted.
     *
     * @return behavior of the pool when all idle objects have been exhasted.
     */
    public ExhaustionBehavior getExhaustionBehavior() {
        return factory.getExhaustionBehavior();
    }

    /**
     * Set the behavior for when the pool is exhausted of any idle objects.
     *
     * @param exhaustionBehavior the desired exhausted behavior of the pool.
     * @throws IllegalArgumentException when <code>exhaustionBehavior</code> is <code>null</code>.
     */
    public void setExhaustionBehavior(final ExhaustionBehavior exhaustionBehavior) throws IllegalArgumentException {
        factory.setExhaustionBehavior(exhaustionBehavior);
    }

    /**
     * Maximum number of idle objects in the pool.
     * A negative value means unlimited.
     * Zero means the pool will behave like a factory.
     * A positve value limits the number of idle objects.
     *
     * @return a non-negative value is the maximum number of idle objects in the pool, else unlimited.
     */
    public int getMaxIdle() {
        return factory.getMaxIdle();
    }

    /**
     * Set the maximum number of idle objects in the pool.
     * A negative value means unlimited.
     * Zero means the pool will behave like a factory.
     * A positve value limits the number of idle objects.
     *
     * @param maxIdle a non-negative value is the maximum number of idle objects in the pool, else unlimited.
     */
    public void setMaxIdle(final int maxIdle) {
        factory.setMaxIdle(maxIdle);
    }

    /**
     * Maximum number of active objects borrowed from this pool. A non-positive value means there is no limit.
     *
     * @return if &gt; 0 the the maximum number of active objects else unlimited.
     */
    public int getMaxActive() {
        return factory.getMaxActive();
    }

    /**
     * Set the maximum active objects borrowed from this pool. Any non-positive value means there is no limit.
     *
     * @param maxActive the limit of active objects from the pool or &lt;= 0 for unlimited.
     */
    public void setMaxActive(final int maxActive) {
        factory.setMaxActive(maxActive);
    }

    /**
     * Behavior of the pool when the limit of active objects has been reached.
     *
     * @return the behavior of the pool when the limit of active objects has been reached.
     * @see #getMaxWaitMillis()
     */
    public LimitBehavior getLimitBehavior() {
        return factory.getLimitBehavior();
    }

    /**
     * Set the behavior of when the pool's limit of active objects has been reached.
     *
     * @param limitBehavior action to take if the pool has an active object limit and is exhausted.
     * @throws IllegalArgumentException when <code>limitBehavior</code> is <code>null</code>.
     * @see #setMaxWaitMillis(int)
     */
    public void setLimitBehavior(final LimitBehavior limitBehavior) throws IllegalArgumentException {
        factory.setLimitBehavior(limitBehavior);
    }

    /**
     * Wait time in milli-seconds for an object to become available to the pool when the {@link LimitBehavior#WAIT WAIT}
     * {@link #setLimitBehavior(LimitBehavior) limit behavior} is used.
     *
     * @return the wait time for an object to become available to the pool.
     * @see #getLimitBehavior()
     */
    public int getMaxWaitMillis() {
        return factory.getMaxWaitMillis();
    }

    /**
     * Set the wait time in milli-seconds for an object to become available to the pool when it was exhausted.
     * This has no effect unless the {@link #setLimitBehavior(LimitBehavior) limit behavior}
     * is set to {@link LimitBehavior#WAIT}.
     *
     * @param maxWaitMillis the milli-seconds to wait for an available object in the pool or &lt;= 0 for no limit.
     * @see #setLimitBehavior(LimitBehavior)
     */
    public void setMaxWaitMillis(final int maxWaitMillis) {
        factory.setMaxWaitMillis(maxWaitMillis);
    }

    /**
     * Type of tracking for active objects while they are borrowed from the pool.
     *
     * @return Type of tracking for active objects while they are borrowed from the pool.
     */
    public TrackingType getTrackerType() {
        return factory.getTrackerType();
    }

    /**
     * Set the type of tracking for active objects while they are borrowed from the pool.
     *
     * @param trackerType type of tracking for active objects.
     * @throws IllegalArgumentException when <code>trackerType</code> is <code>null</code>.
     */
    public void setTrackerType(final TrackingType trackerType) throws IllegalArgumentException {
        factory.setTrackerType(trackerType);
    }

    /**
     * Are objects validated when they are returned to the pool.
     *
     * @return are objects validated when they are returned to the pool.
     */
    public boolean isValidateOnReturn() {
        return factory.isValidateOnReturn();
    }

    /**
     * Set if objects should be validated when returned to the pool.
     *
     * @param validateOnReturn <code>true</code> to validate objects when they are added to the pool.
     */
    public void setValidateOnReturn(final boolean validateOnReturn) {
        factory.setValidateOnReturn(validateOnReturn);
    }

    /**
     * Idle timeout for idle objects to be evicted.
     * A non-positive value means do not evict objects just because they are idle.
     *
     * @return if positive the time in milli-seconds to evict idle objects.
     */
    public long getEvictIdleMillis() {
        return factory.getEvictIdleMillis();
    }

    /**
     * Set the idle timeout for idle objects to be evicted.
     * A non-positive value means do not evict objects just because they are idle.
     *
     * @param evictIdleMillis if positive the time in milli-seconds to evict idle objects.
     */
    public void setEvictIdleMillis(final long evictIdleMillis) {
        factory.setEvictIdleMillis(evictIdleMillis);
    }

    /**
     * Frequency idle objects should be checked to be still valid.
     * A non-positive value means do not evict objects just because they fail to validate.
     *
     * @return if positive the frequency in milli-seconds to check that idle objects are still valid.
     */
    public long getEvictInvalidFrequencyMillis() {
        return factory.getEvictInvalidFrequencyMillis();
    }

    /**
     * Set the frequency idle objects should be checked to be still valid.
     * A non-positive value means do not evict objects just because they fail to validate.
     *
     * @param evictInvalidFrequencyMillis if positive the frequency in milli-seconds to check that
     * idle objects are still valid.
     */
    public void setEvictInvalidFrequencyMillis(final long evictInvalidFrequencyMillis) {
        factory.setEvictInvalidFrequencyMillis(evictInvalidFrequencyMillis);
    }


    public String toString() {
        return "CompositeKeyedObjectPoolFactory{" +
                "factory=" + factory +
                '}';
    }

    /**
     * A copy of this factory with the same settings.
     *
     * @return a copy of this CompositeKeyedObjectPoolFactory.
     * @throws CloneNotSupportedException if a subclass calls this.
     */
    public Object clone() throws CloneNotSupportedException {
        if (!getClass().equals(CompositeKeyedObjectPoolFactory.class)) {
            throw new CloneNotSupportedException("Subclasses must not call super.clone()");
        }
        // factory will be cloned in the constuctor
        return new CompositeKeyedObjectPoolFactory(factory);
    }
}