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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests for {@link EvictionConfig}.
 */
public class TestEvictionConfig {

    @Test
    public void testConstructor() {
        EvictionConfig config = new EvictionConfig(0, 0, 0);

        assertEquals(Long.MAX_VALUE, config.getIdleEvictTime());
        assertEquals(Long.MAX_VALUE, config.getIdleSoftEvictTime());
        assertEquals(0, config.getMinIdle());

        config = new EvictionConfig(1, 1, 1);

        assertEquals(1, config.getIdleEvictTime());
        assertEquals(1, config.getIdleSoftEvictTime());
        assertEquals(1, config.getMinIdle());
    }

}
