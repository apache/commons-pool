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
package org.apache.commons.pool3;

/**
 * Test pooled object class. Keeps track of how many times it has been validated, activated, passivated.
 *
 * @param <K> The key type.
 */
public class VisitTracker<K> {

    private int validateCount;
    private int activateCount;
    private int passivateCount;
    private boolean destroyed;
    private int id;
    private K key;

    public VisitTracker() {
        reset();
    }

    public VisitTracker(final int id) {
        this.id = id;
        reset();
    }

    public VisitTracker(final int id, final K key) {
        this.id = id;
        this.key = key;
        reset();
    }

    public void activate() {
        if (destroyed) {
            fail("attempted to activate a destroyed object");
        }
        activateCount++;
    }

    public void destroy() {
        destroyed = true;
    }

    private void fail(final String message) {
        throw new IllegalStateException(message);
    }

    public int getActivateCount() {
        return activateCount;
    }

    public int getId() {
        return id;
    }

    public K getKey() {
        return key;
    }

    public int getPassivateCount() {
        return passivateCount;
    }

    public int getValidateCount() {
        return validateCount;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void passivate() {
        if (destroyed) {
            fail("attempted to passivate a destroyed object");
        }
        passivateCount++;
    }

    public void reset() {
        validateCount = 0;
        activateCount = 0;
        passivateCount = 0;
        destroyed = false;
    }

    @Override
    public String toString() {
        return "Key: " + key + " id: " + id;
    }

    public boolean validate() {
        if (destroyed) {
            fail("attempted to validate a destroyed object");
        }
        validateCount++;
        return true;
    }
}
