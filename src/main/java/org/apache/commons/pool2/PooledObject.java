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
package org.apache.commons.pool2;

import java.util.Deque;

/**
 * Defines the wrapper that is used to track the additional information, such as
 * state, for the pooled objects.
 * <p>
 * Implementations of this class are required to be thread-safe.
 *
 * @param <T> the type of object in the pool
 *
 * @version $Revision: $
 *
 * @since 2.0
 */
public interface PooledObject<T> extends Comparable<PooledObject<T>> {

    /**
     * Obtain the underlying object that is wrapped by this instance of
     * {@link PooledObject}.
     */
    public T getObject();

    /**
     * Obtain the time (using the same basis as
     * {@link System#currentTimeMillis()}) that this object was created.
     */
    public long getCreateTime();

    /**
     * Obtain the time in milliseconds that this object last spent in the the
     * active state (it may still be active in which case subsequent calls will
     * return an increased value).
     */
    public long getActiveTimeMillis();
    /**
     * Obtain the time in milliseconds that this object last spend in the the
     * idle state (it may still be idle in which case subsequent calls will
     * return an increased value).
     */
    public long getIdleTimeMillis();

    public long getLastBorrowTime();

    public long getLastReturnTime();

    /**
     * Return an estimate of the last time this object was used.  If the class
     * of the pooled object implements {@link TrackedUse}, what is returned is
     * the maximum of {@link TrackedUse#getLastUsed()} and
     * {@link #getLastBorrowTime()}; otherwise this method gives the same
     * value as {@link #getLastBorrowTime()}.
     *
     * @return the last time this object was used
     */
    public long getLastUsed();

    /**
     * Orders instances based on idle time - i.e. the length of time since the
     * instance was returned to the pool. Used by the GKOP idle object evictor.
     * <p>
     * Note: This class has a natural ordering that is inconsistent with
     *       equals if distinct objects have the same identity hash code.
     */
    @Override
    public int compareTo(PooledObject<T> other);

    @Override
    public boolean equals(Object obj);

    @Override
    public int hashCode();

    /**
     * Provides a String form of the wrapper for debug purposes. The format is
     * not fixed and may change at any time.
     */
    @Override
    public String toString();

    public boolean startEvictionTest();

    public boolean endEvictionTest(Deque<PooledObject<T>> idleQueue);

    /**
     * Allocates the object.
     *
     * @return {@code true} if the original state was {@link PooledObjectState#IDLE IDLE}
     */
    public boolean allocate();

    /**
     * Deallocates the object and sets it {@link PooledObjectState#IDLE IDLE}
     * if it is currently {@link PooledObjectState#ALLOCATED ALLOCATED}.
     *
     * @return {@code true} if the state was {@link PooledObjectState#ALLOCATED ALLOCATED}
     */
    public boolean deallocate();

    /**
     * Sets the state to {@link PooledObjectState#INVALID INVALID}
     */
    public void invalidate();

    /**
     * Prints the stack trace of the code that created this pooled object to
     * the configured log writer.  Does nothing of no PrintWriter was supplied
     * to the constructor.
     */
    public void printStackTrace();

    /**
     * Returns the state of this object.
     * @return state
     */
    public PooledObjectState getState();

    /**
     * Marks the pooled object as abandoned.
     */
    public void markAbandoned();

    /**
     * Marks the object as returning to the pool.
     */
    public void markReturning();
}
