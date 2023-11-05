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

package org.apache.commons.pool3.pool407;

import java.time.Duration;

import org.apache.commons.pool3.impl.BaseObjectPoolConfig;

/**
 * Tests POOL-407.
 */
final class Pool407Constants {

    static final int AWAIT_TERMINATION_SECONDS = 10;
    static final boolean BLOCK_WHEN_EXHAUSTED = true;
    static final int MAX_TOTAL = 1;
    static final int POOL_SIZE = 3;
    static final Duration WAIT_FOREVER = BaseObjectPoolConfig.DEFAULT_MAX_WAIT;
    static final Duration WAIT_SHORT = Duration.ofSeconds(1);

}
