/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/BaseKeyedObjectPool.java,v 1.4 2002/10/30 22:54:41 rwaldhoff Exp $
 * $Revision: 1.4 $
 * $Date: 2002/10/30 22:54:41 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
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
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.pool;

/**
 * A simple base impementation of {@link ObjectPool}.
 * All optional operations are implemented as throwing
 * {@link UnsupportedOperationException}.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.4 $ $Date: 2002/10/30 22:54:41 $
 */
public abstract class BaseKeyedObjectPool implements KeyedObjectPool {
    public abstract Object borrowObject(Object key) throws Exception;
    public abstract void returnObject(Object key, Object obj) throws Exception;
    public abstract void invalidateObject(Object key, Object obj) throws Exception;

    /** @deprecated Use {@link #getNumIdle} instead. Will be removed by release 2.0. */
    public final int numIdle(Object key) throws UnsupportedOperationException {
        return getNumIdle(key);
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumIdle(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Use {@link #getNumActive} instead. Will be removed by release 2.0. */
    public final int numActive(Object key) throws UnsupportedOperationException {
        return getNumActive(key);
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumActive(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Use {@link #getNumIdle} instead. Will be removed by release 2.0. */
    public final int numIdle() throws UnsupportedOperationException {
        return getNumIdle();
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumIdle() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /** @deprecated Use {@link #getNumActive} instead. Will be removed by release 2.0. */
    public final int numActive() throws UnsupportedOperationException {
        return getNumActive();
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumActive() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     */
    public void clear() throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     */
    public void clear(Object key)
    throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Does nothing this base implementation.
     */
    public void close() throws Exception {
    }


    /**
     * Not supported in this base implementation.
     */
    public void setFactory(KeyedPoolableObjectFactory factory)
    throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
