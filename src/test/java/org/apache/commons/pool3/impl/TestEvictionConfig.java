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

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EvictionConfig}.
 */
public class TestEvictionConfig {

    @Test
    public void testConstructor1s() {
        final EvictionConfig config = new EvictionConfig(Duration.ofMillis(1), Duration.ofMillis(1), 1);

        assertEquals(1, config.getIdleEvictDuration().toMillis());
        assertEquals(1, config.getIdleSoftEvictDuration().toMillis());
        assertEquals(1, config.getMinIdle());
    }

    @Test
    public void testConstructorZerosDurations() {
        final EvictionConfig config = new EvictionConfig(Duration.ZERO, Duration.ZERO, 0);

        assertEquals(Long.MAX_VALUE, config.getIdleEvictDuration().toMillis());
        assertEquals(Long.MAX_VALUE, config.getIdleSoftEvictDuration().toMillis());
        assertEquals(0, config.getMinIdle());
    }

}
