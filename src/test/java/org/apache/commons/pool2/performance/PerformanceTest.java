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

package org.apache.commons.pool2.performance;

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
 *
 */
public class PerformanceTest {
    private int logLevel = 0;
    private int nrIterations = 5;

    private GenericObjectPool<Integer> pool;

    public void setLogLevel(final int i) {
        logLevel = i;
    }

    private static class TaskStats {
        public int waiting = 0;
        public int complete = 0;
        public long totalBorrowTime = 0;
        public long totalReturnTime = 0;
        public int nrSamples = 0;
    }

    class PerfTask implements Callable<TaskStats> {
        final TaskStats taskStats = new TaskStats();
        long borrowTimeMillis;
        long returnTimeMillis;

        public void runOnce() {
            try {
                taskStats.waiting++;
                if (logLevel >= 5) {
                    final String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name +
                            "   waiting: " + taskStats.waiting +
                            "   complete: " + taskStats.complete);
                }
                final long bbeginMillis = System.currentTimeMillis();
                final Integer o = pool.borrowObject();
                final long bendMillis = System.currentTimeMillis();
                taskStats.waiting--;

                if (logLevel >= 3) {
                    final String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name +
                            "    waiting: " + taskStats.waiting +
                            "   complete: " + taskStats.complete);
                }

                final long rbeginMillis = System.currentTimeMillis();
                pool.returnObject(o);
                final long rendMillis = System.currentTimeMillis();
                Thread.yield();
                taskStats.complete++;
                borrowTimeMillis = bendMillis - bbeginMillis;
                returnTimeMillis = rendMillis - rbeginMillis;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

       @Override
    public TaskStats call() throws Exception {
           runOnce(); // warmup
           for (int i = 0; i < nrIterations; i++) {
               runOnce();
               taskStats.totalBorrowTime += borrowTimeMillis;
               taskStats.totalReturnTime += returnTimeMillis;
               taskStats.nrSamples++;
               if (logLevel >= 2) {
                   final String name = "thread" + Thread.currentThread().getName();
                   System.out.println("result " + taskStats.nrSamples + '\t' +
                           name + '\t' + "borrow time: " + borrowTimeMillis + '\t' +
                           "return time: " + returnTimeMillis + '\t' + "waiting: " +
                           taskStats.waiting + '\t' + "complete: " +
                           taskStats.complete);
               }
           }
           return taskStats;
       }
    }

    private void run(final int iterations, final int nrThreads, final int maxTotal, final int maxIdle) {
        this.nrIterations = iterations;

        final SleepingObjectFactory factory = new SleepingObjectFactory();
        if (logLevel >= 4) { factory.setDebug(true); }
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
            e.printStackTrace();
        }

        if (logLevel >= 1) { System.out.println("started"); }
        Thread.yield();

        if (logLevel >= 1) { System.out.println("go"); }
        Thread.yield();

        if (logLevel >= 1) { System.out.println("finish"); }

        final TaskStats aggregate = new TaskStats();
        if (futures != null) {
            for (final Future<TaskStats> future : futures) {
                TaskStats taskStats = null;
                try {
                    taskStats = future.get();
                } catch (final InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (taskStats != null) {
                    aggregate.complete += taskStats.complete;
                    aggregate.nrSamples += taskStats.nrSamples;
                    aggregate.totalBorrowTime += taskStats.totalBorrowTime;
                    aggregate.totalReturnTime += taskStats.totalReturnTime;
                    aggregate.waiting += taskStats.waiting;
                }
            }
        }

        System.out.println("-----------------------------------------");
        System.out.println("nrIterations: " + iterations);
        System.out.println("nrThreads: " + nrThreads);
        System.out.println("maxTotal: " + maxTotal);
        System.out.println("maxIdle: " + maxIdle);
        System.out.println("nrSamples: " + aggregate.nrSamples);
        System.out.println("totalBorrowTime: " + aggregate.totalBorrowTime);
        System.out.println("totalReturnTime: " + aggregate.totalReturnTime);
        System.out.println("avg BorrowTime: " +
                aggregate.totalBorrowTime / aggregate.nrSamples);
        System.out.println("avg ReturnTime: " +
                aggregate.totalReturnTime / aggregate.nrSamples);

        threadPool.shutdown();
    }

    public static void main(final String[] args) {
        final PerformanceTest test = new PerformanceTest();
        test.setLogLevel(0);
        System.out.println("Increase threads");
        test.run(1,  50,  5,  5);
        test.run(1, 100,  5,  5);
        test.run(1, 200,  5,  5);
        test.run(1, 400,  5,  5);

        System.out.println("Increase threads & poolSize");
        test.run(1,  50,  5,  5);
        test.run(1, 100, 10, 10);
        test.run(1, 200, 20, 20);
        test.run(1, 400, 40, 40);

        System.out.println("Increase maxIdle");
        test.run(1, 400, 40,  5);
        test.run(1, 400, 40, 40);

//      System.out.println("Show creation/destruction of objects");
//      test.setLogLevel(4);
//      test.run(1, 400, 40,  5);
    }
}
