/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool.composite;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A collection of benchmarks to confirm and establish performance properties.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
public class PerformanceTest {
    public static final Object LOCK = new Object();
    private static final List borrowPolicies = Arrays.asList(BorrowPolicy.values());
    private static final List exhaustionPolicies = Arrays.asList(ExhaustionPolicy.values());
    private static final List maxIdles = Arrays.asList(new Integer[] {new Integer(-1), new Integer(10)});
    private static final List maxActives = Arrays.asList(new Integer[] {new Integer(-1), new Integer(10)});
    private static final List limitPolicies = Arrays.asList(LimitPolicy.values());
    private static final List maxWaits = Arrays.asList(new Integer[] {new Integer(-1), new Integer(50)}); // not sure how to use this
    private static final List trackingPolicies = new ArrayList(Arrays.asList(TrackingPolicy.values()));
    private static final List validateOnReturns = Arrays.asList(new Boolean[] {Boolean.FALSE, Boolean.TRUE});
    // evictIdleMillis
    // evictInvalidFrequencyMillis
    static {
        trackingPolicies.remove(TrackingPolicy.DEBUG); // based off of TrackingPolicy.REFERENCE and slower (about 1/5 as fast)
    }

    private Iterator borrowIter = borrowPolicies.iterator();
    private Iterator exhaustionIter = exhaustionPolicies.iterator();
    private Iterator maxIdleIter = maxIdles.iterator();
    private Iterator maxActiveIter = maxActives.iterator();
    private Iterator limitIter = limitPolicies.iterator();
    private Iterator trackingIter = trackingPolicies.iterator();
    private Iterator validateIter = validateOnReturns.iterator();

    private PoolableObjectFactory objectFactory = new IntegerFactory();

    private final CompositeObjectPoolFactory poolFactory = new CompositeObjectPoolFactory(objectFactory);

    private Set ranCombinations = new HashSet();

    private boolean nextCompositeSettings() {
        boolean newCombination = true;
        if (!validateIter.hasNext()) {
            validateIter = validateOnReturns.iterator();

            if (!trackingIter.hasNext()) {
                trackingIter = trackingPolicies.iterator();

                if (!limitIter.hasNext()) {
                    limitIter = limitPolicies.iterator();

                    if (!maxActiveIter.hasNext()) {
                        maxActiveIter = maxActives.iterator();

                        if (!maxIdleIter.hasNext()) {
                            maxIdleIter = maxIdles.iterator();

                            if (!exhaustionIter.hasNext()) {
                                exhaustionIter = exhaustionPolicies.iterator();

                                if (!borrowIter.hasNext()) {
                                    borrowIter = borrowPolicies.iterator();
                                    newCombination = false;
                                }
                                poolFactory.setBorrowPolicy((BorrowPolicy)borrowIter.next());
                            }
                            poolFactory.setExhaustionPolicy((ExhaustionPolicy)exhaustionIter.next());
                        }
                        poolFactory.setMaxIdle(((Integer)maxIdleIter.next()).intValue());
                    }
                    poolFactory.setMaxActive(((Integer)maxActiveIter.next()).intValue());
                }
                poolFactory.setLimitPolicy((LimitPolicy)limitIter.next());
            }
            poolFactory.setTrackerType((TrackingPolicy)trackingIter.next());
        }
        poolFactory.setValidateOnReturn(((Boolean)validateIter.next()).booleanValue());

        return newCombination;
    }

    private void runEveryComposite(final int seconds) {
        do {
            try {
                final String combination = poolFactory.toString();
                if (ranCombinations.add(combination)) { // skip dups
                    ((IntegerFactory)objectFactory).reset();
                    final ObjectPool pool = poolFactory.createPool();
                    System.out.print(combination);
                    System.out.print("\t");

                    testForNumSeconds(seconds, pool);
                }
            } catch (Exception e) {
                // ignore
            }
            gc();
        } while (nextCompositeSettings());
        ranCombinations.clear();
    }

    private static void gc() {
        System.gc();
        System.gc();
        System.gc();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // ignored
        }
    }

    private static double testForNumSeconds(final int seconds, final ObjectPool pool) throws Exception {
        // Prime for FAIL exhaust
        try {
            if (pool.getNumIdle() == 0) {
                pool.addObject();
            }
        } catch (UnsupportedOperationException uoe) {
            // ignore
        }

        long startTime = System.currentTimeMillis();
        while (startTime == System.currentTimeMillis()) ;
        startTime = System.currentTimeMillis();
        final long endTime = startTime + (seconds * 1000);
        long actualEndTime;
        int loops = 0;
        while (endTime >= (actualEndTime = System.currentTimeMillis())) {
            Object obj = null;
            try {
                obj = pool.borrowObject();
                pool.returnObject(obj);
            } catch (Exception e) {
                if (obj != null) {
                    pool.invalidateObject(obj);
                }
            }
            loops++;
        }
        final double borrowsPerSecond = ((double)loops / ((double)(actualEndTime - startTime) / 1000D));
        System.out.println(borrowsPerSecond + " borrows per second");
        return borrowsPerSecond;
    }


    private void runEveryGeneric(final int seconds) {
        ((IntegerFactory)objectFactory).reset();
        ObjectPool pool = new GenericObjectPool(objectFactory, -1, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, -1, 0, true, false, -1, -1, -1, false);
        try {
            System.out.print("GenericObjectPool{maxActive=-1, maxIdle=-1}");
            System.out.print("\t");
            testForNumSeconds(seconds, pool);
            gc();

            pool = new GenericObjectPool(objectFactory, 10, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, -1, 0, true, false, -1, -1, -1, false);
            System.out.print("GenericObjectPool{maxActive=10, maxIdle=-1}");
            System.out.print("\t");
            testForNumSeconds(seconds, pool);
            gc();

            pool = new GenericObjectPool(objectFactory, -1, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, 10, 0, true, false, -1, -1, -1, false);
            System.out.print("GenericObjectPool{maxActive=-1, maxIdle=10}");
            System.out.print("\t");
            testForNumSeconds(seconds, pool);
            gc();

            pool = new GenericObjectPool(objectFactory, 10, GenericObjectPool.WHEN_EXHAUSTED_GROW, -1, 10, 0, true, false, -1, -1, -1, false);
            System.out.print("GenericObjectPool{maxActive=10, maxIdle=10}");
            System.out.print("\t");
            testForNumSeconds(seconds, pool);
            gc();
        } catch (Exception e) {

        }
    }


    private void compareCompositeGerneic(final int seconds) {
        final CompareCompositeGeneric ccg = new CompareCompositeGeneric();
        do {
            gc();

            ObjectPool objectPool;
            gc();

            try {
                System.out.print("GOP:noTestOnBorrow\t" + ccg + "\t");
                objectPool = ccg.getGeneric();
                ((GenericObjectPool)objectPool).setTestOnBorrow(false);
                testForNumSeconds(seconds, objectPool);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            double gopBPS = -1;
            try {
                System.out.print("GenericObjectPool\t" + ccg + "\t");
                objectPool = ccg.getGeneric();
                ((GenericObjectPool)objectPool).setTestOnBorrow(true);
                gopBPS = testForNumSeconds(seconds, objectPool);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            gc();

            double copBPS = -1;
            try {
                System.out.print("CompositeObjectPool\t" + ccg + "\t");
                objectPool = ccg.getComposite();
                copBPS = testForNumSeconds(seconds, objectPool);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            System.out.println("CompositeObjectPool/GenericObjectPool = " + (copBPS / gopBPS));

            gc();
        } while (ccg.nextSettings());
    }

    private double runThreadedTest(final ObjectPool pool, final int numThreads, final int seconds) {
        final ThreadGroup threadGroup = new ThreadGroup("Testers");
        final Thread[] threads = new Thread[numThreads];
        final Borrower[] borrowers = new Borrower[threads.length];

        long startTime = System.currentTimeMillis();
        long actualEndTime;
        synchronized (LOCK) {
            for (int i=0; i < threads.length; i++) {
                borrowers[i] = new Borrower(pool);
                threads[i] = new Thread(threadGroup, borrowers[i]);
                threads[i].start();
            }
            while (startTime == System.currentTimeMillis()) ;
            startTime = System.currentTimeMillis();
        }

        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            // ignore
        }
        try {
            pool.close();
        } catch (Exception e) {
            // ignored
        }
        actualEndTime = System.currentTimeMillis();

        Thread.yield();
        int loops = 0;
        for (int i=0; i < threads.length; i++) {
            if (threads[i].isAlive()) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            loops += borrowers[i].getLoops();
        }
        final double borrowsPerSecond = ((double)loops / ((double)(actualEndTime - startTime) / 1000D));
        System.out.println(borrowsPerSecond + " borrows per second");
        return borrowsPerSecond;
    }

    private void compareThreadedCompositeGerneic(final int numThreads, final int seconds) {
        final CompareCompositeGeneric ccg = new CompareCompositeGeneric();
        do {
            gc();

            ObjectPool objectPool;
            gc();

            try {
                System.out.print("GOP:noTestOnBorrow\t" + ccg + "\t");
                objectPool = ccg.getGeneric();
                ((GenericObjectPool)objectPool).setTestOnBorrow(false);
                runThreadedTest(objectPool, numThreads, seconds);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            double gopBPS = -1;
            try {
                System.out.print("GenericObjectPool\t" + ccg + "\t");
                objectPool = ccg.getGeneric();
                ((GenericObjectPool)objectPool).setTestOnBorrow(true);
                gopBPS = runThreadedTest(objectPool, numThreads, seconds);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            gc();

            double copBPS = -1;
            try {
                System.out.print("CompositeObjectPool\t" + ccg + "\t");
                objectPool = ccg.getComposite();
                copBPS = runThreadedTest(objectPool, numThreads, seconds);
            } catch (Exception e) {
                System.out.println("exception thrown! " + e.getMessage());
            }

            System.out.println("CompositeObjectPool/GenericObjectPool = " + (copBPS / gopBPS));

            gc();
        } while (ccg.nextSettings());
    }



    public static void main(final String[] args) throws Exception {
        System.out.println("Testing Class: " + PerformanceTest.class.getName());
        PerformanceTest test = new PerformanceTest();

        if (true) {
            System.out.println("Single Threaded Test");
            System.out.println("Warm up run (15): " + new Date());
            test.compareCompositeGerneic(15);
            //test.runEveryGeneric(20);
            //test.runEveryComposite(20);
            System.out.println("Go go go (60): " + new Date());
            test.compareCompositeGerneic(60);
            //test.runEveryGeneric(60);
            //test.runEveryComposite(60);
            System.out.println("Done: " + new Date());
        }

        if (false) {
            int numThreads = 5;
            System.out.println("Threaded Test: " + numThreads);
            System.out.println("Warm up run (15): " + new Date());
            test.compareThreadedCompositeGerneic(numThreads, 15);
            System.out.println("Go go go (60): " + new Date());
            test.compareThreadedCompositeGerneic(numThreads, 60);
            System.out.println("Done: " + new Date());
        }

        if (false) {
            System.out.println("List performances");
            CompositeObjectPool cop;
            System.out.print("ArrayList:w:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(1), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 1; i++) cop.addObject();
            testForNumSeconds(25, cop);
            gc();

            System.out.print("LinkedList:w:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 1; i++) cop.addObject();
            testForNumSeconds(25, cop);
            gc();

            System.out.print("ArrayList:1:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(1), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 1; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:1:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 1; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:2:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(2), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 2; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:2:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 2; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:3:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(3), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 3; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:3:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 3; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:5:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(5), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 5; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:5:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 5; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:10:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(10), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 10; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:10:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 10; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:25:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(25), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 25; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:25:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 25; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("ArrayList:50:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new ArrayList(50), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 50; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

            System.out.print("LinkedList:50:\t");
            cop = new CompositeObjectPool(new IntegerFactory(), new LinkedList(), new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            for (int i=0; i < 50; i++) cop.addObject();
            testForNumSeconds(60, cop);
            gc();

        }

    }

    private static class CompareCompositeGeneric {
        private static final List maxIdles = Arrays.asList(new Integer[] {new Integer(-1), new Integer(10)});
        private static final List maxActives = Arrays.asList(new Integer[] {new Integer(-1), new Integer(10)});
        private static final List validateOnReturns = Arrays.asList(new Boolean[] {Boolean.FALSE, Boolean.TRUE});

        private Iterator maxIdleIter = maxIdles.iterator();
        private Iterator maxActiveIter = maxActives.iterator();
        private Iterator validateIter = validateOnReturns.iterator();

        private IntegerFactory objectFactory = new IntegerFactory();
        private CompositeObjectPoolFactory compositeFactory = new CompositeObjectPoolFactory(objectFactory);
        private GenericObjectPool.Config genericConfig = new GenericObjectPool.Config();

        private Integer maxIdle;
        private Integer maxActive;
        private Boolean validateOnReturn;

        public CompareCompositeGeneric() {
            maxIdle = (Integer)maxIdleIter.next();
            maxActive = (Integer)maxActiveIter.next();
            validateOnReturn = (Boolean)validateIter.next();

            compositeFactory.setBorrowPolicy(BorrowPolicy.FIFO);
            compositeFactory.setExhaustionPolicy(ExhaustionPolicy.GROW);
            compositeFactory.setLimitPolicy(LimitPolicy.FAIL);
            compositeFactory.setTrackerType(TrackingPolicy.SIMPLE);

            genericConfig.minIdle = 0;
            genericConfig.testOnBorrow = true;
            genericConfig.testOnReturn = false;
            genericConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        }

        public boolean nextSettings() {
            boolean newCombination = true;
            if (!validateIter.hasNext()) {
                validateIter = validateOnReturns.iterator();

                if (!maxActiveIter.hasNext()) {
                    maxActiveIter = maxActives.iterator();

                    if (!maxIdleIter.hasNext()) {
                        maxIdleIter = maxIdles.iterator();
                        newCombination = false;
                    }
                    maxIdle = (Integer)maxIdleIter.next();
                }
                maxActive = (Integer)maxActiveIter.next();
            }
            validateOnReturn = (Boolean)validateIter.next();

            return newCombination;
        }

        public CompositeObjectPool getComposite() {
            objectFactory.reset();
            compositeFactory.setMaxActive(maxActive.intValue());
            compositeFactory.setMaxIdle(maxIdle.intValue());
            compositeFactory.setValidateOnReturn(validateOnReturn.booleanValue());
            return (CompositeObjectPool)compositeFactory.createPool();
        }

        public GenericObjectPool getGeneric() {
            objectFactory.reset();
            genericConfig.maxActive = maxActive.intValue();
            genericConfig.maxIdle = maxIdle.intValue();
            genericConfig.testOnReturn = validateOnReturn.booleanValue();
            return new GenericObjectPool(objectFactory, genericConfig);
        }


        public String toString() {
            return '{' +
                    "maxIdle=" + maxIdle +
                    ", maxActive=" + maxActive +
                    ", validateOnReturn=" + validateOnReturn +
                    '}';
        }
    }
    private static class IntegerFactory extends BasePoolableObjectFactory {
        private int count = 0;
        private boolean oddValid = true;
        private boolean evenValid = true;

        public Object makeObject() throws Exception {
            return new Integer(count++);
        }

        public boolean validateObject(final Object obj) {
            final Integer num = (Integer)obj;
            if (num.intValue() % 2 == 0) {
                return evenValid;
            } else {
                return oddValid;
            }
        }

        public void setValid(final boolean valid) {
            setEvenValid(valid);
            setOddValid(valid);
        }

        public void setOddValid(final boolean oddValid) {
            this.oddValid = oddValid;
        }

        public void setEvenValid(final boolean evenValid) {
            this.evenValid = evenValid;
        }

        public void reset() {
            count = 0;
            oddValid = true;
            evenValid = true;
        }

        public String toString() {
            return "IntegerFactory{}";
        }
    }

    private static class Borrower implements Runnable {
        private final ObjectPool pool;
        private int loops = 0;

        public Borrower(final ObjectPool pool) {
            this.pool = pool;
        }

        public void run() {
            synchronized(LOCK) {
                loops = 0;
            }
            while (true) {
                try {
                    final Object obj = pool.borrowObject();
                    loops++;
                    pool.returnObject(obj);
                } catch (IllegalStateException ise) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public int getLoops() {
            return loops;
        }
    }
}
