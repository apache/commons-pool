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
import java.lang.ref.Reference;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class for a {@link Lender} that evicts objects from the idle object pool.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
abstract class EvictorLender extends DelegateLender implements Serializable {

    private static final long serialVersionUID = 4040627184050939757L;

    /**
     * Shared evictor timer used by all {@link EvictorLender}s.
     */
    private static final Timer EVICTOR = new Timer(true);

    /**
     * If this evictor delegates to another evictor then don't bother pruning when {@link #size()} is called because the
     * delegate will do that too.
     */
    private final boolean prune;

    /**
     * Create a lender that may evict objects from the idle object pool.
     *
     * @param delegate delegate the lender to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    protected EvictorLender(final Lender delegate) throws IllegalArgumentException {
        super(delegate);
        prune = !(delegate instanceof EvictorLender);
    }

    /**
     * Calls the delegate's {@link Lender#borrow()} method and unwrapps any {@link EvictorLender.EvictorReference}.
     *
     * @return a previously idle object.
     */
    public Object borrow() {
        final EvictorReference ref = (EvictorReference)super.borrow();
        Object obj = null;
        if (ref != null) {
            obj = ref.get();
            ref.clear();
        }
        return obj;
    }

    /**
     * Calls the delegate's {@link Lender#repay(Object)} method.
     * Calls {@link #createReference(Object)} to wrap the object.
     *
     * @param obj the object to return to the idle object pool.
     */
    public void repay(final Object obj) {
        super.repay(createReference(obj));
    }

    public ListIterator listIterator() {
        return new EvictorListIterator(super.listIterator());
    }

    /**
     * Return the size of the idle object pool. Also removes any broken {@link EvictorLender.EvictorReference}s so the
     * size is more accurate.
     *
     * @return the size of the idle object pool the lender is accessing.
     */
    public int size() {
        if (prune) {
            synchronized (getObjectPool().getPool()) {
                final Iterator iter = super.listIterator();
                while (iter.hasNext()) {
                    final EvictorReference ref = (EvictorReference)iter.next();
                    if (ref.get() == null) {
                        iter.remove();
                    }
                }
            }
        }
        return super.size();
    }

    /**
     * Wrap an idle object in an {@link EvictorLender.EvictorReference} and schedule it's eviction with the
     * {@link #getTimer() evictor timer}. {@link TimerTask}s used by the evictor timer must synchronize on the
     * {@link CompositeObjectPool#getPool() idle object pool} to be thread-safe.
     *
     * @param obj idle object to be wrapped in an {@link EvictorLender.EvictorReference}.
     * @return the wrapped idle object.
     */
    protected abstract EvictorReference createReference(Object obj);

    /**
     * Get the {@link Timer} used for eviction by this {@link EvictorLender}.
     *
     * @return the {@link Timer} used for evictions.
     */
    protected final Timer getTimer() {
        // Future version may want to manage more than one TimerTask. For now one is fine.
        return EVICTOR;
    }

    /**
     * This is designed to mimick the {@link Reference} api.
     * The only reason a {@link Reference} subclass isn't used is there is no "StrongReference" implementation.
     */
    protected interface EvictorReference {
        /**
         * Returns this evictor reference's referent.
         *
         * @return The object to which this evictor reference refers,
         * or <code>null<code> if this reference object has been cleared.
         * @see Reference#get()
         */
        public Object get();

        /**
         * Clears this reference.
         *
         * @see Reference#clear()
         */
        public void clear();
    }

    /**
     * A {@link ListIterator} that unwrapps {@link EvictorLender.EvictorReference}s.
     */
    private static class EvictorListIterator implements ListIterator {
        private final ListIterator iter;

        private EvictorListIterator(final ListIterator iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * Unwrap an {@link EvictorLender.EvictorReference} and return the next object if it hasn't been evicted.
         *
         * @return an unwrapped object or <code>null</code> if an object has been evicted.
         * @see ListIterator#next()
         */
        public Object next() {
            final EvictorReference ref = (EvictorReference)iter.next();
            return ref != null ? ref.get() : null;
        }

        public boolean hasPrevious() {
            return iter.hasPrevious();
        }

        /**
         * Unwrap an {@link EvictorLender.EvictorReference} and return the previous object if it hasn't been evicted.
         *
         * @return an unwrapped object or <code>null</code> if an object has been evicted.
         * @see ListIterator#previous()
         */
        public Object previous() {
            final EvictorReference ref = (EvictorReference)iter.previous();
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