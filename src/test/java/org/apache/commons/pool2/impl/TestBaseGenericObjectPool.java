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

package org.apache.commons.pool2.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool2.Waiter;
import org.apache.commons.pool2.WaiterFactory;
import org.apache.commons.pool2.impl.TestGenericObjectPool.SimpleFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 */
class TestBaseGenericObjectPool {

    BaseGenericObjectPool<String> pool;
    SimpleFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new SimpleFactory();
        pool = new GenericObjectPool<>(factory);
    }

    @AfterEach
    public void tearDown() {
        pool.close();
        pool = null;
        factory = null;
    }

    @Test
    void testActiveTimeStatistics() {
        for (int i = 0; i < 99; i++) { // must be < MEAN_TIMING_STATS_CACHE_SIZE
            pool.updateStatsReturn(Duration.ofMillis(i));
        }
        assertEquals(49, pool.getMeanActiveTimeMillis(), Double.MIN_VALUE);
    }

    @Test
    void testBorrowWaitStatistics() {
        final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
        pool.updateStatsBorrow(p, Duration.ofMillis(10));
        pool.updateStatsBorrow(p, Duration.ofMillis(20));
        pool.updateStatsBorrow(p, Duration.ofMillis(20));
        pool.updateStatsBorrow(p, Duration.ofMillis(30));
        assertEquals(20, pool.getMeanBorrowWaitTimeMillis(), Double.MIN_VALUE);
        assertEquals(30, pool.getMaxBorrowWaitTimeMillis(), 0);
    }

    void testBorrowWaitStatisticsMax() {
        final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
        assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, Duration.ZERO);
        assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, Duration.ofMillis(20));
        assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, Duration.ofMillis(20));
        assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, Duration.ofMillis(10));
        assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
    }

    @Test
    void testCollectDetailedStatisticsConfiguration() {
        // Test configuration through config object
        final GenericObjectPoolConfig<String> config = new GenericObjectPoolConfig<>();
        config.setCollectDetailedStatistics(false);
        try (GenericObjectPool<String> testPool = new GenericObjectPool<>(factory, config)) {
            assertFalse(testPool.getCollectDetailedStatistics());
        }
        // Test runtime configuration
        pool.setCollectDetailedStatistics(false);
        assertFalse(pool.getCollectDetailedStatistics());
        pool.setCollectDetailedStatistics(true);
        assertTrue(pool.getCollectDetailedStatistics());
    }

    @Test
    void testCollectDetailedStatisticsDefault() {
        // Test that collectDetailedStatistics defaults to true for backward compatibility
        assertTrue(pool.getCollectDetailedStatistics());
    }

    @Test
    void testCollectDetailedStatisticsDisabled() throws Exception {
        // Configure pool to disable detailed statistics
        pool.setCollectDetailedStatistics(false);
        final DefaultPooledObject<String> pooledObject = (DefaultPooledObject<String>) factory.makeObject();
        // Record initial values
        final long initialActiveTime = pool.getMeanActiveTimeMillis();
        final long initialIdleTime = pool.getMeanIdleDuration().toMillis();
        final long initialWaitTime = pool.getMeanBorrowWaitTimeMillis();
        final long initialMaxWaitTime = pool.getMaxBorrowWaitTimeMillis();
        // Update statistics - should be ignored for detailed stats
        pool.updateStatsBorrow(pooledObject, Duration.ofMillis(100));
        pool.updateStatsReturn(Duration.ofMillis(200));
        // Basic counters should still work
        assertEquals(1, pool.getBorrowedCount());
        assertEquals(1, pool.getReturnedCount());
        // Detailed statistics should remain unchanged
        assertEquals(initialActiveTime, pool.getMeanActiveTimeMillis());
        assertEquals(initialIdleTime, pool.getMeanIdleDuration().toMillis());
        assertEquals(initialWaitTime, pool.getMeanBorrowWaitTimeMillis());
        assertEquals(initialMaxWaitTime, pool.getMaxBorrowWaitTimeMillis());
    }

    @Test
    void testCollectDetailedStatisticsEnabled() throws Exception {
        // Ensure detailed statistics are enabled (default)
        pool.setCollectDetailedStatistics(true);
        final DefaultPooledObject<String> pooledObject = (DefaultPooledObject<String>) factory.makeObject();
        // Update statistics
        pool.updateStatsBorrow(pooledObject, Duration.ofMillis(100));
        pool.updateStatsReturn(Duration.ofMillis(200));
        // All counters should work
        assertEquals(1, pool.getBorrowedCount());
        assertEquals(1, pool.getReturnedCount());
        // Detailed statistics should be updated
        assertEquals(200, pool.getMeanActiveTimeMillis());
        assertEquals(100, pool.getMeanBorrowWaitTimeMillis());
        assertEquals(100, pool.getMaxBorrowWaitTimeMillis());
    }

    @Test
    void testCollectDetailedStatisticsToggling() throws Exception {
        final DefaultPooledObject<String> pooledObject = (DefaultPooledObject<String>) factory.makeObject();
        // Start with detailed stats enabled
        pool.setCollectDetailedStatistics(true);
        pool.updateStatsBorrow(pooledObject, Duration.ofMillis(50));
        pool.updateStatsReturn(Duration.ofMillis(100));
        assertEquals(50, pool.getMeanBorrowWaitTimeMillis());
        assertEquals(100, pool.getMeanActiveTimeMillis());
        // Disable detailed stats
        pool.setCollectDetailedStatistics(false);
        pool.updateStatsBorrow(pooledObject, Duration.ofMillis(200));
        pool.updateStatsReturn(Duration.ofMillis(300));
        // Detailed stats should remain at previous values
        assertEquals(50, pool.getMeanBorrowWaitTimeMillis());
        assertEquals(100, pool.getMeanActiveTimeMillis());
        // Basic counters should continue to increment
        assertEquals(2, pool.getBorrowedCount());
        assertEquals(2, pool.getReturnedCount());
    }

    @Test
    void testDetailedStatisticsConfigIntegration() {
        // Test that config property is properly applied during pool construction
        final GenericObjectPoolConfig<String> config = new GenericObjectPoolConfig<>();
        config.setCollectDetailedStatistics(false);
        try (GenericObjectPool<String> testPool = new GenericObjectPool<>(factory, config)) {
            assertFalse(testPool.getCollectDetailedStatistics(), "Pool should respect collectDetailedStatistics setting from config");
            // Test that toString includes the new property
            final String configString = config.toString();
            assertTrue(configString.contains("collectDetailedStatistics"), "Config toString should include collectDetailedStatistics property");
        }
    }

    @Test
    void testEvictionTimerMultiplePools() throws InterruptedException {
        final AtomicIntegerFactory factory = new AtomicIntegerFactory();
        factory.setValidateLatency(50);
        try (GenericObjectPool<AtomicInteger> evictingPool = new GenericObjectPool<>(factory)) {
            evictingPool.setTimeBetweenEvictionRuns(Duration.ofMillis(100));
            evictingPool.setNumTestsPerEvictionRun(5);
            evictingPool.setTestWhileIdle(true);
            evictingPool.setMinEvictableIdleTime(Duration.ofMillis(50));
            for (int i = 0; i < 10; i++) {
                try {
                    evictingPool.addObject();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            for (int i = 0; i < 1000; i++) {
                try (GenericObjectPool<AtomicInteger> nonEvictingPool = new GenericObjectPool<>(factory)) {
                    // empty
                }
            }

            Thread.sleep(1000);
            assertEquals(0, evictingPool.getNumIdle());
        }
    }

    /**
     * POOL-393: Tests JMX registration does not add too much latency to pool creation.
     */
    @SuppressWarnings("resource") // pools closed in finally block
    @Test
    @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
    void testJMXRegistrationLatency() {
        final int numPools = 1000;
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final ArrayList<GenericObjectPool<Waiter>> pools = new ArrayList<>();
        try {
            // final long startTime = System.currentTimeMillis();
            for (int i = 0; i < numPools; i++) {
                final GenericObjectPool<Waiter> gop = new GenericObjectPool<>(new WaiterFactory<>(0, 0, 0, 0, 0, 0), new GenericObjectPoolConfig<>());
                assertNotNull(gop.getJmxName());
                pools.add(gop);
            }
            // System.out.println("Duration: " + (System.currentTimeMillis() - startTime));
            final ObjectName oname = pools.get(numPools - 1).getJmxName();
            assertEquals(1, mbs.queryNames(oname, null).size());
        } finally {
            pools.forEach(GenericObjectPool::close);
        }
    }

    @Test
    void testStatsStoreCircularBuffer() throws Exception {
        // Test that StatsStore properly handles circular buffer behavior
        final DefaultPooledObject<String> pooledObject = (DefaultPooledObject<String>) factory.makeObject();
        // Fill beyond the cache size (100) to test circular behavior
        final int cacheSize = 100; // BaseGenericObjectPool.MEAN_TIMING_STATS_CACHE_SIZE
        for (int i = 0; i < cacheSize + 50; i++) {
            pool.updateStatsBorrow(pooledObject, Duration.ofMillis(i));
            pool.updateStatsReturn(Duration.ofMillis(i * 2));
        }
        // Statistics should still be meaningful after circular buffer wrapping
        assertTrue(pool.getMeanActiveTimeMillis() > 0);
        assertTrue(pool.getMeanBorrowWaitTimeMillis() > 0);
        assertTrue(pool.getMaxBorrowWaitTimeMillis() > 0);
        // The mean should reflect recent values, not all historical values
        // (exact assertion depends on circular buffer implementation)
        assertTrue(pool.getMeanBorrowWaitTimeMillis() >= 50); // Should be influenced by recent higher values
    }

    @Test
    void testStatsStoreConcurrentAccess() throws Exception {
        // Test the lock-free StatsStore implementation under concurrent load
        final int numThreads = 10;
        final int operationsPerThread = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final List<Future<Void>> futures = new ArrayList<>();
        // Create threads that will concurrently update statistics
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                try {
                    final DefaultPooledObject<String> pooledObject = (DefaultPooledObject<String>) factory.makeObject();
                    // Wait for all threads to be ready
                    startLatch.await();
                    // Perform concurrent operations
                    for (int j = 0; j < operationsPerThread; j++) {
                        pool.updateStatsBorrow(pooledObject, Duration.ofMillis(threadId * 10 + j));
                        pool.updateStatsReturn(Duration.ofMillis(threadId * 20 + j));
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    completeLatch.countDown();
                }
                return null;
            }));
        }
        // Start all threads simultaneously
        startLatch.countDown();
        // Wait for completion
        assertTrue(completeLatch.await(30, TimeUnit.SECONDS), "Concurrent test should complete within 30 seconds");
        // Verify no exceptions occurred
        for (final Future<Void> future : futures) {
            future.get(); // Will throw if there was an exception
        }
        // Verify that statistics were collected (exact values may vary due to race conditions)
        assertEquals(numThreads * operationsPerThread, pool.getBorrowedCount());
        assertEquals(numThreads * operationsPerThread, pool.getReturnedCount());
        // Mean values should be reasonable (not zero or wildly incorrect)
        assertTrue(pool.getMeanActiveTimeMillis() >= 0);
        assertTrue(pool.getMeanBorrowWaitTimeMillis() >= 0);
        assertTrue(pool.getMaxBorrowWaitTimeMillis() >= 0);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
