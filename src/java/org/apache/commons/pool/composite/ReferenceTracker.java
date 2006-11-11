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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of active objects with {@link Reference}s and detectes when they are not returned to the pool.
 *
 * @see TrackingPolicy#REFERENCE
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
class ReferenceTracker implements Tracker, Serializable {

    private static final long serialVersionUID = 3271870427019790961L;

    /**
     * ReferenceQueue of borrowed objects so we can detect when they have been garbage collected instead of being
     * returned to the pool.
     */
    protected final transient ReferenceQueue rq = new ReferenceQueue();

    /**
     * Map used to track active objects.
     */
    protected final transient Map map = Collections.synchronizedMap(new HashMap());

    /**
     * The number of borrowed objects that were lost to the garbage collector.
     */
    private transient int lost = 0;

    public void borrowed(final Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot track null borrowed from pool.");
        }
        workQueue();
        final IdentityReference ref;
        synchronized (rq) {
            ref = wrapBorrowed(obj);
        }
        map.put(ref.getKey(), ref);
    }

    /**
     * Wrap the object in a reference.
     *
     * @param obj the object to be wrapped.
     * @return a type of reference around obj.
     */
    protected IdentityReference wrapBorrowed(final Object obj) {
        return new IdentityWeakReference(obj, rq);
    }

    /**
     * Stop tracking an active object. Remove it from the active object {@link #map} and remove it from the reference
     * queue.
     *
     * @param obj a returning active object.
     * @throws IllegalStateException when an object that wasn't brorrowed from this pool is returned.
     */
    public void returned(final Object obj) throws IllegalStateException {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot track null returned to the pool.");
        }
        workQueue();
        final IdentityKey key = new IdentityKey(obj);
        final IdentityReference ref = (IdentityReference)map.remove(key);
        if (ref != null) {
            ref.clear();
        } else {
            throw new IllegalStateException("Cannot return an object that wasn't borrowed from this pool or has already been returned to this pool. " + obj);
        }
    }

    public int getBorrowed() {
        workQueue();
        return map.size();
    }

    /**
     * Work the {@link #rq reference queue} to detected any lost objects. This calls
     * {@link #referenceToBeRemoved(ReferenceTracker.IdentityReference)} just before removal from the {@link #map}.
     */
    private void workQueue() {
        synchronized (rq) {
            for (Reference ref = rq.poll(); ref != null; ref = rq.poll()) {
                final IdentityReference identRef = (IdentityReference)ref;
                referenceToBeRemoved(identRef);
                map.remove(identRef.getKey());
                lost++;
            }
        }
    }

    /**
     * Give sub-classes a chance to take action just before a reference is removed. The parameter <code>ref</code> will
     * be a {@link Reference} aquired from the {@link #wrapBorrowed(Object)} method.
     *
     * @param ref the reference that is being removed from the reference queue.
     */
    protected void referenceToBeRemoved(final IdentityReference ref) {
        // nothing to do here
    }

    /**
     * Get the number of borrowed objects that were lost. An object is lost when it is borrowed from a pool
     * and garbage collected instead of being returned.
     *
     * @return the number of borrowed objects that were lost.
     */
    protected int getLost() {
        return lost;
    }

    public String toString() {
        return "ReferenceTracker{" +
                "active=" + map.size() +
                ", lost=" + getLost() +
                '}';
    }

    /**
     * A wrapper for an object has an {@link #equals(Object)} and {@link #hashCode()} based on the
     * {@link System#identityHashCode(Object) identityHashCode} of an object. This is needed else
     * {@link ReferenceTracker#map} could not find and track more than one object that are
     * {@link Object#equals(Object) equal}. If the JDK ever adds a WeakIdentityHashMap we should use that instead.
     */
    protected static final class IdentityKey {

        /**
         * The {@link System#identityHashCode(Object)} of the object.
         */
        private final int ident; // XXX: Going need something more than this on 64 bit systems.

        IdentityKey(final Object obj) {
            this(System.identityHashCode(obj));
        }

        IdentityKey(final int ident) {
            this.ident = ident;
        }

        public int hashCode() {
            return ident;
        }

        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            final IdentityKey that = (IdentityKey)obj;

            return ident == that.ident;
        }
    }

    /**
     * Implementator of this interface should usually also extend a {@link Reference} class.
     */
    protected interface IdentityReference {

        /**
         * A key that can be used by a map to store/find this reference but does not make this reference strongly
         * reachable.
         *
         * @return key to store/find this reference.
         */
        public IdentityKey getKey();

        /**
         * Clears this reference.
         *
         * @see Reference#clear()
         */
        public void clear();
    }

    /**
     * A {@link WeakReference} that keeps track of enough information to create an {@link ReferenceTracker.IdentityKey}.
     */
    private static final class IdentityWeakReference extends WeakReference implements IdentityReference {

        /**
         * The {@link System#identityHashCode(Object)} of the object.
         */
        private final int ident; // XXX: Going need something more than this on 64 bit systems.

        IdentityWeakReference(final Object referent, final ReferenceQueue q) {
            super(referent, q);
            ident = System.identityHashCode(referent);
        }

        public IdentityKey getKey() {
            return new IdentityKey(ident);
        }
    }
}