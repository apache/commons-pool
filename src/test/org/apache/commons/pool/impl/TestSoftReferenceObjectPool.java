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
 * @version $Id: TestSoftReferenceObjectPool.java 1229442 2012-01-10 01:34:05Z ggregory $
 */
public class TestSoftReferenceObjectPool extends TestBaseObjectPool<String> {
    public TestSoftReferenceObjectPool(String testName) {
        super(testName);
    }

    @Override
    protected ObjectPool<String> makeEmptyPool(int cap) {
        return new SoftReferenceObjectPool<String>(
            new PoolableObjectFactory<String>()  {
                int counter = 0;
                public String makeObject() { return String.valueOf(counter++); }
                public void destroyObject(String obj) { }
                public boolean validateObject(String obj) { return true; }
                public void activateObject(String obj) { }
                public void passivateObject(String obj) { }
            }
            );
    }

    @Override
    protected ObjectPool<Integer> makeEmptyPool(final PoolableObjectFactory<Integer> factory) {
        return new SoftReferenceObjectPool<Integer>(factory);
    }

    @Override
    protected String getNthObject(int n) {
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
