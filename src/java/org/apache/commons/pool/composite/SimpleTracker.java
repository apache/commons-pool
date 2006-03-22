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

/**
 * Pretends to track active objects by using a counter. This implementation just increments and decrements a counter and
 * trusts the user will properly return an object. If the counter goes negative it throws an
 * {@link IllegalStateException}.
 *
 * @see TrackingType#SIMPLE
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class SimpleTracker implements Tracker, Serializable {

    private static final long serialVersionUID = -7300626285071421255L;

    /**
     * The number of "borrowed" or active objects from the pool.
     */
    private transient volatile int active = 0;

    /**
     * Increment {@link #active} by one.
     *
     * @param obj was borrowed from the pool.
     */
    public void borrowed(final Object obj) {
        active++;
    }

    /**
     * Decrement {@link #active} by one.
     *
     * @param obj being returned to the pool.
     * @throws IllegalStateException when more objects have been returned than borrowed.
     */
    public void returned(final Object obj) throws IllegalStateException {
        if (--active < 0) {
            active++; // undo, object won't be returned
            throw new IllegalStateException("More objects returned than were borrowed. Most recent object: " + obj);
        }
    }

    /**
     * The number of "borrowed" or active objects from the pool.
     *
     * @return the number of "borrowed" active objects.
     */
    public int getBorrowed() {
        return active;
    }

    public String toString() {
        return "SimpleTracker{" +
                "active=" + active +
                '}';
    }
}