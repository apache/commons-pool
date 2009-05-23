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

package org.apache.commons.pool.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.pool.BaseObjectPool;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * A simple, {@link java.util.Stack Stack}-based {@link ObjectPool} implementation.
 * <p>
 * Given a {@link PoolableObjectFactory}, this class will maintain
 * a simple pool of instances.  A finite number of "sleeping"
 * or idle instances is enforced, but when the pool is
 * empty, new instances are created to support the new load.
 * Hence this class places no limit on the number of "active"
 * instances created by the pool, but is quite useful for
 * re-using <tt>Object</tt>s without introducing
 * artificial limits.
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class StackObjectPool extends BaseObjectPool implements ObjectPool {
    /**
     * Create a new pool using
     * no factory.
     * Clients must first {@link #setFactory(PoolableObjectFactory) set the factory}
     * else this pool will not behave correctly.
     * Clients may first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed} but this useage is <strong>discouraged</strong>.
     *
     * @see #StackObjectPool(PoolableObjectFactory)
     */
    public StackObjectPool() {
        this((PoolableObjectFactory)null,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory.
     * Clients must first {@link #setFactory(PoolableObjectFactory) set the factory}
     * else this pool will not behave correctly.
     * Clients may first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed} but this useage is <strong>discouraged</strong>.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     * @see #StackObjectPool(PoolableObjectFactory, int)
     */
    public StackObjectPool(int maxIdle) {
        this((PoolableObjectFactory)null,maxIdle,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory.
     * Clients must first {@link #setFactory(PoolableObjectFactory) set the factory}
     * else this pool will not behave correctly.
     * Clients may first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed} but this useage is <strong>discouraged</strong>.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     * @param initIdleCapacity initial size of the pool (this specifies the size of the container,
     *             it does not cause the pool to be pre-populated.)
     * @see #StackObjectPool(PoolableObjectFactory, int, int)
     */
    public StackObjectPool(int maxIdle, int initIdleCapacity) {
        this((PoolableObjectFactory)null,maxIdle,initIdleCapacity);
    }

    /**
     * Create a new <tt>StackObjectPool</tt> using
     * the specified <i>factory</i> to create new instances.
     *
     * @param factory the {@link PoolableObjectFactory} used to populate the pool
     */
    public StackObjectPool(PoolableObjectFactory factory) {
        this(factory,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new <tt>SimpleObjectPool</tt> using
     * the specified <i>factory</i> to create new instances,
     * capping the number of "sleeping" instances to <i>max</i>.
     *
     * @param factory the {@link PoolableObjectFactory} used to populate the pool
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     */
    public StackObjectPool(PoolableObjectFactory factory, int maxIdle) {
        this(factory,maxIdle,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new <tt>SimpleObjectPool</tt> using
     * the specified <i>factory</i> to create new instances,
     * capping the number of "sleeping" instances to <i>max</i>,
     * and initially allocating a container capable of containing
     * at least <i>init</i> instances.
     *
     * @param factory the {@link PoolableObjectFactory} used to populate the pool
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     * @param initIdleCapacity initial size of the pool (this specifies the size of the container,
     *             it does not cause the pool to be pre-populated.)
     */
    public StackObjectPool(PoolableObjectFactory factory, int maxIdle, int initIdleCapacity) {
        _factory = factory;
        _maxSleeping = (maxIdle < 0 ? DEFAULT_MAX_SLEEPING : maxIdle);
        int initcapacity = (initIdleCapacity < 1 ? DEFAULT_INIT_SLEEPING_CAPACITY : initIdleCapacity);
        _pool = new Stack();
        _pool.ensureCapacity( initcapacity > _maxSleeping ? _maxSleeping : initcapacity);
    }

    public synchronized Object borrowObject() throws Exception {
        assertOpen();
        Object obj = null;
        boolean newlyCreated = false;
        while (null == obj) {
            if (!_pool.empty()) {
                obj = _pool.pop();
            } else {
                if(null == _factory) {
                    throw new NoSuchElementException();
                } else {
                    obj = _factory.makeObject();
                    newlyCreated = true;
                  if (obj == null) {
                    throw new NoSuchElementException("PoolableObjectFactory.makeObject() returned null.");
                  }
                }
            }
            if (null != _factory && null != obj) {
                try {
                    _factory.activateObject(obj);
                    if (!_factory.validateObject(obj)) {
                        throw new Exception("ValidateObject failed");
                    }
                } catch (Throwable t) {
                    try {
                        _factory.destroyObject(obj);
                    } catch (Throwable t2) {
                        // swallowed
                    } finally {
                        obj = null;
                    }
                    if (newlyCreated) {
                        throw new NoSuchElementException(
                            "Could not create a validated object, cause: " +
                            t.getMessage());
                    }
                }
            }
        }
        _numActive++;
        return obj;
    }

    public synchronized void returnObject(Object obj) throws Exception {
        boolean success = !isClosed();
        if(null != _factory) {
            if(!_factory.validateObject(obj)) {
                success = false;
            } else {
                try {
                    _factory.passivateObject(obj);
                } catch(Exception e) {
                    success = false;
                }
            }
        }

        boolean shouldDestroy = !success;

        _numActive--;
        if (success) {
            Object toBeDestroyed = null;
            if(_pool.size() >= _maxSleeping) {
                shouldDestroy = true;
                toBeDestroyed = _pool.remove(0); // remove the stalest object
            }
            _pool.push(obj);
            obj = toBeDestroyed; // swap returned obj with the stalest one so it can be destroyed
        }
        notifyAll(); // _numActive has changed

        if(shouldDestroy) { // by constructor, shouldDestroy is false when _factory is null
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    public synchronized void invalidateObject(Object obj) throws Exception {
        _numActive--;
        if (null != _factory) {
            _factory.destroyObject(obj);
        }
        notifyAll(); // _numActive has changed
    }

    /**
     * Return the number of instances
     * currently idle in this pool.
     *
     * @return the number of instances currently idle in this pool
     */
    public synchronized int getNumIdle() {
        return _pool.size();
    }

    /**
     * Return the number of instances currently borrowed from this pool.
     *
     * @return the number of instances currently borrowed from this pool
     */
    public synchronized int getNumActive() {
        return _numActive;
    }

    /**
     * Clears any objects sitting idle in the pool.
     */
    public synchronized void clear() {
        if(null != _factory) {
            Iterator it = _pool.iterator();
            while(it.hasNext()) {
                try {
                    _factory.destroyObject(it.next());
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
            }
        }
        _pool.clear();
    }

    /**
     * Close this pool, and free any resources associated with it.
     * <p>
     * Calling {@link #addObject} or {@link #borrowObject} after invoking
     * this method on a pool will cause them to throw an
     * {@link IllegalStateException}.
     * </p>
     *
     * @throws Exception <strong>deprecated</strong>: implementations should silently fail if not all resources can be freed.
     */
    public void close() throws Exception {
        super.close();
        clear();
    }

    /**
     * Create an object, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     * @throws Exception when the {@link #_factory} has a problem creating an object.
     */
    public synchronized void addObject() throws Exception {
        assertOpen();
        if (_factory == null) {
            throw new IllegalStateException("Cannot add objects without a factory.");
        }
        Object obj = _factory.makeObject();

        boolean success = true;
        if(!_factory.validateObject(obj)) {
            success = false;
        } else {
            _factory.passivateObject(obj);
        }

        boolean shouldDestroy = !success;

        if (success) {
            Object toBeDestroyed = null;
            if(_pool.size() >= _maxSleeping) {
                shouldDestroy = true;
                toBeDestroyed = _pool.remove(0); // remove the stalest object
            }
            _pool.push(obj);
            obj = toBeDestroyed; // swap returned obj with the stalest one so it can be destroyed
        }
        notifyAll(); // _numIdle has changed

        if(shouldDestroy) { // by constructor, shouldDestroy is false when _factory is null
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    /**
     * Sets the {@link PoolableObjectFactory factory} this pool uses
     * to create new instances. Trying to change
     * the <code>factory</code> while there are borrowed objects will
     * throw an {@link IllegalStateException}.
     *
     * @param factory the {@link PoolableObjectFactory} used to create new instances.
     * @throws IllegalStateException when the factory cannot be set at this time
     */
    public synchronized void setFactory(PoolableObjectFactory factory) throws IllegalStateException {
        assertOpen();
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    /** The default cap on the number of "sleeping" instances in the pool. */
    protected static final int DEFAULT_MAX_SLEEPING  = 8;

    /**
     * The default initial size of the pool
     * (this specifies the size of the container, it does not
     * cause the pool to be pre-populated.)
     */
    protected static final int DEFAULT_INIT_SLEEPING_CAPACITY = 4;

    /** My pool. */
    protected Stack _pool = null;

    /** My {@link PoolableObjectFactory}. */
    protected PoolableObjectFactory _factory = null;

    /** The cap on the number of "sleeping" instances in the pool. */
    protected int _maxSleeping = DEFAULT_MAX_SLEEPING;

    /** Number of object borrowed but not yet returned to the pool. */
    protected int _numActive = 0;
}
