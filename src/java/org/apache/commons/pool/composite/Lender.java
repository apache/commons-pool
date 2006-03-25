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

import java.util.ListIterator;
import java.lang.ref.Reference;

/**
 * Handles how idle objects are added and removed from the idle object pool.
 * Implementations are expected to be called from a synchronized context on the idle object pool.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
interface Lender {

    /**
     * Called once to associate this manager with an object pool by the {@link CompositeObjectPool} constructor.
     *
     * @param objectPool the pool to associate with.
     * @throws IllegalArgumentException if <code>objectPool</code> is <code>null</code>.
     * @throws IllegalStateException if this method is called more than once.
     */
    public void setCompositeObjectPool(CompositeObjectPool objectPool) throws IllegalArgumentException, IllegalStateException;

    /**
     * Take an object from the idle object pool.
     *
     * @return a previously idle object.
     */
    public Object borrow();

    /**
     * Return an object to the idle object pool. Implementations must call {@link Object#notifyAll()} on the idle object
     * pool so the {@link WaitLimitManager} can work.
     *
     * @param obj the object to return to the idle object pool.
     */
    public void repay(Object obj);

    /**
     * Returns a list iterator of the elements in this pool. The list iterator should be implemented such that the first
     * element retuend by {@link ListIterator#next()} is most likely to be the least desirable idle object.
     * Implementations of the {@link Lender} interface that wrap/unwrap idle objects should do the same here.
     * Clients should be aware that it is likely that {@link ListIterator#next()} or {@link ListIterator#previous()}
     * will return null. Client that receive a null should call {@link ListIterator#remove()} but {@link Lender}s that
     * receive an unexpected null must never automatically {@link ListIterator#remove()} an element as that would break
     * the {@link ListIterator#hasNext()} and {@link ListIterator#hasPrevious()} contracts for the client.
     * Use of this list iternator <b>must be</b> synchronized on the {@link CompositeObjectPool#getPool() pool}. Clients
     * that synchronize on the {@link CompositeObjectPool#getPool() pool} should make special effort to be quick or not
     * hold on to that lock for too long of a period as no other threads will be able to access the pool at the same
     * time.
     *
     * @return a list iterator of the elements in this pool.
     */
    public ListIterator listIterator();

    /**
     * Return the size of the idle object pool.
     *
     * @return the size of the idle object pool the lender is accessing.
     */
    public int size();

    /**
     * Like a {@link Reference} but allows a "strong reference".
     */
    interface LenderReference {
        /**
         * Like {@link Reference#get}.
         */
        public Object get();
    }
}