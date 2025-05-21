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

import org.apache.commons.pool3.impl.DefaultPooledObject;

/**
 * Factory that creates VisitTracker instances. Used to test Evictor runs.
 *
 * @param <K> The VisitTracker key type.
 */
public class VisitTrackerFactory<K>
        implements PooledObjectFactory<VisitTracker<K>, RuntimeException>, KeyedPooledObjectFactory<K, VisitTracker<K>, RuntimeException> {

    private int nextId;

    @Override
    public void activateObject(final K key, final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().activate();
    }

    @Override
    public void activateObject(final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().activate();
    }

    @Override
    public void destroyObject(final K key, final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().destroy();
    }

    @Override
    public void destroyObject(final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().destroy();
    }

    @Override
    public PooledObject<VisitTracker<K>> makeObject() {
        return new DefaultPooledObject<>(new VisitTracker<>(nextId++));
    }

    @Override
    public PooledObject<VisitTracker<K>> makeObject(final K key) {
        return new DefaultPooledObject<>(new VisitTracker<>(nextId++, key));
    }

    @Override
    public void passivateObject(final K key, final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().passivate();
    }

    @Override
    public void passivateObject(final PooledObject<VisitTracker<K>> ref) {
        ref.getObject().passivate();
    }

    public void resetId() {
        nextId = 0;
    }

    @Override
    public boolean validateObject(final K key, final PooledObject<VisitTracker<K>> ref) {
        return ref.getObject().validate();
    }

    @Override
    public boolean validateObject(final PooledObject<VisitTracker<K>> ref) {
        return ref.getObject().validate();
    }
}
