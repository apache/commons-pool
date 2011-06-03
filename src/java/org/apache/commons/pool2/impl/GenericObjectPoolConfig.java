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

import org.apache.commons.pool2.PoolableObjectFactory;

/**
 * A simple "struct" encapsulating the configuration for a
 * {@link GenericObjectPool}.
 * 
 * @since Pool 2.0
 */
public class GenericObjectPoolConfig extends BaseObjectPoolConfig {

    /**
     * The default value for {@link #getSoftMinEvictableIdleTimeMillis}.
     */
    public static final long DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME_MILLIS = -1;

    
    private PoolableObjectFactory<?> factory = null;

    private long softMinEvictableIdleTimeMillis =
        DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;


    public PoolableObjectFactory<?> getFactory() {
        return factory;
    }

    public void setFactory(PoolableObjectFactory<?> factory) {
        this.factory = factory;
    }
    
    public long getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    public void setSoftMinEvictableIdleTimeMillis(
            long softMinEvictableIdleTimeMillis) {
        this.softMinEvictableIdleTimeMillis = softMinEvictableIdleTimeMillis;
    }
}
