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

import org.apache.commons.pool2.impl.TestGenericObjectPool.SimpleFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class TestBaseGenericObjectPool {

    BaseGenericObjectPool<String> pool = null;
    SimpleFactory factory = null;

    @Before
    public void setUp() throws Exception {
        factory = new SimpleFactory();
        pool = new GenericObjectPool<>(factory);
    }

    @After
    public void tearDown() throws Exception {
        pool.close();
        pool = null;
        factory = null;
    }

    @Test
    public void testBorrowWaitStatistics() {
        final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
        pool.updateStatsBorrow(p, 10);
        pool.updateStatsBorrow(p, 20);
        pool.updateStatsBorrow(p, 20);
        pool.updateStatsBorrow(p, 30);
        Assert.assertEquals(20, pool.getMeanBorrowWaitTimeMillis(), Double.MIN_VALUE);
        Assert.assertEquals(30, pool.getMaxBorrowWaitTimeMillis(), 0);
    }

    public void testBorrowWaitStatisticsMax() {
        final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
        Assert.assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, 0);
        Assert.assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, 20);
        Assert.assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, 20);
        Assert.assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
        pool.updateStatsBorrow(p, 10);
        Assert.assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
    }

    @Test
    public void testActiveTimeStatistics() {
    	for (int i = 0; i < 99; i++) {  // must be < MEAN_TIMING_STATS_CACHE_SIZE
    		pool.updateStatsReturn(i);
    	}
    	Assert.assertEquals(49, pool.getMeanActiveTimeMillis(), Double.MIN_VALUE);
    }
}
