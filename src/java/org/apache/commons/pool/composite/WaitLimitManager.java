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
import java.util.NoSuchElementException;

/**
 * Waits for an object to become available if the pool is exhausted. A {@link NoSuchElementException} is thrown if an
 * object doesn't become available in the specified amount of time. For this to work all {@link Lender}s must call
 * {@link Object#notifyAll()} on the idle object pool once they've returned an object.
 *
 * @see LimitPolicy#WAIT
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
final class WaitLimitManager extends ActiveLimitManager implements Serializable {

    private static final long serialVersionUID = 2536750269978303711L;

    private long maxWaitMillis = 0; // forever

    WaitLimitManager(final Manager delegate) throws IllegalArgumentException {
        super(delegate);
    }

    /**
     * Checks to see how many objects are in play and will not allow more than that.
     * Delegates object management to another {@link Manager}.
     *
     * @return a new or activated object.
     * @throws NoSuchElementException if the pool is empty and no new object can be created.
     * @throws Exception              usually from {@link PoolableObjectFactory} methods.
     * @throws InterruptedException   when the {@link Thread} is interrupted.
     */
    public Object nextFromPool() throws NoSuchElementException, Exception, InterruptedException {
        final Object poolLock = objectPool.getPool();
        assert Thread.holdsLock(poolLock);
        final long endTime = maxWaitMillis > 0 ? System.currentTimeMillis() + maxWaitMillis : Long.MAX_VALUE;
        while (maxWaitMillis <= 0 || endTime > System.currentTimeMillis()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interruption while waiting for an available object.");
            }
            if (objectPool.getNumActive() < getMaxActive()) {
                return super.nextFromPool();
            }
            // Don't wait if the pool was closed between the start of the while and here.
            if (objectPool.isOpen()) {
                final long waitTime = Math.max(1, endTime - System.currentTimeMillis());
                poolLock.wait(maxWaitMillis > 0 ? waitTime : 0);
            } else {
                throw new IllegalStateException("Trying to aquire an object from a closed pool.");
            }
        }
        if (objectPool.isOpen()) {
            throw new NoSuchElementException("Unable to aquire new object in allowed time of: " + maxWaitMillis);
        } else {
            throw new IllegalStateException("Trying to aquire an object from a closed pool.");
        }
    }

    /**
     * The max wait time in milliseconds for a pooled object to become available.
     *
     * @return max wait time in milliseconds for a pooled object to become available.
     */
    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }

    /**
     * Set the max wait time in milliseconds for a pooled object to become available.
     * A non-positve value means wait forever.
     *
     * @param maxWaitMillis max wait for an object to become available or &lt;= 0 for no limit.
     */
    public void setMaxWaitMillis(final long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public String toString() {
        return "WaitLimitManager{" +
                "maxActive=" + getMaxActive() +
                ", maxWaitMillis=" + maxWaitMillis +
                ", delegate=" + super.toString() +
                '}';
    }
}