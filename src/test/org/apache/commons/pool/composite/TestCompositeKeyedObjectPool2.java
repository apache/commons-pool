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

import org.apache.commons.pool.TestKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for {@link CompositeKeyedObjectPool}
 * when backed by a generic {@link ObjectPool} implementation.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestCompositeKeyedObjectPool2 extends TestCompositeKeyedObjectPool {
    public TestCompositeKeyedObjectPool2(final String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestCompositeKeyedObjectPool2.class);
    }

    protected KeyedObjectPool makeEmptyPool(final KeyedPoolableObjectFactory factory) {
        return CompositeKeyedObjectPoolFactory.createPool(new GenericObjectPoolFactory(PoolUtils.adapt(factory, KEY)));
    }

    public void testConstructors() {
        new CompositeKeyedObjectPool(new GenericObjectPoolFactory(new MethodCallPoolableObjectFactory()));
    }
}
