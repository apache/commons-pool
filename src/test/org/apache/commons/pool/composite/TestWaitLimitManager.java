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
import java.util.TimerTask;

/**
 * Tests for {@link WaitLimitManager}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestWaitLimitManager extends TestActiveLimitManager {

    /**
     * Because {@link System#currentTimeMillis()} or {@link Object#wait} aren't perfectly granular,
     * allow a little leeway.
     */
    private static long FUZZ = 5L;

    /**
     * Constructs a test case with the given name.
     */
    public TestWaitLimitManager(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestWaitLimitManager.class);
    }

    protected Manager createManager(final Manager delegate) {
        final WaitLimitManager wlm = new WaitLimitManager(delegate);
        wlm.setMaxActive(Integer.MAX_VALUE);
        wlm.setMaxWaitMillis(15L);
        return wlm;
    }

    public void testMaxWaitMillis() throws Exception {
        WaitLimitManager manager = (WaitLimitManager)createManager();
        manager.setMaxActive(1);
        manager.setMaxWaitMillis(100L);
        assertEquals(100L, manager.getMaxWaitMillis());

        final CompositeObjectPool cop = createPool(manager);

        final Object theOne = cop.borrowObject();

        long startTime = System.currentTimeMillis();
        try {
            cop.borrowObject();
            fail("Should have thrown a NoSuchElementException after 100 milliseconds.");
        } catch (NoSuchElementException nsee) {
            // expected
        }
        long delay = System.currentTimeMillis() - startTime;
        assertTrue("Delay should have been at least 100ms. was: " + delay, 100L - FUZZ < delay);


        final TimerTask returnTheOne = new TimerTask() {
            public void run() {
                cop.returnObject(theOne);
            }
        };
        CompositeObjectPool.COMPOSITE_TIMER.schedule(returnTheOne, 25L);
        startTime = System.currentTimeMillis();
        cop.borrowObject();
        delay = System.currentTimeMillis() - startTime;
        assertTrue("Delay should have been more than 25ms but less than 100ms.", 25L - FUZZ <= delay && delay <= 100L + FUZZ);
        cop.close();
    }

    public void testWaitInterruptedException() throws Exception {
        WaitLimitManager manager = (WaitLimitManager)createManager();
        manager.setMaxActive(1);
        manager.setMaxWaitMillis(-1L); // forever
        final CompositeObjectPool cop = createPool(manager);

        final Object theOne = cop.borrowObject();

        final Thread currentThread = Thread.currentThread();
        final TimerTask interrupter = new TimerTask() {
            public void run() {
                currentThread.interrupt();
            }
        };
        CompositeObjectPool.COMPOSITE_TIMER.schedule(interrupter, 150L);
        long startTime = System.currentTimeMillis();
        try {
            cop.borrowObject();
            fail("Expected an InterruptedException while waiting for an available object.");
        } catch (InterruptedException ie) {
            // expected
        }
        long delay = System.currentTimeMillis() - startTime;
        assertTrue("Delay should have been at least 150ms. was: " + delay, 150L - FUZZ < delay);

        cop.close();
    }
}
