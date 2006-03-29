/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool.composite;

import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * Tests for all {@link ActiveLimitManager}s.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestActiveLimitManager extends TestDelegateManager {
    /**
     * Constructs a test case with the given name.
     */
    public TestActiveLimitManager(final String name) {
        super(name);
    }

    public void testMaxActive() throws Exception {
        ActiveLimitManager manager = (ActiveLimitManager)createManager();
        manager.setMaxActive(3);
        assertEquals(3, manager.getMaxActive());
        final CompositeObjectPool cop = createPool(manager);

        final List borrowed = new ArrayList(5);
        for (int i=1; i < 5; i++) {
            final int numActive = Math.min(3, i);
            try {
                borrowed.add(cop.borrowObject());
                assertEquals("Exception should have been thrown on the fourth iteration.", numActive, i);
            } catch (NoSuchElementException nsee) {
                // expected on thrird loop
            }
            assertEquals(numActive, cop.getNumActive());
            assertEquals(numActive, borrowed.size());
        }
    }

    public void testNullDelegate() {
        try {
            createManager(null);
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
