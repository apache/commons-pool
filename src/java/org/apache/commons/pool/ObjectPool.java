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

package org.apache.commons.pool;

/**
 * A pooling interface.
 * <p>
 * <code>ObjectPool</code> defines a trivially simple pooling interface. The only 
 * required methods are {@link #borrowObject borrowObject} and {@link #returnObject returnObject}.
 * <p>
 * Example of use:
 * <table border="1" cellspacing="0" cellpadding="3" align="center" bgcolor="#FFFFFF"><tr><td><pre>
 * Object obj = <font color="#0000CC">null</font>;
 * 
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
 * See {@link org.apache.commons.pool.BaseObjectPool BaseObjectPool} for a simple base implementation.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.11 $ $Date: 2004/02/28 11:46:33 $ 
 *
 */
public interface ObjectPool {
    /**
     * Obtain an instance from my pool.
     * By contract, clients MUST return
     * the borrowed instance using
     * {@link #returnObject(java.lang.Object) returnObject}
     * or a related method as defined in an implementation
     * or sub-interface.
     * <p>
     * The behaviour of this method when the pool has been exhausted
     * is not specified (although it may be specified by implementations).
     *
     * @return an instance from my pool.
     */
    Object borrowObject() throws Exception;

    /**
     * Return an instance to my pool.
     * By contract, <i>obj</i> MUST have been obtained
     * using {@link #borrowObject() borrowObject}
     * or a related method as defined in an implementation
     * or sub-interface.
     *
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    void returnObject(Object obj) throws Exception;

    /**
     * Invalidates an object from the pool
     * By contract, <i>obj</i> MUST have been obtained
     * using {@link #borrowObject() borrowObject}
     * or a related method as defined in an implementation
     * or sub-interface.
     * <p>
     * This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.
     * If the connection should be validated before or after borrowing,
     * then the {@link PoolableObjectFactory#validateObject} method should be
     * used instead.
     *
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    void invalidateObject(Object obj) throws Exception;

    /**
     * Create an object using my {@link #setFactory factory} or other
     * implementation dependent mechanism, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     * (Optional operation).
     */
    void addObject() throws Exception;

    /**
     * Return the number of instances
     * currently idle in my pool (optional operation).  
     * This may be considered an approximation of the number
     * of objects that can be {@link #borrowObject borrowed}
     * without creating any new instances.
     *
     * @return the number of instances currently idle in my pool
     * @throws UnsupportedOperationException if this implementation does not support the operation
     */
    int getNumIdle() throws UnsupportedOperationException;

    /**
     * Return the number of instances
     * currently borrowed from my pool 
     * (optional operation).
     *
     * @return the number of instances currently borrowed in my pool
     * @throws UnsupportedOperationException if this implementation does not support the operation
     */
    int getNumActive() throws UnsupportedOperationException;

    /**
     * Clears any objects sitting idle in the pool, releasing any
     * associated resources (optional operation).
     *
     * @throws UnsupportedOperationException if this implementation does not support the operation
     */
    void clear() throws Exception, UnsupportedOperationException;

    /**
     * Close this pool, and free any resources associated with it.
     */
    void close() throws Exception;

    /**
     * Sets the {@link PoolableObjectFactory factory} I use
     * to create new instances (optional operation).
     * @param factory the {@link PoolableObjectFactory} I use to create new instances.
     *
     * @throws IllegalStateException when the factory cannot be set at this time
     * @throws UnsupportedOperationException if this implementation does not support the operation
     */
    void setFactory(PoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException;
}
