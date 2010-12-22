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

import java.util.Map;

/**
 * The <code>Stack</code>-based <code>KeyedObjectPool</code> JMX interface.
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public interface StackKeyedObjectPoolMBean<K> extends StackObjectPoolMBean {

    /**
     * Returns the number of instances currently borrowed from but not yet returned
     * to the pool corresponding to the given <code>key</code>.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently borrowed in this pool
     */
    int getNumActive(K key);

    /**
     * Returns the number of instances corresponding to the given <code>key</code> currently idle in this pool.
     *
     * @param key the key to query
     * @return the number of instances corresponding to the given <code>key</code> currently idle in this pool
     */
    int getNumIdle(K key);

    /**
     * @return the initial capacity of each pool.
     */
    int getInitSleepingCapacity();

    /**
     * @return the _totActive
     */
    int getTotActive();

    /**
     * @return the _totIdle
     */
    int getTotIdle();

    /**
     * @return the _activeCount
     */
    Map<K, Integer> getActiveCount();

}
