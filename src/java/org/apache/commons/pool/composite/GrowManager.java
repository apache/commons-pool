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

import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;
import java.util.TimerTask;
import java.util.Timer;

/**
 * Grows the pool automatically when it is exhausted.
 * Whe the idle object pool is exhausted a new new object will be created via
 * {@link PoolableObjectFactory#makeObject()}.
 *
 * @see ExhaustionPolicy#GROW
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
final class GrowManager extends AbstractManager implements Serializable {

    private static final long serialVersionUID = 1225746308358794900L;
    private static final Timer PREFILL_TIMER = CompositeObjectPool.COMPOSITE_TIMER;

    private final Object avgLock = new Object();
    private long activateAvg = 0;
    private long makeAvg = 0;

    /**
     * Retreives the next object from the pool, creating new objects if the pool has been exhausted.
     *
     * @return a new or activated object.
     * @throws Exception when {@link PoolableObjectFactory#makeObject()} fails.
     */
    public Object nextFromPool() throws Exception {
        assert Thread.holdsLock(objectPool.getPool());
        Object obj = null;

        final long startActivateTime = System.currentTimeMillis();
        // Drain until good or empty
        while (objectPool.getLender().size() > 0 && obj == null) {
            obj = objectPool.getLender().borrow();

            if (obj != null) {
                obj = activateOrDestroy(obj);

                try {
                    if (obj != null && !objectPool.getFactory().validateObject(obj)) {
                        deferDestroyObject(obj);
                        obj = null; // try again
                    }
                } catch (Exception e) {
                    deferDestroyObject(obj);
                    obj = null; // try again
                }
            }
        }
        if (obj != null) {
            updateActivateTimings(startActivateTime, System.currentTimeMillis());
        } else {
            final long startMakeTime = System.currentTimeMillis();
            obj = objectPool.getFactory().makeObject();
            updateMakeTimings(startMakeTime, System.currentTimeMillis());
        }

        schedulePrefill();
        return obj;
    }

    /**
     * {@link PoolableObjectFactory#activateObject(Object) Activate} an object or if that fails
     * {@link PoolableObjectFactory#destroyObject(Object) destroy} it.
     *
     * @param obj the object to be activated or destroyed.
     * @return the activated object or null if it was destroyed.
     */
    private Object activateOrDestroy(final Object obj) {
        try {
            objectPool.getFactory().activateObject(obj);
        } catch (Exception e) {
            deferDestroyObject(obj);
            return null; // try again
        }
        return obj;
    }

    /**
     * Update the moving average of how long it takes to activate and validate idle objects.
     * @param start start of activation and validation
     * @param end end of activation and validation
     */
    private void updateActivateTimings(final long start, final long end) {
        final long elapsed = end - start;
        if (elapsed > 0L) {
            synchronized (avgLock) {
                activateAvg = (activateAvg * 9L + elapsed) / 10L;
            }
        }
    }

    /**
     * Update the moving average of how long it takes to make a new objects.
     * @param start start of makeObject
     * @param end end of makeObject
     */
    private void updateMakeTimings(final long start, final long end) {
        final long elapsed = end - start;
        if (elapsed > 0L) {
            synchronized (avgLock) {
                makeAvg = (makeAvg * 9L + elapsed) / 10L;
            }
        }
    }

    /**
     * Does {@link PoolableObjectFactory#makeObject} take a relativly long time compared to
     * {@link PoolableObjectFactory#activateObject} and {@link PoolableObjectFactory#validateObject}.
     * @return <code>true</code> if {@link PoolableObjectFactory#makeObject} takes a long time.
     */
    private boolean isMakeObjectExpensive() {
        synchronized (avgLock) {
            // XXX: This is a guess at an optimal balance.
            // Considering makeObject  to be expensive if it takes 3 times longer than activation takes.
            // That is based on a benchmark by Peter Steijn for database connection pooling.
            return activateAvg > 0L && activateAvg * 3L < makeAvg;
        }
    }

    /**
     * Schedule a <code>PrefillTask</code> if the idle object pool is empty
     * and <code>makeObject</code> is relatively expensive.
     */
    private void schedulePrefill() {
        if (objectPool.getLender().size() == 0 && isMakeObjectExpensive()) {
            // When we are part of a CompositeKeyedObjectPool we need to pass the key to the Timer's thread.
            final CompositeKeyedObjectPool ckop = objectPool.getOwningCompositeKeyedObjectPool();
            if (ckop != null) {
                final Object key = ckop.getKeys().get();
                PREFILL_TIMER.schedule(new KeyedPrefillTask(key), 0L);
            } else {
                PREFILL_TIMER.schedule(new PrefillTask(), 0L);
            }
        }
    }

    public String toString() {
        return "GrowManager{makeObjectExpensive=" + isMakeObjectExpensive() + "}";
    }

    /**
     * A <code>TimerTask</code> that will add another object if the pool is empty.
     */
    private class PrefillTask extends TimerTask {
        public void run() {
            try {
                if (objectPool.getNumIdle() == 0) {
                    objectPool.addObject();
                }
            } catch (Exception e) {
                // swallowed
            }
        }
    }

    /**
     * A <code>TimerTask</code> that will add another object if the pool is empty.
     */
    private class KeyedPrefillTask extends TimerTask {
        private final Object key;

        KeyedPrefillTask(final Object key) {
            this.key = key;
        }

        public void run() {
            ThreadLocal keys = null;
            try {
                if (objectPool.getNumIdle() == 0) {
                    keys = objectPool.getOwningCompositeKeyedObjectPool().getKeys();
                    keys.set(key);
                    objectPool.addObject();
                }
            } catch (Exception e) {
                // swallowed
            } finally {
                // clear the key
                if (keys != null) {
                    keys.set(null);
                }
            }
        }
    }
}