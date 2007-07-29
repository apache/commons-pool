/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import java.util.TimerTask;

/**
 * A {@link Lender} that evicts objects that have been idle for a while.
 *
 * @author Sandy McArthur
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
final class IdleEvictorLender extends EvictorLender implements Serializable {

    private static final long serialVersionUID = 2422278988668384937L;

    /**
     * Time, in milliseconds, before the idle objects are evicted.
     */
    private long idleTimeoutMillis = 60L * 60L * 1000L; // 60 minute

    /**
     * Create a lender that will evict idle objects after a period of time.
     *
     * @param delegate delegate the lender to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    IdleEvictorLender(final Lender delegate) throws IllegalArgumentException {
        super(delegate);
    }

    protected EvictorReference createReference(final Object obj) {
        return new IdleEvictorReference(obj);
    }

    /**
     * Get the time, in milliseconds, before the idle objects are evicted.
     * @return the time, in milliseconds, before the idle objects are evicted.
     */
    public long getIdleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    /**
     * Set the time, in milliseconds, before the idle objects are evicted.
     *
     * @param idleTimeoutMillis the time, in milliseconds, before the idle objects are evicted.
     */
    public void setIdleTimeoutMillis(final long idleTimeoutMillis) {
        this.idleTimeoutMillis = idleTimeoutMillis;
    }

    public String toString() {
        return "IdleEvictor{" +
                "idleTimeoutMillis=" + idleTimeoutMillis +
                ", delegate=" + super.toString() +
                '}';
    }

    /**
     * An evictor reference that allows idle objects to be evicted for being idle for a period of time.
     */
    private class IdleEvictorReference implements EvictorReference {

        /**
         * The idle object.
         */
        private Object referant;

        /**
         * The timer task that when run will evict the idle object.
         */
        private final TimerTask task;

        /**
         * Create an evictor reference that allows idle objects to be evicted for being idle for a period of time.
         *
         * @param referant the idle object.
         */
        IdleEvictorReference(final Object referant) {
            this.referant = referant;
            task = new IdleEvictorTask();
            getTimer().schedule(task, idleTimeoutMillis);
        }

        public Object get() {
            synchronized (this) {
                return referant;
            }
        }

        public void clear() {
            synchronized (this) {
                task.cancel();
                if (referant instanceof EvictorReference) {
                    ((EvictorReference)referant).clear();
                }
                referant = null;
            }
        }

        /**
         * A timer task that when run evicts the idle object.
         */
        private class IdleEvictorTask extends TimerTask {

            /**
             * Evict the idle object.
             */
            public void run() {
                synchronized(IdleEvictorReference.this) {
                    clear();
                }
            }
        }
    }
}