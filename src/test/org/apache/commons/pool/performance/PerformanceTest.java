/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/test/org/apache/commons/pool/performance/PerformanceTest.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/10/09 21:45:57 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * http://www.apache.org/
 *
 */

package org.apache.commons.pool.performance;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Multi-thread performance test
 * 
 * @author Dirk Verbeeck
 */
public class PerformanceTest {
    private int logLevel = 0;
    private int nrIterations = 5;
    private int nrThreads = 100;

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
        this.nrThreads = nrThreads;
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
