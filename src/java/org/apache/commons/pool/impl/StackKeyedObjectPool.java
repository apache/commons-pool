/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.commons.pool.impl;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.pool.BaseKeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A simple, {@link java.util.Stack Stack}-based {@link KeyedObjectPool} implementation.
 * <p>
 * Given a {@link KeyedPoolableObjectFactory}, this class will maintain
 * a simple pool of instances.  A finite number of "sleeping"
 * or inactive instances is enforced, but when the pool is
 * empty, new instances are created to support the new load.
 * Hence this class places no limit on the number of "active"
 * instances created by the pool, but is quite useful for
 * re-using <tt>Object</tt>s without introducing
 * artificial limits.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.14 $ $Date: 2004/02/28 12:16:21 $ 
 */
public class StackKeyedObjectPool extends BaseKeyedObjectPool implements KeyedObjectPool {
    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object,java.lang.Object)}
     * before they can be {@link #borrowObject(java.lang.Object) borrowed}.
     */
    public StackKeyedObjectPool() {
        this((KeyedPoolableObjectFactory)null,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object,java.lang.Object)}
     * before they can be {@link #borrowObject(java.lang.Object) borrowed}.
     *
     * @param max cap on the number of "sleeping" instances in the pool
     */
    public StackKeyedObjectPool(int max) {
        this((KeyedPoolableObjectFactory)null,max,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object,java.lang.Object)}
     * before they can be {@link #borrowObject(java.lang.Object) borrowed}.
     *
     * @param max cap on the number of "sleeping" instances in the pool
     * @param init initial size of the pool (this specifies the size of the container,
     *             it does not cause the pool to be pre-populated.)
     */
    public StackKeyedObjectPool(int max, int init) {
        this((KeyedPoolableObjectFactory)null,max,init);
    }

    /**
     * Create a new <tt>SimpleKeyedObjectPool</tt> using
     * the specified <i>factory</i> to create new instances.
     *
     * @param factory the {@link KeyedPoolableObjectFactory} used to populate the pool
     */
    public StackKeyedObjectPool(KeyedPoolableObjectFactory factory) {
        this(factory,DEFAULT_MAX_SLEEPING);
    }

    /**
     * Create a new <tt>SimpleKeyedObjectPool</tt> using
     * the specified <i>factory</i> to create new instances.
     * capping the number of "sleeping" instances to <i>max</i>
     *
     * @param factory the {@link KeyedPoolableObjectFactory} used to populate the pool
     * @param max cap on the number of "sleeping" instances in the pool
     */
    public StackKeyedObjectPool(KeyedPoolableObjectFactory factory, int max) {
        this(factory,max,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new <tt>SimpleKeyedObjectPool</tt> using
     * the specified <i>factory</i> to create new instances.
     * capping the number of "sleeping" instances to <i>max</i>,
     * and initially allocating a container capable of containing
     * at least <i>init</i> instances.
     *
     * @param factory the {@link KeyedPoolableObjectFactory} used to populate the pool
     * @param max cap on the number of "sleeping" instances in the pool
     * @param init initial size of the pool (this specifies the size of the container,
     *             it does not cause the pool to be pre-populated.)
     */
    public StackKeyedObjectPool(KeyedPoolableObjectFactory factory, int max, int init) {
        _factory = factory;
        _maxSleeping = (max < 0 ? DEFAULT_MAX_SLEEPING : max);
        _initSleepingCapacity = (init < 1 ? DEFAULT_INIT_SLEEPING_CAPACITY : init);
        _pools = new HashMap();
        _activeCount = new HashMap();
    }

    public synchronized Object borrowObject(Object key) throws Exception {
        Object obj = null;
        Stack stack = (Stack)(_pools.get(key));
        if(null == stack) {
            stack = new Stack();
            stack.ensureCapacity( _initSleepingCapacity > _maxSleeping ? _maxSleeping : _initSleepingCapacity);
            _pools.put(key,stack);
        }
        try {
            obj = stack.pop();
            _totIdle--;
        } catch(Exception e) {
            if(null == _factory) {
                throw new NoSuchElementException();
            } else {
                obj = _factory.makeObject(key);
            }
        }
        if(null != obj && null != _factory) {
            _factory.activateObject(key,obj);
        }
        incrementActiveCount(key);
        return obj;
    }

    public synchronized void returnObject(Object key, Object obj) throws Exception {
        decrementActiveCount(key);
        if(null == _factory || _factory.validateObject(key,obj)) {
            Stack stack = (Stack)(_pools.get(key));
            if(null == stack) {
                stack = new Stack();
                stack.ensureCapacity( _initSleepingCapacity > _maxSleeping ? _maxSleeping : _initSleepingCapacity);
                _pools.put(key,stack);
            }
            if(null != _factory) {
                try {
                    _factory.passivateObject(key,obj);
                } catch(Exception e) {
                    _factory.destroyObject(key,obj);
                    return;
                }
            }
            if(stack.size() < _maxSleeping) {
                stack.push(obj);
                _totIdle++;
            } else {
                if(null != _factory) {
                    _factory.destroyObject(key,obj);
                }
            }
        } else {
            if(null != _factory) {
                _factory.destroyObject(key,obj);
            }
        }
    }

    public synchronized void invalidateObject(Object key, Object obj) throws Exception {
        decrementActiveCount(key);
        if(null != _factory) {
            _factory.destroyObject(key,obj);
        }
        notifyAll(); // _totalActive has changed
    }

    public void addObject(Object key) throws Exception {
        Object obj = _factory.makeObject(key);
        synchronized(this) {
            incrementActiveCount(key); // returnObject will decrement this
            returnObject(key,obj);
        }
    }

    public int getNumIdle() {
        return _totIdle;
    }

    public int getNumActive() {
        return _totActive;
    }

    public int getNumActive(Object key) {
        return getActiveCount(key);
    }

    public synchronized int getNumIdle(Object key) {
        try {
            return((Stack)(_pools.get(key))).size();
        } catch(Exception e) {
            return 0;
        }
    }

    public synchronized void clear() {
        Iterator it = _pools.keySet().iterator();
        while(it.hasNext()) {
            Object key = it.next();
            Stack stack = (Stack)(_pools.get(key));
            destroyStack(key,stack);
        }
        _totIdle = 0;
        _pools.clear();
        _activeCount.clear();
    }

    public synchronized void clear(Object key) {
        Stack stack = (Stack)(_pools.remove(key));
        destroyStack(key,stack);
    }

    private synchronized void destroyStack(Object key, Stack stack) {
        if(null == stack) {
            return;
        } else {
            if(null != _factory) {
                Enumeration enum = stack.elements();
                while(enum.hasMoreElements()) {
                    try {
                        _factory.destroyObject(key,enum.nextElement());
                    } catch(Exception e) {
                        // ignore error, keep destroying the rest
                    }
                }
            }
            _totIdle -= stack.size();
            _activeCount.remove(key);
            stack.clear();
        }
    }

    public synchronized String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getClass().getName());
        buf.append(" contains ").append(_pools.size()).append(" distinct pools: ");
        Iterator it = _pools.keySet().iterator();
        while(it.hasNext()) {
            Object key = it.next();
            buf.append(" |").append(key).append("|=");
            Stack s = (Stack)(_pools.get(key));
            buf.append(s.size());
        }
        return buf.toString();
    }

    public synchronized void close() throws Exception {
        clear();
        _pools = null;
        _factory = null;
        _activeCount = null;
    }

    public synchronized void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException {
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    private int getActiveCount(Object key) {
        try {
            return ((Integer)_activeCount.get(key)).intValue();
        } catch(NoSuchElementException e) {
            return 0;
        } catch(NullPointerException e) {
            return 0;
        }
    }

    private void incrementActiveCount(Object key) {
        _totActive++;
        Integer old = (Integer)(_activeCount.get(key));
        if(null == old) { 
            _activeCount.put(key,new Integer(1));
        } else {
            _activeCount.put(key,new Integer(old.intValue() + 1));
        }
    }

    private void decrementActiveCount(Object key) {
        _totActive--;
        Integer active = (Integer)(_activeCount.get(key));
        if(null == active) {
            // do nothing, either null or zero is OK
        } else if(active.intValue() <= 1) {
            _activeCount.remove(key);
        } else {
            _activeCount.put(key, new Integer(active.intValue() - 1));
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

    /** My named-set of pools. */
    protected HashMap _pools = null;

    /** My {@link KeyedPoolableObjectFactory}. */
    protected KeyedPoolableObjectFactory _factory = null;

    /** The cap on the number of "sleeping" instances in <i>each</i> pool. */
    protected int _maxSleeping = DEFAULT_MAX_SLEEPING;

    /** The initial capacity of each pool. */
    protected int _initSleepingCapacity = DEFAULT_INIT_SLEEPING_CAPACITY;

    /** Total number of object borrowed and not yet retuened for all pools */
    protected int _totActive = 0;

    /** Total number of objects "sleeping" for all pools */
    protected int _totIdle = 0;

    /** Number of active objects borrowed and not yet returned by pool */
    protected HashMap _activeCount = null;

}
