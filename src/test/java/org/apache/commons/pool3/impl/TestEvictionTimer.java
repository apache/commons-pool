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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.pool3.BasePooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EvictionTimer}.
 */
public class TestEvictionTimer {

    @Test
    public void testStartStopEvictionTimer() throws Exception {

        try (GenericObjectPool<String, RuntimeException> pool = new GenericObjectPool<>(new BasePooledObjectFactory<String, RuntimeException>() {

            @Override
            public String create() {
                return null;
            }

            @Override
            public PooledObject<String> wrap(final String obj) {
                return new DefaultPooledObject<>(obj);
            }
        })) {

            // Start evictor #1
            final BaseGenericObjectPool<String, RuntimeException>.Evictor evictor1 = pool.new Evictor();
            EvictionTimer.schedule(evictor1, TestConstants.ONE_MINUTE_DURATION, TestConstants.ONE_MINUTE_DURATION);

            // Assert that eviction objects are correctly allocated
            // 1 - the evictor timer task is created
            final Field evictorTaskFutureField =
                    evictor1.getClass().getDeclaredField("scheduledFuture");
            evictorTaskFutureField.setAccessible(true);
            ScheduledFuture<?> sf = (ScheduledFuture<?>) evictorTaskFutureField.get(evictor1);
            assertFalse(sf.isCancelled());
            // 2- and, the eviction action is added to executor thread pool
            final Field evictorExecutorField = EvictionTimer.class.getDeclaredField("executor");
            evictorExecutorField.setAccessible(true);
            final ThreadPoolExecutor evictionExecutor = (ThreadPoolExecutor) evictorExecutorField.get(null);
            assertEquals(2, evictionExecutor.getQueue().size()); // Reaper plus one eviction task
            assertEquals(1, EvictionTimer.getNumTasks());

            // Start evictor #2
            final BaseGenericObjectPool<String, RuntimeException>.Evictor evictor2 = pool.new Evictor();
            EvictionTimer.schedule(evictor2, TestConstants.ONE_MINUTE_DURATION, TestConstants.ONE_MINUTE_DURATION);

            // Assert that eviction objects are correctly allocated
            // 1 - the evictor timer task is created
            sf = (ScheduledFuture<?>) evictorTaskFutureField.get(evictor2);
            assertFalse(sf.isCancelled());
            // 2- and, the eviction action is added to executor thread pool
            assertEquals(3, evictionExecutor.getQueue().size()); // Reaper plus 2 eviction tasks
            assertEquals(2, EvictionTimer.getNumTasks());

            // Stop evictor #1
            EvictionTimer.cancel(evictor1, BaseObjectPoolConfig.DEFAULT_EVICTOR_SHUTDOWN_TIMEOUT, false);

            // Assert that eviction objects are correctly cleaned
            // 1 - the evictor timer task is cancelled
            sf = (ScheduledFuture<?>) evictorTaskFutureField.get(evictor1);
            assertTrue(sf.isCancelled());
            // 2- and, the eviction action is removed from executor thread pool
            final ThreadPoolExecutor evictionExecutorOnStop = (ThreadPoolExecutor) evictorExecutorField.get(null);
            assertEquals(2, evictionExecutorOnStop.getQueue().size());
            assertEquals(1, EvictionTimer.getNumTasks());

            // Stop evictor #2
            EvictionTimer.cancel(evictor2, BaseObjectPoolConfig.DEFAULT_EVICTOR_SHUTDOWN_TIMEOUT, false);

            // Assert that eviction objects are correctly cleaned
            // 1 - the evictor timer task is cancelled
            sf = (ScheduledFuture<?>) evictorTaskFutureField.get(evictor2);
            assertTrue(sf.isCancelled());
            // 2- and, the eviction thread pool executor is freed
            assertNull(evictorExecutorField.get(null));
            assertEquals(0, EvictionTimer.getNumTasks());
        }
    }
}
