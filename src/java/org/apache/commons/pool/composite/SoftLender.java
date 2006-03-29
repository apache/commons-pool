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

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Wraps object in a {@link SoftReference} before letting a delegate {@link Lender} add them to the idle object pool.
 * Idle objects that are garbage collected will not be {@link ObjectPool#invalidateObject(Object) invalidated}.  
 *
 * @see BorrowPolicy#SOFT_FIFO
 * @see BorrowPolicy#SOFT_LIFO
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
final class SoftLender extends DelegateLender implements Serializable {

    private static final long serialVersionUID = 8589090657730065177L;

    /**
     * Create a lnder that allows the garbage collector to remove objects from the idle object pool.
     *
     * @param delegate delegate the lender to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    SoftLender(final Lender delegate) throws IllegalArgumentException {
        super(delegate);
    }

    /**
     * Unwrap the {@link Reference} from the borrowed object.
     *
     * @return a previously idle object.
     */
    public Object borrow() {
        return ((LenderReference)super.borrow()).get();
    }

    /**
     * Wrap the object in a {@link SoftReference} before letting the delegate {@link Lender} return an object to the
     * idle object pool.
     *
     * @param obj the object to return to the idle object pool.
     */
    public void repay(final Object obj) {
        super.repay(new SoftLenderReference(obj));
    }

    /**
     * Returns a list iterator of the elements in this pool. The list iterator should be implemented such that the first
     * element retuend by {@link ListIterator#next()} is most likely to be the least desirable idle object.
     * Implementations of the {@link Lender} interface that wrap/unwrap idle objects should do the same here.
     *
     * @return a list iterator of the elements in this pool.
     */
    public ListIterator listIterator() {
        return new SoftListIterator(super.listIterator());
    }

    /**
     * Return the size of the idle object pool. Also removes any broken {@link Reference}s so the size is more accurate.
     *
     * @return the size of the idle object pool the lender is accessing.
     */
    public int size() {
        final Object lock = getObjectPool().getPool();
        synchronized (lock) {
            final Iterator iter = super.listIterator();
            while (iter.hasNext()) {
                final Reference ref = (Reference)iter.next();
                if (ref.get() == null) {
                    iter.remove();
                }
            }
        }
        return super.size();
    }

    public String toString() {
        return "Soft{" + super.toString() + "}";
    }

    /**
     * Make a {@link SoftReference} implement {@link LenderReference}.
     */
    private static class SoftLenderReference extends SoftReference implements LenderReference {
        SoftLenderReference(final Object referent) {
            super(referent);
        }
    }

    /**
     * A {@link ListIterator} that unwrapps {@link Reference}s.
     */
    private static class SoftListIterator implements ListIterator {
        private final ListIterator iter;

        private SoftListIterator(final ListIterator iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * Unwrap an {@link Reference} and return the next object if it hasn't been garbage collected.
         *
         * @return an unwrapped object or <code>null</code> if an object has been garbage collected.
         * @see ListIterator#next()
         */
        public Object next() {
            final Reference ref = (Reference)iter.next();
            return ref != null ? ref.get() : null;
        }

        public boolean hasPrevious() {
            return iter.hasPrevious();
        }

        /**
         * Unwrap an {@link Reference} and return the previous object if it hasn't been garbage collected.
         *
         * @return an unwrapped object or <code>null</code> if an object has been garbage collected.
         * @see ListIterator#previous()
         */
        public Object previous() {
            final Reference ref = (Reference)iter.previous();
            return ref != null ? ref.get() : null;
        }

        public int nextIndex() {
            return iter.nextIndex();
        }

        public int previousIndex() {
            return iter.previousIndex();
        }

        public void remove() {
            iter.remove();
        }

        public void set(final Object o) {
            iter.set(o);
        }

        public void add(final Object o) {
            iter.add(o);
        }
    }
}