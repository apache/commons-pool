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

package org.apache.commons.pool2.performance;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Sleepy ObjectFactory (everything takes a while longer)
 */
public class SleepingObjectFactory implements PooledObjectFactory<Integer> {

    private int counter = 0;
    private boolean debug = false;

    @Override
    public PooledObject<Integer> makeObject() throws Exception {
        // Deliberate choice to create a new object in case future unit tests
        // check for a specific object.
        final Integer obj = new Integer(counter++);
        debug("makeObject", obj);
        sleep(500);
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(final PooledObject<Integer> obj) throws Exception {
        debug("destroyObject", obj);
        sleep(250);
    }

    @Override
    public boolean validateObject(final PooledObject<Integer> obj) {
        debug("validateObject", obj);
        sleep(30);
        return true;
    }

    @Override
    public void activateObject(final PooledObject<Integer> obj) throws Exception {
        debug("activateObject", obj);
        sleep(10);
    }

    @Override
    public void passivateObject(final PooledObject<Integer> obj) throws Exception {
        debug("passivateObject", obj);
        sleep(10);
    }

    private void debug(final String method, final Object obj) {
        if (debug) {
            final String thread = "thread" + Thread.currentThread().getName();
            System.out.println(thread + ": " + method + " " + obj);
        }
    }

    private void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (final InterruptedException e) {
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(final boolean b) {
        debug = b;
    }
}
