/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.commons.pool3.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CallStackTest {

    public static Stream<Arguments> data() {
        // @formatter:off
        return Stream.of(
                Arguments.arguments(new ThrowableCallStack("Test", false)),
                Arguments.arguments(new ThrowableCallStack("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", true)),
                Arguments.arguments(new SecurityManagerCallStack("Test", false)),
                Arguments.arguments(new SecurityManagerCallStack("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", true))
        );
        // @formatter:on
    }

    private final StringWriter writer = new StringWriter();

    @ParameterizedTest
    @MethodSource("data")
    void testPrintClearedStackTraceIsNoOp(final CallStack stack) {
        stack.fillInStackTrace();
        stack.clear();
        stack.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString();
        assertEquals("", stackTrace);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testPrintFilledStackTrace(final CallStack stack) {
        stack.fillInStackTrace();
        stack.printStackTrace(new PrintWriter(writer));
        final String stackTrace = writer.toString();
        assertTrue(stackTrace.contains(getClass().getName()));
    }
}
