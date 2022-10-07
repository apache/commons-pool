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

import java.util.Objects;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public final class KeyedPool407Factory extends BaseKeyedPooledObjectFactory<String, KeyedPool407Fixture, RuntimeException> {

    private final KeyedPool407Fixture fixture;

    KeyedPool407Factory(final KeyedPool407Fixture fixture) {
        this.fixture = fixture;
    }

    @Override
    public KeyedPool407Fixture create(final String key) {
        // This is key to the test, creation failed and returns null for instance see
        // https://github.com/openhab/openhab-core/blob/main/bundles/org.openhab.core.io.transport.modbus/src/main/java/org/openhab/core/io/transport/modbus/internal/pooling/ModbusSlaveConnectionFactoryImpl.java#L163
        // the test passes when this returns new Pool407Fixture();
        return fixture;
    }

    @Override
    public boolean validateObject(final String key, final PooledObject<KeyedPool407Fixture> p) {
        // TODO Should this be enough even if wrap() does throw and returns a DefaultPooledObject wrapping a null?
        return p.getObject() != null;
    }

    @Override
    public PooledObject<KeyedPool407Fixture> wrap(final KeyedPool407Fixture value) {
        // Require a non-null value.
        return new DefaultPooledObject<>(Objects.requireNonNull(value, "value"));
    }
}
