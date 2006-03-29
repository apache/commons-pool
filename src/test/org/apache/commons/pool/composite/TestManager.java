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
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.PrivateException;
import org.apache.commons.pool.MethodCall;

import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;

/**
 * Unit tests common to all {@link Manager}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestManager extends TestCase {
    private CompositeObjectPoolFactory factory = new CompositeObjectPoolFactory(new MethodCallPoolableObjectFactory());
    private static final Integer ZERO = new Integer(0);
    private static final Integer ONE = new Integer(1);

    /**
     * Constructs a test case with the given name.
     */
    public TestManager(String name) {
        super(name);
    }

    protected abstract Manager createManager();

    public void testSetCompositeObjectPool() throws Exception {
        final Manager manager = createManager();
        try {
            manager.setCompositeObjectPool(null);
            fail("Expected an IllegalArgumentException when setCompositeObjectPool is passed null.");
        } catch (IllegalArgumentException iae) {
            // swallowed
        }

        manager.setCompositeObjectPool((CompositeObjectPool)factory.createPool());
        try {
            manager.setCompositeObjectPool((CompositeObjectPool)factory.createPool());
            fail("Expected an IllegalStateException when setCompositeObjectPool is passed a pool twice.");
        } catch (IllegalStateException ise) {
            // swallowed
        }
    }

    public void testNextFromPoolAndReturnToPool() throws Exception {
        final Manager manager = createManager();
        final CompositeObjectPool cop = createPool(manager);
        final MethodCallPoolableObjectFactory mcpof = (MethodCallPoolableObjectFactory)cop.getFactory();
        Object o;

        // pool empty
        try {
            synchronized (cop.getPool()) {
                o = manager.nextFromPool();
                assertNotNull(o);
                manager.returnToPool(o);
            }
        } catch (NoSuchElementException nsee) {
            // possible if the pool doesn't grow
        }

        cop.clear();

        // pool contains an object
        cop.addObject();
        synchronized (cop.getPool()) {
            o = manager.nextFromPool();
            assertNotNull(o);
            manager.returnToPool(o);
        }

        cop.clear();

        // pool contains an object but it fails validation
        cop.addObject();
        mcpof.setValid(false);
        try {
            synchronized (cop.getPool()) {
                o = manager.nextFromPool();
                assertNotNull(o);
                manager.returnToPool(o);
            }
        } catch (NoSuchElementException nsee) {
            // possible if the pool doesn't grow
        }
    }

    public void testNextFromPoolExceptions() throws Exception {
        final Manager manager = createManager();
        final CompositeObjectPool cop = createPool(manager);
        final MethodCallPoolableObjectFactory mcpof = (MethodCallPoolableObjectFactory)cop.getFactory();
        final List expectedMethods = new ArrayList();

        // Test what happens when activateObject throws an exception
        cop.addObject();
        mcpof.setActivateObjectFail(true);
        mcpof.getMethodCalls().clear();
        try {
            synchronized (cop.getPool()) {
                Object o = manager.nextFromPool();
                assertNotNull(o);
                expectedMethods.add(new MethodCall("activateObject", ZERO));
                expectedMethods.add(new MethodCall("makeObject").returned(ONE));
                expectedMethods.add(new MethodCall("destroyObject", ZERO));
                Thread.sleep(100L); // wait for deferred destroyObjects
                assertEquals(expectedMethods, mcpof.getMethodCalls());
                manager.returnToPool(o);
            }
        } catch (NoSuchElementException nsee) {
            // possible if the pool doesn't grow
        }

        // Test what happens when validateObject throws an exception
        cop.clear();
        Thread.sleep(100L); // wait for deferred destroyObjects
        mcpof.reset();
        cop.addObject();
        mcpof.setValidateObjectFail(true);
        expectedMethods.clear();
        mcpof.getMethodCalls().clear();
        try {
            synchronized (cop.getPool()) {
                Object o = manager.nextFromPool();
                assertNotNull(o);
                expectedMethods.add(new MethodCall("activateObject", ZERO));
                expectedMethods.add(new MethodCall("validateObject", ZERO));
                expectedMethods.add(new MethodCall("makeObject").returned(ONE));
                expectedMethods.add(new MethodCall("destroyObject", ZERO));
                Thread.sleep(100L); // wait for deferred destroyObjects
                assertEquals(expectedMethods, mcpof.getMethodCalls());
                manager.returnToPool(o);
            }
        } catch (NoSuchElementException nsee) {
            // possible if the pool doesn't grow
        }
    }

    public void testToString() {
        createManager().toString();
    }

    protected final CompositeObjectPool createPool(final Manager manager) {
        return createPool(new MethodCallPoolableObjectFactory(), manager);
    }

    protected final CompositeObjectPool createPool(final PoolableObjectFactory pof, final Manager manager) {
        return new CompositeObjectPool(pof, manager, new FifoLender(), new SimpleTracker(), false, null);
    }
}
