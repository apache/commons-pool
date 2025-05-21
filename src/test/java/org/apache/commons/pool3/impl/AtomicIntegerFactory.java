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
package org.apache.commons.pool3.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool3.BasePooledObjectFactory;
import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.Waiter;

/**
 * Factory that sources PooledObjects that wrap AtomicIntegers.
 * {@link #create()} creates an AtomicInteger with value 0, activate increments
 * the value of the wrapped AtomicInteger and passivate decrements it. Latency
 * of factory methods is configurable.
 */
public class AtomicIntegerFactory
    extends BasePooledObjectFactory<AtomicInteger, RuntimeException> {

    private long activateLatency;
    private long passivateLatency;
    private long createLatency;
    private long destroyLatency;
    private long validateLatency;

    @Override
    public void activateObject(final PooledObject<AtomicInteger> p) {
        p.getObject().incrementAndGet();
        Waiter.sleepQuietly(activateLatency);
    }

    @Override
    public AtomicInteger create() {
        Waiter.sleepQuietly(createLatency);
        return new AtomicInteger();
    }

    @Override
    public void destroyObject(final PooledObject<AtomicInteger> p) {
        Waiter.sleepQuietly(destroyLatency);
    }

    @Override
    public void passivateObject(final PooledObject<AtomicInteger> p) {
        p.getObject().decrementAndGet();
        Waiter.sleepQuietly(passivateLatency);
    }

    /**
     * @param activateLatency the activateLatency to set
     */
    public void setActivateLatency(final long activateLatency) {
        this.activateLatency = activateLatency;
    }

    /**
     * @param createLatency the createLatency to set
     */
    public void setCreateLatency(final long createLatency) {
        this.createLatency = createLatency;
    }

    /**
     * @param destroyLatency the destroyLatency to set
     */
    public void setDestroyLatency(final long destroyLatency) {
        this.destroyLatency = destroyLatency;
    }

    /**
     * @param passivateLatency the passivateLatency to set
     */
    public void setPassivateLatency(final long passivateLatency) {
        this.passivateLatency = passivateLatency;
    }

    /**
     * @param validateLatency the validateLatency to set
     */
    public void setValidateLatency(final long validateLatency) {
        this.validateLatency = validateLatency;
    }

    @Override
    public boolean validateObject(final PooledObject<AtomicInteger> instance) {
        Waiter.sleepQuietly(validateLatency);
        return instance.getObject().intValue() == 1;
    }

    @Override
    public PooledObject<AtomicInteger> wrap(final AtomicInteger integer) {
        return new DefaultPooledObject<>(integer);
    }
}
