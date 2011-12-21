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

package org.apache.commons.pool.impl;

import org.apache.commons.pool.MethodCallPoolableObjectFactory;
import org.apache.commons.pool.ObjectPoolFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestObjectPoolFactory;

/**
 * Tests for {@link StackObjectPoolFactory}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestStackObjectPoolFactory extends TestObjectPoolFactory {
    public TestStackObjectPoolFactory(final String name) {
        super(name);
    }

    protected ObjectPoolFactory makeFactory(final PoolableObjectFactory objectFactory) throws UnsupportedOperationException {
        return new StackObjectPoolFactory(objectFactory);
    }

    public void testConstructors() throws Exception {
        StackObjectPoolFactory factory = new StackObjectPoolFactory();
        factory.createPool().close();

        
        factory = new StackObjectPoolFactory(1);
        StackObjectPool pool = (StackObjectPool)factory.createPool();
        pool.close();


        factory = new StackObjectPoolFactory(1, 1);
        pool = (StackObjectPool)factory.createPool();
        pool.close();


        factory = new StackObjectPoolFactory(new MethodCallPoolableObjectFactory(), 1);
        pool = (StackObjectPool)factory.createPool();
        Object a = pool.borrowObject();
        Object b = pool.borrowObject();
        pool.returnObject(a);
        pool.returnObject(b);
        assertEquals(1, pool.getNumIdle());
        pool.close();
    }
}
