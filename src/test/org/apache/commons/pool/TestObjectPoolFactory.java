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

package org.apache.commons.pool;

import junit.framework.TestCase;

/**
 * Unit tests for all {@link ObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class TestObjectPoolFactory extends TestCase {
    protected TestObjectPoolFactory(final String name) {
        super(name);
    }

    /**
     * @throws UnsupportedOperationException when this is unsupported by this PoolableObjectFactory type.
     */
    protected ObjectPoolFactory makeFactory() throws UnsupportedOperationException {
        return makeFactory(new MethodCallPoolableObjectFactory());
    }

    /**
     * @throws UnsupportedOperationException when this is unsupported by this PoolableObjectFactory type.
     */
    protected abstract ObjectPoolFactory makeFactory(PoolableObjectFactory objectFactory) throws UnsupportedOperationException;

    public void testCreatePool() throws Exception {
        final ObjectPoolFactory factory;
        try {
            factory = makeFactory();
        } catch (UnsupportedOperationException uoe) {
            return;
        }
        final ObjectPool pool = factory.createPool();
        pool.close();
    }

    public void testToString() {
        final ObjectPoolFactory factory;
        try {
            factory = makeFactory();
        } catch (UnsupportedOperationException uoe) {
            return;
        }
        factory.toString();
    }
}
