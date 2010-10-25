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
    private int maxSleeping = DEFAULT_MAX_SLEEPING;

    /**
     * initial size of the pool (this specifies the size of the container,
     * it does not cause the pool to be pre-populated.)
     */
    private int initIdleCapacity = DEFAULT_INIT_SLEEPING_CAPACITY;

    public int getMaxSleeping() {
        return maxSleeping;
    }

    public void setMaxSleeping(int maxSleeping) {
        this.maxSleeping = (maxSleeping < 0 ? DEFAULT_MAX_SLEEPING : maxSleeping);
    }

    public int getInitIdleCapacity() {
        return initIdleCapacity;
    }

    public void setInitIdleCapacity(int initIdleCapacity) {
        this.initIdleCapacity = (initIdleCapacity < 1 ? DEFAULT_INIT_SLEEPING_CAPACITY : initIdleCapacity);
    }

}
