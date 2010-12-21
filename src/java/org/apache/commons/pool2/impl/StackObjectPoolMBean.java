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
 * The <code>Stack</code>-based <code>ObjectPool</code> JMX interface.
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface StackObjectPoolMBean {

    /**
     * Return the number of instances
     * currently idle in this pool.
     *
     * @return the number of instances currently idle in this pool
     */
    int getNumIdle();

    /**
     * Return the number of instances currently borrowed from this pool.
     *
     * @return the number of instances currently borrowed from this pool
     */
    int getNumActive();

    /**
     * Returns the maximum number of idle instances in the pool.
     * 
     * @return maxSleeping
     * @since 1.5.5
     */
    int getMaxSleeping();

    /**
     * Sets the maximum number of idle instances in the pool.
     *
     * @param maxSleeping
     * @since 2.0
     */
    void setMaxSleeping(int maxSleeping);

}
