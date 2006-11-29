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
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.StackObjectPoolFactory;

import java.util.Random;

/**
 * Tests for {@link CompositeKeyedObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestCompositeKeyedObjectPoolFactory extends TestKeyedObjectPoolFactory {
    private final Random rnd = new Random();

    public TestCompositeKeyedObjectPoolFactory(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeKeyedObjectPoolFactory.class);
    }

    protected KeyedObjectPoolFactory makeFactory(final KeyedPoolableObjectFactory objectFactory) throws UnsupportedOperationException {
        return new CompositeKeyedObjectPoolFactory(objectFactory);
    }

    public void testConstructors() throws Exception {
        CompositeKeyedObjectPoolFactory factory = new CompositeKeyedObjectPoolFactory(createObjectFactory());
        factory.createPool().close();

        try {
            new CompositeKeyedObjectPoolFactory((KeyedPoolableObjectFactory)null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        try {
            new CompositeKeyedObjectPoolFactory((CompositeObjectPoolFactory)null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        factory = new CompositeKeyedObjectPoolFactory(new CompositeObjectPoolFactory(new MethodCallPoolableObjectFactory()));
        factory.createPool().close();

        try {
            new CompositeKeyedObjectPoolFactory((PoolableObjectFactory)null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
        factory = new CompositeKeyedObjectPoolFactory(new MethodCallPoolableObjectFactory());
        factory.createPool().close();
    }

    public void testStaticCreatePool() {
        assertNotNull(CompositeKeyedObjectPoolFactory.createPool(new StackObjectPoolFactory()));
        try {
            CompositeKeyedObjectPoolFactory.createPool(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testFactory() {
        CompositeKeyedObjectPoolFactory factory = (CompositeKeyedObjectPoolFactory)makeFactory();
        try {
            factory.setFactory(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        factory.setFactory(new MethodCallPoolableObjectFactory());
        assertNotNull(factory.getFactory());
        assertNull(factory.getKeyedFactory());
    }

    public void testKeyedFactory() {
        CompositeKeyedObjectPoolFactory factory = (CompositeKeyedObjectPoolFactory)makeFactory();
        try {
            factory.setKeyedFactory(null);
            fail();
        } catch (IllegalArgumentException iae) {
            // expected
        }

        factory.setKeyedFactory(PoolUtils.adapt(new MethodCallPoolableObjectFactory()));
        assertNotNull(factory.getKeyedFactory());
        assertNull(factory.getFactory());
    }

    public void testClone() throws CloneNotSupportedException {
        CompositeKeyedObjectPoolFactory f1 = (CompositeKeyedObjectPoolFactory)makeFactory();
        f1.setBorrowPolicy(BorrowPolicy.SOFT_FIFO);
        f1.setEvictIdleMillis(rnd.nextLong());
        f1.setEvictInvalidFrequencyMillis(rnd.nextLong());
        f1.setExhaustionPolicy(ExhaustionPolicy.FAIL);
        f1.setLimitPolicy(LimitPolicy.FAIL);
        f1.setMaxActivePerKey(rnd.nextInt());
        f1.setMaxIdlePerKey(rnd.nextInt());
        f1.setMaxWaitMillis(rnd.nextInt());
        f1.setTrackingPolicy(TrackingPolicy.REFERENCE);
        f1.setValidateOnReturn(rnd.nextBoolean());
        CompositeKeyedObjectPoolFactory f2 = (CompositeKeyedObjectPoolFactory)f1.clone();

        assertEquals(f1.getBorrowPolicy(), f2.getBorrowPolicy());
        assertEquals(f1.getEvictIdleMillis(), f2.getEvictIdleMillis());
        assertEquals(f1.getEvictInvalidFrequencyMillis(), f2.getEvictInvalidFrequencyMillis());
        assertEquals(f1.getExhaustionPolicy(), f2.getExhaustionPolicy());
        assertEquals(f1.getLimitPolicy(), f2.getLimitPolicy());
        assertEquals(f1.getMaxActivePerKey(), f2.getMaxActivePerKey());
        assertEquals(f1.getMaxIdlePerKey(), f2.getMaxIdlePerKey());
        assertEquals(f1.getMaxWaitMillis(), f2.getMaxWaitMillis());
        assertEquals(f1.getTrackingPolicy(), f2.getTrackingPolicy());
        assertEquals(f1.isValidateOnReturn(), f2.isValidateOnReturn());
    }
}
