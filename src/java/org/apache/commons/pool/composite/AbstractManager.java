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

import java.io.Serializable;
import java.util.NoSuchElementException;

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
     * Return an object to the pool. Object will be {@link PoolableObjectFactory#passivateObject(Object) passivated}.
     *
     * @param obj the object to return to the pool.
     * @throws Exception as thrown by {@link PoolableObjectFactory#passivateObject(Object)}.
     */
    public void returnToPool(final Object obj) throws Exception {
        assert Thread.holdsLock(objectPool.getPool());
        objectPool.getLender().repay(obj);
    }
}