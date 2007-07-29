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

package org.apache.commons.pool.composite;

import junit.framework.TestCase;

/**
 * Common unit tests for all {@link Tracker}s.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestTracker extends TestCase {
    /**
     * Constructs a test case with the given name.
     */
    public TestTracker(final String name) {
        super(name);
    }

    protected abstract Tracker createTracker();

    public void testBorrowed() {
        final Tracker tracker = createTracker();

        tracker.borrowed(new Object());
    }

    public void testReturned() {
        final Tracker tracker = createTracker();

        final Object o = new Object();
        tracker.borrowed(o);
        tracker.returned(o);
    }

    public void testGetBorrowed() {
        final Tracker tracker = createTracker();
        if (tracker.getBorrowed() >= 0) {
            final Object obj = new Object();
            assertEquals(0, tracker.getBorrowed());
            tracker.borrowed(obj);
            assertEquals(1, tracker.getBorrowed());
            tracker.returned(obj);
            assertEquals(0, tracker.getBorrowed());
        }
    }

    public void testToString() {
        createTracker().toString();
    }
}
