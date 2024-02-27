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

package org.apache.commons.pool3.pool407;

import org.apache.commons.pool3.PooledObject;

/**
 * Tests POOL-407.
 */
public final class Pool407NullPoolableObjectFactory extends AbstractPool407Factory {

    @Override
    public Pool407Fixture create() {
        return null;
    }

    @Override
    boolean isDefaultMakeObject() {
        return false;
    }

    @Override
    boolean isNullFactory() {
        return true;
    }

    @Override
    public PooledObject<Pool407Fixture> makeObject() throws RuntimeException {
        return null;
    }

    @Override
    public PooledObject<Pool407Fixture> wrap(final Pool407Fixture value) {
        return null;
    }
}
