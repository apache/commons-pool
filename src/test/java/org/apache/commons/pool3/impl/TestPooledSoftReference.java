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
package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.ref.SoftReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PooledSoftReference.
 */
class TestPooledSoftReference {

    private static final String REFERENT = "test";
    private static final String REFERENT2 = "test2";
    PooledSoftReference<String> ref;

    @BeforeEach
    public void setUp() {
        final SoftReference<String> softRef = new SoftReference<>(REFERENT);
        ref = new PooledSoftReference<>(softRef);
    }

    @Test
    void testPooledSoftReference() {
        assertEquals(REFERENT, ref.getObject());

        SoftReference<String> softRef = ref.getReference();
        assertEquals(REFERENT, softRef.get());
        softRef.clear();

        softRef = new SoftReference<>(REFERENT2);
        ref.setReference(softRef);

        assertEquals(REFERENT2, ref.getObject());

        softRef = ref.getReference();
        assertEquals(REFERENT2, softRef.get());
        softRef.clear();
    }

    @Test
    void testToString() {
        final String expected = "Referenced Object: test, State: IDLE";
        assertEquals(expected, ref.toString());
    }

}
