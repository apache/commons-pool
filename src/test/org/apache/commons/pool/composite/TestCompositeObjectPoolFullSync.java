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

import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;

/**
 * Tests for {@link CompositeObjectPoolFullSync}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestCompositeObjectPoolFullSync extends TestCompositeObjectPool {
    public TestCompositeObjectPoolFullSync(final String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeObjectPoolFullSync.class);
    }

    protected ObjectPool makeEmptyPool(final PoolableObjectFactory factory) {
        return new CompositeObjectPoolFullSync(factory, new GrowManager(), new FifoLender(), new SimpleTracker(), false);
    }

    public void testConstructors() {
        try {
            new CompositeObjectPoolFullSync(null, new GrowManager(), new FifoLender(), new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null PoolableObjectFactory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPoolFullSync(new MethodCallPoolableObjectFactory(), null, new FifoLender(), new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null Manager.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPoolFullSync(new MethodCallPoolableObjectFactory(), new GrowManager(), null, new SimpleTracker(), false);
            fail("IllegalArgumentException expected on null Lender.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPoolFullSync(new MethodCallPoolableObjectFactory(), new GrowManager(), new FifoLender(), null, false);
            fail("IllegalArgumentException expected on null Tracker.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeObjectPoolFullSync(new MethodCallPoolableObjectFactory(), null, new GrowManager(), new FifoLender(), new SimpleTracker(), false, null);
            fail("IllegalArgumentException expected on null List.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    /**
     * Make sure that {@link WaitLimitManager} both times out and returns an object once one is available.
     */
    public void testWaitLimitManager() throws Exception {
        super.testWaitLimitManager();
        final WaitLimitManager manager = new WaitLimitManager(new GrowManager());
        manager.setMaxActive(1);
        manager.setMaxWaitMillis(100);
        final CompositeObjectPool pool = new CompositeObjectPoolFullSync(new IntegerFactory(), manager, new FifoLender(), new DebugTracker(), false);

        assertEquals(0, pool.getNumActive());

        final Integer zero = (Integer)pool.borrowObject();
        assertEquals(1, pool.getNumActive());

        // Test that the max wait
        try {
            pool.borrowObject();
            fail("Should have thrown a NoSuchElementException");
        } catch(NoSuchElementException nsee) {
            // expected
        }

        // test that if an object is returned while waiting it works.
        // What happens is:
        // this thread locks pool.pool and starts Thread t.
        // this thread will get wait for an object to become available and relase the lock on pool.pool
        // Thread t will then be able to lock on pool.pool and return an object
        // this thread will then be able to borrow an object and should reutrn.
        final List actualOrder = new ArrayList();
        final Runnable r = new Runnable() {
            public void run() {
                try {
                    synchronized(pool.getPool()) {
                        pool.returnObject(zero);
                        actualOrder.add("returned");
                    }
                } catch (Exception e) {
                    waitFailed = true;
                }
            }
        };
        final Thread t = new Thread(r);

        synchronized (pool.getPool()) {
            t.start();

            actualOrder.add("waiting");
            assertEquals(zero, pool.borrowObject());
            actualOrder.add("borrowed");
        }

        assertEquals("Wait failed", false, waitFailed);

        List expectedOrder = new ArrayList();
        expectedOrder.add("waiting");
        expectedOrder.add("returned");
        expectedOrder.add("borrowed");

        assertEquals(expectedOrder, actualOrder);
    }
    private boolean waitFailed = false;

}
