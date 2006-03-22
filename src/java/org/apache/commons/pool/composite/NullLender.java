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
import java.util.Collections;
import java.util.ListIterator;

/**
 * Neither borrow nor return objects.
 *
 * @see BorrowType#NULL
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
final class NullLender implements Lender, Serializable {

    private static final long serialVersionUID = -135471856936204860L;

    /**
     * Called once to associate this manager with an object pool by the {@link CompositeObjectPool} constructor.
     *
     * @param objectPool the pool to associate with.
     * @throws IllegalArgumentException if <code>objectPool</code> is <code>null</code>.
     * @throws IllegalStateException if this method is called more than once.
     */
    public void setCompositeObjectPool(final CompositeObjectPool objectPool) throws IllegalArgumentException, IllegalStateException {
        // nothing
    }

    /**
     * Return <code>null</code>.
     *
     * @return <code>null</code>.
     */
    public Object borrow() {
        return null;
    }

    /**
     * Discards the object.
     *
     * @param obj the object to be discarded.
     */
    public void repay(final Object obj) {
        // nothing
    }

    /**
     * Returns a list iterator from an empty list.
     *
     * @return a list iterator from an empty list.
     */
    public ListIterator listIterator() {
        return Collections.EMPTY_LIST.listIterator();
    }

    /**
     * Return <code>0</code>.
     *
     * @return <code>0</code>.
     */
    public int size() {
        return 0;
    }

    public String toString() {
        return "NULL";
    }
}