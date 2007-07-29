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

package org.apache.commons.pool.composite;

import java.io.Serializable;

/**
 * Base class for all {@link Manager}s that limit the number of active objects associate with the pool.
 *
 * @see LimitPolicy
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
abstract class ActiveLimitManager extends DelegateManager implements Serializable {

    private static final long serialVersionUID = 917380099264820020L;

    /**
     * Maximum number of objects activated from this object pool at one time.
     */
    private int maxActive = 0;

    /**
     * Create a manager that limits the number of active objects borrowed from the pool.
     *
     * @param delegate the manager to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    protected ActiveLimitManager(final Manager delegate) throws IllegalArgumentException {
        super(delegate);
    }

    /**
     * Maximum number of active objects from this pool.
     *
     * @return maximum number of active objects from this pool.
     */
    protected final int getMaxActive() {
        return maxActive;
    }

    /**
     * Maximum number of active objects from this pool.
     *
     * @param maxActive maximum number of active objects from this pool.
     */
    final void setMaxActive(final int maxActive) {
        this.maxActive = maxActive;
    }
}