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

import org.apache.commons.pool.PoolableObjectFactory;

import java.util.List;

/**
 * Implementation of <code>CompositeObjectPool</code> that provides full synchronization
 * needed for {@link WaitLimitManager} to work.
 *
 * @see CompositeObjectPool
 * @see CompositeObjectPoolFactory
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since #.#
 */
final class CompositeObjectPoolFullSync extends CompositeObjectPool {

    CompositeObjectPoolFullSync(final PoolableObjectFactory factory, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn) {
        super(factory, manager, lender, tracker, validateOnReturn);
    }

    CompositeObjectPoolFullSync(final PoolableObjectFactory factory, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn, final CompositeObjectPoolFactory.FactoryConfig factoryConfig) {
        super(factory, manager, lender, tracker, validateOnReturn, factoryConfig);
    }

    CompositeObjectPoolFullSync(final PoolableObjectFactory factory, final List pool, final Manager manager, final Lender lender, final Tracker tracker, final boolean validateOnReturn, final CompositeObjectPoolFactory.FactoryConfig factoryConfig) {
        super(factory, pool, manager, lender, tracker, validateOnReturn, factoryConfig);
    }

    protected boolean addObjectToPool(final Object obj) {
        final List pool = getPool();
        synchronized (pool) {
            // if the pool was closed between the assertOpen and the synchronize then discard returned objects
            if (isOpen()) {
                getManager().returnToPool(obj);
                return true;
            }
        }
        return false;
    }

    protected Object borrowObjectFromPool() throws Exception {
        final Object obj;
        final List pool = getPool();
        synchronized (pool) {
            obj = getManager().nextFromPool();

            // Must be synced else getNumActive() could be wrong in WaitLimitManager
            getTracker().borrowed(obj);
        }

        return obj;
    }

    protected boolean returnObjectToPool(final Object obj) {
        synchronized (getPool()) {
            // if the pool is closed, don't return objects
            if (isOpen()) {
                getTracker().returned(obj);
                getManager().returnToPool(obj);
                return true;
            }
        }
        return false;
    }

    protected void returnObjectToPoolManager(final Object obj) {
        synchronized (getPool()) {
            getManager().returnToPool(obj);
        }
    }
}