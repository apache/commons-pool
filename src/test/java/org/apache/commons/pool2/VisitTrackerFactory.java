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

import org.apache.commons.pool2.KeyedPoolableObjectFactory;
import org.apache.commons.pool2.PoolableObjectFactory;

/**
 * Factory that creates VisitTracker instances. Used to
 * test Evictor runs.
 *
 */
public class VisitTrackerFactory<K> implements PoolableObjectFactory<VisitTracker<K>>, 
    KeyedPoolableObjectFactory<K,VisitTracker<K>> {
    private int nextId = 0;
    public VisitTrackerFactory() {
        super();
    }
    @Override
    public VisitTracker<K> makeObject() {
        return new VisitTracker<K>(nextId++);
    }
    @Override
    public VisitTracker<K> makeObject(K key) {
        return new VisitTracker<K>(nextId++, key);
    }
    @Override
    public void destroyObject(VisitTracker<K> obj) {
        obj.destroy();
    }
    @Override
    public void destroyObject(K key, VisitTracker<K> obj) {
        obj.destroy();
    }
    @Override
    public boolean validateObject(VisitTracker<K> obj) {
        return obj.validate();
    }
    @Override
    public boolean validateObject(K key, VisitTracker<K> obj) {
        return obj.validate();
    }
    @Override
    public void activateObject(VisitTracker<K> obj) throws Exception {
        obj.activate();
    }
    @Override
    public void activateObject(K key, VisitTracker<K> obj) throws Exception {
        obj.activate();
    }
    @Override
    public void passivateObject(VisitTracker<K> obj) throws Exception {
        obj.passivate();
    }
    @Override
    public void passivateObject(K key, VisitTracker<K> obj) throws Exception {
        obj.passivate();
    }
    public void resetId() {
        nextId = 0;
    }
}
