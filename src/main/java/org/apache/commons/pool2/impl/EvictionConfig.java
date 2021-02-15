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

import java.time.Duration;

/**
 * This class is used by pool implementations to pass configuration information
 * to {@link EvictionPolicy} instances. The {@link EvictionPolicy} may also have
 * its own specific configuration attributes.
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * @since 2.0
 */
public class EvictionConfig {

    private static final Duration MAX_MILLIS_DURATION = Duration.ofMillis(Long.MAX_VALUE);
    private final Duration idleEvictTime;
    private final Duration idleSoftEvictTime;
    private final int minIdle;

    /**
     * Creates a new eviction configuration with the specified parameters.
     * Instances are immutable.
     *
     * @param poolIdleEvictTime Expected to be provided by
     *        {@link BaseGenericObjectPool#getMinEvictableIdleTimeMillis()}
     * @param poolIdleSoftEvictTime Expected to be provided by
     *        {@link BaseGenericObjectPool#getSoftMinEvictableIdleTimeMillis()}
     * @param minIdle Expected to be provided by
     *        {@link GenericObjectPool#getMinIdle()} or
     *        {@link GenericKeyedObjectPool#getMinIdlePerKey()}
     * @since 2.10.0
     */
    public EvictionConfig(final Duration poolIdleEvictTime, final Duration poolIdleSoftEvictTime, final int minIdle) {
        if (PoolImplUtils.isPositive(poolIdleEvictTime)) {
            idleEvictTime = poolIdleEvictTime;
        } else {
            idleEvictTime = MAX_MILLIS_DURATION;
        }
        if (PoolImplUtils.isPositive(poolIdleSoftEvictTime)) {
            idleSoftEvictTime = poolIdleSoftEvictTime;
        } else {
            idleSoftEvictTime = MAX_MILLIS_DURATION;
        }
        this.minIdle = minIdle;
    }

    /**
     * Creates a new eviction configuration with the specified parameters.
     * Instances are immutable.
     *
     * @param poolIdleEvictTime Expected to be provided by
     *        {@link BaseGenericObjectPool#getMinEvictableIdleTimeMillis()}
     * @param poolIdleSoftEvictTime Expected to be provided by
     *        {@link BaseGenericObjectPool#getSoftMinEvictableIdleTimeMillis()}
     * @param minIdle Expected to be provided by
     *        {@link GenericObjectPool#getMinIdle()} or
     *        {@link GenericKeyedObjectPool#getMinIdlePerKey()}
     * @deprecated Use {@link #EvictionConfig(Duration, Duration, int)}.
     */
    @Deprecated
    public EvictionConfig(final long poolIdleEvictTime, final long poolIdleSoftEvictTime, final int minIdle) {
        this(Duration.ofMillis(poolIdleEvictTime), Duration.ofMillis(poolIdleSoftEvictTime), minIdle);
    }

    /**
     * Gets the {@code idleEvictTime} for this eviction configuration
     * instance.
     * <p>
     * How the evictor behaves based on this value will be determined by the
     * configured {@link EvictionPolicy}.
     * </p>
     *
     * @return The {@code idleEvictTime} in milliseconds
     */
    public long getIdleEvictTime() {
        return idleEvictTime.toMillis();
    }

    /**
     * Gets the {@code idleEvictTime} for this eviction configuration
     * instance.
     * <p>
     * How the evictor behaves based on this value will be determined by the
     * configured {@link EvictionPolicy}.
     * </p>
     *
     * @return The {@code idleEvictTime}.
     * @since 2.10.0
     */
    public Duration getIdleEvictTimeDuration() {
        return idleEvictTime;
    }

    /**
     * Gets the {@code idleSoftEvictTime} for this eviction configuration
     * instance.
     * <p>
     * How the evictor behaves based on this value will be determined by the
     * configured {@link EvictionPolicy}.
     * </p>
     *
     * @return The (@code idleSoftEvictTime} in milliseconds
     */
    public long getIdleSoftEvictTime() {
        return idleSoftEvictTime.toMillis();
    }

    /**
     * Gets the {@code idleSoftEvictTime} for this eviction configuration
     * instance.
     * <p>
     * How the evictor behaves based on this value will be determined by the
     * configured {@link EvictionPolicy}.
     * </p>
     *
     * @return The (@code idleSoftEvictTime} in milliseconds
     */
    public Duration getIdleSoftEvictTimeDuration() {
        return idleSoftEvictTime;
    }

    /**
     * Gets the {@code minIdle} for this eviction configuration instance.
     * <p>
     * How the evictor behaves based on this value will be determined by the
     * configured {@link EvictionPolicy}.
     * </p>
     *
     * @return The {@code minIdle}
     */
    public int getMinIdle() {
        return minIdle;
    }

    /**
     * @since 2.4
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EvictionConfig [idleEvictTime=");
        builder.append(idleEvictTime);
        builder.append(", idleSoftEvictTime=");
        builder.append(idleSoftEvictTime);
        builder.append(", minIdle=");
        builder.append(minIdle);
        builder.append("]");
        return builder.toString();
    }
}
