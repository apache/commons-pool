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

package org.apache.commons.pool.performance;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Multi-thread performance test
 * 
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$ 
 */
public class PerformanceTest {
    private int logLevel = 0;
    private int nrIterations = 5;

    private GenericObjectPool pool;
    private boolean start = false;
    private volatile int waiting = 0;
    private volatile int complete = 0;
    private volatile long totalBorrowTime = 0;
    private volatile long totalReturnTime = 0;
    private volatile int nrSamples = 0; 

    public void setLogLevel(int i) {
        logLevel = i;
    }
    
    private void init() {
        start = false;
        waiting = 0;
        complete = 0;
        totalBorrowTime = 0;
        totalReturnTime = 0;
        nrSamples = 0;     
    }

    class MyThread implements Runnable {
        long borrowTime;
        long returnTime;

        public void runOnce() {
            try {
                waiting++;
                if (logLevel >= 5) {
                    String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name + "   waiting: " + waiting + "   complete: " + complete);
                }
                long bbegin = System.currentTimeMillis();
                Object o = pool.borrowObject();
                long bend = System.currentTimeMillis();
                waiting--;
                do {
                    Thread.yield();
                }
                while (!start);

                if (logLevel >= 3) {
                    String name = "thread" + Thread.currentThread().getName();
                    System.out.println(name + "    waiting: " + waiting + "   complete: " + complete);
                }
                                 
                long rbegin = System.currentTimeMillis();
                pool.returnObject(o);
                long rend = System.currentTimeMillis();
                Thread.yield();
                complete++;
                borrowTime = (bend-bbegin);
                returnTime = (rend-rbegin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            runOnce(); // warmup
            for (int i = 0; i<nrIterations; i++) {
                runOnce();
                totalBorrowTime += borrowTime;
                totalReturnTime += returnTime;
                nrSamples++;
                if (logLevel >= 2) {
                    String name = "thread" + Thread.currentThread().getName();
                    System.out.println(
                        "result " + nrSamples + "\t" + name 
                        + "\t" + "borrow time: " + borrowTime + "\t" + "return time: " + returnTime
                        + "\t" + "waiting: " + waiting + "\t" + "complete: " + complete);
                }
            }
        }
    }

    private void run(int nrIterations, int nrThreads, int maxActive, int maxIdle) {
        this.nrIterations = nrIterations;
        init();
        
        SleepingObjectFactory factory = new SleepingObjectFactory();
        if (logLevel >= 4) { factory.setDebug(true); } 
        pool = new GenericObjectPool(factory);
        pool.setMaxActive(maxActive);
        pool.setMaxIdle(maxIdle);
        pool.setTestOnBorrow(true);

        Thread[] threads = new Thread[nrThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i]= new Thread(new MyThread(), Integer.toString(i));
            Thread.yield();
        }
        if (logLevel >= 1) { System.out.println("created"); } 
        Thread.yield();

        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
            Thread.yield();
        }
        if (logLevel >= 1) { System.out.println("started"); }
        Thread.yield();

        start = true;
        if (logLevel >= 1) { System.out.println("go"); }
        Thread.yield();

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (logLevel >= 1) { System.out.println("finish"); }
        System.out.println("-----------------------------------------");
        System.out.println("nrIterations: " + nrIterations);
        System.out.println("nrThreads: " + nrThreads);
        System.out.println("maxActive: " + maxActive);
        System.out.println("maxIdle: " + maxIdle);
        System.out.println("nrSamples: " + nrSamples);
        System.out.println("totalBorrowTime: " + totalBorrowTime);
        System.out.println("totalReturnTime: " + totalReturnTime);
        System.out.println("avg BorrowTime: " + totalBorrowTime/nrSamples);
        System.out.println("avg ReturnTime: " + totalReturnTime/nrSamples);
    }

    public static void main(String[] args) {
        PerformanceTest test = new PerformanceTest();
        test.setLogLevel(0);
        System.out.println("Increase threads");
        test.run(1,  50,  5,  5);
        test.run(1, 100,  5,  5);
        test.run(1, 200,  5,  5);
        test.run(1, 400,  5,  5);

        System.out.println("Increase threads & poolsize");
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
