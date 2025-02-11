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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;

import org.apache.commons.pool2.PooledObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GenericObjectPoolConfig}.
 */
public class TestGenericObjectPoolConfig {

    public final static class TestEvictionPolicy implements EvictionPolicy<TestObject> {

        @Override
        public boolean evict(final EvictionConfig config, final PooledObject<TestObject> underTest, final int idleCount) {
            return false;
        }

    }

    public final static class TestObject {
        // empty
    }

    private GenericObjectPoolConfig<TestObject> config;

    @BeforeEach
    void beforeEach() {
        config = new GenericObjectPoolConfig<>();
    }

    @Test
    void testSetEvictionPolicy() {
        final TestEvictionPolicy evictionPolicy = new TestEvictionPolicy();
        config.setEvictionPolicy(evictionPolicy);
        assertEquals(evictionPolicy, config.getEvictionPolicy());
    }

    @Test
    void testSetEvictionPolicyClassName() {
        config.setEvictionPolicyClassName(TestEvictionPolicy.class.getName());
        assertEquals(TestEvictionPolicy.class.getName(), config.getEvictionPolicyClassName());
    }

    @Test
    void testSetEvictorShutdownTimeoutDuration() {
        config.setEvictorShutdownTimeout(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), config.getEvictorShutdownTimeoutDuration());
    }

    @Test
    void testSetEvictorShutdownTimeoutMillis() {
        config.setEvictorShutdownTimeoutMillis(10_0000);
        assertEquals(10_0000, config.getEvictorShutdownTimeoutMillis());
    }

    @Test
    void testSetJmxNamePrefix() {
        config.setJmxNamePrefix("prefix");
        assertEquals("prefix", config.getJmxNamePrefix());
    }

    @Test
    void testSetMaxWaitMillis() {
        config.setMaxWaitMillis(10_0000);
        assertEquals(10_0000, config.getMaxWaitMillis());
    }

    @Test
    void testSetSoftMinEvictableIdleDuration() {
        config.setSoftMinEvictableIdleDuration(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), config.getSoftMinEvictableIdleDuration());
    }

    @Test
    void testSetSoftMinEvictableIdleTime() {
        config.setSoftMinEvictableIdleTime(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), config.getSoftMinEvictableIdleTime());
    }

    @Test
    void testSetSoftMinEvictableIdleTimeMillis() {
        config.setSoftMinEvictableIdleTimeMillis(10_0000);
        assertEquals(10_0000, config.getSoftMinEvictableIdleTimeMillis());
    }

    @Test
    void testToString() {
        assertFalse(config.toString().isEmpty());
    }

    @Test
    void testToStringAppendFields() {
        final StringBuilder builder = new StringBuilder();
        config.toStringAppendFields(builder);
        assertFalse(builder.toString().isEmpty());
    }

}
