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
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;

import java.util.List;
import java.util.ListIterator;

/**
 * Common unit tests for all {@link Lender}s.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestLender extends TestCase {
    private CompositeObjectPoolFactory factory = new CompositeObjectPoolFactory(new MethodCallPoolableObjectFactory());

    /**
     * Constructs a test case with the given name.
     */
    public TestLender(final String name) {
        super(name);
    }

    protected abstract Lender createLender() throws Exception;

    public void testSetCompositeObjectPool() throws Exception {
        final Lender lender = createLender();
        try {
            lender.setCompositeObjectPool(null);
            fail("Expected an IllegalArgumentException when setCompositeObjectPool is passed null.");
        } catch (IllegalArgumentException iae) {
            // swallowed
        }

        lender.setCompositeObjectPool((CompositeObjectPool)factory.createPool());
        try {
            lender.setCompositeObjectPool((CompositeObjectPool)factory.createPool());
            fail("Expected an IllegalStateException when setCompositeObjectPool is passed a pool twice.");
        } catch (IllegalStateException ise) {
            // swallowed
        }
    }

    public void testBorrowAndRepay() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(lender);
        final List list = cop.getPool();

        cop.addObject();

        try {
            synchronized (list) {
                final Object obj = lender.borrow();
                lender.repay(obj);
            }
        } finally {
            cop.close();
        }
    }

    public void testListIterator() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(lender);
        cop.addObject();
        final List list = cop.getPool();
        final ListIterator li = lender.listIterator();

        assertNotNull(li);

        try {
            synchronized (list) {
                if (li.hasNext()) {
                    li.next();
                }
                if (li.hasPrevious()) {
                    li.previous();
                }
            }
        } finally {
            cop.close();
        }
    }

    public void testSize() throws Exception {
        final Lender lender = createLender();
        final CompositeObjectPool cop = createPool(lender);
        lender.size();
    }

    public void testToString() throws Exception {
        createLender().toString();
    }

    protected final CompositeObjectPool createPool(final Lender lender) {
        return createPool(new MethodCallPoolableObjectFactory(), lender);
    }

    protected final CompositeObjectPool createPool(final PoolableObjectFactory pof, final Lender lender) {
        return new CompositeObjectPool(pof, new GrowManager(), lender, new SimpleTracker(), false, null);
    }

    protected final CompositeObjectPool createPool(final List list, final Lender lender) {
        return new CompositeObjectPool(new MethodCallPoolableObjectFactory(), list,
                new GrowManager(), lender, new SimpleTracker(), false, null);
    }
}
