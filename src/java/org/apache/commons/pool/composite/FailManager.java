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

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;
import java.util.NoSuchElementException;

/**
 * Throws a {@link NoSuchElementException} when the idle pool is exhausted. If you want to add objects to the pool you
 * should call {@link ObjectPool#addObject()}.
 *
 * @see ExhaustionPolicy#FAIL
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
final class FailManager extends AbstractManager implements Serializable {

    private static final long serialVersionUID = 2245468648709521897L;

    /**
     * Thread local to hold on to the first cause that prevented us from providing an object from the pool.
     * This must be cleared else it will either leak memory or provide confusing results in future invocations of
     * {@link #nextFromPool()}.
     */
    private final transient ThreadLocal cause = new ThreadLocal();

    /**
     * Retreives the next object from the pool. Objects from the pool will be
     * {@link PoolableObjectFactory#activateObject(Object) activated} and
     * {@link PoolableObjectFactory#validateObject(Object) validated}.
     * No objects will be {@link PoolableObjectFactory#makeObject() created}. Use {@link ObjectPool#addObject()} to
     * populate the pool.
     *
     * @return a new or activated object.
     * @throws NoSuchElementException if the pool is empty and no new object can be created.
     * @throws Exception usually from {@link PoolableObjectFactory} methods.
     */
    public Object nextFromPool() throws Exception {
//        assert Thread.holdsLock(objectPool.getPool());
        Object obj = null;
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
                    updateCause(e);
                    deferDestroyObject(obj);
                    obj = null; // try again
                }
            }
        }

        if (obj == null) {
            final Throwable t = (Throwable)cause.get();
            final NoSuchElementException nsee = new NoSuchElementException("Pool configued to fail when exhausted.");
            if (t != null) {
                nsee.initCause(t);
                cause.set(null); // clear reference
            }
            throw nsee;
        }

        cause.set(null); // clear reference
        return obj;
    }

    /**
     * {@link PoolableObjectFactory#activateObject(Object) Activate} an object or if that fails
     * {@link PoolableObjectFactory#destroyObject(Object) destroy} it. If an exception is thrown it is saved to the
     * {@link #cause} so that it can be part of the {@link NoSuchElementException} if this manager fails to get another
     * object.
     *
     * @param obj the object to be activated or destroyed.
     * @return the activated object or null if it was destroyed.
     */
    private Object activateOrDestroy(final Object obj) {
        try {
            objectPool.getFactory().activateObject(obj);
        } catch (Exception e) {
            updateCause(e);
            deferDestroyObject(obj);
            return null; // try again
        }
        return obj;
    }

    /**
     * Set the {@link #cause} linking any previous causes.
     *
     * @param t the real {@link Throwable#initCause(Throwable) cause}.
     */
    private void updateCause(final Throwable t) {
        final Throwable previousCause = (Throwable)cause.get();
        if (previousCause != null) {
            t.initCause(previousCause);
        }
        cause.set(t);
    }

    public String toString() {
        return "FailManager{}";
    }
}