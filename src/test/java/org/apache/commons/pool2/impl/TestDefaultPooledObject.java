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
package org.apache.commons.pool2.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class TestDefaultPooledObject {

    /**
     * JIRA: POOL-279
     * @throws Exception
     */
    @Test
    public void testgetIdleTimeMillis() throws Exception {
        final DefaultPooledObject<Object> dpo = new DefaultPooledObject<>(new Object());
        final AtomicBoolean negativeIdleTimeReturned = new AtomicBoolean(false);
        final ExecutorService executor = Executors.newFixedThreadPool(
                                      Runtime.getRuntime().availableProcessors()*3);
        final Runnable allocateAndDeallocateTask = new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<10000;i++) {
                    if (dpo.getIdleTimeMillis() < 0) {
                        negativeIdleTimeReturned.set(true);
                        break;
                    }
                }
                dpo.allocate();
                for (int i=0;i<10000;i++) {
                    if (dpo.getIdleTimeMillis() < 0) {
                        negativeIdleTimeReturned.set(true);
                        break;
                    }
                }
                dpo.deallocate();
            }
        };
        final Runnable getIdleTimeTask = new Runnable() {
            @Override
            public void run() {
                for (int i=0;i<10000;i++) {
                    if (dpo.getIdleTimeMillis() < 0) {
                        negativeIdleTimeReturned.set(true);
                        break;
                    }
                }
            }
        };
        final double probabilityOfAllocationTask = 0.7;
        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            final Runnable randomTask = Math.random() < probabilityOfAllocationTask ?
                                  allocateAndDeallocateTask : getIdleTimeTask;
            futures.add(executor.submit(randomTask));
        }
        for (final Future<?> future: futures) {
            future.get();
        }
        Assert.assertFalse(
           "DefaultPooledObject.getIdleTimeMillis() returned a negative value",
           negativeIdleTimeReturned.get());
    }

}