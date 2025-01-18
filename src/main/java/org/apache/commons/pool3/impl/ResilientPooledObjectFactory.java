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
package org.apache.commons.pool3.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;

/**
 * Wraps a PooledObjectFactory, extending to provide resilient features.
 * <p>
 * Maintains a cicrular log of makeObject calls and makes strategy-based
 * decisions on whether to keep trying proactively to create objects.
 * Decisions use data in the makeObject log and information reported by
 * the pool that the factory is attached to.
 *
 * @param <T> Type of object managed by the factory
 * @param <E> Type of exception that the factory may throw
 */
public class ResilientPooledObjectFactory<T, E extends Exception> implements PooledObjectFactory<T, E> {
    private static final int DEFAULT_LOG_SIZE = 10;
    private static final Duration DEFAULT_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_LOOK_BACK = Duration.ofMinutes(5);
    private static final Duration DEFAULT_TIME_BETWEEN_CHECKS = Duration.ofSeconds(10);
    /** Wrapped factory */
    private final PooledObjectFactory<T, E> factory;
    /** GOP that factory is attached to. */
    private GenericObjectPool<T, E> pool;
    /** Size of the circular log of makeObject events */
    private int logSize;
    /** Duration of time window for statistics */
    private final Duration lookBack;
    /** Circular log of makeObject events */
    private final ConcurrentLinkedQueue<MakeEvent> makeObjectLog = new ConcurrentLinkedQueue<>();
    /** Time of last factory failure */
    private Instant downStart;
    /** Time factory last returned to "up" state. */
    private Instant upStart;
    /** Exception counts */
    @SuppressWarnings("rawtypes")
    private final ConcurrentHashMap<Class, Integer> exceptionCounts = new ConcurrentHashMap<>();
    /** Whether or not the factory is "up" */
    private boolean up = true;
    /**
     * @return the factory wrapped by this resilient factory
     */
    /** Whether or not the monitor thread is running */
    private boolean monitoring = false;
    /** Time to wait between object creations by the adder thread */
    private final Duration delay;
    /** Time between monitor checks */
    private Duration timeBetweenChecks = Duration.ofSeconds(10);
    /** Adder thread */
    private Adder adder = null;

    /**
     * Construct a ResilientPooledObjectFactory from a factory with specified
     * parameters.
     *
     * @param factory           PooledObjectFactory to wrap
     * @param logSize           length of the makeObject log
     * @param delay             time to wait between object creations by the adder
     *                          thread
     * @param lookBack          length of time over which metrics are kept
     * @param timeBetweenChecks time between checks by the monitor thread
     */
    public ResilientPooledObjectFactory(final PooledObjectFactory<T, E> factory,
            final int logSize, final Duration delay, final Duration lookBack, final Duration timeBetweenChecks) {
        this.logSize = logSize;
        this.factory = factory;
        this.delay = delay;
        this.lookBack = lookBack;
        this.timeBetweenChecks = timeBetweenChecks;
    }

    /**
     * Construct a ResilientPooledObjectFactory from a factory and pool, using
     * defaults for logSize, delay, and lookBack.
     *
     * @param factory PooledObjectFactory to wrap
     */
    public ResilientPooledObjectFactory(final PooledObjectFactory<T, E> factory) {
        this(factory, DEFAULT_LOG_SIZE, DEFAULT_DELAY, DEFAULT_LOOK_BACK, DEFAULT_TIME_BETWEEN_CHECKS);
    }

    /**
     * Sets the underlying pool. For tests.
     *
     * @param pool the underlying pool.
     */
    void setPool(final GenericObjectPool<T, E> pool) {
        this.pool = pool;
    }

    /**
     * Set the time between monitor checks.
     *
     * @param timeBetweenChecks The time between monitor checks.
     */
    public void setTimeBetweenChecks(final Duration timeBetweenChecks) {
        this.timeBetweenChecks = timeBetweenChecks;
    }

    /**
     * Set the makeObject log size.
     *
     * @param logSize the number of makeObject events to keep in the log
     */
    public void setLogSize(final int logSize) {
        this.logSize = logSize;
    }

    /**
     * Delegate to the wrapped factory, but log the makeObject call.
     */
    @Override
    public PooledObject<T> makeObject() throws E {
        final MakeEvent makeEvent = new MakeEvent();
        try {
            final PooledObject<T> obj = factory.makeObject();
            makeEvent.setSuccess(!PooledObject.isNull(obj));
            return obj;
        } catch (final Throwable t) {
            makeEvent.setSuccess(false);
            makeEvent.setException(t);
            exceptionCounts.put(t.getClass(), exceptionCounts.getOrDefault(t, 0) + 1);
            throw t;
        } finally {
            makeEvent.end();
            makeObjectLog.add(makeEvent);
        }
    }

    // Delegate all other methods to the wrapped factory.

    @Override
    public void destroyObject(final PooledObject<T> p) throws E {
        factory.destroyObject(p);
    }

    @Override
    public boolean validateObject(final PooledObject<T> p) {
        return factory.validateObject(p);
    }

    @Override
    public void activateObject(final PooledObject<T> p) throws E {
        factory.activateObject(p);
    }

    @Override
    public void passivateObject(final PooledObject<T> p) throws E {
        factory.passivateObject(p);
    }

    /**
     * Default implementation considers the factory down as soon as a single
     * makeObject call fails and considers it back up if the last logSize makes have
     * succeeded.
     * <p>
     * Sets downStart to time of the first failure found in makeObjectLog and
     * upStart to the time when logSize consecutive makes have succeeded.
     * <p>
     * When a failure is observed, the adder thread is started if the pool
     * is not closed and has take waiters.
     * <p>
     * Removes the oldest event from the log if it is full.
     *
     */
    protected void runChecks() {
        boolean upOverLog = true;
        // 1. If the log is full, remove the oldest (first) event.
        //
        // 2. Walk the event log. If we find a failure, set downStart, set up to false
        // and start the adder thread.
        //
        // 3. If the log contains only successes, if up is false, set upStart and up to
        // true
        // and kill the adder thread.
        while (makeObjectLog.size() > logSize) {
            makeObjectLog.poll();
        }
        for (final MakeEvent makeEvent : makeObjectLog) {
            if (!makeEvent.isSuccess()) {
                upOverLog = false;
                downStart = Instant.now();
                up = false;
                if (pool.getNumWaiters() > 0 && !pool.isClosed() && adder == null) {
                    adder = new Adder();
                    adder.start();
                }
            }

        }
        if (upOverLog && !up) {
            // Kill adder thread and set up to true
            upStart = Instant.now();
            up = true;
            adder.kill();
            adder = null;
        }
    }

    /**
     * @return true if the factory is considered "up".
     */
    public boolean isUp() {
        return up;
    }

    /**
     * @return true if the adder is running
     */
    public boolean isAdderRunning() {
        return adder != null && adder.isRunning();
    }

    /**
     * @return true if the monitor is running
     */
    public boolean isMonitorRunning() {
        return monitoring;
    }

    /**
     * Start the monitor thread with the given time between checks.
     *
     * @param timeBetweenChecks time between checks
     */
    public void startMonitor(final Duration timeBetweenChecks) {
        this.timeBetweenChecks = timeBetweenChecks;
        startMonitor();
    }

    /**
     * Start the monitor thread with the currently configured time between checks.
     */
    public void startMonitor() {
        monitoring = true;
        new Monitor().start();
    }

    /**
     * Stop the monitor thread.
     */
    public void stopMonitor() {
        monitoring = false;
    }

    /**
     * Adder thread that adds objects to the pool, waiting for a fixed delay between
     * adds.
     * <p>
     * The adder thread will stop under any of the following conditions:
     * <ul>
     * <li>The pool is closed.</li>
     * <li>The factory is down.</li>
     * <li>The pool is full.</li>
     * <li>The pool has no waiters.</li>
     * </ul>
     */
    class Adder extends Thread {

        private static final int MAX_FAILURES = 5;

        private boolean killed;
        private boolean running;
        private int failures;

        @Override
        public void run() {
            running = true;
            while (!up && !killed && !pool.isClosed()) {
                try {
                    pool.addObject();
                    if (pool.getNumWaiters() == 0 || pool.getNumActive() + pool.getNumIdle() == pool.getMaxTotal()) {
                        kill();
                    }
                } catch (final Throwable e) {
                    failures++;
                    if (failures > MAX_FAILURES) {
                        kill();
                    }
                } finally {
                    // Wait for delay
                    try {
                        sleep(delay.toMillis());
                    } catch (final InterruptedException e) {
                        killed = true;
                    }
                }
            }
            killed = true;
            running = false;
        }

        public boolean isRunning() {
            return running;
        }

        public void kill() {
            killed = true;
        }
    }

    /**
     * Record of a makeObject event.
     */
    static class MakeEvent {
        private final Instant startTime;
        private Instant endTime;
        private boolean success;
        private Throwable exception;

        /**
         * Constructor set statTime to now.
         */
        public MakeEvent() {
            startTime = Instant.now();
        }

        /**
         * @return the time the makeObject call ended
         */
        public Instant getEndTime() {
            return endTime;
        }

        /**
         * Mark completion of makeObject call.
         */
        public void end() {
            this.endTime = Instant.now();
        }

        /**
         * @return true if the makeObject call succeeded
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Set the success status of the makeObject call.
         *
         * @param success
         */
        public void setSuccess(final boolean success) {
            this.success = success;
        }

        /**
         * @return the exception thrown by the makeObject call
         */
        public Throwable getException() {
            return exception;
        }

        /**
         * Set the exception thrown by the makeObject call.
         *
         * @param exception
         */
        public void setException(final Throwable exception) {
            this.exception = exception;
        }

        /**
         * @return the start time of the makeObject call
         */
        public Instant getStartTime() {
            return startTime;
        }
    }

    /**
     * Monitor thread that runs checks to examine the makeObject log and pool state.
     */
    class Monitor extends Thread {
        @Override
        public void run() {
            while (monitoring && !pool.isClosed()) {
                runChecks();
                try {
                    sleep(timeBetweenChecks.toMillis());
                } catch (final InterruptedException e) {
                    monitoring = false;
                } catch (final Throwable e) {
                    monitoring = false;
                    throw e;
                }
            }
            monitoring = false;
        }
    }

    /**
     * @return the default makeObject log size
     */
    public static int getDefaultLogSize() {
        return DEFAULT_LOG_SIZE;
    }

    /**
     * @return the default time between makeObject calls by the adder thread
     */
    public static Duration getDefaultDelay() {
        return DEFAULT_DELAY;
    }

    /**
     * @return the default look back
     */
    public static Duration getDefaultLookBack() {
        return DEFAULT_LOOK_BACK;
    }

    /**
     * @return the default time between monitor checks
     */
    public static Duration getDefaultTimeBetweenChecks() {
        return DEFAULT_TIME_BETWEEN_CHECKS;
    }

    /**
     * @return the size of the makeObject log
     */
    public int getLogSize() {
        return logSize;
    }

    /**
     * @return the look back duration
     */
    public Duration getLookBack() {
        return lookBack;
    }

    /**
     * @return a copy of the makeObject log
     */
    public List<MakeEvent> getMakeObjectLog() {
        final ArrayList<MakeEvent> makeObjectLog = new ArrayList<>();
        return new ArrayList<>(makeObjectLog.stream().toList());
    }

    /**
     * @return the start time of the last factory outage
     */
    public Instant getDownStart() {
        return downStart;
    }

    /**
     * @return the time of the last factory outage recovery
     */
    public Instant getUpStart() {
        return upStart;
    }

    /**
     * @return the time to wait between object creations by the adder thread
     */
    public Duration getDelay() {
        return delay;
    }

    /**
     * @return the time between monitor checks
     */
    public Duration getTimeBetweenChecks() {
        return timeBetweenChecks;
    }

}
