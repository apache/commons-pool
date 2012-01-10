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
 * @version $Id$
 */
public class TestStackObjectPoolFactory extends TestObjectPoolFactory {
    public TestStackObjectPoolFactory(final String name) {
        super(name);
    }

    @Override
    protected ObjectPoolFactory<Integer> makeFactory(final PoolableObjectFactory<Integer> objectFactory) throws UnsupportedOperationException {
        return new StackObjectPoolFactory<Integer>(objectFactory);
    }

    public void testConstructors() throws Exception {
        StackObjectPoolFactory<Integer> factory = new StackObjectPoolFactory<Integer>();
        factory.createPool().close();

        
        factory = new StackObjectPoolFactory<Integer>(1);
        StackObjectPool<Integer> pool = (StackObjectPool<Integer>)factory.createPool();
        pool.close();


        factory = new StackObjectPoolFactory<Integer>(1, 1);
        pool = (StackObjectPool<Integer>)factory.createPool();
        pool.close();


        factory = new StackObjectPoolFactory<Integer>(new MethodCallPoolableObjectFactory(), 1);
        pool = (StackObjectPool<Integer>)factory.createPool();
        Integer a = pool.borrowObject();
        Integer b = pool.borrowObject();
        pool.returnObject(a);
        pool.returnObject(b);
        assertEquals(1, pool.getNumIdle());
        pool.close();
    }
}
