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
package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;
import org.junit.jupiter.api.Test;

public class TestResilientPooledObjectFactory {
    @Test
    public void testTransientFailure() throws Exception {
        final FailingFactory ff = new FailingFactory();
        // Make the factory fail with exception immediately on make
        ff.setHang(false);
        ff.setSilentFail(false);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(100), Duration.ofMinutes(10), Duration.ofMillis(100));
        // Create a pool with a max size of 2, using the resilient factory
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        // Tell the factory what pool it is attached to (a little awkward)
        rf.setPool(pool);
        rf.startMonitor(Duration.ofMillis(20)); // 20ms monitor interval
        // Base factory is up
        assertTrue(rf.isUp());
        // Check out a couple of objects
        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        // Start a borrower that will wait
        new Thread() {
            @Override
            public void run() {
                try {
                    pool.borrowObject();
                } catch (final Exception e) {
                }
            }
        }.start();
        // Wait for the borrower to join wait queue
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }

        // Crash the base factory
        ff.crash();
        // Resilient factory does not know the base factory is down until a make is
        // attempted
        assertTrue(rf.isUp());
        assertEquals(1, pool.getNumWaiters());
        pool.returnObject(s1);
        pool.returnObject(s2);
        assertEquals(0, pool.getNumIdle());
        assertEquals(1, pool.getNumWaiters());
        // Wait for the monitor to pick up the failed create which should happen on
        // validation destroy
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
        }
        assertFalse(rf.isUp());
        // Adder should be running, but failing
        assertTrue(rf.isAdderRunning());

        // Pool should have one take waiter
        assertEquals(1, pool.getNumWaiters());

        // Restart the factory
        ff.recover();
        // Wait for the adder to succeed
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
        }
        // Pool should have no waiters
        assertTrue(pool.getNumWaiters() == 0);
        // Add 5 objects to clear the rf log
        // First have to expand the pool
        pool.setMaxTotal(10);
        for (int i = 0; i < 5; i++) {
            pool.addObject();
        }
        // Wait for monitor to run
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        // rf should be up now
        assertTrue(rf.isUp());

        // Adder should be stopped
        assertFalse(rf.isAdderRunning());

        pool.close();
        rf.stopMonitor();
    }

    @Test
    public void testNulls() throws Exception {
        final FailingFactory ff = new FailingFactory();
        // Make the factory fail with exception immediately on make
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(50), Duration.ofMinutes(10), Duration.ofMillis(50));
        // Create a pool with a max size of 2, using the resilient factory
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        // Tell the factory what pool it is attached to (a little awkward)
        rf.setPool(pool);
        rf.startMonitor(Duration.ofMillis(20)); // 20ms monitor interval

        // Exhaust the pool
        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        ff.crash();
        // Start two borrowers that will block waiting
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                    pool.returnObject(s);
                } catch (final Exception e) {
                }
            }
        }.start();
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                    pool.returnObject(s);
                } catch (final Exception e) {
                }
            }
        }.start();
        // Return borrowed objects - validation will fail
        // Wait for the borrowers to get in the queue
        try {
            Thread.sleep(50);
        } catch (final InterruptedException e) {
        }
        pool.returnObject(s1);
        pool.returnObject(s2);
        assertEquals(0, pool.getNumIdle());
        assertTrue(pool.getNumWaiters() > 0);
        // Wait for the monitor to pick up the failed create which should happen on
        // validation destroy
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        assertFalse(rf.isUp());
        // Restart the factory
        ff.recover();
        // Wait for the adder to succeed
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        // Pool should have no waiters
        assertEquals(0, pool.getNumWaiters());
        pool.close();
        // Wait for monitor to run
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        // Monitor and adder should be stopped by pool close
        assertFalse(rf.isAdderRunning());
        assertFalse(rf.isMonitorRunning());
    }

    @Test
    public void testAdderStartStop() throws Exception {
        final FailingFactory ff = new FailingFactory();
        // Make the factory fail with exception immediately on make
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(200), Duration.ofMinutes(10), Duration.ofMillis(20));
        // Create a pool with a max size of 2, using the resilient factory
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        pool.setMaxTotal(2);
        pool.setBlockWhenExhausted(true);
        pool.setTestOnReturn(true);
        rf.setPool(pool);
        rf.startMonitor();
        // Exhasut the pool
        final String s1 = pool.borrowObject();
        final String s2 = pool.borrowObject();
        // Start a borrower that will block waiting
        new Thread() {
            @Override
            public void run() {
                try {
                    final String s = pool.borrowObject();
                } catch (final Exception e) {
                }
            }
        }.start();
        // Wait for the borrower to get in the queue
        try {
            Thread.sleep(50);
        } catch (final InterruptedException e) {
        }
        // Crash the base factory
        ff.crash();
        // Return object will create capacity in the pool
        try {
            pool.returnObject(s1);
        } catch (final Exception e) {
        }
        // Wait for the adder to run
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
        }
        // Adder should be running
        assertTrue(rf.isAdderRunning());
        // Restart the factory
        ff.recover();
        // Wait for the adder to succeed
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        // Pool should have no waiters
        assertEquals(0, pool.getNumWaiters());
        // Adder should still be running because there is a failure in the log
        assertTrue(rf.isAdderRunning());
        // Expand the pool - don't try this at home
        pool.setMaxTotal(10);
        // Run enough adds to homogenize the log
        for (int i = 0; i < 6; i++) {
            pool.addObject();
        }
        // Wait for the monitor to run
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        assertTrue(rf.isUp());
        // Adder should be stopped
        assertFalse(rf.isAdderRunning());
    }

    @Test
    public void testIsMonitorRunning() throws Exception {
        final FailingFactory ff = new FailingFactory();
        // Make the factory fail with exception immediately on make
        ff.setSilentFail(true);
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff,
                5, Duration.ofMillis(200), Duration.ofMinutes(10), Duration.ofMillis(20));
        final GenericObjectPool<String, Exception> pool = new GenericObjectPool<>(rf);
        rf.setPool(pool);
        rf.startMonitor();
        assertTrue(rf.isMonitorRunning());
        rf.stopMonitor();
        assertFalse(rf.isMonitorRunning());
        rf.startMonitor();
        pool.close();
        // Wait for monitor to run so it can kill itself
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
        }
        assertFalse(rf.isMonitorRunning());
    }

    @Test
    public void testConstructorWithDefaults() {
        final FailingFactory ff = new FailingFactory();
        final ResilientPooledObjectFactory<String, Exception> rf = new ResilientPooledObjectFactory<>(ff);
        assertFalse(rf.isMonitorRunning());
        assertFalse(rf.isAdderRunning());
        assertEquals(ResilientPooledObjectFactory.getDefaultLogSize(), rf.getLogSize());
        assertEquals(ResilientPooledObjectFactory.getDefaultTimeBetweenChecks(), rf.getTimeBetweenChecks());
        assertEquals(ResilientPooledObjectFactory.getDefaultDelay(), rf.getDelay());
        assertEquals(ResilientPooledObjectFactory.getDefaultLookBack(), rf.getLookBack());
        assertEquals(0, rf.getMakeObjectLog().size());
        rf.setLogSize(5);
        assertEquals(5, rf.getLogSize());
        rf.setTimeBetweenChecks(Duration.ofMillis(200));
    }

    /**
     * Factory that suffers outages and fails in configurable ways when it is down.
     */
    class FailingFactory implements PooledObjectFactory<String, Exception> {

        /** Whether or not the factory is up */
        private boolean up = true;

        /** Whether or not to fail silently */
        private boolean silentFail = true;

        /** Whether or not to hang */
        private boolean hang;

        public void crash() {
            this.up = false;
        }

        public void recover() {
            this.up = true;
        }

        public void setSilentFail(final boolean silentFail) {
            this.silentFail = silentFail;
        }

        public void setHang(final boolean hang) {
            this.hang = hang;
        }

        @Override
        public PooledObject<String> makeObject() throws Exception {
            if (up) {
                return new DefaultPooledObject<>(UUID.randomUUID().toString());
            }
            if (!silentFail) {
                throw new Exception("makeObject failed");
            }
            if (hang) {
                while (!up) {
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }
                }
            }
            return null;
        }

        @Override
        public void destroyObject(final PooledObject<String> p) throws Exception {
        }

        @Override
        public boolean validateObject(final PooledObject<String> p) {
            return up;
        }

        @Override
        public void activateObject(final PooledObject<String> p) throws Exception {
        }

        @Override
        public void passivateObject(final PooledObject<String> p) throws Exception {
        }
    }
}
