/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/ObjectPool.java,v 1.1 2001/04/14 16:40:49 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2001/04/14 16:40:49 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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
 * A simple {@link Object} pooling interface.
 * <p>
 * Example of use:
 * <table border="1" cellspacing="0" cellpadding="3" align="center" bgcolor="#FFFFFF"><tr><td><pre>
 * Object obj = <font color="#0000CC">null</font>;
 * <font color="#0000CC">try</font> {
 *    obj = pool.borrowObject();
 *    <font color="#00CC00">//...use the object...</font>
 * } <font color="#0000CC">catch</font>(Exception e) {
 *    <font color="#00CC00">//...handle any exceptions...</font>
 * } <font color="#0000CC">finally</font> {
 *    <font color="#00CC00">// make sure the object is returned to the pool</font>
 *    <font color="#0000CC">if</font>(<font color="#0000CC">null</font> != obj) {
 *       pool.returnObject(obj);
 *    }
 * }</pre></td></tr></table>
 *
 * @author Rodney Waldhoff
 * @version $Id: ObjectPool.java,v 1.1 2001/04/14 16:40:49 rwaldhoff Exp $
 *
 * @see KeyedObjectPool
 * @see ObjectPoolFactory
 * @see PoolableObjectFactory
 */
public interface ObjectPool {
    /**
     * Obtain an <tt>Object</tt> from my pool.
     * By contract, clients MUST return
     * the borrowed object using
     * {@link #returnObject(java.lang.Object) <tt>returnObject</tt>}
     * or a related method as defined in an implementation
     * or sub-interface.
     * <p>
     * The behaviour of this method when the pool has been exhausted
     * is not specified (although it may be specified by implementations).
     *
     * @return an <tt>Object</tt> from my pool.
     */
    public abstract Object borrowObject();

    /**
     * Return an <tt>Object</tt> to my pool.
     * By contract, <i>obj</i> MUST have been obtained
     * using {@link #borrowObject() <tt>borrowObject</tt>}
     * or a related method as defined in an implementation
     * or sub-interface.
     *
     * @param obj a {@link #borrowObject() borrowed} <tt>Object</tt> to be returned.
     */
    public abstract void returnObject(Object obj);

    /**
     * Return the number of <tt>Object</tt>s
     * currently idle in my pool, or
     * throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @return the number of <tt>Object</tt>s currently idle in my pool
     * @throws UnsupportedOperationException
     */
    public abstract int numIdle() throws UnsupportedOperationException;

    /**
     * Return the number of <tt>Object</tt>s
     * currently borrowed from my pool, or
     * throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @return the number of <tt>Object</tt>s currently borrowed in my pool
     * @throws UnsupportedOperationException
     */
    public abstract int numActive() throws UnsupportedOperationException;

    /**
     * Clears any objects sitting idle in the pool, releasing any
     * associated resources, or throws {@link UnsupportedOperationException}
     * if the pool cannot be cleared.
     *
     * @throws UnsupportedOperationException
     */
    public abstract void clear() throws UnsupportedOperationException;

    /**
     * Close this pool, and free any resources associated with it.
     */
    public abstract void close();

    /**
     * Sets the {@link PoolableObjectFactory factory} I use
     * to create new instances.
     * @param factory the {@link PoolableObjectFactory} I use to create new instances.
     */
    public abstract void setFactory(PoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException;
}
