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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit tests for {@link FailManager}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestFailManager extends TestManager {
    /**
     * Constructs a test case with the given name.
     */
    public TestFailManager(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestFailManager.class);
    }

    protected Manager createManager() {
        return new FailManager();
    }
}
