/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/impl/StackObjectPool.java,v $
 * $Revision: 1.14 $
 * $Date: 2003/10/09 21:45:56 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * http://www.apache.org/
 *
 */

package org.apache.commons.pool.impl;

import java.util.EmptyStackException;
import java.util.Enumeration;
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
 * @version $Revision: 1.14 $ $Date: 2003/10/09 21:45:56 $
 */
public class StackObjectPool extends BaseObjectPool implements ObjectPool {
    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed}.
     */
    public StackObjectPool() {
        this((PoolableObjectFactory)null,DEFAULT_MAX_SLEEPING,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed}.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     */
    public StackObjectPool(int maxIdle) {
        this((PoolableObjectFactory)null,maxIdle,DEFAULT_INIT_SLEEPING_CAPACITY);
    }

    /**
     * Create a new pool using
     * no factory. Clients must first populate the pool
     * using {@link #returnObject(java.lang.Object)}
     * before they can be {@link #borrowObject borrowed}.
     *
     * @param maxIdle cap on the number of "sleeping" instances in the pool
     * @param initIdleCapacity initial size of the pool (this specifies the size of the container,
     *             it does not cause the pool to be pre-populated.)
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
     * capping the number of "sleeping" instances to <i>max</i>
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
        try {
            obj = _pool.pop();
        } catch(EmptyStackException e) {
            if(null == _factory) {
                throw new NoSuchElementException();
            } else {
                obj = _factory.makeObject();
            }
        }
        if(null != _factory && null != obj) {
            _factory.activateObject(obj);
        }
        _numActive++;
        return obj;
    }

    public void returnObject(Object obj) throws Exception {
        assertOpen();
        boolean success = true;
        if(null != _factory) {
            if(!(_factory.validateObject(obj))) {
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

        synchronized(this) {
            _numActive--;
            if(_pool.size() >= _maxSleeping) {
                shouldDestroy = true;
            } else if(success) {
                _pool.push(obj);
            }
            notifyAll(); // _numActive has changed
        }

        if(shouldDestroy) { // by constructor, shouldDestroy is false when _factory is null
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    public synchronized void invalidateObject(Object obj) throws Exception {
        assertOpen();
        _numActive--;
        if(null != _factory ) {
            _factory.destroyObject(obj);
        }
        notifyAll(); // _numActive has changed
    }

    public int getNumIdle() {
        assertOpen();
        return _pool.size();
    }

    public int getNumActive() {
        assertOpen();
        return _numActive;
    }

    public synchronized void clear() {
        assertOpen();
        if(null != _factory) {
            Enumeration enum = _pool.elements();
            while(enum.hasMoreElements()) {
                try {
                    _factory.destroyObject(enum.nextElement());
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
            }
        }
        _pool.clear();
    }

    synchronized public void close() throws Exception {
        clear();
        _pool = null;
        _factory = null;
        super.close();
    }

    /**
     * Create an object, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     */
    public void addObject() throws Exception {
        Object obj = _factory.makeObject();
        synchronized(this) {
            _numActive++;   // A little slimy - must do this because returnObject decrements it.
            this.returnObject(obj);
        }
    }

    synchronized public void setFactory(PoolableObjectFactory factory) throws IllegalStateException {
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

    /** Number of object borrowed but not yet returned to the pool */
    protected int _numActive = 0;
}
