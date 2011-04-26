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

package org.apache.commons.pool2;

/**
 * A base implementation of <code>KeyedPoolableObjectFactory</code>.
 * <p>
 * All operations defined here are essentially no-op's.
 * </p>
 *
 * @see KeyedPoolableObjectFactory
 *
 * @param <K> The type of keys managed by this factory.
 * @param <V> Type of element managed by this factory.
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public abstract class BaseKeyedPoolableObjectFactory<K,V> implements KeyedPoolableObjectFactory<K,V> {
    /**
     * Create an instance that can be served by the pool.
     *
     * @param key the key used when constructing the object
     * @return an instance that can be served by the pool
     */
    public abstract V makeObject(K key)
        throws Exception;

    /**
     * Destroy an instance no longer needed by the pool.
     * <p>
     * The default implementation is a no-op.
     * </p>
     *
     * @param key the key used when selecting the instance
     * @param obj the instance to be destroyed
     */
    public void destroyObject(K key, V obj)
        throws Exception {
    }

    /**
     * Ensures that the instance is safe to be returned by the pool.
     * <p>
     * The default implementation always returns <tt>true</tt>.
     * </p>
     *
     * @param key the key used when selecting the object
     * @param obj the instance to be validated
     * @return always <code>true</code> in the default implementation
     */ 
    public boolean validateObject(K key, V obj) {
        return true;
    }

    /**
     * Reinitialize an instance to be returned by the pool.
     * <p>
     * The default implementation is a no-op.
     * </p>
     *
     * @param key the key used when selecting the object
     * @param obj the instance to be activated
     */
    public void activateObject(K key, V obj)
        throws Exception {
    }

    /**
     * Uninitialize an instance to be returned to the idle object pool.
     * <p>
     * The default implementation is a no-op.
     * </p>
     *
     * @param key the key used when selecting the object
     * @param obj the instance to be passivated
     */
    public void passivateObject(K key, V obj)
        throws Exception {
    }
}
