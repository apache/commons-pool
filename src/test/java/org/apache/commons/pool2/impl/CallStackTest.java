/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.commons.pool2.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CallStackTest {

    private final CallStack stack;
    private final StringWriter writer = new StringWriter();

    public CallStackTest(final CallStack stack) {
        this.stack = stack;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
            new ThrowableCallStack("Test", false),
            new SecurityManagerCallStack("Test", false)
        };
    }

    @Test
    public void testPrintClearedStackTraceIsNoOp() throws Exception {
        stack.fillInStackTrace();
        stack.clear();
        stack.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString();
        assertEquals("", stackTrace);
    }

    @Test
    public void testPrintFilledStackTrace() throws Exception {
        stack.fillInStackTrace();
        stack.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString();
        assertTrue(stackTrace.contains(getClass().getName()));
    }
}