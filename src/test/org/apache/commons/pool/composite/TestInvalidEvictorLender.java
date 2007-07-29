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
import org.apache.commons.pool.MethodCallPoolableObjectFactory;

/**
 * Tests for {@link InvalidEvictorLender}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestInvalidEvictorLender extends TestLender {
    /**
     * Constructs a test case with the given name.
     */
    public TestInvalidEvictorLender(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestInvalidEvictorLender.class);
    }

    protected Lender createLender() throws Exception {
        return createLender(60L * 1000L);
    }

    protected InvalidEvictorLender createLender(final long timeout) throws Exception {
        final InvalidEvictorLender invalidEvictorLender = new InvalidEvictorLender(new FifoLender());
        invalidEvictorLender.setValidationFrequencyMillis(timeout);
        return invalidEvictorLender;
    }

    public void testValidationFrequencyMillis() throws Exception {
        final InvalidEvictorLender lender = createLender(10L);
        assertEquals(10L, lender.getValidationFrequencyMillis());
        lender.setValidationFrequencyMillis(25L);
        assertEquals(25L, lender.getValidationFrequencyMillis());
        try {
            lender.setValidationFrequencyMillis(-1L);
            fail("Negative values are not legal for validationFrequencyMillis.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testInvalidEviction() throws Exception {
        InvalidEvictorLender lender = createLender(50L);
        final MethodCallPoolableObjectFactory mcpof = new MethodCallPoolableObjectFactory();
        CompositeObjectPool cop = createPool(mcpof, lender);

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        mcpof.setValid(false);
        Thread.sleep(100L);
        assertEquals(0, cop.getNumIdle());
        cop.close();

        mcpof.setValid(true);

        // Test when InvalidEvictorLender delegates to another EvictorLender
        lender = new InvalidEvictorLender(new IdleEvictorLender(new FifoLender()));
        lender.setValidationFrequencyMillis(50L);
        cop = createPool(mcpof, lender);

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        mcpof.setValid(false);
        Thread.sleep(100L);
        assertEquals(0, cop.getNumIdle());
        cop.close();

        mcpof.setValid(true);

        // Test when another EvictorLender delegates to InvalidEvictorLender
        lender = new InvalidEvictorLender(new FifoLender());
        lender.setValidationFrequencyMillis(50L);
        cop = createPool(mcpof, new IdleEvictorLender(lender));

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        mcpof.setValid(false);
        Thread.sleep(100L);
        assertEquals(0, cop.getNumIdle());
        cop.close();
    }
}
