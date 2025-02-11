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

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Tests POOL-407.
 */
public final class Pool407NormalFactory extends AbstractPool407Factory {

    private final Pool407Fixture fixture;

    Pool407NormalFactory(final Pool407Fixture fixture) {
        this.fixture = fixture;
    }

    @Override
    public Pool407Fixture create() {
        // When this returns null, we fail-fast internally and borrowsObject() throws an exception.
        //
        // Old note:
        // This is key to the test, creation failed and returns null for instance see
        // https://github.com/openhab/openhab-core/blob/main/bundles/org.openhab.core.io.transport.modbus/
        // src/main/java/org/openhab/core/io/transport/modbus/internal/pooling/ModbusSlaveConnectionFactoryImpl.java#L163
        // the test passes when this returns new Pool407Fixture();
        return fixture;
    }

    @Override
    boolean isDefaultMakeObject() {
        return true;
    }

    @Override
    boolean isNullFactory() {
        return fixture == null;
    }

    @Override
    public PooledObject<Pool407Fixture> wrap(final Pool407Fixture value) {
        // value will never be null here.
        return new DefaultPooledObject<>(value);
    }
}
