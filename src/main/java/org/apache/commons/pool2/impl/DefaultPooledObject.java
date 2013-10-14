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
package org.apache.commons.pool2.impl;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectState;
import org.apache.commons.pool2.TrackedUse;

/**
 * This wrapper is used to track the additional information, such as state, for
 * the pooled objects.
 * <p>
 * This class is intended to be thread-safe.
 *
 * @param <T> the type of object in the pool
 *
 * @version $Revision: $
 *
 * @since 2.0
 */
public class DefaultPooledObject<T> implements PooledObject<T> {

    private final T object;
    private PooledObjectState state = PooledObjectState.IDLE; // @GuardedBy("this") to ensure transitions are valid
    private final long createTime = System.currentTimeMillis();
    private volatile long lastBorrowTime = createTime;
    private volatile long lastUseTime = createTime;
    private volatile long lastReturnTime = createTime;
    private volatile boolean logAbandoned = false;
    private volatile Exception borrowedBy = null;
    private volatile Exception usedBy = null;

    public DefaultPooledObject(T object) {
        this.object = object;
    }

    /**
     * Obtain the underlying object that is wrapped by this instance of
     * {@link PooledObject}.
     */
    @Override
    public T getObject() {
        return object;
    }

    /**
     * Obtain the time (using the same basis as
     * {@link System#currentTimeMillis()}) that this object was created.
     */
    @Override
    public long getCreateTime() {
        return createTime;
    }

    /**
     * Obtain the time in milliseconds that this object last spent in the the
     * active state (it may still be active in which case subsequent calls will
     * return an increased value).
     */
    @Override
    public long getActiveTimeMillis() {
        // Take copies to avoid threading issues
        long rTime = lastReturnTime;
        long bTime = lastBorrowTime;

        if (rTime > bTime) {
            return rTime - bTime;
        } else {
            return System.currentTimeMillis() - bTime;
        }
    }

    /**
     * Obtain the time in milliseconds that this object last spend in the the
     * idle state (it may still be idle in which case subsequent calls will
     * return an increased value).
     */
    @Override
    public long getIdleTimeMillis() {
        return System.currentTimeMillis() - lastReturnTime;
    }

    @Override
    public long getLastBorrowTime() {
        return lastBorrowTime;
    }

    @Override
    public long getLastReturnTime() {
        return lastReturnTime;
    }

    /**
     * Return an estimate of the last time this object was used.  If the class
     * of the pooled object implements {@link TrackedUse}, what is returned is
     * the maximum of {@link TrackedUse#getLastUsed()} and
     * {@link #getLastBorrowTime()}; otherwise this method gives the same
     * value as {@link #getLastBorrowTime()}.
     *
     * @return the last time this object was used
     */
    @Override
    public long getLastUsedTime() {
        if (object instanceof TrackedUse) {
            return Math.max(((TrackedUse) object).getLastUsed(), lastUseTime);
        } else {
            return lastUseTime;
        }
    }

    /**
     * Orders instances based on idle time - i.e. the length of time since the
     * instance was returned to the pool. Used by the GKOP idle object evictor.
     * <p>
     * Note: This class has a natural ordering that is inconsistent with
     *       equals if distinct objects have the same identity hash code.
     */
    @Override
    public int compareTo(PooledObject<T> other) {
        final long lastActiveDiff =
                this.getLastReturnTime() - other.getLastReturnTime();
        if (lastActiveDiff == 0) {
            // Make sure the natural ordering is broadly consistent with equals
            // although this will break down if distinct objects have the same
            // identity hash code.
            // see java.lang.Comparable Javadocs
            return System.identityHashCode(this) - System.identityHashCode(other);
        }
        // handle int overflow
        return (int)Math.min(Math.max(lastActiveDiff, Integer.MIN_VALUE), Integer.MAX_VALUE);
    }


    /**
     * Provides a String form of the wrapper for debug purposes. The format is
     * not fixed and may change at any time.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Object: ");
        result.append(object.toString());
        result.append(", State: ");
        synchronized (this) {
            result.append(state.toString());
        }
        return result.toString();
        // TODO add other attributes
    }

    @Override
    public synchronized boolean startEvictionTest() {
        if (state == PooledObjectState.IDLE) {
            state = PooledObjectState.EVICTION;
            return true;
        }

        return false;
    }

    @Override
    public synchronized boolean endEvictionTest(
            Deque<PooledObject<T>> idleQueue) {
        if (state == PooledObjectState.EVICTION) {
            state = PooledObjectState.IDLE;
            return true;
        } else if (state == PooledObjectState.EVICTION_RETURN_TO_HEAD) {
            state = PooledObjectState.IDLE;
            if (!idleQueue.offerFirst(this)) {
                // TODO - Should never happen
            }
        }

        return false;
    }

    /**
     * Allocates the object.
     *
     * @return {@code true} if the original state was {@link PooledObjectState#IDLE IDLE}
     */
    @Override
    public synchronized boolean allocate() {
        if (state == PooledObjectState.IDLE) {
            state = PooledObjectState.ALLOCATED;
            lastBorrowTime = System.currentTimeMillis();
            lastUseTime = lastBorrowTime;
            if (logAbandoned) {
                borrowedBy = new AbandonedObjectCreatedException();
            }
            return true;
        } else if (state == PooledObjectState.EVICTION) {
            // TODO Allocate anyway and ignore eviction test
            state = PooledObjectState.EVICTION_RETURN_TO_HEAD;
            return false;
        }
        // TODO if validating and testOnBorrow == true then pre-allocate for
        // performance
        return false;
    }

    /**
     * Deallocates the object and sets it {@link PooledObjectState#IDLE IDLE}
     * if it is currently {@link PooledObjectState#ALLOCATED ALLOCATED}.
     *
     * @return {@code true} if the state was {@link PooledObjectState#ALLOCATED ALLOCATED}
     */
    @Override
    public synchronized boolean deallocate() {
        if (state == PooledObjectState.ALLOCATED ||
                state == PooledObjectState.RETURNING) {
            state = PooledObjectState.IDLE;
            lastReturnTime = System.currentTimeMillis();
            if (borrowedBy != null) {
                borrowedBy = null;
            }
            return true;
        }

        return false;
    }

    /**
     * Sets the state to {@link PooledObjectState#INVALID INVALID}
     */
    @Override
    public synchronized void invalidate() {
        state = PooledObjectState.INVALID;
    }

    @Override
    public void use() {
        lastUseTime = System.currentTimeMillis();
        usedBy = new Exception("The last code to use this object was:");
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        Exception borrowedBy = this.borrowedBy;
        if (borrowedBy != null) {
            borrowedBy.printStackTrace(writer);
        }
        Exception usedBy = this.usedBy;
        if (usedBy != null) {
            usedBy.printStackTrace(writer);
        }
    }

    /**
     * Returns the state of this object.
     * @return state
     */
    @Override
    public synchronized PooledObjectState getState() {
        return state;
    }

    /**
     * Marks the pooled object as abandoned.
     */
    @Override
    public synchronized void markAbandoned() {
        state = PooledObjectState.ABANDONED;
    }

    /**
     * Marks the object as returning to the pool.
     */
    @Override
    public synchronized void markReturning() {
        state = PooledObjectState.RETURNING;
    }

    @Override
    public void setLogAbandoned(boolean logAbandoned) {
        this.logAbandoned = logAbandoned;
    }

    static class AbandonedObjectCreatedException extends Exception {

        private static final long serialVersionUID = 7398692158058772916L;

        /** Date format */
        //@GuardedBy("this")
        private static final SimpleDateFormat format = new SimpleDateFormat
            ("'Pooled object created' yyyy-MM-dd HH:mm:ss Z " +
             "'by the following code has not been returned to the pool:'");

        private final long _createdTime;

        public AbandonedObjectCreatedException() {
            _createdTime = System.currentTimeMillis();
        }

        // Override getMessage to avoid creating objects and formatting
        // dates unless the log message will actually be used.
        @Override
        public String getMessage() {
            String msg;
            synchronized(format) {
                msg = format.format(new Date(_createdTime));
            }
            return msg;
        }
    }
}
