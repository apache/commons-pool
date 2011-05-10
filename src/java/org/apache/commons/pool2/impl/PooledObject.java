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

import java.util.concurrent.BlockingDeque;

/**
 * This wrapper is used to track the additional information, such as state, for
 * the pooled objects.
 */
public class PooledObject<T> {

    private T _object = null;
    private volatile PooledObjectState _state = PooledObjectState.IDLE;
    private long _lastActiveTime = System.currentTimeMillis();

    public PooledObject(T object) {
        _object = object;
    }
    
    /**
     * Obtain the underlying object that is wrapped by this instance of
     * {@link PooledObject}.
     */
    public T getObject() {
        return _object;
    }

    /**
     * Obtain the time in milliseconds since this object was last active.
     */
    public long getIdleTimeMillis() {
        return System.currentTimeMillis() - _lastActiveTime;
    }

    /**
     * Provides a String form of the wrapper for debug purposes. The format is
     * not fixed and may change at any time.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Object: ");
        result.append(_object.toString());
        result.append(", State: ");
        result.append(_state.toString());
        return result.toString();
    }

    public synchronized boolean startEvictionTest() {
        if (_state == PooledObjectState.IDLE) {
            _state = PooledObjectState.MAINTAIN_EVICTION;
            return true;
        }
        
        return false;
    }

    public synchronized boolean endEvictionTest(
            BlockingDeque<PooledObject<T>> idleQueue) {
        if (_state == PooledObjectState.MAINTAIN_EVICTION) {
            _state = PooledObjectState.IDLE;
            return true;
        } else if (_state == PooledObjectState.MAINTAIN_EVICTION_RETURN_TO_HEAD) {
            _state = PooledObjectState.IDLE;
            if (!idleQueue.offerFirst(this)) {
                // TODO - Should never happen
            }
        }
        
        return false;
    }

    public synchronized boolean allocate() {
        if (_state == PooledObjectState.IDLE) {
            _state = PooledObjectState.ALLOCATED;
            return true;
        } else if (_state == PooledObjectState.MAINTAIN_EVICTION) {
            _state = PooledObjectState.MAINTAIN_EVICTION_RETURN_TO_HEAD;
            return false;
        }
        // TODO if validating and testOnBorrow == true then pre-allocate for
        // performance
        return false;
    }

    public synchronized boolean deallocate() {
        if (_state == PooledObjectState.ALLOCATED) {
            _state = PooledObjectState.IDLE;
            return true;
        }

        return false;
    }
}
