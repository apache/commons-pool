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

package org.apache.commons.pool3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PooledObject}.
 */
public class PooledObjectTest {

    @Test
    void testGetObject() {
        assertNull(PooledObject.getObject(null));
        assertNull(PooledObject.getObject(new DefaultPooledObject<>(null)));
        assertNotNull(PooledObject.getObject(new DefaultPooledObject<>("a")));
    }

    @Test
    void testIsNull() {
        assertTrue(PooledObject.isNull(null));
        assertTrue(PooledObject.isNull(new DefaultPooledObject<>(null)));
        assertFalse(PooledObject.isNull(new DefaultPooledObject<>("a")));
    }

    @Test
    void testNonNull() {
        assertFalse(PooledObject.nonNull(null));
        assertFalse(PooledObject.nonNull(new DefaultPooledObject<>(null)));
        assertTrue(PooledObject.nonNull(new DefaultPooledObject<>("a")));
    }
}
