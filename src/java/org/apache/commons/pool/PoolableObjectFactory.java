/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//pool/src/java/org/apache/commons/pool/PoolableObjectFactory.java,v 1.3 2002/03/17 14:55:21 rwaldhoff Exp $
 * $Revision: 1.3 $
 * $Date: 2002/03/17 14:55:21 $
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
 * An interface defining life-cycle methods for
 * instances to be used in an
 * {@link ObjectPool ObjectPool}.
 * <p>
 * By contract, when an {@link ObjectPool ObjectPool}
 * delegates to a <tt>PoolableObjectFactory</tt>,
 * <ol>
 *  <li>
 *   {@link #makeObject makeObject} 
 *   is called  whenever a new instance is needed.
 *  </li>
 *  <li>
 *   {@link #activateObject activateObject} 
 *   is invoked on every instance before it is returned from the
 *   pool.
 *  </li>
 *  <li>
 *   {@link #passivateObject passivateObject} 
 *   is invoked on every instance when it is returned to the
 *   pool.
 *  </li>
 *  <li>
 *   {@link #destroyObject destroyObject} 
 *   is invoked on every instance when it is being "dropped" from the
 *   pool (whether due to the response from
 *   {@link #validateObject validateObject}, or
 *   for reasons specific to the pool implementation.)
 *  </li>
 *  <li>
 *   {@link #validateObject validateObject} 
 *   is invoked in an implementation-specific fashion to determine if an instance
 *   is still valid to be returned by the pool.
 *   It will only be invoked on an {@link #activateObject "activated"}
 *   instance.
 *  </li>
 * </ol>
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.3 $ $Date: 2002/03/17 14:55:21 $ 
 *
 * @see ObjectPool
 */
public interface PoolableObjectFactory {
  /**
   * Creates an instance that can be returned by the pool.
   * @return an instance that can be returned by the pool.
   */
  public abstract Object makeObject() throws Exception;

  /**
   * Destroys an instance no longer needed by the pool.
   * @param obj the instance to be destroyed
   */
  public abstract void destroyObject(Object obj) throws Exception;

  /**
   * Ensures that the instance is safe to be returned by the pool.
   * Returns <tt>false</tt> if this object should be destroyed.
   * @param obj the instance to be validated
   * @return <tt>false</tt> if this <i>obj</i> is not valid and should
   *         be dropped from the pool, <tt>true</tt> otherwise.
   */
  public abstract boolean validateObject(Object obj);

  /**
   * Reinitialize an instance to be returned by the pool.
   * @param obj the instance to be activated
   */
  public abstract void activateObject(Object obj) throws Exception;

  /**
   * Uninitialize an instance to be returned to the pool.
   * @param obj the instance to be passivated
   */
  public abstract void passivateObject(Object obj) throws Exception;
}
