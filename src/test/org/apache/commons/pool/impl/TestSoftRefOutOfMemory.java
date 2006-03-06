/*
 * Copyright 1999-2006 The Apache Software Foundation.
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

package org.apache.commons.pool.impl;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool.PoolableObjectFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestSoftRefOutOfMemory extends TestCase {
    private SoftReferenceObjectPool pool;

    public TestSoftRefOutOfMemory(String testName) {
        super(testName);
    }

    public static TestSuite suite() {
        return new TestSuite(TestSoftRefOutOfMemory.class);
    }

    public void tearDown() throws Exception {
        if (pool != null) {
            pool.close();
            pool = null;
        }
        System.gc();
    }

    public void testOutOfMemory() throws Exception {
        pool = new SoftReferenceObjectPool(new SmallPoolableObjectFactory());

        Object obj = pool.borrowObject();
        assertEquals("1", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());

        final List garbage = new LinkedList();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory())]);
            System.gc();
        }
        garbage.clear();
        System.gc();

        obj = pool.borrowObject();
        assertEquals("2", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());
    }

    public void testOutOfMemory1000() throws Exception {
        pool = new SoftReferenceObjectPool(new SmallPoolableObjectFactory());

        for (int i = 0 ; i < 1000 ; i++) {
            pool.addObject();
        }

        Object obj = pool.borrowObject();
        assertEquals("1000", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1000, pool.getNumIdle());

        final List garbage = new LinkedList();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory())]);
            System.gc();
        }
        garbage.clear();
        System.gc();

        obj = pool.borrowObject();
        assertEquals("1001", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());
    }

    public void testOutOfMemoryLarge() throws Exception {
        pool = new SoftReferenceObjectPool(new LargePoolableObjectFactory(1000000));

        Object obj = pool.borrowObject();
        assertTrue(((String)obj).startsWith("1."));
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());

        final List garbage = new LinkedList();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            garbage.add(new byte[Math.min(1024 * 1024, (int)runtime.freeMemory())]);
            System.gc();
        }
        garbage.clear();
        System.gc();

        obj = pool.borrowObject();
        assertTrue(((String)obj).startsWith("2."));
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());
    }

    public void testOutOfMemoryKeepMap() throws Exception {
        pool = new SoftReferenceObjectPool(new LargePoolableObjectFactory(1000000));

        Object obj = pool.borrowObject();
        assertTrue(((String)obj).startsWith("1."));
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());

        // allocate map outside try/catch block
        final Map map = new HashMap();
        try {
            final Runtime runtime = Runtime.getRuntime();
            int i = 0;
            while (true) {
                final int size = Math.max(1, Math.min(1048576, (int)runtime.freeMemory() / 2));
                final byte[] data = new byte[size];
                map.put(new Integer(i++), data);
            }
        }
        catch (OutOfMemoryError ex) { }

        try {
            obj = pool.borrowObject();
            fail("Expected out of memory");
        }
        catch (OutOfMemoryError ex) { }
    }


    public static class SmallPoolableObjectFactory implements PoolableObjectFactory {
        private int counter = 0;

        public Object makeObject() {
            counter++;
            // It seems that as of Java 1.4 String.valueOf may return an
            // intern()'ed String this may cause problems when the tests
            // depend on the returned object to be eventually garbaged
            // collected. Either way, making sure a new String instance
            // is returned eliminated false failures.
            return new String(String.valueOf(counter));
        }
        public boolean validateObject(Object obj) {
            return true;
        }
        public void activateObject(Object obj) { }
        public void passivateObject(Object obj) { }
        public void destroyObject(Object obj) { }
    }

    public static class LargePoolableObjectFactory implements PoolableObjectFactory {
        private String buffer;
        private int counter = 0;

        public LargePoolableObjectFactory(int size) {
            char[] data = new char[size];
            Arrays.fill(data, '.');
            buffer = new String(data);
        }

        public Object makeObject() {
            counter++;
            return String.valueOf(counter) + buffer;
        }
        public boolean validateObject(Object obj) {
            return true;
        }
        public void activateObject(Object obj) { }
        public void passivateObject(Object obj) { }
        public void destroyObject(Object obj) { }
    }
}