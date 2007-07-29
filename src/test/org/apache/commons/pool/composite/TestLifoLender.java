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

import org.apache.commons.pool.PoolUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test that {@link LifoLender} is infact a LIFO.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestLifoLender extends TestLender {
    private static final Integer ZERO = new Integer(0);
    private static final Integer ONE = new Integer(1);

    /**
     * Constructs a test case with the given name.
     */
    public TestLifoLender(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestLifoLender.class);
    }

    protected Lender createLender() {
        return new LifoLender();
    }

    public void testBorrowAndRepay() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(lender);
        final List list = cop.getPool();

        PoolUtils.prefill(cop, 2);

        try {
            synchronized (list) {
                borrowAndRepay(lender);
            }
        } finally {
            cop.close();
        }
    }

    public void testBorrowWithArrayList() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(new ArrayList(), lender);
        final List list = cop.getPool();

        assertTrue(list instanceof ArrayList);

        PoolUtils.prefill(cop, 2);

        try {
            synchronized (list) {
                borrowAndRepay(lender);
            }
        } finally {
            cop.close();
        }
    }

    public void testBorrowWithLinkedList() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(new LinkedList(), lender);
        final List list = cop.getPool();

        assertTrue(list instanceof LinkedList);

        PoolUtils.prefill(cop, 2);

        try {
            synchronized (list) {
                borrowAndRepay(lender);
            }
        } finally {
            cop.close();
        }
    }

    private static void borrowAndRepay(final Lender lender) {
        Object obj = lender.borrow();
        assertEquals(ONE, obj);
        lender.repay(obj);

        obj = lender.borrow();
        assertEquals(ONE, obj);
        lender.repay(obj);

        obj = lender.borrow();
        assertEquals(ONE, obj);
        Object obj2 = lender.borrow();
        assertEquals(ZERO, obj2);
        lender.repay(obj);
        lender.repay(obj2);

        obj = lender.borrow();
        assertEquals(ZERO, obj);
        lender.repay(obj);
    }
}
