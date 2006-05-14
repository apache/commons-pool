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

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A base {@link Manager} implementation that provides the common implementations of methods.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
abstract class AbstractManager implements Manager, Serializable {

    private static final long serialVersionUID = -1729636795986138892L;

    /**
     * Timer to schedule object for deferred destroyal.
     */
    private static final Timer DEFER_TIMER = CompositeObjectPool.COMPOSITE_TIMER;

    /**
     * Number of milliseconds to delay destroying an object.
     */
    private static final long DEFER_DELAY = 20L;

    /**
     * CompositeObjectPool this {@link Lender} is associated with.
     */
    protected CompositeObjectPool objectPool;

    /**
     * Called once to associate this manager with an object pool by the {@link CompositeObjectPool} constructor.
     *
     * @param objectPool the pool to associate with.
     * @throws IllegalArgumentException if <code>objectPool</code> is <code>null</code>.
     * @throws IllegalStateException if this method is called more than once.
     */
    public void setCompositeObjectPool(final CompositeObjectPool objectPool) {
        if (this.objectPool != null) {
            throw new IllegalStateException("Manager cannot be reattached.");
        }
        if (objectPool == null) {
            throw new IllegalArgumentException("objectPool must not be null.");
        }
        this.objectPool = objectPool;
    }

    /**
     * Retreives the next object from the pool. Objects from the pool will be
     * {@link PoolableObjectFactory#activateObject(Object) activated} and
     * {@link PoolableObjectFactory#validateObject(Object) validated}.
     * Newly {@link PoolableObjectFactory#makeObject() created} objects will not be activated or validated.
     *
     * @return a new or activated object.
     * @throws NoSuchElementException if the pool is empty and no new object can be created.
     * @throws Exception              usually from {@link PoolableObjectFactory} methods.
     */
    public abstract Object nextFromPool() throws Exception;

    /**
     * Return an object to the pool.
     * The Object's state will no longer be "active".
     * The Object will be passes to a delegate to be made "idle".
     *
     * @param obj the object to return to the pool.
     */
    public void returnToPool(final Object obj) {
        objectPool.getTracker().returned(obj);
        objectPool.getLender().repay(obj);
    }

    /**
     * Schedule an object for {@link PoolableObjectFactory#destroyObject destruction} at a later time.
     * @param obj the object to be destroyed.
     */
    protected void deferDestroyObject(final Object obj) {
        final CompositeKeyedObjectPool ckop = objectPool.getOwningCompositeKeyedObjectPool();
        if (ckop != null) {
            final Object key = ckop.getKeys().get();
            DEFER_TIMER.schedule(new DeferredKeyedDestroyTask(key, obj), DEFER_DELAY);
        } else {
            DEFER_TIMER.schedule(new DeferredDestroyTask(obj), DEFER_DELAY);
        }
    }

    /**
     * Schedule a {@link PoolableObjectFactory#destroyObject}.
     */
    private class DeferredDestroyTask extends TimerTask {
        private final Object obj;

        DeferredDestroyTask(final Object obj) {
            this.obj = obj;
        }

        public void run() {
            try {
                objectPool.getFactory().destroyObject(obj);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Schedule a {@link KeyedPoolableObjectFactory#destroyObject}.
     */
    private class DeferredKeyedDestroyTask extends TimerTask {
        private final Object key;
        private final Object obj;

        DeferredKeyedDestroyTask(final Object key, final Object obj) {
            this.key = key;
            this.obj = obj;
        }

        public void run() {
            ThreadLocal keys = null;
            try {
                keys = objectPool.getOwningCompositeKeyedObjectPool().getKeys();
                keys.set(key);
                objectPool.getFactory().destroyObject(obj);
            } catch (Exception e) {
                // ignore
            } finally {
                // clear the key
                if (keys != null) {
                    keys.set(null);
                }
            }
        }
    }
}