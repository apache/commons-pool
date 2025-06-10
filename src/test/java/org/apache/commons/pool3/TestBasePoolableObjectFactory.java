/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool3.impl.DefaultPooledObject;
import org.junit.jupiter.api.Test;

/**
 */
class TestBasePoolableObjectFactory {

    private static final class TestFactory extends BasePooledObjectFactory<AtomicInteger, RuntimeException> {
        @Override
        public AtomicInteger create() {
            return new AtomicInteger();
        }

        @Override
        public void destroyObject(final PooledObject<AtomicInteger> p, final DestroyMode destroyMode) {
            if (destroyMode.equals(DestroyMode.ABANDONED)) {
                p.getObject().incrementAndGet();
            }
        }

        @Override
        public PooledObject<AtomicInteger> wrap(final AtomicInteger value) {
            return new DefaultPooledObject<>(value);
        }
    }

    @Test
    void testDefaultMethods() {
        final PooledObjectFactory<AtomicInteger, RuntimeException> factory = new TestFactory();

        factory.activateObject(null); // a no-op
        factory.passivateObject(null); // a no-op
        factory.destroyObject(null); // a no-op
        assertTrue(factory.validateObject(null)); // constant true
    }

    /**
     * Default destroy does nothing to underlying AtomicInt, ABANDONED mode increments the value. Verify that destroy with no mode does default, destroy with
     * ABANDONED mode increments.
     *
     * @throws RuntimeException May occur in some failure modes
     */
    @Test
    void testDestroyModes() {
        final PooledObjectFactory<AtomicInteger, RuntimeException> factory = new TestFactory();
        final PooledObject<AtomicInteger> pooledObj = factory.makeObject();
        final AtomicInteger obj = pooledObj.getObject();
        factory.destroyObject(pooledObj);
        assertEquals(0, obj.get());
        factory.destroyObject(pooledObj, DestroyMode.ABANDONED);
        assertEquals(1, obj.get());
    }
}
