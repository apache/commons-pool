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

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * JUnit test suite for the {@link org.apache.commons.pool.composite}
 * package.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestAll extends TestCase {
    /**
     * Constructs a test case with the given name.
     */
    public TestAll(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite();

        // Lenders unit tests
        suite.addTest(TestFifoLender.suite());
        suite.addTest(TestIdleEvictorLender.suite());
        suite.addTest(TestInvalidEvictorLender.suite());
        suite.addTest(TestLifoLender.suite());
        suite.addTest(TestNullLender.suite());
        suite.addTest(TestSoftLender.suite());

        // Managers unit tests
        suite.addTest(TestFailManager.suite());
        suite.addTest(TestGrowManager.suite());
        suite.addTest(TestIdleLimitManager.suite());
        suite.addTest(TestFailLimitManager.suite());
        suite.addTest(TestWaitLimitManager.suite());

        // Tackers unit tests
        suite.addTest(TestReferenceTracker.suite());
        suite.addTest(TestDebugTracker.suite());
        suite.addTest(TestSimpleTracker.suite());

        // Remaining unit tests
        suite.addTest(TestCompositeObjectPool.suite());
        suite.addTest(TestCompositeKeyedObjectPool.suite());
        suite.addTest(TestCompositeKeyedObjectPool2.suite());
        suite.addTest(TestCompositeObjectPoolFactory.suite());
        suite.addTest(TestCompositeKeyedObjectPoolFactory.suite());
        return suite;
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestAll.class.getName() };
        TestRunner.main(testCaseName);
    }
}
