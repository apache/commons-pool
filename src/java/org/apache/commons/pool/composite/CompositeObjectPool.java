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
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

/**
 * An object pool who's behavior and functionality is determined by composition.
 *
 * <p>Instances of this class should only be instantiated by {@link CompositeObjectPoolFactory} or other package-local
 * classes that are intimately familiar with it's proper usage.</p>
 *
 * <p>Composite object pools are divided into three parts.</p>
 *
 * <p>{@link Lender}: a lender's sole responsibilty is to maintain idle objects. A lender will never touch an object
 * that is considered to be active with the possible exception of an idle object being validated for possible eviction.
 * </p>
 *
 * <p>{@link Manager}: a manager does most of the heavy lifting between a {@link Lender} and a {@link Tracker}. A
 * manager is responsible for activating and validating idle objects and passivating and possibly validating active
 * objects. It is also responsible for controling the growth and size of the pool.</p>
 *
 * <p>{@link Tracker}: a tracker's sole responsibility is keeping track of active objects borrowed from the pool. A
 * tracker will never touch an object that is considered to be idle.</p>
 *
 * @see CompositeObjectPoolFactory
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class CompositeObjectPool implements ObjectPool, Cloneable, Serializable {

    private static final long serialVersionUID = -5874499972956918952L;

    /**
     * Shared Timer for use by various parts of the composite implementation.
     * XXX: When Java 1.5 is acceptable convert this to a java.util.concurrent.ScheduledThreadPoolExecutor
     */
    static final Timer COMPOSITE_TIMER = new Timer(true);

    /**
     * Factory used by this pool.
     */
    // XXX: Add better handling of when this instance is not Serializable
    private final PoolableObjectFactory factory;

    /**
     * The collection of idle pooled objects.
     */
    private final transient List pool;

    /**
     * Maintains idle objects and the order in which objects are borrowed.
     */
    private final Lender lender;

    /**
     * Manages object transitions and controls the growth of the pool.
     */
    private final Manager manager;

    /**
     * Tracks active objects.
     */
    private final Tracker tracker;

    /**
     * Should returning objects be validated.
     */
    private final boolean validateOnReturn;

    /**
     * Is this object pool still open.
     */
    private volatile boolean open = true;

    /**
     * The configuration of the {@link CompositeObjectPoolFactory} that created this instance. This is needed for
     * cloning and helps {@link #toString()} be more friendly.
     */
    private final CompositeObjectPoolFactory.FactoryConfig factoryConfig;

    /**
     * When not-<code>null</code> this pool is part of a {@link CompositeKeyedObjectPool}.
     */
    private CompositeKeyedObjectPool owningCompositeKeyedObjectPool = null;

    CompositeObjectPool(final PoolableObjectFactory factory, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn) {
        this(factory, manager, lender, tracker, validateOnReturn, null);
    }

    CompositeObjectPool(final PoolableObjectFactory factory, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn, final CompositeObjectPoolFactory.FactoryConfig factoryConfig) {
        this(factory, new LinkedList(), manager, lender, tracker, validateOnReturn, factoryConfig);
    }

    CompositeObjectPool(final PoolableObjectFactory factory, final List pool, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn, final CompositeObjectPoolFactory.FactoryConfig factoryConfig) {
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null.");
        }
        if (pool == null) {
            throw new IllegalArgumentException("pool cannot be null.");
        }
        if (manager == null) {
            throw new IllegalArgumentException("manager cannot be null.");
        }
        if (lender == null) {
            throw new IllegalArgumentException("lender cannot be null.");
        }
        if (tracker == null) {
            throw new IllegalArgumentException("tracker cannot be null.");
        }
        this.factory = factory;
        this.pool = pool;
        this.manager = manager;
        this.lender = lender;
        this.tracker = tracker;
        this.validateOnReturn = validateOnReturn;
        this.factoryConfig = factoryConfig;

        updateCompositeObjectPools();
    }

    /**
     * Provide {@link Manager}s and {@link Lender}s with a reference back to this {@link CompositeObjectPool} if needed.
     */
    private void updateCompositeObjectPools() {
        lender.setCompositeObjectPool(this);
        manager.setCompositeObjectPool(this);
    }

    /**
     * The order in which objects are borrowed.
     *
     * @return order in which objects are borrowed.
     */
    Lender getLender() {
        return lender;
    }

    /**
     * Factory used by this pool.
     *
     * @return factory used by this pool.
     */
    PoolableObjectFactory getFactory() {
        return factory;
    }

    /**
     * The collection of idle pooled objects.
     *
     * @return collection of idle pooled objects.
     */
    List getPool() {
        return pool;
    }

    /**
     * Set the owner of this pool.
     * @param ckop the owner of this pool
     */
    void setOwningCompositeKeyedObjectPool(final CompositeKeyedObjectPool ckop) {
        if (owningCompositeKeyedObjectPool != null) {
            throw new IllegalStateException("CompositeObjectPools cannot change ownership.");
        }
        owningCompositeKeyedObjectPool = ckop;
    }

    /**
     * Get the owner of this pool or <code>null</code>.
     * @return the owner of this pool or <code>null</code>
     */
    CompositeKeyedObjectPool getOwningCompositeKeyedObjectPool() {
        return owningCompositeKeyedObjectPool;
    }

    /**
     * Create an object using the {@link #setFactory factory} and place it into the pool.
     * <code>addObject</code> is useful for "pre-loading" a pool with idle objects.
     *
     * @throws Exception if there is an unexpected problem.
     */
    public void addObject() throws Exception {
        assertOpen();
        final Object obj = factory.makeObject();
        factory.passivateObject(obj);
        synchronized (pool) {
            // if the pool was closed between the asserOpen and the synchronize then discard returned objects
            if (isOpen()) {
                manager.returnToPool(obj);
            } else {
                factory.destroyObject(obj);
            }
        }
    }

    /**
     * Obtain an instance from this pool. By contract, clients
     * <strong>must</strong> return the borrowed instance using
     * {@link #returnObject returnObject} or a related method as defined
     * in an implementation or sub-interface.
     * <p>
     * The behaviour of this method when the pool has been exhausted will
     * depend on the requested configuration.
     * </p>
     *
     * @return an instance from this pool.
     * @throws Exception if there is an unexpected problem.
     */
    public Object borrowObject() throws Exception {
        assertOpen();

        return internalBorrowObject();
    }

    /**
     * Basicly just the {@link #borrowObject()} method that doesn't check if the pool has been {@link #close() closed}.
     * Needed by {@link #clear()}.
     *
     * @return an instance from this pool.
     * @throws Exception if there is an unexpected problem.
     * @see #borrowObject()
     */
    private Object internalBorrowObject() throws Exception {
        final Object obj;

        synchronized (pool) {
            obj = manager.nextFromPool();

            // Must be synced else getNumActive() could be wrong in WaitLimitManager
            tracker.borrowed(obj);
        }

        return obj;
    }

    /**
     * Return an instance to this pool. By contract, <code>obj</code>
     * <strong>must</strong> have been obtained using
     * {@link #borrowObject() borrowObject} or a related method as defined
     * in an implementation or sub-interface.
     *
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    public void returnObject(final Object obj) {
        if (validateOnReturn) {
            if (!factory.validateObject(obj)) {
                invalidateObject(obj);
                return;
            }
        }

        try {
            factory.passivateObject(obj);
        } catch (Exception e) {
            invalidateObject(obj);
            return;
        }

        synchronized (pool) {
            // if the pool is closed, discard returned objects
            if (isOpen()) {
                tracker.returned(obj);
                manager.returnToPool(obj);
            } else {
                invalidateObject(obj);
            }
        }
    }

    /**
     * Invalidates an object from the pool
     * By contract, <code>obj</code> <strong>must</strong> have been obtained
     * using {@link #borrowObject() borrowObject}
     * or a related method as defined in an implementation
     * or sub-interface.
     * <p>
     * This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.
     * If the connection should be validated before or after borrowing,
     * then the {@link PoolableObjectFactory#validateObject} method should be
     * used instead.
     * </p>
     *
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    public void invalidateObject(final Object obj) {
        synchronized (pool) {
            if (pool.contains(obj)) {
                throw new IllegalStateException("An object currently in the pool cannot be invalidated.");
            }

            tracker.returned(obj);
            try {
                factory.destroyObject(obj);
            } catch (Exception e) {
                // ignored
            } finally {
                pool.notifyAll(); // tell any wait managers to try again.
            }
        }
    }

    /**
     * Clears any objects sitting idle in the pool, releasing any
     * associated resources (optional operation).
     *
     * @throws UnsupportedOperationException if this implementation does not support the operation
     * @throws Exception if there is an unexpected problem.
     */
    public void clear() throws Exception, UnsupportedOperationException {
        synchronized (pool) {
            while (pool.size() > 0) {
                final Object obj = internalBorrowObject();
                invalidateObject(obj);
            }
            if (pool instanceof ArrayList) {
                ((ArrayList)pool).trimToSize();

            }
        }
    }

    /**
     * Close this pool, and free any resources associated with it.
     */
    public void close() {
        open = false;
        Thread.yield(); // encourage any threads currently executing in the pool to finish first.
        synchronized (pool) {
            try {
                clear();
            } catch (Exception e) {
                // swallowed
            }
            pool.notifyAll(); // Tell any WaitLimitManagers currently blocking to exit
        }
    }

    /**
     * Always throws <code>UnsupportedOperationException</code>.
     *
     * @param factory the {@link PoolableObjectFactory} used to create new instances.
     * @throws UnsupportedOperationException if this implementation does not support the operation
     */
    public void setFactory(final PoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException {
        if (this.factory != factory) {
            throw new UnsupportedOperationException("Replacing the factory not supported. Create a new pool instance instead.");
        }
    }

    /**
     * Return the number of instances currently borrowed from this pool.
     *
     * @return the number of instances currently borrowed from this pool
     */
    public int getNumActive() {
        return tracker.getBorrowed();
    }

    /**
     * Return the number of instances currently idle in this pool.
     * This may be considered an approximation of the number
     * of objects that can be {@link #borrowObject borrowed}
     * without creating any new instances.
     *
     * @return the number of instances currently idle in this pool
     */
    public int getNumIdle() {
        return lender.size();
    }

    /**
     * Is this pool still open.
     * This is needed by the {@link WaitLimitManager} so it can terminate when the pool is {@link #close() closed}.
     * @return <code>false</code> if this pool has been closed.
     */
    boolean isOpen() {
        return open;
    }

    /**
     * Throws an {@link IllegalStateException} when the pool has been closed.
     * This should not be called by methods that are for returning objects to the pool.
     * It's better to silently discard those objects over interupting program flow.
     *
     * @throws IllegalStateException when the pool has been closed.
     */
    private void assertOpen() throws IllegalStateException {
        if (!isOpen()) {
            throw new IllegalStateException("pool has been closed.");
        }
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer(128);
        sb.append("CompositeObjectPool{");
        if (factoryConfig != null) {
            sb.append(factoryConfig);
        } else {
            sb.append("factory=").append(factory);
            sb.append(", lender=").append(lender);
            sb.append(", manager=").append(manager);
            sb.append(", tracker=").append(tracker);
            sb.append(", validateOnReturn=").append(validateOnReturn);
        }
        sb.append(", open=").append(open);
        try {
            final int numActive = getNumActive();
            sb.append(", activeObjects=").append(numActive);
        } catch (Exception e) {
            // ignored
        }
        try {
            final int numIdle = getNumIdle();
            sb.append(", idleObjects=").append(numIdle);
        } catch (Exception e) {
            // ignored
        }
        sb.append('}');
        return sb.toString();
    }
}