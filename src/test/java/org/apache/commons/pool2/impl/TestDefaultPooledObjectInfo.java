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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Set;

import org.apache.commons.pool2.impl.TestGenericObjectPool.SimpleFactory;
import org.junit.jupiter.api.Test;

class TestDefaultPooledObjectInfo {

    @Test
    void testGetLastBorrowTrace() throws Exception {
        final AbandonedConfig abandonedConfig = new AbandonedConfig();
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        abandonedConfig.setRemoveAbandonedTimeout(TestConstants.ONE_SECOND_DURATION);
        abandonedConfig.setLogAbandoned(true);
        try (GenericObjectPool<String> pool = new GenericObjectPool<>(new SimpleFactory(), new GenericObjectPoolConfig<>(), abandonedConfig)) {
            pool.borrowObject();
            // pool.returnObject(s1); // Object not returned, causes abandoned object created exception
            final Set<DefaultPooledObjectInfo> strings = pool.listAllObjects();
            final DefaultPooledObjectInfo s1Info = strings.iterator().next();
            final String lastBorrowTrace = s1Info.getLastBorrowTrace();
            assertTrue(lastBorrowTrace.startsWith("Pooled object created"));
        }
    }

    @Test
    void testGetPooledObjectToString() throws Exception {
        try (GenericObjectPool<String> pool = new GenericObjectPool<>(new SimpleFactory())) {
            final String s1 = pool.borrowObject();
            final Set<DefaultPooledObjectInfo> strings = pool.listAllObjects();
            assertEquals(1, strings.size());
            final DefaultPooledObjectInfo s1Info = strings.iterator().next();
            assertEquals(s1, s1Info.getPooledObjectToString());
        }
    }

    @Test
    void testGetPooledObjectType() throws Exception {
        try (GenericObjectPool<String> pool = new GenericObjectPool<>(new SimpleFactory())) {
            pool.borrowObject();
            final Set<DefaultPooledObjectInfo> strings = pool.listAllObjects();
            assertEquals(1, strings.size());
            final DefaultPooledObjectInfo s1Info = strings.iterator().next();
            assertEquals(String.class.getName(), s1Info.getPooledObjectType());
        }
    }

    @Test
    void testTiming() throws Exception {
        try (GenericObjectPool<String> pool = new GenericObjectPool<>(new SimpleFactory())) {
            final long t1Millis = System.currentTimeMillis();
            Thread.sleep(50);
            final String s1 = pool.borrowObject();
            Thread.sleep(50);
            final long t2Millis = System.currentTimeMillis();
            Thread.sleep(50);
            pool.returnObject(s1);
            Thread.sleep(50);
            final long t3Millis = System.currentTimeMillis();
            Thread.sleep(50);
            pool.borrowObject();
            Thread.sleep(50);
            final long t4Millis = System.currentTimeMillis();
            final Set<DefaultPooledObjectInfo> strings = pool.listAllObjects();
            assertEquals(1, strings.size());
            final DefaultPooledObjectInfo s1Info = strings.iterator().next();
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            assertTrue(s1Info.getCreateTime() > t1Millis);
            assertEquals(sdf.format(Long.valueOf(s1Info.getCreateTime())), s1Info.getCreateTimeFormatted());
            assertTrue(s1Info.getCreateTime() < t2Millis);
            assertTrue(s1Info.getLastReturnTime() > t2Millis);
            assertEquals(sdf.format(Long.valueOf(s1Info.getLastReturnTime())), s1Info.getLastReturnTimeFormatted());
            assertTrue(s1Info.getLastReturnTime() < t3Millis);
            assertTrue(s1Info.getLastBorrowTime() > t3Millis);
            assertEquals(sdf.format(Long.valueOf(s1Info.getLastBorrowTime())), s1Info.getLastBorrowTimeFormatted());
            assertTrue(s1Info.getLastBorrowTime() < t4Millis);
        }
    }
}
