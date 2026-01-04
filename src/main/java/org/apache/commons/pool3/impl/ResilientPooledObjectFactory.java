/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * Maintains a circular log of makeObject calls and makes strategy-based
 * decisions on whether to keep trying proactively to create objects.
 * Decisions use data in the makeObject log and information reported by
 * the pool that the factory is attached to.
 * </p>
 *
 * @param <T> Type of object managed by the factory
 * @param <E> Type of exception that the factory may throw
 */
public class ResilientPooledObjectFactory<T, E extends Exception> implements PooledObjectFactory<T, E> {

    /**
     * Adder thread that adds objects to the pool, waiting for a fixed delay between
     * adds.
     * <p>
     * The adder thread will stop under any of the following conditions:
     * </p>
     * <ul>
     * <li>The pool is closed.</li>
     * <li>The factory is down.</li>
     * <li>The pool is full.</li>
     * <li>The pool has no waiters.</li>
     * </ul>
     */
    final class Adder extends Thread {

        private static final int MAX_FAILURES = 5;

        private volatile boolean killed;
        private volatile boolean running;
        private int failures;

        public boolean isRunning() {
            return running;
        }

        public void kill() {
            killed = true;
        }

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
                        kill();
                    }
                }
            }
            kill();
            running = false;
        }
    }

    /**
     * Record of a makeObject event.
     */
    static final class MakeEvent {
        private final Instant startTime;
        private Instant endTime;
        private boolean success;
        private Throwable exception;

        /**
         * Constructs a new instance and set statTime to now.
         */
        MakeEvent() {
            startTime = Instant.now();
        }

        /**
         * Mark completion of makeObject call.
         */
        public void end() {
            this.endTime = Instant.now();
        }

        /**
         * @return the time the makeObject call ended
         */
        public Instant getEndTime() {
            return endTime;
        }

        /**
         * @return the exception thrown by the makeObject call
         */
        public Throwable getException() {
            return exception;
        }

        /**
         * @return the start time of the makeObject call
         */
        public Instant getStartTime() {
            return startTime;
        }

        /**
         * @return true if the makeObject call succeeded
         */
        public boolean isSuccess() {
            return success;
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
         * Set the success status of the makeObject call.
         *
         * @param success
         */
        public void setSuccess(final boolean success) {
            this.success = success;
        }
    }

    /**
     * Monitor thread that runs checks to examine the makeObject log and pool state.
     */
    final class Monitor extends Thread {
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

    private static final int DEFAULT_LOG_SIZE = 10;
    private static final Duration DEFAULT_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_LOOK_BACK = Duration.ofMinutes(5);
    private static final Duration DEFAULT_TIME_BETWEEN_CHECKS = Duration.ofSeconds(10);

    /**
     * Gets the default time between makeObject calls by the adder thread.
     *
     * @return the default time between makeObject calls by the adder thread.
     */
    public static Duration getDefaultDelay() {
        return DEFAULT_DELAY;
    }

    /**
     * Gets the default makeObject log size.
     *
     * @return the default makeObject log size.
     */
    public static int getDefaultLogSize() {
        return DEFAULT_LOG_SIZE;
    }

    /**
     * Gets the default look back duration.
     *
     * @return the default look back duration.
     */
    public static Duration getDefaultLookBack() {
        return DEFAULT_LOOK_BACK;
    }

    /**
     * Gets the default time between monitor checks.
     *
     * @return the default time between monitor checks.
     */
    public static Duration getDefaultTimeBetweenChecks() {
        return DEFAULT_TIME_BETWEEN_CHECKS;
    }

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
    private boolean monitoring;

    /** Time to wait between object creations by the adder thread */
    private final Duration delay;

    /** Time between monitor checks */
    private Duration timeBetweenChecks = Duration.ofSeconds(10);

    // Delegate all other methods to the wrapped factory.

    /** Adder thread */
    private Adder adder;

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

    @Override
    public void activateObject(final PooledObject<T> p) throws E {
        factory.activateObject(p);
    }

    @Override
    public void destroyObject(final PooledObject<T> p) throws E {
        factory.destroyObject(p);
    }

    /**
     * Gets the time to wait between object creations by the adder thread.
     *
     * @return the time to wait between object creations by the adder thread.
     */
    public Duration getDelay() {
        return delay;
    }

    /**
     * Gets the start time of the last factory outage.
     *
     * @return the start time of the last factory outage.
     */
    public Instant getDownStart() {
        return downStart;
    }

    /**
     * Gets the size of the makeObject log.
     *
     * @return the size of the makeObject log.
     */
    public int getLogSize() {
        return logSize;
    }

    /**
     * Gets the look back duration.
     *
     * @return the look back duration.
     */
    public Duration getLookBack() {
        return lookBack;
    }

    /**
     * Gets a copy of the makeObject log.
     *
     * @return a copy of the makeObject log.
     */
    public List<MakeEvent> getMakeObjectLog() {
        final ArrayList<MakeEvent> makeObjectLog = new ArrayList<>();
        return new ArrayList<>(makeObjectLog.stream().toList());
    }

    /**
     * Gets the duration between monitor checks.
     *
     * @return the duration between monitor checks.
     */
    public Duration getTimeBetweenChecks() {
        return timeBetweenChecks;
    }

    /**
     * Gets the time of the last factory outage recovery.
     *
     * @return the time of the last factory outage recovery.
     */
    public Instant getUpStart() {
        return upStart;
    }

    /**
     * Tests whether the adder is running.
     *
     * @return true if the adder is running.
     */
    public boolean isAdderRunning() {
        return adder != null && adder.isRunning();
    }

    /**
     * Tests whether the monitor is running.
     *
     * @return true if the monitor is running.
     */
    public boolean isMonitorRunning() {
        return monitoring;
    }

    /**
     * Tests whether the factory is considered "up".
     *
     * @return true if the factory is considered "up".
     */
    public boolean isUp() {
        return up;
    }

    /**
     * Delegate to the wrapped factory, but log the makeObject call.
     */
    @Override
    public PooledObject<T> makeObject() throws E {
        final MakeEvent makeEvent = new MakeEvent();
        try {
            final PooledObject<T> obj = factory.makeObject();
            makeEvent.setSuccess(PooledObject.nonNull(obj));
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
     * </p>
     * <p>
     * When a failure is observed, the adder thread is started if the pool
     * is not closed and has take waiters.
     * </p>
     * <p>
     * Removes the oldest event from the log if it is full.
     * </p>
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
     * Sets the makeObject log size.
     *
     * @param logSize the number of makeObject events to keep in the log
     */
    public void setLogSize(final int logSize) {
        this.logSize = logSize;
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
     * Sets the time between monitor checks.
     *
     * @param timeBetweenChecks The time between monitor checks.
     */
    public void setTimeBetweenChecks(final Duration timeBetweenChecks) {
        this.timeBetweenChecks = timeBetweenChecks;
    }

    /**
     * Starts the monitor thread with the currently configured time between checks.
     */
    public void startMonitor() {
        monitoring = true;
        new Monitor().start();
    }

    /**
     * Starts the monitor thread with the given time between checks.
     *
     * @param timeBetweenChecks time between checks
     */
    public void startMonitor(final Duration timeBetweenChecks) {
        this.timeBetweenChecks = timeBetweenChecks;
        startMonitor();
    }

    /**
     * Stops the monitor thread.
     */
    public void stopMonitor() {
        monitoring = false;
    }

    @Override
    public boolean validateObject(final PooledObject<T> p) {
        return factory.validateObject(p);
    }

}
