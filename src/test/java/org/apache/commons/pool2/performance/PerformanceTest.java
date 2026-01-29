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
package org.apache.commons.pool2.performance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Multi-thread performance test
 */
class PerformanceTest {
    final class PerfTask implements Callable<TaskStats> {
        final TaskStats taskStats = new TaskStats();
        long borrowTimeNanos;
        long returnTimeNanos;

        @Override
        public TaskStats call() {
            runOnce(); // warmup
            for (int i = 0; i < nrIterations; i++) {
                runOnce();
                taskStats.totalBorrowNanos += borrowTimeNanos;
                taskStats.totalReturnNanos += returnTimeNanos;
                taskStats.nrSamples++;
                if (logLevel >= 2) {
                    final String name = "thread" + Thread.currentThread().getName();
                    System.out.println(
                            "result " + taskStats.nrSamples + '\t' + name + '\t' + "borrow time: " + Duration.ofNanos(borrowTimeNanos) + '\t' + "return time: "
                                    + Duration.ofNanos(returnTimeNanos) + '\t' + "waiting: " + taskStats.waiting + '\t' + "complete: " + taskStats.complete);
                }
            }
            return taskStats;
        }

        public void runOnce() {
            try {
                taskStats.waiting++;
                if (logLevel >= 5) {
                    final String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name + "   waiting: " + taskStats.waiting + "   complete: " + taskStats.complete);
                }
                final long bbeginNanos = System.nanoTime();
                final Integer o = pool.borrowObject();
                final long bendNanos = System.nanoTime();
                taskStats.waiting--;
                if (logLevel >= 3) {
                    final String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name + "    waiting: " + taskStats.waiting + "   complete: " + taskStats.complete);
                }
                final long rbeginNanos = System.nanoTime();
                pool.returnObject(o);
                final long rendNanos = System.nanoTime();
                Thread.yield();
                taskStats.complete++;
                borrowTimeNanos = bendNanos - bbeginNanos;
                returnTimeNanos = rendNanos - rbeginNanos;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final class TaskStats {
        int waiting;
        int complete;
        long totalBorrowNanos;
        long totalReturnNanos;
        int nrSamples;
    }

    public static void main(final String[] args) {
        final PerformanceTest test = new PerformanceTest();
        test.setLogLevel(0);
        System.out.println("Increase threads");
        test.run(1, 50, 5, 5);
        test.run(1, 100, 5, 5);
        test.run(1, 200, 5, 5);
        test.run(1, 400, 5, 5);
        System.out.println("Increase threads & poolSize");
        test.run(1, 50, 5, 5);
        test.run(1, 100, 10, 10);
        test.run(1, 200, 20, 20);
        test.run(1, 400, 40, 40);
        System.out.println("Increase maxIdle");
        test.run(1, 400, 40, 5);
        test.run(1, 400, 40, 40);
//      System.out.println("Show creation/destruction of objects");
//      test.setLogLevel(4);
//      test.run(1, 400, 40,  5);
    }

    private int logLevel;
    private int nrIterations = 5;
    private GenericObjectPool<Integer> pool;

    private void run(final int iterations, final int nrThreads, final int maxTotal, final int maxIdle) {
        this.nrIterations = iterations;
        final SleepingObjectFactory factory = new SleepingObjectFactory();
        if (logLevel >= 4) {
            factory.setDebug(true);
        }
        pool = new GenericObjectPool<>(factory);
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(maxIdle);
        pool.setTestOnBorrow(true);
        final ExecutorService threadPool = Executors.newFixedThreadPool(nrThreads);
        final List<Callable<TaskStats>> tasks = new ArrayList<>();
        for (int i = 0; i < nrThreads; i++) {
            tasks.add(new PerfTask());
            Thread.yield();
        }
        if (logLevel >= 1) {
            System.out.println("created");
        }
        Thread.yield();
        List<Future<TaskStats>> futures = null;
        try {
            futures = threadPool.invokeAll(tasks);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        if (logLevel >= 1) {
            System.out.println("started");
        }
        Thread.yield();
        if (logLevel >= 1) {
            System.out.println("go");
        }
        Thread.yield();
        if (logLevel >= 1) {
            System.out.println("finish");
        }
        final TaskStats aggregate = new TaskStats();
        if (futures != null) {
            for (final Future<TaskStats> future : futures) {
                TaskStats taskStats = null;
                try {
                    taskStats = future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
                if (taskStats != null) {
                    aggregate.complete += taskStats.complete;
                    aggregate.nrSamples += taskStats.nrSamples;
                    aggregate.totalBorrowNanos += taskStats.totalBorrowNanos;
                    aggregate.totalReturnNanos += taskStats.totalReturnNanos;
                    aggregate.waiting += taskStats.waiting;
                }
            }
        }
        final Duration totalBorrowDuration = Duration.ofNanos(aggregate.totalBorrowNanos);
        final Duration totalReturnDuration = Duration.ofNanos(aggregate.totalReturnNanos);
        System.out.println("-----------------------------------------");
        System.out.println("nrIterations: " + iterations);
        System.out.println("nrThreads: " + nrThreads);
        System.out.println("maxTotal: " + maxTotal);
        System.out.println("maxIdle: " + maxIdle);
        System.out.println("nrSamples: " + aggregate.nrSamples);
        System.out.println("totalBorrowTime: " + totalBorrowDuration);
        System.out.println("totalReturnTime: " + totalReturnDuration);
        System.out.println("avg BorrowTime: " + totalBorrowDuration.dividedBy(aggregate.nrSamples));
        System.out.println("avg ReturnTime: " + totalReturnDuration.dividedBy(aggregate.nrSamples));
        threadPool.shutdown();
    }

    public void setLogLevel(final int i) {
        logLevel = i;
    }
}
