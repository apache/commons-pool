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
import java.util.List;
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
 * @since #.#
 */
final class GrowManager extends AbstractManager implements Serializable {

    private static final long serialVersionUID = 1225746308358794900L;

    private static final Timer GENERATOR_TIMER = CompositeObjectPool.COMPOSITE_TIMER;

    /**
     * Retreives the next object from the pool, creating new objects if the pool has been exhausted.
     *
     * @return a new or activated object.
     * @throws Exception when {@link PoolableObjectFactory#makeObject()} fails.
     */
    public Object nextFromPool() throws Exception {
        final List pool = objectPool.getPool();
        Object obj = null;

        // Drain until good or empty
        Generator generator = null;
        while (obj == null) {
            synchronized (pool) {
                if (objectPool.getLender().size() > 0) {
                    obj = objectPool.getLender().borrow();
                    if (generator != null) {
                        generator.cancel();
                    }
                }
            }

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
            } else {
                if (generator == null) {
                    generator = new Generator();
                    GENERATOR_TIMER.schedule(generator, 0L);
                }
                synchronized (pool) {
                    pool.wait(10L);
                    obj = generator.getObj();
                }
            }
        }
        if (generator != null) {
            generator.cancel();
        }
        return obj;
    }

    private class Generator extends TimerTask {
        private boolean returnToThread = true;
        private volatile boolean done = false;
        private volatile Object obj;
        private Throwable throwable;

        private final Object key;
        private final ThreadLocal keys;

        Generator() {
            final CompositeKeyedObjectPool ockop = objectPool.getOwningCompositeKeyedObjectPool();
            if (ockop != null) {
                keys = ockop.getKeys();
                key = keys.get();
            } else {
                keys = null;
                key = null;
            }
        }

        public void run() {
            try {
                if (keys != null) {
                    keys.set(key);
                }
                final Object obj;
                try {
                    obj = objectPool.getFactory().makeObject();
                } catch (Throwable t) {
                    throwable = t;
                    return;
                }
                final List pool = objectPool.getPool();
                synchronized (pool) {
                    done = true;
                    if (returnToThread) {
                        this.obj = obj;
                        pool.notifyAll();
                    } else {
                        try {
                            objectPool.getFactory().passivateObject(obj);
                        } catch (Throwable t) {
                            throwable = t;
                            return;
                        }
                        objectPool.returnObjectToPoolManager(obj);
                    }
                }
            } finally {
                if (keys != null) {
                    keys.set(null);
                }
            }
        }

        public boolean cancel() {
            synchronized (objectPool.getPool()) {
                returnToThread = false;
            }
            return super.cancel();
        }

        public Object getObj() throws Exception {
            assert Thread.holdsLock(objectPool.getPool());
            if (throwable != null) {
                if (throwable instanceof Exception) {
                    throw (Exception)throwable;
                } else if (throwable instanceof Error) {
                    throw (Error)throwable;
                } else {
                    throw new Exception(throwable);
                }
            }
            return obj;
        }
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

    public String toString() {
        return "GrowManager{}";
    }
}
