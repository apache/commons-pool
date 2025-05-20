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
package org.apache.commons.pool2.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.Waiter;
import org.apache.commons.pool2.WaiterFactory;

public class DisconnectingWaiterFactory<K> extends WaiterFactory<K> {
    private static final Duration DEFAULT_TIME_BETWEEN_CONNECTION_CHECKS = Duration.ofMillis(100);

    private static final Duration DEFAULT_MAX_WAIT = Duration.ofSeconds(10);

    /**
     * Default function to perform for activate, passivate, destroy in disconnected
     * mode - no-op
     */
    protected static final Consumer<PooledObject<Waiter>> DEFAULT_DISCONNECTED_LIFECYCLE_ACTION = w -> {
    };

    /**
     * Default supplier determining makeObject action when invoked in disconnected
     * mode. Default behavior is to block until reconnected for up to maxWait
     * duration. If maxWait is exceeded, throw ISE; if reconnect happens in time,
     * return a new DefaultPooledObject<Waiter>.
     */
    protected static final Supplier<PooledObject<Waiter>> DEFAULT_DISCONNECTED_CREATE_ACTION = () -> {
        waitForConnection(null, DEFAULT_TIME_BETWEEN_CONNECTION_CHECKS, DEFAULT_MAX_WAIT);
        return new DefaultPooledObject<>(new Waiter(true, true, 0));
    };

    /**
     * Default predicate determining what validate does in disconnected state -
     * default is to always return false
     */
    protected static final Predicate<PooledObject<Waiter>> DEFAULT_DISCONNECTED_VALIDATION_ACTION = w -> false;

    /*
     * Blocks until connected or maxWait is exceeded.
     *
     * @throws TimeoutException if maxWait is exceeded.
     */
    private static void waitForConnection(final AtomicBoolean connected, final Duration timeBetweenConnectionChecks, final Duration maxWait) {
        final Instant start = Instant.now();
        while (!connected.get()) {
            try {
                Thread.sleep(timeBetweenConnectionChecks.toMillis());
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            if (Duration.between(start, Instant.now()).compareTo(maxWait) > 0) {
                throw new IllegalStateException(new TimeoutException("Timed out waiting for connection"));
            }
        }
    }

    /**
     *
     * A WaiterFactory that simulates a resource required by factory methods going
     * down (and coming back).
     * <p>
     * When connected, this factory behaves like a normal WaiterFactory.
     * When disconnected, factory methods are determined by functional parameters.
     * </p>
     */
    private final AtomicBoolean connected = new AtomicBoolean(true);

    /** Time between reconnection checks */
    final Duration timeBetweenConnectionChecks;

    /**
     * Maximum amount of time a factory method will wait for reconnect before
     * throwing TimeOutException
     */
    final Duration maxWait;

    /** Function to perform when makeObject is executed in disconnected mode */
    final Supplier<PooledObject<Waiter>> disconnectedCreateAction;

    /**
     * Function to perform for activate, passsivate and destroy when invoked in
     * disconnected mode
     */
    final Consumer<PooledObject<Waiter>> disconnectedLifcycleAction;

    /** Function to perform for validate when invoked in disconnected mode */
    final Predicate<PooledObject<Waiter>> disconnectedValidationAction;

    public DisconnectingWaiterFactory() {
        this(DEFAULT_DISCONNECTED_CREATE_ACTION, DEFAULT_DISCONNECTED_LIFECYCLE_ACTION,
                DEFAULT_DISCONNECTED_VALIDATION_ACTION);
    }

    public DisconnectingWaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency) {
        this(activateLatency, destroyLatency, makeLatency, passivateLatency,
                validateLatency, waiterLatency, Long.MAX_VALUE, Long.MAX_VALUE, 0);
    }

    public DisconnectingWaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency, final long maxActive) {
        this(activateLatency, destroyLatency, makeLatency, passivateLatency,
                validateLatency, waiterLatency, maxActive,
                Long.MAX_VALUE, 0);
    }

    public DisconnectingWaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency, final long maxActive, final long maxActivePerKey,
            final double passivateInvalidationProbability) {
        super(activateLatency, destroyLatency, makeLatency, passivateLatency,
                validateLatency, waiterLatency, maxActive, maxActivePerKey,
                passivateInvalidationProbability);
        this.timeBetweenConnectionChecks = DEFAULT_TIME_BETWEEN_CONNECTION_CHECKS;
        this.maxWait = DEFAULT_MAX_WAIT;
        this.disconnectedCreateAction = DEFAULT_DISCONNECTED_CREATE_ACTION;
        this.disconnectedLifcycleAction = DEFAULT_DISCONNECTED_LIFECYCLE_ACTION;
        this.disconnectedValidationAction = DEFAULT_DISCONNECTED_VALIDATION_ACTION;
    }

    public DisconnectingWaiterFactory(final Supplier<PooledObject<Waiter>> disconnectedCreateAction,
            final Consumer<PooledObject<Waiter>> disconnectedLifcycleAction,
            final Predicate<PooledObject<Waiter>> disconnectedValidationAction) {
        super(0, 0, 0,
                0, 0, 0,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                0);
        this.timeBetweenConnectionChecks = DEFAULT_TIME_BETWEEN_CONNECTION_CHECKS;
        this.maxWait = DEFAULT_MAX_WAIT;
        this.disconnectedCreateAction = disconnectedCreateAction;
        this.disconnectedLifcycleAction = disconnectedLifcycleAction;
        this.disconnectedValidationAction = disconnectedValidationAction;
    }

    private void activate(final PooledObject<Waiter> obj) {
        if (connected.get()) {
            super.activateObject(obj);
        } else {
            disconnectedLifcycleAction.accept(obj);
        }
    }

    @Override
    public void activateObject(final K key, final PooledObject<Waiter> obj) {
        activate(obj);
    }

    @Override
    public void activateObject(final PooledObject<Waiter> obj) {
        activate(obj);
    }

    /**
     * Reconnect the factory.
     */
    public void connect() {
        connected.set(true);
    }
    /*
     * TODO: add builder to clean up constructors and make maxWait,
     * timeBetweenConnectionChecks configurable.
     */

    /**
     * Disconnect the factory.
     */
    public void disconnect() {
        connected.set(false);
    }

    private PooledObject<Waiter> make() {
        if (connected.get()) {
            return super.makeObject();
        }
        return disconnectedCreateAction.get();
    }

    @Override
    public PooledObject<Waiter> makeObject() {
        return make();
    }

    @Override
    public PooledObject<Waiter> makeObject(final K key) {
        return make();
    }

    private void passivate(final PooledObject<Waiter> obj) {
        if (connected.get()) {
            super.passivateObject(obj);
        } else {
            disconnectedLifcycleAction.accept(obj);
        }
    }

    @Override
    public void passivateObject(final K key, final PooledObject<Waiter> obj) {
        passivate(obj);
    }

    @Override
    public void passivateObject(final PooledObject<Waiter> obj) {
        passivate(obj);
    }

    private boolean validate(final PooledObject<Waiter> obj) {
        if (connected.get()) {
            return super.validateObject(obj);
        }
        return disconnectedValidationAction.test(obj);
    }

    @Override
    public boolean validateObject(final K key, final PooledObject<Waiter> obj) {
        return validate(obj);
    }

    @Override
    public boolean validateObject(final PooledObject<Waiter> obj) {
        return validate(obj);
    }

}
