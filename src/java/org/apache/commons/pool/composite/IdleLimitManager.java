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
import java.util.ListIterator;

/**
 * A {@link Manager} that limit the number of idle objects associate with the pool.
 *
 * @see CompositeObjectPoolFactory#setMaxIdle(int)
 * @author Sandy McArthur
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
final class IdleLimitManager extends DelegateManager implements Serializable {

    private static final long serialVersionUID = -8037318859951361774L;

    /**
     * Maximum number of idle objects in the idle object pool.
     */
    private int maxIdle = 0;

    IdleLimitManager(final Manager delegate) throws IllegalArgumentException {
        super(delegate);
    }

    /**
     * Possible remove an idle object and delegates to another {@link Manager}.
     *
     * @param obj the object to return to the pool.
     */
    public void returnToPool(final Object obj) {
        assert Thread.holdsLock(objectPool.getPool());
        if (maxIdle > 0 && maxIdle <= objectPool.getNumIdle()) {
            // XXX Does this remove the most stale object in
            final ListIterator iter = objectPool.getLender().listIterator();
            iter.next();
            iter.remove();
        }
        super.returnToPool(obj);
    }

    /**
     * Maximum number of idle objects in the idle object pool.
     *
     * @return maximum number of idle objects in the idle object pool.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Set the maximum number of idle objects in the idle object pool.
     *
     * @param maxIdle maximum number of idle objects.
     */
    public void setMaxIdle(final int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public String toString() {
        return "IdleLimitManager{" +
                "maxIdle=" + maxIdle +
                ", delegate=" + super.toString() +
                '}';
    }
}