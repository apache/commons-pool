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

package org.apache.commons.pool3.performance;

import org.apache.commons.pool3.PooledObject;
import org.apache.commons.pool3.PooledObjectFactory;
import org.apache.commons.pool3.Waiter;
import org.apache.commons.pool3.impl.DefaultPooledObject;

/**
 * Sleepy ObjectFactory (everything takes a while longer)
 */
public class SleepingObjectFactory implements PooledObjectFactory<Integer, RuntimeException> {

    private int counter;
    private boolean debug;

    @Override
    public void activateObject(final PooledObject<Integer> obj) {
        debug("activateObject", obj);
        Waiter.sleepQuietly(10);
    }

    private void debug(final String method, final Object obj) {
        if (debug) {
            final String thread = "thread" + Thread.currentThread().getName();
            System.out.println(thread + ": " + method + " " + obj);
        }
    }

    @Override
    public void destroyObject(final PooledObject<Integer> obj) {
        debug("destroyObject", obj);
        Waiter.sleepQuietly(250);
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public PooledObject<Integer> makeObject() {
        // Deliberate choice to create a new object in case future unit tests
        // check for a specific object.
        final Integer obj = Integer.valueOf(counter++);
        debug("makeObject", obj);
        Waiter.sleepQuietly(500);
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void passivateObject(final PooledObject<Integer> obj) {
        debug("passivateObject", obj);
        Waiter.sleepQuietly(10);
    }

    public void setDebug(final boolean b) {
        debug = b;
    }

    @Override
    public boolean validateObject(final PooledObject<Integer> obj) {
        debug("validateObject", obj);
        Waiter.sleepQuietly(30);
        return true;
    }
}
