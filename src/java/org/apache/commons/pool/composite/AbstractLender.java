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

import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;

/**
 * A base {@link Lender} implementation that provides the common implementations of methods.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
abstract class AbstractLender implements Lender, Serializable {

    private static final long serialVersionUID = -1338771389484034430L;

    /**
     * CompositeObjectPool this {@link Lender} is associated with.
     */
    private CompositeObjectPool objectPool;

    public void setCompositeObjectPool(final CompositeObjectPool objectPool) throws IllegalArgumentException, IllegalStateException {
        if (objectPool == null) {
            throw new IllegalArgumentException("objectPool must not be null.");
        }
        if (this.objectPool != null) {
            throw new IllegalStateException("Manager cannot be reattached.");
        }
        this.objectPool = objectPool;
    }

    public abstract Object borrow();

    public void repay(final Object obj) {
        final List pool = getObjectPool().getPool();
        synchronized (pool) {
            pool.add(obj);
            pool.notifyAll();
        }
    }

    public ListIterator listIterator() {
        final List pool = getObjectPool().getPool();
        assert Thread.holdsLock(pool);
        return pool.listIterator();
    }

    public int size() {
        final List pool = getObjectPool().getPool();
        synchronized (pool) {
            return getObjectPool().getPool().size();
        }
    }

    /**
     * Get the CompositeObjectPool this {@link Lender} is associated with.
     *
     * @return the CompositeObjectPool this {@link Lender} is associated with.
     */
    protected CompositeObjectPool getObjectPool() {
        return objectPool;
    }
}