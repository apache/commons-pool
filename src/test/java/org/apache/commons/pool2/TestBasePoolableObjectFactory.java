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
package org.apache.commons.pool2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.Test;

/**
 */
public class TestBasePoolableObjectFactory {

    @Test
    public void testDefaultMethods() throws Exception {
        final PooledObjectFactory<AtomicInteger> factory = new TestFactory();

        factory.activateObject(null); // a no-op
        factory.passivateObject(null); // a no-op
        factory.destroyObject(null); // a no-op
        assertTrue(factory.validateObject(null)); // constant true
    }
    
    /**
     * Default destroy does nothing to underlying AtomicInt, ABANDONED mode
     * increments the value.  Verify that destroy with no mode does default,
     * destroy with ABANDONED mode increments.
     *
     * @throws Exception
     */
    @Test
    public void testDestroyModes() throws Exception {
        final PooledObjectFactory<AtomicInteger> factory = new TestFactory();
        final PooledObject<AtomicInteger> pooledObj = factory.makeObject();
        final AtomicInteger obj = pooledObj.getObject();
        factory.destroyObject(pooledObj);
        assertEquals(0, obj.get());
        factory.destroyObject(pooledObj, DestroyMode.ABANDONED);
        assertEquals(1, obj.get());    
    }

    private static class TestFactory extends BasePooledObjectFactory<AtomicInteger> {
        @Override
        public AtomicInteger create() throws Exception {
            return new AtomicInteger(0);
        }
        @Override 
        public void destroyObject(PooledObject<AtomicInteger> p, DestroyMode mode){
            if (mode.equals(DestroyMode.ABANDONED)) {
                p.getObject().incrementAndGet();
            }
        } 
        @Override
        public PooledObject<AtomicInteger> wrap(final AtomicInteger value) {
            return new DefaultPooledObject<>(value);
        }
    }
}
