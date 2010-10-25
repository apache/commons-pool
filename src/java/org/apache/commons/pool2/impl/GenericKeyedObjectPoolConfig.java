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
 * Configuration class for {@link GenericKeyedObjectPool} and {@link GenericKeyedObjectPoolFactory} instances.
 *
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
public class GenericKeyedObjectPoolConfig extends AbstractGenericObjectPoolConfig {

    /**
     * The default cap on the the overall maximum number of objects that can
     * exist at one time.
     *
     * @see #getMaxTotal
     * @see #setMaxTotal
     */
    public static final int DEFAULT_MAX_TOTAL  = -1;

    /**
     * The cap on the total number of instances from the pool if non-positive.
     *
     * @see #getMaxTotal
     * @see #setMaxTotal
     */
    private int maxTotal = DEFAULT_MAX_TOTAL;

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

}
