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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool3.PooledObject;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DefaultPooledObject}.
 */
public class TestDefaultPooledObject {

    /**
     * JIRA: POOL-279
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testGetIdleTimeMillis() throws Exception {
        final DefaultPooledObject<Object> dpo = new DefaultPooledObject<>(new Object());
        final AtomicBoolean negativeIdleTimeReturned = new AtomicBoolean();
        final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);
        final Runnable allocateAndDeallocateTask = () -> {
            for (int i1 = 0; i1 < 10000; i1++) {
                if (dpo.getIdleDuration().isNegative()) {
                    negativeIdleTimeReturned.set(true);
                    break;
                }
                if (dpo.getIdleDuration().isNegative()) {
                    negativeIdleTimeReturned.set(true);
                    break;
                }
            }
            dpo.allocate();
            for (int i2 = 0; i2 < 10000; i2++) {
                if (dpo.getIdleDuration().isNegative()) {
                    negativeIdleTimeReturned.set(true);
                    break;
                }
            }
            dpo.deallocate();
        };
        final Runnable getIdleTimeTask = () -> {
            for (int i = 0; i < 10000; i++) {
                if (dpo.getIdleDuration().isNegative()) {
                    negativeIdleTimeReturned.set(true);
                    break;
                }
            }
        };
        final double probabilityOfAllocationTask = 0.7;
        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            final Runnable randomTask = Math.random() < probabilityOfAllocationTask ? allocateAndDeallocateTask : getIdleTimeTask;
            futures.add(executor.submit(randomTask));
        }
        for (final Future<?> future : futures) {
            future.get();
        }
        assertFalse(negativeIdleTimeReturned.get(), "DefaultPooledObject.getIdleTimeMillis() returned a negative value");
    }

    @Test
    public void testInitialStateActiveDuration() throws InterruptedException {
        final PooledObject<Object> dpo = new DefaultPooledObject<>(new Object());
        // Sleep MUST be "long enough" to test that we are not returning a negative time.
        // Need an API in Java 8 to get the clock granularity.
        Thread.sleep(200);
        // In the initial state, all instants are the creation instant: last borrow, last use, last return.
        // In the initial state, the active duration is the time between "now" and the creation time.
        // In the initial state, the idle duration is the time between "now" and the last return, which is the creation time.
        assertFalse(dpo.getActiveDuration().isNegative());
        assertFalse(dpo.getActiveDuration().isZero());
        // We use greaterThanOrEqualTo instead of equal because "now" many be different when each argument is evaluated.
        assertThat(1L, lessThanOrEqualTo(2L)); // sanity check
        assertThat(Duration.ZERO, lessThanOrEqualTo(Duration.ZERO.plusNanos(1))); // sanity check
        assertThat(dpo.getActiveDuration(), lessThanOrEqualTo(dpo.getIdleDuration()));
        // Deprecated
        assertThat(dpo.getActiveDuration(), lessThanOrEqualTo(dpo.getActiveDuration()));
    }

    @Test
    public void testInitialStateCreateInstant() {
        final PooledObject<Object> dpo = new DefaultPooledObject<>(new Object());

        // In the initial state, all instants are the creation instant: last borrow, last use, last return.

        // Instant vs. Instant
        assertEquals(dpo.getCreateInstant(), dpo.getLastBorrowInstant());
        assertEquals(dpo.getCreateInstant(), dpo.getLastReturnInstant());
        assertEquals(dpo.getCreateInstant(), dpo.getLastUsedInstant());

        assertEquals(dpo.getCreateInstant(), dpo.getCreateInstant());

    }

    @Test
    public void testInitialStateDuration() throws InterruptedException {
        final PooledObject<Object> dpo = new DefaultPooledObject<>(new Object());
        final Duration duration1 = dpo.getFullDuration();
        assertNotNull(duration1);
        assertFalse(duration1.isNegative());
        Thread.sleep(100);
        final Duration duration2 = dpo.getFullDuration();
        assertNotNull(duration2);
        assertFalse(duration2.isNegative());
        assertThat(duration1, lessThan(duration2));
    }

    @Test
    public void testInitialStateIdleDuration() throws InterruptedException {
        final PooledObject<Object> dpo = new DefaultPooledObject<>(new Object());
        // Sleep MUST be "long enough" to test that we are not returning a negative time.
        Thread.sleep(200);
        // In the initial state, all instants are the creation instant: last borrow, last use, last return.
        // In the initial state, the active duration is the time between "now" and the creation time.
        // In the initial state, the idle duration is the time between "now" and the last return, which is the creation time.
        assertFalse(dpo.getIdleDuration().isNegative());
        assertFalse(dpo.getIdleDuration().isZero());
        // We use greaterThanOrEqualTo instead of equal because "now" many be different when each argument is evaluated.
        assertThat(dpo.getIdleDuration(), lessThanOrEqualTo(dpo.getActiveDuration()));
    }
}