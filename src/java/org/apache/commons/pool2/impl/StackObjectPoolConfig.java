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

/**
 * 
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
public class StackObjectPoolConfig {

    /**
     * The cap on the number of "sleeping" instances in the pool.
     */
    public static final int DEFAULT_MAX_SLEEPING  = 8;

    /**
     * The default initial size of the pool
     * (this specifies the size of the container, it does not
     * cause the pool to be pre-populated.)
     */
    public static final int DEFAULT_INIT_SLEEPING_CAPACITY = 4;

    /**
     * cap on the number of "sleeping" instances in the pool
     */
    private final int maxSleeping;

    /**
     * initial size of the pool (this specifies the size of the container,
     * it does not cause the pool to be pre-populated.)
     */
    private final int initIdleCapacity;

    /**
     * Creates a new StackObjectPoolConfig instance, resetting the parameters
     * if they are out of range.
     *
     * If maxSleeping parameter is less than 0, the configuration instance takes
     * the {@link #DEFAULT_MAX_SLEEPING} value.
     *
     * If initIdleCapacity parameter is less than 1, the configuration instance takes
     * the {@link #DEFAULT_INIT_SLEEPING_CAPACITY} value.
     *
     * @param maxSleeping
     * @param initIdleCapacity
     */
    private StackObjectPoolConfig(final int maxSleeping, final int initIdleCapacity) {
        this.maxSleeping = (maxSleeping < 0 ? DEFAULT_MAX_SLEEPING : maxSleeping);
        this.initIdleCapacity = (initIdleCapacity < 1 ? DEFAULT_INIT_SLEEPING_CAPACITY : initIdleCapacity);
    }

    public int getMaxSleeping() {
        return maxSleeping;
    }

    public int getInitIdleCapacity() {
        return initIdleCapacity;
    }

    /**
     * Helper class to easily build {@link StackObjectPoolConfig} instances.
     */
    public static final class Builder {

        /**
         * cap on the number of "sleeping" instances in the pool
         */
        private int maxSleeping = DEFAULT_MAX_SLEEPING;

        /**
         * initial size of the pool (this specifies the size of the container,
         * it does not cause the pool to be pre-populated.)
         */
        private int initIdleCapacity = DEFAULT_INIT_SLEEPING_CAPACITY;

        /**
         * Set the number of "sleeping" instances in the pool.
         *
         * @param maxSleeping the number of "sleeping" instances in the pool.
         * @return this builder instance.
         */
        public Builder setMaxSleeping(int maxSleeping) {
            this.maxSleeping = maxSleeping;
            return this;
        }

        /**
         * Set the default initial size of the pool.
         *
         * @param initIdleCapacity the default initial size of the pool.
         * @return this builder instance.
         */
        public Builder setInitIdleCapacity(int initIdleCapacity) {
            this.initIdleCapacity = initIdleCapacity;
            return this;
        }

        /**
         * Creates a {@link StackObjectPoolConfig} instance.
         *
         * @return the created {@link StackObjectPoolConfig} instance.
         */
        public StackObjectPoolConfig createConfig() {
            return new StackObjectPoolConfig(this.maxSleeping, this.initIdleCapacity);
        }

    }

}
