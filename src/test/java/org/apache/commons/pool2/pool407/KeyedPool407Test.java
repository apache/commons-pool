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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class KeyedPool407Test {

    private static class KeyedPool407Borrower implements Runnable {
        private final KeyedPool407 pool;

        public KeyedPool407Borrower(final KeyedPool407 pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            try {
                final String key = "key";
                final KeyedPool407Fixture foo = pool.borrowObject(key);
                if (foo != null) {
                    pool.returnObject(key, foo);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final int POOL_SIZE = 3;

    private void test(final KeyedPool407Fixture fixture, final int poolSize) throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        final KeyedPool407 pool = new KeyedPool407(fixture);

        // start 'poolSize' threads that try to borrow a Pool407Fixture with the same key
        for (int i = 0; i < poolSize; i++) {
            executor.execute(new KeyedPool407Borrower(pool));
        }

        // this never finishes because two threads are stuck forever
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void testFail() throws InterruptedException {
        test(null, POOL_SIZE);
    }

    @Test
    public void testPass() throws InterruptedException {
        test(new KeyedPool407Fixture(), 3);
    }
}
