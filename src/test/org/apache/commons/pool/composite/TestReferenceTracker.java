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

import junit.framework.Test;
import junit.framework.TestSuite;

import java.util.List;
import java.util.LinkedList;

/**
 * Unit tests for {@link ReferenceTracker}s.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestReferenceTracker extends TestTracker {
    /**
     * Constructs a test case with the given name.
     */
    public TestReferenceTracker(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestReferenceTracker.class);
    }

    protected Tracker createTracker() {
        return new ReferenceTracker();
    }

    public void testBorrowed() {
        super.testBorrowed();

        final Tracker tracker = createTracker();
        try {
            tracker.borrowed(null);
            fail("Tracking of null is now allowed.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testLostBorrowed() {
        final Tracker tracker = createTracker();

        Object obj = new String("This stack trace to stderr is expected by the unit tests.");
        tracker.borrowed(obj);
        assertEquals(1, tracker.getBorrowed());
        obj = null;

        final List garbage = new LinkedList();
        final Runtime runtime = Runtime.getRuntime();
        while (tracker.getBorrowed() > 0) {
            try {
                garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory()/2)]);
            } catch (OutOfMemoryError oome) {
                System.gc();
            }
            System.gc();
        }
        garbage.clear();
        System.gc();

        assertEquals(0, tracker.getBorrowed());
    }

    public void testReturned() {
        super.testReturned();

        final Tracker tracker = createTracker();
        try {
            tracker.returned(null);
            fail("Tracking of null is now allowed.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            tracker.returned(new Object());
            fail("Cannot return an object that wasn't borrowed.");
        } catch (IllegalStateException ise) {
            // expected
        }


    }
}
