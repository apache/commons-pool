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

package org.apache.commons.pool3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;

import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericObjectPool;
import org.apache.commons.pool3.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class PoolTest {

    private static final class Foo {
    }

    private static final class PooledFooFactory implements PooledObjectFactory<Foo, RuntimeException> {
        private static final long VALIDATION_WAIT_IN_MILLIS = 1000;

        @Override
        public void activateObject(final PooledObject<Foo> pooledObject) {
        }

        @Override
        public void destroyObject(final PooledObject<Foo> pooledObject) {
        }

        @Override
        public PooledObject<Foo> makeObject() {
            return new DefaultPooledObject<>(new Foo());
        }

        @Override
        public void passivateObject(final PooledObject<Foo> pooledObject) {
        }

        @Override
        public boolean validateObject(final PooledObject<Foo> pooledObject) {
            try {
                Thread.sleep(VALIDATION_WAIT_IN_MILLIS);
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
            return false;
        }
    }

    private static final CharSequence COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME = "commons-pool-EvictionTimer";

    private static final long EVICTION_PERIOD_IN_MILLIS = 100;

    private static final Duration EVICTION_DURATION = Duration.ofMillis(100);

    @Test
    public void testPool() {
        final GenericObjectPoolConfig<Foo> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setTestWhileIdle(true /* testWhileIdle */);
        final PooledFooFactory pooledFooFactory = new PooledFooFactory();
        try (GenericObjectPool<Foo, RuntimeException> pool = new GenericObjectPool<>(pooledFooFactory, poolConfig)) {
            pool.setDurationBetweenEvictionRuns(EVICTION_DURATION);
            assertEquals(EVICTION_PERIOD_IN_MILLIS, pool.getDurationBetweenEvictionRuns().toMillis());
            assertEquals(EVICTION_PERIOD_IN_MILLIS, pool.getDurationBetweenEvictionRuns().toMillis());
            pool.setDurationBetweenEvictionRuns(Duration.ofMillis(EVICTION_PERIOD_IN_MILLIS));
            assertEquals(EVICTION_PERIOD_IN_MILLIS, pool.getDurationBetweenEvictionRuns().toMillis());
            pool.addObject();
            try {
                Thread.sleep(EVICTION_PERIOD_IN_MILLIS);
            } catch (final InterruptedException e) {
                Thread.interrupted();
            }
        }
        final Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (final Thread thread : threads) {
            if (thread == null) {
                continue;
            }
            final String name = thread.getName();
            assertFalse( name.contains(COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME),name);
        }
    }
}