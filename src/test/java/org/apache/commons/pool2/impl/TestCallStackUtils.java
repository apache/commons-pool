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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CallStackUtils}.
 */
public class TestCallStackUtils {

    private static final String MESSAGE_FORMAT = "'Timestamp:' yyyy-MM-dd HH:mm:ss Z";

    private void assertNewCallStack(final CallStack callStack) {
        callStack.fillInStackTrace();
        final StringWriter out = new StringWriter();
        callStack.printStackTrace(new PrintWriter(out));
        assertFalse(out.toString().isEmpty());
        callStack.clear();
        out.getBuffer().setLength(0);
        callStack.printStackTrace(new PrintWriter(out));
        assertTrue(out.toString().isEmpty());
    }

    @Test
    public void testNewCallStack2() {
        assertNewCallStack(CallStackUtils.newCallStack(MESSAGE_FORMAT, false));
        assertNewCallStack(CallStackUtils.newCallStack(MESSAGE_FORMAT, true));
    }

    @Test
    public void testNewCallStack3() {
        assertNewCallStack(CallStackUtils.newCallStack(MESSAGE_FORMAT, false, false));
        assertNewCallStack(CallStackUtils.newCallStack(MESSAGE_FORMAT, false, true));
    }
}
