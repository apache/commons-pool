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

package org.apache.commons.pool;

/**
 * Factory that creates VisitTracker instances. Used to
 * test Evictor runs.
 *
 */
public class VisitTrackerFactory implements PoolableObjectFactory, 
    KeyedPoolableObjectFactory {
    private int nextId = 0;
    public VisitTrackerFactory() {
        super();
    }
    public Object makeObject() {
        return new VisitTracker(nextId++);
    }
    public Object makeObject(Object key) {
        return new VisitTracker(nextId++, key);
    }
    public void destroyObject(Object obj) {
        ((VisitTracker) obj).destroy();
    }
    public void destroyObject(Object key, Object obj) {
        ((VisitTracker) obj).destroy();
    }
    public boolean validateObject(Object obj) {
        return ((VisitTracker) obj).validate();
    }
    public boolean validateObject(Object key, Object obj) {
        return ((VisitTracker) obj).validate();
    }
    public void activateObject(Object obj) throws Exception {
        ((VisitTracker) obj).activate();
    }
    public void activateObject(Object key, Object obj) throws Exception {
        ((VisitTracker) obj).activate();
    }
    public void passivateObject(Object obj) throws Exception {
        ((VisitTracker) obj).passivate();
    }
    public void passivateObject(Object key, Object obj) throws Exception {
        ((VisitTracker) obj).passivate();
    }
    public void resetId() {
        nextId = 0;
    }
}
