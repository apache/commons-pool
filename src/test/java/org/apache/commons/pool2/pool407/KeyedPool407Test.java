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

package org.apache.commons.pool2.pool407;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.PooledObject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests POOL-407.
 */
public class KeyedPool407Test extends AbstractPool407Test {

    /**
     * Borrows from a pool and then immediately returns to that a pool.
     */
    private static final class KeyedPool407RoundtripRunnable implements Runnable {
        private final KeyedPool407 pool;

        public KeyedPool407RoundtripRunnable(final KeyedPool407 pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            try {
                final KeyedPool407Fixture object = pool.borrowObject(KEY);
                if (object != null) {
                    pool.returnObject(KEY, object);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final String KEY = "key";

    protected void assertShutdown(final ExecutorService executor, final Duration poolConfigMaxWait, final AbstractKeyedPool407Factory factory) throws Exception {
        // Old note: This never finishes when the factory makes nulls because two threads are stuck forever
        // If a factory always returns a null object or a null poolable object, then we will wait forever.
        // This would also be true is object validation always fails.
        executor.shutdown();
        final boolean termination = executor.awaitTermination(Pool407Constants.AWAIT_TERMINATION_SECONDS, TimeUnit.SECONDS);
        // Order matters: test create() before makeObject()
        // Calling create() here in this test should not have side-effects
        final KeyedPool407Fixture obj = factory.create(KEY);
        // Calling makeObject() here in this test should not have side-effects
        final PooledObject<KeyedPool407Fixture> pooledObject = obj != null ? factory.makeObject(KEY) : null;
        assertShutdown(termination, poolConfigMaxWait, obj, pooledObject);
    }

    private void test(final AbstractKeyedPool407Factory factory, final int poolSize, final Duration poolConfigMaxWait) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try (final KeyedPool407 pool = new KeyedPool407(factory, poolConfigMaxWait)) {
            // Start 'poolSize' threads that try to borrow a Pool407Fixture with the same key
            for (int i = 0; i < poolSize; i++) {
                executor.execute(new KeyedPool407RoundtripRunnable(pool));
            }
            assertShutdown(executor, poolConfigMaxWait, factory);
        }
    }

    @Test
    public void testNormalFactoryNonNullFixtureWaitMax() throws Exception {
        test(new KeyedPool407NormalFactory(new KeyedPool407Fixture()), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Disabled
    @Test
    public void testNormalFactoryNullFixtureWaitMax() throws Exception {
        test(new KeyedPool407NormalFactory(null), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Disabled
    @Test
    public void testNullObjectFactoryWaitMax() throws Exception {
        test(new KeyedPool407NullObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Disabled
    @Test
    public void testNullObjectFactoryWaitShort() throws Exception {
        test(new KeyedPool407NullObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_SHORT);
    }

    @Disabled
    @Test
    public void testNullPoolableFactoryWaitMax() throws Exception {
        test(new KeyedPool407NullPoolableObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_FOREVER);
    }

    @Disabled
    @Test
    public void testNullPoolableFactoryWaitShort() throws Exception {
        test(new KeyedPool407NullPoolableObjectFactory(), Pool407Constants.POOL_SIZE, Pool407Constants.WAIT_SHORT);
    }
}
