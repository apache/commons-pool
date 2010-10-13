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

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.TestBaseObjectPool;

/**
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public class TestSoftReferenceObjectPool extends TestBaseObjectPool {
    @Override
    protected ObjectPool<Object> makeEmptyPool(int cap) {
        return new SoftReferenceObjectPool<Object>(
            new PoolableObjectFactory<Object>()  {
                int counter = 0;
                public Object makeObject() { return String.valueOf(counter++); }
                public void destroyObject(Object obj) { }
                public boolean validateObject(Object obj) { return true; }
                public void activateObject(Object obj) { }
                public void passivateObject(Object obj) { }
            }
            );
    }

    @Override
    protected ObjectPool<Object> makeEmptyPool(final PoolableObjectFactory<Object> factory) {
        return new SoftReferenceObjectPool<Object>(factory);
    }

    @Override
    protected Object getNthObject(int n) {
        return String.valueOf(n);
    }

    @Override
    protected boolean isLifo() {
        return false;
    }

    @Override
    protected boolean isFifo() {
        return false;
    }

}
