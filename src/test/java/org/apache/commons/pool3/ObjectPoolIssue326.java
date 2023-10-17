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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.pool3.impl.BaseObjectPoolConfig;
import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.apache.commons.pool3.impl.GenericKeyedObjectPool;
import org.apache.commons.pool3.impl.GenericKeyedObjectPoolConfig;

/**
 * On my box with 4 cores this test fails at between 5s and 900s with an average
 * of 240s (data from 10 runs of test).
 *
 * It is hard to turn this in a unit test because it can affect the build
 * negatively since you need to run it for a while.
 */
public final class ObjectPoolIssue326 {
    private static final class ObjectFactory extends BaseKeyedPooledObjectFactory<Integer, Object, RuntimeException> {
        @Override
        public Object create(final Integer s) {
            return new TestObject();
        }

        @Override
        public PooledObject<Object> wrap(final Object o) {
            return new DefaultPooledObject<>(o);
        }
    }

    private static final class Task<E extends Exception> implements Callable<Object> {
        private final GenericKeyedObjectPool<Integer, Object, E> pool;
        private final int key;

        Task(final GenericKeyedObjectPool<Integer, Object, E> pool, final int count) {
            this.pool = pool;
            this.key = count % 20;
        }

        private void busyWait(final long timeMillis) {
            // busy waiting intentionally as a simple thread.sleep fails to reproduce
            final long endTimeMillis = System.currentTimeMillis() + timeMillis;
            while (System.currentTimeMillis() < endTimeMillis) {
                // empty
            }
        }

        @Override
        public Object call() throws E {
            try {
                final Object value;
                value = pool.borrowObject(key);
                // don't make this too long or it won't reproduce, and don't make it zero or it
                // won't reproduce
                // constant low value also doesn't reproduce
                busyWait(System.currentTimeMillis() % 4);
                pool.returnObject(key, value);
                return "success";
            } catch (final NoSuchElementException e) {
                // ignore, we've exhausted the pool
                // not sure whether what we do here matters for reproducing
                busyWait(System.currentTimeMillis() % 20);
                return "exhausted";
            }
        }
    }

    private static final class TestObject {
    }

    public static void main(final String[] args) {
        try {
            new ObjectPoolIssue326().run();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private <E extends Exception> List<Task<E>> createTasks(final GenericKeyedObjectPool<Integer, Object, E> pool) {
        final List<Task<E>> tasks = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            tasks.add(new Task<>(pool, i));
        }
        return tasks;
    }

    private void run() throws InterruptedException, ExecutionException {
        final GenericKeyedObjectPoolConfig<Object> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxTotalPerKey(5);
        poolConfig.setMinIdlePerKey(-1);
        poolConfig.setMaxIdlePerKey(-1);
        poolConfig.setLifo(true);
        poolConfig.setFairness(true);
        poolConfig.setMaxWait(Duration.ofSeconds(30));
        poolConfig.setMinEvictableIdleDuration(Duration.ofMillis(-1));
        poolConfig.setSoftMinEvictableIdleDuration(Duration.ofMillis(-1));
        poolConfig.setNumTestsPerEvictionRun(1);
        poolConfig.setTestOnCreate(false);
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);
        poolConfig.setTestWhileIdle(false);
        poolConfig.setDurationBetweenEvictionRuns(Duration.ofSeconds(5));
        poolConfig.setEvictionPolicyClassName(BaseObjectPoolConfig.DEFAULT_EVICTION_POLICY_CLASS_NAME);
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setJmxEnabled(false);
        poolConfig.setJmxNameBase(null);
        poolConfig.setJmxNamePrefix(null);

        final GenericKeyedObjectPool<Integer, Object, RuntimeException> pool = new GenericKeyedObjectPool<>(new ObjectFactory(), poolConfig);

        // number of threads to reproduce is finicky. this count seems to be best for my
        // 4 core box.
        // too many doesn't reproduce it ever, too few doesn't either.
        final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        final Instant startTime = Instant.now();
        long testIter = 0;
        try {
            while (true) {
                testIter++;
                if (testIter % 1000 == 0) {
                    System.out.println(testIter);
                }
                final List<Task<RuntimeException>> tasks = createTasks(pool);
                final List<Future<Object>> futures = service.invokeAll(tasks);
                for (final Future<Object> future : futures) {
                    future.get();
                }
            }
        } finally {
            System.out.println("Time: " + Duration.between(startTime, Instant.now()));
            service.shutdown();
        }
    }
}

/*
 * Example stack trace: java.util.concurrent.ExecutionException:
 * java.lang.NullPointerException at
 * java.util.concurrent.FutureTask.report(FutureTask.java:122) at
 * java.util.concurrent.FutureTask.get(FutureTask.java:192) at
 * threading_pool.ObjectPoolIssue.run(ObjectPoolIssue.java:63) at
 * threading_pool.ObjectPoolIssue.main(ObjectPoolIssue.java:23) at
 * sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) at
 * sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 * at
 * sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.
 * java:43) at java.lang.reflect.Method.invoke(Method.java:498) at
 * com.intellij.rt.execution.application.AppMain.main(AppMain.java:147) Caused
 * by: java.lang.NullPointerException at
 * org.apache.commons.pool3.impl.GenericKeyedObjectPool.returnObject(
 * GenericKeyedObjectPool.java:474) at
 * threading_pool.ObjectPoolIssue$Task.call(ObjectPoolIssue.java:112) at
 * java.util.concurrent.FutureTask.run(FutureTask.java:266) at
 * java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:
 * 1142) at
 * java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:
 * 617) at java.lang.Thread.run(Thread.java:745)
 */
