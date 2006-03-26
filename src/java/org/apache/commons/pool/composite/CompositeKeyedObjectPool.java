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
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A keyed object pool that is basically implemented as a map of object pools. Almost all the functionality in this
 * implementation derives from an {@link ObjectPoolFactory} that creates {@link ObjectPool}s as needed to be associated
 * with a key.
 *
 * <p>Instances of this class should only be instantiated by {@link CompositeKeyedObjectPoolFactory} or other
 * package-local classes that are intimately familiar with it's proper usage.</p>
 *
 * @see CompositeObjectPoolFactory
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
final class CompositeKeyedObjectPool implements KeyedObjectPool, Cloneable, Serializable {

    private static final long serialVersionUID = -5886463772111164686L;

    /**
     * Map of keys to {@link ObjectPool}s.
     */
    private final Map objectPools = new HashMap();

    /**
     * Object pool factory used to create new object pools as needed.
     */
    // XXX: Add better handling of when this instance is not Serializable
    private final ObjectPoolFactory poolFactory;

    /**
     * Thread local used to pass keys through an object pool to a {@link KeyedPoolableObjectFactory}.
     * If this is null it means a {@link KeyedPoolableObjectFactoryAdapter}
     * is not being used and this isn't needed.
     */
    private final transient ThreadLocal keys;

    /**
     * Is this object pool still open.
     */
    private volatile boolean open = true;

    CompositeKeyedObjectPool(final ObjectPoolFactory poolFactory) throws IllegalArgumentException {
        if (poolFactory == null) {
            throw new IllegalArgumentException("object pool factory must not be null.");
        }
        this.poolFactory = poolFactory;
        if (poolFactory instanceof CompositeObjectPoolFactory) {
            final PoolableObjectFactory pof = ((CompositeObjectPoolFactory)poolFactory).getFactory();
            if (pof instanceof KeyedPoolableObjectFactoryAdapter) {
                keys = new ThreadLocal();
                ((KeyedPoolableObjectFactoryAdapter)pof).setCompositeKeyedObjectPool(this);
            } else {
                keys = null;
            }
        } else {
            keys = null;
        }
    }

    /**
     * Get or create an object pool for the <code>key</code>.
     *
     * @param key which object pool to get or create.
     * @return the object pool for <code>key</code>.
     */
    private ObjectPool getObjectPool(final Object key) {
        ObjectPool pool;
        synchronized (objectPools) {
            pool = (ObjectPool)objectPools.get(key);
            if (pool == null) {
                pool = poolFactory.createPool();
                objectPools.put(key, pool);
                // Tell CompositeObjectPools that we own them.
                if (pool instanceof CompositeObjectPool) {
                    ((CompositeObjectPool)pool).setOwningCompositeKeyedObjectPool(this);
                }
            }
        }
        return pool;
    }

    /**
     * Obtain an instance from this pool for the specified <code>key</code>.
     * By contract, clients <strong>must</strong> return the borrowed object using
     * {@link #returnObject(Object,Object) <code>returnObject</code>},
     * or a related method as defined in an implementation or sub-interface,
     * using a <code>key</code> that is equivalent to the one used to
     * borrow the instance in the first place.
     *
     * @param key the key used to obtain the object
     * @return an instance from this pool.
     * @throws Exception if there is an unexpected problem.
     */
    public Object borrowObject(final Object key) throws Exception {
        assertOpen();
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            return pool.borrowObject();
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Return an instance to this pool. By contract, <code>obj</code>
     * <strong>must</strong> have been obtained using
     * {@link #borrowObject borrowObject} or a related method as defined
     * in an implementation or sub-interface using a <code>key</code> that
     * is equivalent to the one used to borrow the <code>Object</code> in
     * the first place.
     *
     * @param key the key used to obtain the object
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    public void returnObject(final Object key, final Object obj) {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            pool.returnObject(obj);
        } catch (Exception e) {
            // swallowed
            // XXX: In pool 3 this catch block will not be necessary and shouled be removed
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Invalidates an object from this pool. By contract, <code>obj</code>
     * <strong>must</strong> have been obtained using
     * {@link #borrowObject borrowObject} or a related method as defined
     * in an implementation or sub-interface using a <code>key</code> that
     * is equivalent to the one used to borrow the <code>Object</code> in
     * the first place.
     * <p>
     * This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.
     * If the connection should be validated before or after borrowing,
     * then the {@link PoolableObjectFactory#validateObject} method should be
     * used instead.
     * </p>
     *
     * @param key the key used to obtain the object
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    public void invalidateObject(final Object key, final Object obj) {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            pool.invalidateObject(obj);
        } catch (Exception e) {
            // swallowed
            // XXX: In pool 3 this catch block will not be necessary and shouled be removed
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Create an object using the {@link #setFactory factory} and place it into the pool.
     * <code>addObject</code> is useful for "pre-loading" a pool with idle objects.
     *
     * @param key the key used to obtain the object
     * @throws Exception if there is an unexpected problem.
     */
    public void addObject(final Object key) throws Exception {
        assertOpen();
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            pool.addObject();
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Returns the number of instances corresponding to the given <code>key</code> currently idle in this pool.
     *
     * @param key the key
     * @return the number of instances corresponding to the given <code>key</code> currently idle in this pool
     */
    public int getNumIdle(final Object key) {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            return pool.getNumIdle();
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Returns the number of instances
     * currently borrowed from but not yet returned
     * to this pool corresponding to the
     * given <code>key</code>.
     *
     * @param key the key
     * @return the number of instances corresponding to the given <code>key</code> currently borrowed in this pool
     */
    public int getNumActive(final Object key) {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            return pool.getNumActive();
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Returns the total number of instances currently idle in this pool.
     *
     * @return the total number of instances currently idle in this pool
     */
    public int getNumIdle() {
        int numIdle = 0;
        synchronized (objectPools) {
            final Iterator iter = objectPools.values().iterator();
            while (iter.hasNext()) {
                final ObjectPool pool = (ObjectPool)iter.next();
                numIdle += pool.getNumIdle();
            }
        }
        return numIdle;
    }

    /**
     * Returns the total number of instances current borrowed
     * from this pool but not yet returned.
     *
     * @return the total number of instances currently borrowed from this pool
     */
    public int getNumActive() {
        int numActive = 0;
        synchronized (objectPools) {
            final Iterator iter = objectPools.values().iterator();
            while (iter.hasNext()) {
                final ObjectPool pool = (ObjectPool)iter.next();
                numActive += pool.getNumActive();
            }
        }
        return numActive;
    }

    /**
     * Clears this pool, removing all pooled instances.
     *
     * @throws Exception if there is an unexpected problem.
     */
    public void clear() throws Exception {
        synchronized (objectPools) {
            final Iterator iter = objectPools.keySet().iterator();
            while (iter.hasNext()) {
                final Object key = iter.next();
                clear(key);
            }
        }
    }

    /**
     * Clears the specified pool, removing all pooled instances
     * corresponding to the given <code>key</code>.
     *
     * @param key the key to clear
     * @throws Exception if there is an unexpected problem.
     */
    public void clear(final Object key) throws Exception {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            pool.clear();

            // Remove this pool if we know no more active objects will be returned.
            synchronized (objectPools) {
                if (pool.getNumActive() == 0) {
                    objectPools.remove(key);
                    pool.close();
                }
            }
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Close this pool, and free any resources associated with it.
     */
    public void close() {
        open = false;
        Thread.yield(); // encourage any threads currently executing in the pool to finish first.
        synchronized (objectPools) {
            final Iterator iter = objectPools.keySet().iterator();
            while (iter.hasNext()) {
                final Object key = iter.next();
                close(key);
            }
        }
    }

    private void close(final Object key) {
        final ObjectPool pool = getObjectPool(key);
        try {
            if (keys != null) {
                keys.set(key);
            }
            pool.close();

            // Remove this pool if we know no more active objects will be returned.
            synchronized (objectPools) {
                if (pool.getNumActive() == 0) {
                    objectPools.remove(key);
                    pool.close();
                }
            }
        } catch (Exception e) {
            // swallowed
            // XXX: In pool 3 this catch block will not be necessary and shouled be removed
        } finally {
            if (keys != null) {
                keys.set(null); // unset key
            }
        }
    }

    /**
     * Sets the {@link KeyedPoolableObjectFactory factory} I use
     * to create new instances (optional operation).
     * @param factory the {@link KeyedPoolableObjectFactory} I use to create new instances.
     * @throws IllegalStateException when the factory cannot be set at this time
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    public void setFactory(final KeyedPoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Replacing the factory not supported. Create a new pool instance instead.");
    }

    /**
     * Throws an {@link IllegalStateException} when the pool has been closed.
     * This should not be called by methods that are for returning objects to the pool.
     * It's better to silently discard those objects over interupting program flow.
     *
     * @throws IllegalStateException when the pool has been closed.
     */
    private void assertOpen() throws IllegalStateException {
        if (!open) {
            throw new IllegalStateException("keyed pool has been closed.");
        }
    }

    /**
     * Return a thread local with this thread's current key. This is needed by {@link KeyedPoolableObjectFactoryAdapter}
     * so it can adapt a {@link KeyedPoolableObjectFactory} for use with an {@link ObjectPool} which needs a
     * {@link PoolableObjectFactory}.
     *
     * @return a ThreadLocal with the current key for this thread.
     */
    ThreadLocal getKeys() {
        return keys;
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer(128);
        sb.append("CompositeKeyedObjectPool{");
        sb.append("poolFactory=").append(poolFactory);
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

    /**
     * Creates a new keyed object pool with the same settings as this one. The new instance will not contian any
     * existing idle objects nor should you return active objects to it.
     *
     * @return a new keyed object pool with the same settings.
     */
    public Object clone() throws CloneNotSupportedException {
        if (!getClass().equals(CompositeKeyedObjectPool.class)) {
            throw new CloneNotSupportedException("Subclasses must not call super.clone()");
        }
        if (poolFactory instanceof CompositeObjectPoolFactory) {
            final PoolableObjectFactory pof = ((CompositeObjectPoolFactory)poolFactory).getFactory();
            if (pof instanceof KeyedPoolableObjectFactoryAdapter) {
                final KeyedPoolableObjectFactory kopf = ((KeyedPoolableObjectFactoryAdapter)pof).getDelegate();
                final CompositeObjectPoolFactory opf = (CompositeObjectPoolFactory)((CompositeObjectPoolFactory)poolFactory).clone();
                opf.setFactory(new KeyedPoolableObjectFactoryAdapter(kopf));
                return new CompositeKeyedObjectPool(opf);
            }
        }
        return new CompositeKeyedObjectPool(poolFactory);
    }

    /**
     * The {@link ThreadLocal} keys is not serializable and final, must create a new instance for this to be correct.
     */
    private Object readResolve() throws ObjectStreamException {
        final CompositeKeyedObjectPool pool = new CompositeKeyedObjectPool(poolFactory);
        if (!open) {
            try {
                pool.close();
            } catch (Exception e) {
                // don't know how this could happen...
                final InvalidObjectException ioe = new InvalidObjectException("pool close failed on serialized closed pool.");
                ioe.initCause(e);
                throw ioe;
            }
        }
        return pool;
    }
}