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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.Waiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests POOL-340.
 */
public class TestGenericObjectPoolFactoryCreateFailure {

    private static final class SingleObjectFactory extends BasePooledObjectFactory<Object> {
        private final AtomicBoolean created = new AtomicBoolean();

        @Override
        public Object create() throws Exception {
            if (!created.getAndSet(true)) {
                return new Object();
            }
            throw new Exception("Already created");
        }

        @Override
        public boolean validateObject(final PooledObject<Object> p) {
            return true;
        }

        @Override
        public PooledObject<Object> wrap(final Object obj) {
            return new DefaultPooledObject<>(new Object());
        }
    }

    private static final class WinnerRunnable implements Runnable {
        private final CountDownLatch barrier;
        private final AtomicBoolean failed;
        private final GenericObjectPool<Object> pool;
        private WinnerRunnable(final GenericObjectPool<Object> pool, final CountDownLatch barrier, final AtomicBoolean failed) {
            this.pool = pool;
            this.failed = failed;
            this.barrier = barrier;
        }
        @Override
        public void run() {
            try {
                println("start borrowing in parallel thread");
                final Object obj = pool.borrowObject();

                // wait for another thread to start borrowObject
                if (!barrier.await(5, TimeUnit.SECONDS)) {
                    println("Timeout waiting");
                    failed.set(true);
                } else {
                    // just to make sure, borrowObject has started waiting on queue
                    Waiter.sleepQuietly(1000);
                }

                pool.returnObject(obj);
                println("ended borrowing in parallel thread");
            } catch (final Exception e) {
                failed.set(true);
                e.printStackTrace();
            }
        }
    }

    private static final Duration NEG_ONE_DURATION = Duration.ofMillis(-1);

    private static void println(final String msg) {
        // System.out.println(msg);
    }

    @Test
    @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
    void testBorrowObjectStuck() {
        final SingleObjectFactory factory = new SingleObjectFactory();
        final GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
        config.setMaxIdle(1);
        config.setMaxTotal(1);
        config.setBlockWhenExhausted(true);
        config.setMinIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(false);
        config.setTimeBetweenEvictionRuns(NEG_ONE_DURATION);
        config.setMinEvictableIdleTime(NEG_ONE_DURATION);
        config.setMinEvictableIdleDuration(NEG_ONE_DURATION);
        config.setSoftMinEvictableIdleDuration(NEG_ONE_DURATION);

        config.setMaxWait(NEG_ONE_DURATION);
        try (GenericObjectPool<Object> pool = new GenericObjectPool<>(factory, config)) {

            final AtomicBoolean failed = new AtomicBoolean();
            final CountDownLatch barrier = new CountDownLatch(1);
            final Thread thread1 = new Thread(new WinnerRunnable(pool, barrier, failed));
            thread1.start();

            // wait for object to be created
            while (!factory.created.get()) {
                Waiter.sleepQuietly(5);
            }

            // now borrow
            barrier.countDown();
            try {
                println("try borrow in main thread");

                final Object o = pool.borrowObject();
                println("Success borrow in main thread " + o);
            } catch (final Exception e) {
                e.printStackTrace();
            }

            assertFalse(failed.get());
        }

    }
}
