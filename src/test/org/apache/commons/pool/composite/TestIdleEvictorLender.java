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
 * Tests for {@link IdleEvictorLender}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestIdleEvictorLender extends TestLender {
    /**
     * Constructs a test case with the given name.
     */
    public TestIdleEvictorLender(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestIdleEvictorLender.class);
    }

    protected Lender createLender() throws Exception {
        return createLender(60L * 1000L);
    }

    private IdleEvictorLender createLender(final long timeout) throws Exception {
        final IdleEvictorLender idleEvictorLender = new IdleEvictorLender(new FifoLender());
        idleEvictorLender.setIdleTimeoutMillis(timeout);
        return idleEvictorLender;
    }

    public void testGetIdleTimeoutMillis() throws Exception {
        final IdleEvictorLender lender = createLender(10);
        assertEquals(10L, lender.getIdleTimeoutMillis());
    }

    public void testIdleEviction() throws Exception {
        IdleEvictorLender lender = createLender(50L);
        CompositeObjectPool cop = createPool(lender);

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        Thread.sleep(100L);
        assertEquals(0, cop.getNumIdle());

        cop.close();

        // Test when IdleEvictorLender delegates to another EvictorLender
        lender = new IdleEvictorLender(new InvalidEvictorLender(new FifoLender()));
        lender.setIdleTimeoutMillis(50L);
        cop = createPool(lender);

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        Thread.sleep(100L);
        lender.size();
        assertEquals(0, cop.getNumIdle());
        cop.close();

        // Test when another EvictorLender delegates to IdleEvictorLender
        lender = new IdleEvictorLender(new FifoLender());
        lender.setIdleTimeoutMillis(50L);
        cop = createPool(new InvalidEvictorLender(lender));

        cop.addObject();
        assertEquals(1, cop.getNumIdle());
        Thread.sleep(100L);
        lender.size();
        assertEquals(0, cop.getNumIdle());
        cop.close();
    }
}
