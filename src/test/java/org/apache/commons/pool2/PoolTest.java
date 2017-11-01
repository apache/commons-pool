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

import static org.junit.Assert.assertFalse;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class PoolTest {
    private static final CharSequence COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME = "commons-pool-EvictionTimer";
    private static final long EVICTION_PERIOD_IN_MILLIS = 100;

    private static class Foo {
    }

    private static class PooledFooFactory implements PooledObjectFactory<Foo> {
        private static final long VALIDATION_WAIT_IN_MILLIS = 1000;

        @Override
        public PooledObject<Foo> makeObject() throws Exception {
            return new DefaultPooledObject<>(new Foo());
        }

        @Override
        public void destroyObject(final PooledObject<Foo> pooledObject) throws Exception {
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

        @Override
        public void activateObject(final PooledObject<Foo> pooledObject) throws Exception {
        }

        @Override
        public void passivateObject(final PooledObject<Foo> pooledObject) throws Exception {
        }
    }

    @Test
    public void testPool() throws Exception {
        final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setTestWhileIdle(true /* testWhileIdle */);
        final PooledFooFactory pooledFooFactory = new PooledFooFactory();
        try (GenericObjectPool<Foo> pool = new GenericObjectPool<>(pooledFooFactory, poolConfig)) {
            pool.setTimeBetweenEvictionRunsMillis(EVICTION_PERIOD_IN_MILLIS);
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
            assertFalse(name, name.contains(COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME));
        }
    }
}