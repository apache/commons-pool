/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

import java.io.Serializable;

/**
 * Adapter to let an {@link ObjectPool} use a {@link KeyedPoolableObjectFactory}. Before first use
 * {@link #setCompositeKeyedObjectPool(CompositeKeyedObjectPool)} must be called so that a reference to the
 * {@link ThreadLocal} used to pass the key through the {@link ObjectPool} can be aquired.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
final class KeyedPoolableObjectFactoryAdapter implements PoolableObjectFactory, Serializable {

    private static final long serialVersionUID = 8664321206626066192L;

    /**
     * The keyed object factory we're adapting.
     */
    // XXX: Add better handling of when this instance is not Serializable
    private final KeyedPoolableObjectFactory delegate;

    /**
     * Where we get the current key before delegating to the keyed object factory.
     * On deserialization this will be set by {@link CompositeKeyedObjectPool}'s constructor.
     */
    private transient ThreadLocal keys;

    KeyedPoolableObjectFactoryAdapter(final KeyedPoolableObjectFactory delegate) {
        this.delegate = delegate;
    }

    /**
     * The keyed object pool that uses this adapter.
     *
     * @param pool the keyed object pool that uses this adapter.
     */
    public void setCompositeKeyedObjectPool(final CompositeKeyedObjectPool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool must not be null.");
        }
        if (pool.getKeys() == null) {
            throw new IllegalArgumentException("pool's keys must not be null.");
        }
        keys = pool.getKeys();
    }

    /**
     * Creates an instance that can be returned by the pool.
     *
     * @return an instance that can be returned by the pool.
     * @throws Exception when the delegate does.
     */
    public Object makeObject() throws Exception {
        return delegate.makeObject(keys.get());
    }

    /**
     * Destroys an instance no longer needed by the pool.
     *
     * @param obj the instance to be destroyed
     * @throws Exception when the delegate does.
     */
    public void destroyObject(final Object obj) throws Exception {
        delegate.destroyObject(keys.get(), obj);
    }

    /**
     * Ensures that the instance is safe to be returned by the pool.
     * Returns <tt>false</tt> if this object should be destroyed.
     * @param obj the instance to be validated
     * @return <tt>false</tt> if this <i>obj</i> is not valid and should
     *         be dropped from the pool, <tt>true</tt> otherwise.
     */
    public boolean validateObject(final Object obj) {
        return delegate.validateObject(keys.get(), obj);
    }

    /**
     * Reinitialize an instance to be returned by the pool.
     *
     * @param obj the instance to be activated
     * @throws Exception when the delegate does.
     */
    public void activateObject(final Object obj) throws Exception {
        delegate.activateObject(keys.get(), obj);
    }

    /**
     * Uninitialize an instance to be returned to the pool.
     *
     * @param obj the instance to be passivated
     * @throws Exception when the delegate does.
     */
    public void passivateObject(final Object obj) throws Exception {
        delegate.passivateObject(keys.get(), obj);
    }

    /**
     * This adapter's delegate keyed object factory. Needed by {@link CompositeKeyedObjectPoolFactory#getKeyedFactory()}.
     *
     * @return this adapter's delegate keyed object factory.
     */
    KeyedPoolableObjectFactory getDelegate() {
        return delegate;
    }
}