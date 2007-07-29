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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.pool.TestKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;

/**
 * Tests for {@link CompositeKeyedObjectPool}
 * when backed by a {@link CompositeObjectPool}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestCompositeKeyedObjectPool extends TestKeyedObjectPool {
    public TestCompositeKeyedObjectPool(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeKeyedObjectPool.class);
    }

    protected KeyedObjectPool makeEmptyPool(final KeyedPoolableObjectFactory factory) {
        final CompositeKeyedObjectPoolFactory ckopf = new CompositeKeyedObjectPoolFactory(factory);
        return ckopf.createPool();
    }

    public void testConstructors() {
        try {
            new CompositeKeyedObjectPool(null);
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testSetFactory() {
        final KeyedObjectPool pool;
        try {
            pool = makeEmptyPool(new FailingKeyedPoolableObjectFactory());
        } catch (UnsupportedOperationException uoe) {
            return;
        }
        try {
            pool.setFactory(new FailingKeyedPoolableObjectFactory());
            fail("Expected setFactory to throw an UnsupportedOperationException.");
        } catch (UnsupportedOperationException uoe) {
            // expected
        }
    }
}
