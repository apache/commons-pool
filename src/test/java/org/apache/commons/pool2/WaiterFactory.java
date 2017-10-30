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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Object factory with configurable latencies for object lifecycle methods.
 * This factory will also track and enforce maxActive, maxActivePerKey contracts.
 * If the factory's maxActive / maxActivePerKey are set to match those of the
 * pool, makeObject will throw IllegalStateException if the number of makes - destroys
 * (per key) exceeds the configured max.
 *
 */
public class WaiterFactory<K> implements PooledObjectFactory<Waiter>,
KeyedPooledObjectFactory<K,Waiter> {

    /** Latency of activateObject */
    private final long activateLatency;

    /** Latency of destroyObject */
    private final long destroyLatency;

    /** Latency of makeObject */
    private final long makeLatency;

    /** Latency of passivateObject */
    private final long passivateLatency;

    /** Latency of validateObject */
    private final long validateLatency;

    /** Latency of doWait for Waiter instances created by this factory */
    private final long waiterLatency;

    /** Probability that passivation will invalidate Waiter instances */
    private final double passivateInvalidationProbability;

    /** Count of (makes - destroys) since last reset */
    private long activeCount = 0;

    /** Count of (makes - destroys) per key since last reset */
    private final Map<K,Integer> activeCounts = new HashMap<>();

    /** Maximum of (makes - destroys) - if exceeded IllegalStateException */
    private final long maxActive;  // GKOP 1.x calls this maxTotal

    /** Maximum of (makes - destroys) per key */
    private final long maxActivePerKey;  // GKOP 1.x calls this maxActive

    public WaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency,final long maxActive, final long maxActivePerKey,
            final double passivateInvalidationProbability) {
        this.activateLatency = activateLatency;
        this.destroyLatency = destroyLatency;
        this.makeLatency = makeLatency;
        this.passivateLatency = passivateLatency;
        this.validateLatency = validateLatency;
        this.waiterLatency = waiterLatency;
        this.maxActive = maxActive;
        this.maxActivePerKey = maxActivePerKey;
        this.passivateInvalidationProbability = passivateInvalidationProbability;
    }

    public WaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency) {
        this(activateLatency, destroyLatency, makeLatency, passivateLatency,
                validateLatency, waiterLatency, Long.MAX_VALUE, Long.MAX_VALUE, 0);
    }

    public WaiterFactory(final long activateLatency, final long destroyLatency,
            final long makeLatency, final long passivateLatency, final long validateLatency,
            final long waiterLatency,final long maxActive) {
        this(activateLatency, destroyLatency, makeLatency, passivateLatency,
                validateLatency, waiterLatency, maxActive, Long.MAX_VALUE, 0);
    }

    @Override
    public void activateObject(final PooledObject<Waiter> obj) throws Exception {
        doWait(activateLatency);
        obj.getObject().setActive(true);
    }

    @Override
    public void destroyObject(final PooledObject<Waiter> obj) throws Exception {
        doWait(destroyLatency);
        obj.getObject().setValid(false);
        obj.getObject().setActive(false);
        // Decrement *after* destroy
        synchronized (this) {
            activeCount--;
        }
    }

    @Override
    public PooledObject<Waiter> makeObject() throws Exception {
        // Increment and test *before* make
        synchronized (this) {
            if (activeCount >= maxActive) {
                throw new IllegalStateException("Too many active instances: " +
                activeCount + " in circulation with maxActive = " + maxActive);
            }
            activeCount++;
        }
        doWait(makeLatency);
        return new DefaultPooledObject<>(new Waiter(false, true, waiterLatency));
    }

    @Override
    public void passivateObject(final PooledObject<Waiter> obj) throws Exception {
        obj.getObject().setActive(false);
        doWait(passivateLatency);
        if (Math.random() < passivateInvalidationProbability) {
            obj.getObject().setValid(false);
        }
    }

    @Override
    public boolean validateObject(final PooledObject<Waiter> obj) {
        doWait(validateLatency);
        return obj.getObject().isValid();
    }

    protected void doWait(final long latency) {
        if (latency == 0) {
            return;
        }
        try {
            Thread.sleep(latency);
        } catch (final InterruptedException ex) {
            // ignore
        }
    }

    public synchronized void reset() {
        activeCount = 0;
        if (activeCounts.isEmpty()) {
            return;
        }
        final Iterator<K> it = activeCounts.keySet().iterator();
        while (it.hasNext()) {
            final K key = it.next();
            activeCounts.put(key, Integer.valueOf(0));
        }
    }

    /**
     * @return the maxActive
     */
    public synchronized long getMaxActive() {
        return maxActive;
    }

    // KeyedPoolableObjectFactory methods

    @Override
    public void activateObject(final K key, final PooledObject<Waiter> obj) throws Exception {
        activateObject(obj);
    }

    @Override
    public void destroyObject(final K key,final PooledObject<Waiter> obj) throws Exception {
        destroyObject(obj);
        synchronized (this) {
            final Integer count = activeCounts.get(key);
            activeCounts.put(key, Integer.valueOf(count.intValue() - 1));
        }
    }

    @Override
    public PooledObject<Waiter> makeObject(final K key) throws Exception {
        synchronized (this) {
            Integer count = activeCounts.get(key);
            if (count == null) {
                count = Integer.valueOf(1);
                activeCounts.put(key, count);
            } else {
                if (count.intValue() >= maxActivePerKey) {
                    throw new IllegalStateException("Too many active " +
                    "instances for key = " + key + ": " + count.intValue() +
                    " in circulation " + "with maxActivePerKey = " +
                    maxActivePerKey);
                }
                activeCounts.put(key, Integer.valueOf(count.intValue() + 1));
            }
        }
        return makeObject();
    }

    @Override
    public void passivateObject(final K key, final PooledObject<Waiter> obj) throws Exception {
        passivateObject(obj);
    }

    @Override
    public boolean validateObject(final K key, final PooledObject<Waiter> obj) {
        return validateObject(obj);
    }

}
