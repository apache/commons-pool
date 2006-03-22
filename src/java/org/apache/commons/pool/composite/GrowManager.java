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

/**
 * Grows the pool automatically when it is exhausted.
 * Whe the idle object pool is exhausted a new new object will be created via
 * {@link PoolableObjectFactory#makeObject()}.
 *
 * @see ExhaustionBehavior#GROW
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class GrowManager extends AbstractManager implements Serializable {

    private static final long serialVersionUID = 1225746308358794900L;

    /**
     * Retreives the next object from the pool, creating new objects if the pool has been exhausted.
     *
     * @return a new or activated object.
     * @throws Exception when {@link PoolableObjectFactory#makeObject()} fails.
     */
    public Object nextFromPool() throws Exception {
        assert Thread.holdsLock(objectPool.getPool());
        Object obj = null;
        // Drain until good or empty
        while (objectPool.getLender().size() > 0 && obj == null) {
            obj = objectPool.getLender().borrow();

            if (obj != null) {
                obj = activateOrDestroy(obj);

                try {
                    if (obj != null && !objectPool.getFactory().validateObject(obj)) {
                        objectPool.invalidateObject(obj);
                        obj = null; // try again
                    }
                } catch (Exception e1) {
                    try {
                        objectPool.getFactory().destroyObject(obj);
                    } catch (Exception e2) {
                        // ignore
                    }
                    obj = null; // try again
                }
            }
        }

        if (obj == null) {
            obj = objectPool.getFactory().makeObject();
        }
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
        } catch (Exception e1) {
            try {
                objectPool.getFactory().destroyObject(obj);
            } catch (Exception e2) {
                // ignore
            }
            return null; // try again
        }
        return obj;
    }

    public String toString() {
        return "GrowManager{}";
    }
}