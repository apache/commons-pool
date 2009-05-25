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

package org.apache.commons.pool.impl;

import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * Provides a shared idle object eviction timer for all pools. This class wraps
 * the standard {@link Timer} and keeps track of how many pools are using it.
 * If no pools are using the timer, it is canceled. This prevents a thread
 * being left running which, in application server environments, can lead to
 * memory leads and/or prevent applications from shutting down or reloading
 * cleanly.
 * </p>
 * <p>
 * This class has package scope to prevent its inclusion in the pool public API.
 * The class declaration below should *not* be changed to public.
 * </p>
 */
class EvictionTimer {
    
    /** Timer instance */
    private static Timer _timer;
    
    /** Static usage count tracker */
    private static int _usageCount;

    /** Prevent instantiation */
    private EvictionTimer() {
        // Hide the default constuctor
    }

    /**
     * Add the specified eviction task to the timer. Tasks that are added with a
     * call to this method *must* call {@link #cancel(TimerTask)} to cancel the
     * task to prevent memory and/or thread leaks in application server
     * environments.
     * @param task      Task to be scheduled
     * @param delay     Delay in milliseconds before task is executed
     * @param period    Time in milliseconds between executions
     */
    static synchronized void schedule(TimerTask task, long delay, long period) {
        if (null == _timer) {
            _timer = new Timer(true);
        }
        _usageCount++;
        _timer.schedule(task, delay, period);
    }

    /**
     * Remove the specified eviction task from the timer.
     * @param task      Task to be scheduled
     */
    static synchronized void cancel(TimerTask task) {
        task.cancel();
        _usageCount--;
        if (_usageCount == 0) {
            _timer.cancel();
            _timer = null;
        }
    }
}
