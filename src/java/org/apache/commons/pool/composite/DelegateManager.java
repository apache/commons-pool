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
 * Delegates all work to another manager. Subclasses should call <code>super.method(...)</code> to access the delegates.
 *
 * @author Sandy McArthur
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
abstract class DelegateManager extends AbstractManager implements Serializable {

    private static final long serialVersionUID = -8516695099130531284L;

    /**
     * The manager that actually handles the interaction with the pool.
     * This is only accessed by subclasses by calling super.method(...).
     */
    private final Manager delegate;

    /**
     * Create a manager that delegates some behavior to another manager.
     *
     * @param delegate the manager to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    protected DelegateManager(final Manager delegate) throws IllegalArgumentException {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null.");
        }
        this.delegate = delegate;
    }

    /**
     * Called once to associate this limit manager with an object pool by the {@link CompositeObjectPool} constructor.
     * This also sets the {@link Manager#setCompositeObjectPool(CompositeObjectPool)} for the {@link #delegate}.
     *
     * @param objectPool the pool to associate with.
     * @throws IllegalArgumentException if <code>objectPool</code> is <code>null</code>.
     * @throws IllegalStateException if this method is called more than once.
     */
    public void setCompositeObjectPool(final CompositeObjectPool objectPool) {
        super.setCompositeObjectPool(objectPool);
        delegate.setCompositeObjectPool(objectPool);
    }

    /**
     * Delegates to another {@link Manager}.
     *
     * @return a new or activated object.
     * @throws NoSuchElementException if the pool is empty and no new object can be created.
     * @throws Exception              usually from {@link PoolableObjectFactory} methods.
     */
    public Object nextFromPool() throws Exception {
        return delegate.nextFromPool();
    }

    /**
     * Delegates to another {@link Manager}.
     *
     * @param obj the object to return to the pool.
     */
    public void returnToPool(final Object obj) {
        delegate.returnToPool(obj);
    }

    public String toString() {
        return delegate.toString();
    }
}