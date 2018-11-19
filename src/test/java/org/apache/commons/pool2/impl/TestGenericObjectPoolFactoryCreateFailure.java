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

package org.apache.commons.pool2.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavel Kolesov as contributed in POOL-340
 */
public class TestGenericObjectPoolFactoryCreateFailure {

    @Test(timeout = 10_000)
    public void testBorrowObjectStuck() {
        SingleObjectFactory factory = new SingleObjectFactory();
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxIdle(1);
        config.setMaxTotal(1);
        config.setBlockWhenExhausted(true);
        config.setMinIdle(0);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(false);
        config.setTimeBetweenEvictionRunsMillis(-1);
        config.setMinEvictableIdleTimeMillis(-1);
        config.setSoftMinEvictableIdleTimeMillis(-1);

        config.setMaxWaitMillis(-1);
        GenericObjectPool<Object> pool = new GenericObjectPool<>(factory, config);

        AtomicBoolean failed = new AtomicBoolean();
        CountDownLatch barrier = new CountDownLatch(1);
        Thread thread1 = new Thread(new WinnerRunnable(pool, barrier, failed));
        thread1.start();

        // wait for object to be created
        while(!factory.created.get()) {
            sleepIgnoreException(5);
        }

        // now borrow
        barrier.countDown();
        try {
            System.out.println("try borrow in main thread");

            Object o = pool.borrowObject();
            System.out.println("Success borrow in main thread " + o);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Assert.assertFalse(failed.get());

    }

    private static class SingleObjectFactory extends BasePooledObjectFactory<Object> {
        private final AtomicBoolean created = new AtomicBoolean();
        private final AtomicBoolean validated = new AtomicBoolean();
        @Override
        public Object create() throws Exception {
            if (!created.getAndSet(true)) {
                return new Object();
            }
            throw new Exception("Already created");
        }

        @Override
        public PooledObject<Object> wrap(Object obj) {
            return new DefaultPooledObject<>(new Object());
        }

        @Override
        public boolean validateObject(PooledObject<Object> p) {
            return true;
        }
    }

    private static class WinnerRunnable implements Runnable {
        private final GenericObjectPool<Object> pool;
        private final AtomicBoolean failed;
        private final CountDownLatch barrier;
        private WinnerRunnable(GenericObjectPool<Object> pool, CountDownLatch barrier, AtomicBoolean failed) {
            this.pool = pool;
            this.failed = failed;
            this.barrier = barrier;
        }
        @Override
        public void run() {
            try {
                System.out.println("start borrowing in parallel thread");
                Object obj = pool.borrowObject();

                // wait for another thread to start borrowObject
                if (!barrier.await(5, TimeUnit.SECONDS)) {
                    System.out.println("Timeout waiting");
                    failed.set(true);
                } else {
                    // just to make sure, borrowObject has started waiting on queue
                    sleepIgnoreException(1000);
                }

                pool.returnObject(obj);
                System.out.println("ended borrowing in parallel thread");
            } catch (Exception e) {
                failed.set(true);
                e.printStackTrace();
            }
        }
    }

    private static void sleepIgnoreException(long millis) {
        try {
            Thread.sleep(millis);
        } catch(Throwable e) {
            // ignore
        }
    }
}