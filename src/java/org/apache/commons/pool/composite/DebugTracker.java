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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps track of active objects with {@link Reference}s and logs when they are not returned to the pool.
 *
 * @see TrackingType#DEBUG
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class DebugTracker extends ReferenceTracker implements Serializable {

    private static final long serialVersionUID = -5536120104213707789L;

    /**
     * Logger for lost objects.
     */
    private static final Logger LOGGER = Logger.getLogger(DebugTracker.class.getName());

    /**
     * Message for logger.
     */
    private static final String LOG_MESSAGE = "Borrowed object was not returned to the pool.";

    /**
     * Wrap the object in a reference that records the stack.
     *
     * @param obj the object to be wrapped.
     * @return a {@link StackWeakReference} around obj.
     */
    protected IdentityReference wrapBorrowed(final Object obj) {
        return new StackWeakReference(obj, rq);
    }

    /**
     * Log that an object was lost.
     *
     * @param ref the reference to the lost former object.
     */
    protected void referenceToBeRemoved(final IdentityReference ref) {
        LOGGER.log(Level.WARNING, LOG_MESSAGE, ((StackWeakReference)ref).getStack());
    }

    public String toString() {
        return "DebugTracker{" +
                "active=" + map.size() +
                ", lost=" + getLost() +
                "}";
    }

    /**
     * A {@link WeakReference} that keeps track of the stack trace from when the reference was created.
     */
    private static final class StackWeakReference extends WeakReference implements IdentityReference {

        /**
         * The message for the {@link Throwable} that was used to capture a stack trace.
         */
        private static final String THROWABLE_MESSAGE = "Stack trace at time of borrow for: ";

        /**
         * The {@link System#identityHashCode(Object)} of the object.
         */
        private final int ident; // XXX: Going need something more than this on 64 bit systems.

        /**
         * The {@link Throwable} caputing the stack trace.
         */
        private final Throwable stack;

        /**
         * Calculate a key for <code>referent</code> and record the current stack.
         * @param referent object the new weak reference will refer to
         * @param q queue the weak reference is registered with
         * @see WeakReference#WeakReference(Object, ReferenceQueue)
         */
        StackWeakReference(final Object referent, final ReferenceQueue q) {
            super(referent, q);
            ident = System.identityHashCode(referent);
            // Once JSK 1.5 is a requirement of pool it may be worth using 1.5's Thread.getStackTrace()
            stack = new Throwable(THROWABLE_MESSAGE + referent);
        }

        public IdentityKey getKey() {
            return new IdentityKey(ident);
        }

        /**
         * The {@link Throwable} that captured the stack trace.
         *
         * @return the captured the stack trace.
         */
        public Throwable getStack() {
            return stack;
        }
    }
}