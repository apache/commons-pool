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
public class PooledObject<T> implements Comparable<PooledObject<T>> {

    private final T object;
    private PooledObjectState state = PooledObjectState.IDLE; // @GuardedBy("this") to ensure transitions are valid
    private final long createTime = System.currentTimeMillis();
    private volatile long lastBorrowTime = createTime;
    private volatile long lastReturnTime = createTime;
    private final Exception createdBy;
    private final PrintWriter logWriter;

    public PooledObject(T object) {
        this.object = object;
        createdBy = null;
        logWriter = null;
    }
    
    public PooledObject(T object, PrintWriter logWriter) {
        this.object = object;
        this.logWriter = logWriter;
        createdBy = new AbandonedObjectException();
    }

    /**
     * Obtain the underlying object that is wrapped by this instance of
     * {@link PooledObject}.
     */
    public T getObject() {
        return object;
    }

    /**
     * Obtain the time (using the same basis as
     * {@link System#currentTimeMillis()}) that this object was created.
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * Obtain the time in milliseconds that this object last spent in the the
     * active state (it may still be active in which case subsequent calls will
     * return an increased value).
     */
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
    public long getIdleTimeMillis() {
        return System.currentTimeMillis() - lastReturnTime;
    }

    public long getLastBorrowTime() {
        return lastBorrowTime;
    }

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
    public long getLastUsed() {
        if (object instanceof TrackedUse) {
            return Math.max(((TrackedUse) object).getLastUsed(), lastBorrowTime);
        } else {
            return lastBorrowTime;
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


    @Override
    public boolean equals(Object obj) {
        // Overridden purely to stop FindBugs complaining because compareTo()
        // has been defined.
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        // Overridden because equals() had to be overridden (see above)
        return super.hashCode();
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

    public synchronized boolean startEvictionTest() {
        if (state == PooledObjectState.IDLE) {
            state = PooledObjectState.EVICTION;
            return true;
        }

        return false;
    }

    public synchronized boolean endEvictionTest(
            LinkedBlockingDeque<PooledObject<T>> idleQueue) {
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
    public synchronized boolean allocate() {
        if (state == PooledObjectState.IDLE) {
            state = PooledObjectState.ALLOCATED;
            lastBorrowTime = System.currentTimeMillis();
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
    public synchronized boolean deallocate() {
        if (state == PooledObjectState.ALLOCATED ||
                state == PooledObjectState.RETURNING) {
            state = PooledObjectState.IDLE;
            lastReturnTime = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    /**
     * Sets the state to {@link PooledObjectState#INVALID INVALID}
     */
    public synchronized void invalidate() {
        state = PooledObjectState.INVALID;
    }
    
    /**
     * Prints the stack trace of the code that created this pooled object to
     * the configured log writer.  Does nothing of no PrintWriter was supplied
     * to the constructor.
     */
    public void printStackTrace() {
        if (createdBy != null && logWriter != null) {
            createdBy.printStackTrace(logWriter);
        }
    }
    
    /**
     * Returns the state of this object.
     * @return state
     */
    public synchronized PooledObjectState getState() {
        return state;
    }
    
    /**
     * Marks the pooled object as abandoned.
     */
    public synchronized void markAbandoned() {
        state = PooledObjectState.ABANDONED;
    }
    
    /**
     * Marks the object as returning to the pool.
     */
    public synchronized void markReturning() {
        state = PooledObjectState.RETURNING;
    }
    
    static class AbandonedObjectException extends Exception {

        private static final long serialVersionUID = 7398692158058772916L;

        /** Date format */
        //@GuardedBy("this")
        private static final SimpleDateFormat format = new SimpleDateFormat
            ("'Pooled object created' yyyy-MM-dd HH:mm:ss " +
             "'by the following code was never returned to the pool:'");

        private final long _createdTime;

        public AbandonedObjectException() {
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
