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

package org.apache.commons.pool3.pool407;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool3.PooledObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests POOL-407.
 */
public class Pool407Test extends AbstractPool407Test {

    /**
     * Borrows from a pool and then immediately returns to that a pool.
     */
    private static final class Pool407RoundtripRunnable implements Runnable {
        private final Pool407 pool;

        Pool407RoundtripRunnable(final Pool407 pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            try {
                final Pool407Fixture object = pool.borrowObject();
                if (object != null) {
                    pool.returnObject(object);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void assertShutdown(final ExecutorService executor, final Duration poolConfigMaxWait, final AbstractPool407Factory factory)
            throws InterruptedException {
        // Old note: This never finishes when the factory makes nulls because two threads are stuck forever
        // If a factory always returns a null object or a null poolable object, then we will wait forever.
        // This would also be true is object validation always fails.
        executor.shutdown();
        final boolean termination = executor.awaitTermination(Pool407Constants.AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS);
        // Order matters: test create() before makeObject()
        // Calling create() here in this test should not have side-effects
        final Pool407Fixture obj = factory.create();
        // Calling makeObject() here in this test should not have side-effects
        final PooledObject<Pool407Fixture> pooledObject = obj != null ? factory.makeObject() : null;
        assertShutdown(termination, poolConfigMaxWait, obj, pooledObject);
    }

    private void test(final AbstractPool407Factory factory, final int poolSize, final Duration poolConfigMaxWait) throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try (Pool407 pool = new Pool407(factory, poolConfigMaxWait)) {
            // Start 'poolSize' threads that try to borrow a Pool407Fixture with the same key
            for (int i = 0; i < poolSize; i++) {
                executor.execute(new Pool407RoundtripRunnable(pool));
            }
            assertShutdown(executor, poolConfigMaxWait, factory);
        }
    }

    @Test
    public void testNormalFactoryNonNullFixtureWaitMax() throws InterruptedException {
        test(new Pool407NormalFactory(new Pool407Fixture()), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Test
    @Disabled
    public void testNormalFactoryNullFixtureWaitMax() throws InterruptedException {
        test(new Pool407NormalFactory(null), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Disabled
    @Test
    public void testNullObjectFactoryWaitMax() throws InterruptedException {
        test(new Pool407NullObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Test
    @Disabled
    public void testNullObjectFactoryWaitShort() throws InterruptedException {
        test(new Pool407NullObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_SHORT);
    }

    @Test
    @Disabled
    public void testNullPoolableFactoryWaitMax() throws InterruptedException {
        test(new Pool407NullPoolableObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Test
    @Disabled
    public void testNullPoolableFactoryWaitShort() throws InterruptedException {
        test(new Pool407NullPoolableObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_SHORT);
    }
}
