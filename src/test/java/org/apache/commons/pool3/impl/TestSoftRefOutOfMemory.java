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

package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.pool3.BasePooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestSoftRefOutOfMemory {
    public static class LargePoolableObjectFactory extends BasePooledObjectFactory<String, RuntimeException> {
        private final String buffer;
        private int counter;

        public LargePoolableObjectFactory(final int size) {
            final char[] data = new char[size];
            Arrays.fill(data, '.');
            buffer = new String(data);
        }

        @Override
        public String create() {
            counter++;
            return String.valueOf(counter) + buffer;
        }

        @Override
        public PooledObject<String> wrap(final String value) {
            return new DefaultPooledObject<>(value);
        }
    }

    private static final class OomeFactory extends BasePooledObjectFactory<String, RuntimeException> {

        private final OomeTrigger trigger;

        public OomeFactory(final OomeTrigger trigger) {
            this.trigger = trigger;
        }

        @Override
        public String create() {
            if (trigger.equals(OomeTrigger.CREATE)) {
                throw new OutOfMemoryError();
            }
            // It seems that as of Java 1.4 String.valueOf may return an
            // intern()'ed String this may cause problems when the tests
            // depend on the returned object to be eventually garbaged
            // collected. Either way, making sure a new String instance
            // is returned eliminated false failures.
            return new String();
        }

        @Override
        public void destroyObject(final PooledObject<String> p) {
            if (trigger.equals(OomeTrigger.DESTROY)) {
                throw new OutOfMemoryError();
            }
            super.destroyObject(p);
        }

        @Override
        public boolean validateObject(final PooledObject<String> p) {
            if (trigger.equals(OomeTrigger.VALIDATE)) {
                throw new OutOfMemoryError();
            }
            return !trigger.equals(OomeTrigger.DESTROY);
        }

        @Override
        public PooledObject<String> wrap(final String value) {
            return new DefaultPooledObject<>(value);
        }
    }

    private enum OomeTrigger {
        CREATE,
        VALIDATE,
        DESTROY
    }

    public static class SmallPoolableObjectFactory extends BasePooledObjectFactory<String, RuntimeException> {
        private int counter;

        @Override
        public String create() {
            counter++;
            // It seems that as of Java 1.4 String.valueOf may return an
            // intern()'ed String this may cause problems when the tests
            // depend on the returned object to be eventually garbaged
            // collected. Either way, making sure a new String instance
            // is returned eliminated false failures.
            return new String(String.valueOf(counter));
        }
        @Override
        public PooledObject<String> wrap(final String value) {
            return new DefaultPooledObject<>(value);
        }
    }

    private SoftReferenceObjectPool<String, RuntimeException> pool;

    @AfterEach
    public void tearDown() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
        System.gc();
    }

    @Test
    public void testOutOfMemory() throws Exception {
        pool = new SoftReferenceObjectPool<>(new SmallPoolableObjectFactory());

        String obj = pool.borrowObject();
        assertEquals("1", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());

        final List<byte[]> garbage = new LinkedList<>();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            try {
                long freeMemory = runtime.freeMemory();
                if (freeMemory > Integer.MAX_VALUE) {
                    freeMemory = Integer.MAX_VALUE;
                }
                garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
            } catch (final OutOfMemoryError oome) {
                System.gc();
            }
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

    @Test
    public void testOutOfMemory1000() throws Exception {
        pool = new SoftReferenceObjectPool<>(new SmallPoolableObjectFactory());

        for (int i = 0 ; i < 1000 ; i++) {
            pool.addObject();
        }

        String obj = pool.borrowObject();
        assertEquals("1000", obj);
        pool.returnObject(obj);
        obj = null;

        assertEquals(1000, pool.getNumIdle());

        final List<byte[]> garbage = new LinkedList<>();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            try {
                long freeMemory = runtime.freeMemory();
                if (freeMemory > Integer.MAX_VALUE) {
                    freeMemory = Integer.MAX_VALUE;
                }
                garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
            } catch (final OutOfMemoryError oome) {
                System.gc();
            }
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

    /**
     * Makes sure an {@link OutOfMemoryError} isn't swallowed.
     *
     * @throws Exception May occur in some failure modes
     */
    @Test
    public void testOutOfMemoryError() throws Exception {
        pool = new SoftReferenceObjectPool<>(new OomeFactory(OomeTrigger.CREATE));
        assertThrows(OutOfMemoryError.class, pool::borrowObject);
        pool.close();

        pool = new SoftReferenceObjectPool<>(new OomeFactory(OomeTrigger.VALIDATE));
        assertThrows(OutOfMemoryError.class, pool::borrowObject);
        pool.close();

        pool = new SoftReferenceObjectPool<>(new OomeFactory(OomeTrigger.DESTROY));
        assertThrows(OutOfMemoryError.class, pool::borrowObject);
        pool.close();
    }

    @Test
    public void testOutOfMemoryLarge() throws Exception {
        pool = new SoftReferenceObjectPool<>(new LargePoolableObjectFactory(1000000));

        String obj = pool.borrowObject();
        assertTrue(obj.startsWith("1."));
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());

        final List<byte[]> garbage = new LinkedList<>();
        final Runtime runtime = Runtime.getRuntime();
        while (pool.getNumIdle() > 0) {
            try {
                long freeMemory = runtime.freeMemory();
                if (freeMemory > Integer.MAX_VALUE) {
                    freeMemory = Integer.MAX_VALUE;
                }
                garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
            } catch (final OutOfMemoryError oome) {
                System.gc();
            }
            System.gc();
        }
        garbage.clear();
        System.gc();

        obj = pool.borrowObject();
        assertTrue(obj.startsWith("2."));
        pool.returnObject(obj);
        obj = null;

        assertEquals(1, pool.getNumIdle());
    }
}