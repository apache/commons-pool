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

package org.apache.commons.pool3.pool407;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.apache.commons.pool3.PooledObject;

/**
 * Tests POOL-407.
 */
public class AbstractPool407Test {

    protected <T> void assertShutdown(final boolean termination, final Duration poolConfigMaxWait, final T obj, final PooledObject<T> pooledObject) {
        if (pooledObject != null) {
            // The factory makes non-null objects and non-null PooledObjects,
            // therefore the ExecutorService should terminate when requested, without delay.
            assertTrue(termination);
        } else {
            // The factory makes null objects or null PooledObjects,
            // therefore the ExecutorService should keep trying to create objects as configured in the pool's config object.
            if (poolConfigMaxWait.equals(Pool407Constants.WAIT_FOREVER)) {
                // If poolConfigMaxWait is maxed out, then the ExecutorService will not shutdown without delay.
                if (obj == null) {
                    // create() returned null, so wrap() was not even called, and borrowObject() fails fast.
                    assertTrue(termination);
                } else {
                    // The ExecutorService fails to terminate when requested because
                    assertFalse(true);
                }
            } else {
                // If poolConfigMaxWait is short, then the ExecutorService should usually shutdown without delay.
                assertTrue(termination);
            }
        }
    }

}
