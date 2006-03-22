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
 * Throws {@link NoSuchElementException} when the max number of active objects has been reached.
 *
 * @see LimitBehavior#FAIL
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class FailLimitManager extends ActiveLimitManager implements Serializable {

    private static final long serialVersionUID = 4055528475643314990L;

    /**
     * Create a manager that when the limit of active objects has been reached throws a {@link NoSuchElementException}.
     *
     * @param delegate the manager to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    FailLimitManager(final Manager delegate) throws IllegalArgumentException {
        super(delegate);
    }

    /**
     * Checks to see how many total objects are in play and will not allow more than that.
     * Delegates object management to another {@link Manager}.
     *
     * @return a new or activated object.
     * @throws NoSuchElementException if the pool is empty and no new object can be created.
     * @throws Exception usually from {@link PoolableObjectFactory} methods.
     */
    public Object nextFromPool() throws NoSuchElementException, Exception {
        if (objectPool.getNumActive() < getMaxActive()) {
            return super.nextFromPool();
        } else {
            throw new NoSuchElementException("No more than " + getMaxActive() + " objects allowed from this pool. Currently " + objectPool.getNumActive() + " have been borrowed.");
        }
    }

    public String toString() {
        return "FailLimitManager{" +
                "maxActive=" + getMaxActive() +
                ", delegate=" + super.toString() +
                "}";
    }
}