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

import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;
import java.util.TimerTask;

/**
 * A {@link Lender} that evicts objects from the idle pool if they fail
 * {@link PoolableObjectFactory#validateObject(Object) validation}. 
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
final class InvalidEvictorLender extends EvictorLender implements Serializable {

    private static final long serialVersionUID = -3200445766813431919L;

    /**
     * Time, in milliseconds, between the checks that idle objects are still
     * {@link PoolableObjectFactory#validateObject(Object) valid}.
     */
    private long validationFrequencyMillis = 10L * 60L * 1000L; // 10 minute

    /**
     * Create a lender that will evict idle object if they fail to pass
     * {@link PoolableObjectFactory#validateObject(Object)}.
     *
     * @param delegate delegate the lender to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    InvalidEvictorLender(final Lender delegate) throws IllegalArgumentException {
        super(delegate);
    }

    protected EvictorReference createReference(final Object obj) {
        return new InvalidEvictorReference(obj);
    }

    /**
     * Get the time, in milliseconds, between the checks that idle objects are still
     * {@link PoolableObjectFactory#validateObject(Object) valid}.
     *
     * @return time, in milliseconds, between the checks that idle objects are still valid.
     */
    public long getValidationFrequencyMillis() {
        return validationFrequencyMillis;
    }

    /**
     * Set the time, in milliseconds, between the checks that idle objects are still
     * {@link PoolableObjectFactory#validateObject(Object) valid}.
     *
     * @param validationFrequencyMillis time, in milliseconds, between the checks that idle objects are still valid.
     * @throws IllegalArgumentException if validationFrequencyMillis is negative
     */
    public void setValidationFrequencyMillis(final long validationFrequencyMillis) throws IllegalArgumentException {
        if (validationFrequencyMillis < 0) {
            throw new IllegalArgumentException("validationFrequencyMillis must not be negative. was: " + validationFrequencyMillis);
        }
        this.validationFrequencyMillis = validationFrequencyMillis;
    }

    public String toString() {
        return "InvalidEvictor{" +
                "validationFrequencyMillis=" + validationFrequencyMillis +
                ", delegate=" + super.toString() +
                '}';
    }

    /**
     * An evictor reference that evicts idle objects that fail
     * {@link PoolableObjectFactory#validateObject(Object) validation}.
     */
    private class InvalidEvictorReference implements EvictorReference {

        /**
         * The idle object.
         */
        private Object referant;

        /**
         * The timer task that when run will validate and possibly evict the idle object.
         */
        private final TimerTask task;

        /**
         * Create an evictor reference that checks if idle objects are still valid.
         *
         * @param referant the idle object.
         */
        InvalidEvictorReference(final Object referant) {
            this.referant = referant;
            task = new InvalidEvictorTask();
            getTimer().schedule(task, validationFrequencyMillis, validationFrequencyMillis);
        }

        public Object get() {
            return referant;
        }

        public void clear() {
            task.cancel();
            if (referant instanceof EvictorReference) {
                ((EvictorReference)referant).clear();
            }
            referant = null;
        }

        /**
         * A TimerTask that checks the {@link PoolableObjectFactory#validateObject(Object) validity} of it idle object.
         * If the idle object fails validation then the {@link InvalidEvictorLender.InvalidEvictorReference} is
         * {@link InvalidEvictorReference#clear() cleared}.
         */
        private class InvalidEvictorTask extends TimerTask {

            /**
             * Check the idle object for {@link PoolableObjectFactory#validateObject(Object) validity} and
             * {@link InvalidEvictorReference#clear() clear} it if it fails.
             */
            public void run() {
                // Skip some synchronization if we can
                if (referant == null) {
                    cancel();
                    return;
                }

                final PoolableObjectFactory factory = getObjectPool().getFactory();
                synchronized(getObjectPool().getPool()) {
                    if (referant == null) {
                        cancel();
                        return;
                    }
                    try {
                        factory.activateObject(referant);
                        if (factory.validateObject(referant)) {
                            factory.passivateObject(referant);
                        } else {
                            factory.destroyObject(referant);
                            clear();
                        }
                    } catch (Exception e) {
                        clear();
                    }
                }
            }
        }
    }
}