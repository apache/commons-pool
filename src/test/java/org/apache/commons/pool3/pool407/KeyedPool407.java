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

import org.apache.commons.pool3.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool3.impl.BaseObjectPoolConfig;
import org.apache.commons.pool3.impl.GenericKeyedObjectPool;

public final class KeyedPool407 extends GenericKeyedObjectPool<String, KeyedPool407Fixture, RuntimeException> {

    public KeyedPool407(final BaseKeyedPooledObjectFactory<String, KeyedPool407Fixture, RuntimeException> factory, final Duration maxWait) {
        super(factory, new KeyedPool407Config(BaseObjectPoolConfig.DEFAULT_MAX_WAIT));
    }

}
