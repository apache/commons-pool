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
 * Doesn't actually track active objects. Not compatiable with any {@link ActiveLimitManager} implementation.
 *
 * @see TrackingPolicy#NULL
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class NullTracker implements Tracker, Serializable {

    private static final long serialVersionUID = -5846405955762769678L;

    /**
     * Don't do anything.
     *
     * @param obj was borrowed from the pool.
     */
    public void borrowed(final Object obj) {
    }

    /**
     * Don't do anything.
     *
     * @param obj being returned to the pool.
     */
    public void returned(final Object obj) {
    }

    /**
     * Unsupported, returns a negative value.
     * 
     * @return a negative value.
     */
    public int getBorrowed() {
        return -1;
    }

    public String toString() {
        return "NullTracker{}";
    }
}
