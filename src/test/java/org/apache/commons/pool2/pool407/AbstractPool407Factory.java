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

package org.apache.commons.pool2.pool407;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;

/**
 * Tests POOL-407.
 */
public abstract class AbstractPool407Factory extends BasePooledObjectFactory<Pool407Fixture> {

    /**
     * Tests whether the subclass relies on the Pool's implementation of makeObject(). If the subclass returns false, then it implements makeObject(), in which
     * case makeObject() returns a bad object like null or a null wrapper.
     *
     * @return whether the subclass relies on the Pool's implementation of makeObject().
     */
    abstract boolean isDefaultMakeObject();

    /**
     * Tests whether this instance makes null or null wrappers.
     *
     * @return whether this instance makes null or null wrappers.
     */
    abstract boolean isNullFactory();

    @Override
    public boolean validateObject(final PooledObject<Pool407Fixture> p) {
        // TODO Should this be enough even if wrap() does throw and returns a DefaultPooledObject wrapping a null?
        return !PooledObject.isNull(p);
    }

}
