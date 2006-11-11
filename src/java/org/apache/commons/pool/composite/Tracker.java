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

/**
 * Tracks active objects. Implementations are expected to be called from a synchronized context so
 * {@link #getBorrowed()} is accurate.</p>
 *
 * <p>{@link #borrowed(Object)} for an object will always be called before {@link #returned(Object)} is called.</p>
 *
 * <p>Implementations must not make any assumptions about nor modify the state of the objects it sees. This is simply
 * a tool for active object accounting.</p>
 *
 * @see TrackingPolicy
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
interface Tracker {

    /**
     * An object has been borrowed from the pool. The object will have been
     * {@link PoolableObjectFactory#activateObject(Object) activated} and
     * {@link PoolableObjectFactory#validateObject(Object) validated} and is about to be returned to the client.
     *
     * @param obj was borrowed from the pool.
     * @throws IllegalArgumentException may be thrown if <code>null</code> is not allowed.
     */
    public void borrowed(Object obj) throws IllegalArgumentException;

    /**
     * An object is being {@link ObjectPool#returnObject(Object) returned} to the pool.
     * {@link ObjectPool#invalidateObject(Object) Invalid} objects are also "returned" via this method.
     *
     * @param obj being returned to the pool.
     * @throws IllegalArgumentException may be thrown if <code>null</code> is not allowed.
     * @throws IllegalStateException may be thrown if an object is returned that wasn't borrowed.
     */
    public void returned(Object obj) throws IllegalArgumentException, IllegalStateException;

    /**
     * The number of "borrowed" or active objects from the pool.
     *
     * @return the number of "borrowed" active objects or negative if unsupported.
     */
    public int getBorrowed();
}