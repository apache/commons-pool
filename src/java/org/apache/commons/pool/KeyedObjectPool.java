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
 * A  "keyed" pooling interface.
 * <p>
 * A keyed pool pools instances of multiple types. Each
 * type may be accessed using an arbitrary key.
 * <p>
 * Example of use:
 * <table border="1" cellspacing="0" cellpadding="3" align="center" bgcolor="#FFFFFF"><tr><td><pre>
 * Object obj = <font color="#0000CC">null</font>;
 * Object key = <font color="#CC0000">"Key"</font>;
 *
 * <font color="#0000CC">try</font> {
 *    obj = pool.borrowObject(key);
 *    <font color="#00CC00">//...use the object...</font>
 * } <font color="#0000CC">catch</font>(Exception e) {
 *    <font color="#00CC00">//...handle any exceptions...</font>
 * } <font color="#0000CC">finally</font> {
 *    <font color="#00CC00">// make sure the object is returned to the pool</font>
 *    <font color="#0000CC">if</font>(<font color="#0000CC">null</font> != obj) {
 *       pool.returnObject(key,obj);
 *    }
 * }</pre></td></tr></table>
 *
 * <p>
 * {@link KeyedObjectPool} implementations <i>may</i> choose to store at most
 * one instance per key value, or may choose to maintain a pool of instances
 * for each key (essentially creating a {@link java.util.Map Map} of
 * {@link ObjectPool pools}).
 * </p>
 *
 * @see KeyedPoolableObjectFactory
 * @see KeyedObjectPoolFactory
 * @see ObjectPool
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.14 $ $Date: 2004/02/28 12:16:21 $
 */
public interface KeyedObjectPool {
    /**
     * Obtain an instance from my pool
     * for the specified <i>key</i>.
     * By contract, clients MUST return
     * the borrowed object using
     * {@link #returnObject(java.lang.Object,java.lang.Object) <tt>returnObject</tt>},
     * or a related method as defined in an implementation
     * or sub-interface,
     * using a <i>key</i> that is equivalent to the one used to
     * borrow the instance in the first place.
     *
     * @param key the key used to obtain the object
     * @return an instance from my pool.
     */
    Object borrowObject(Object key) throws Exception;

    /**
     * Return an instance to my pool.
     * By contract, <i>obj</i> MUST have been obtained
     * using {@link #borrowObject(java.lang.Object) <tt>borrowObject</tt>}
     * or a related method as defined in an implementation
     * or sub-interface 
     * using a <i>key</i> that is equivalent to the one used to
     * borrow the <tt>Object</tt> in the first place.
     *
     * @param key the key used to obtain the object
     * @param obj a {@link #borrowObject(java.lang.Object) borrowed} instance to be returned.
     */
    void returnObject(Object key, Object obj) throws Exception;

    /**
     * Invalidates an object from the pool
     * By contract, <i>obj</i> MUST have been obtained
     * using {@link #borrowObject borrowObject}
     * or a related method as defined in an implementation
     * or sub-interface 
     * using a <i>key</i> that is equivalent to the one used to
     * borrow the <tt>Object</tt> in the first place.
     * <p>
     * This method should be used when an object that has been borrowed
     * is determined (due to an exception or other problem) to be invalid.
     * If the connection should be validated before or after borrowing,
     * then the {@link PoolableObjectFactory#validateObject} method should be
     * used instead.
     *
     * @param obj a {@link #borrowObject borrowed} instance to be returned.
     */
    void invalidateObject(Object key, Object obj) throws Exception;

    /**
     * Create an object using my {@link #setFactory factory} or other
     * implementation dependent mechanism, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.
     * (Optional operation).
     */
    void addObject(Object key) throws Exception;

    /**
     * Returns the number of instances
     * corresponding to the given <i>key</i>
     * currently idle in my pool (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @param key the key
     * @return the number of instances corresponding to the given <i>key</i> currently idle in my pool
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    int getNumIdle(Object key) throws UnsupportedOperationException;

    /**
     * Returns the number of instances
     * currently borrowed from but not yet returned
     * to my pool corresponding to the
     * given <i>key</i> (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @param key the key
     * @return the number of instances corresponding to the given <i>key</i> currently borrowed in my pool
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    int getNumActive(Object key) throws UnsupportedOperationException;

    /**
     * Returns the total number of instances
     * currently idle in my pool (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @return the total number of instances currently idle in my pool
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    int getNumIdle() throws UnsupportedOperationException;

    /**
     * Returns the total number of instances
     * current borrowed from my pool but not
     * yet returned (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if this information is not available.
     *
     * @return the total number of instances currently borrowed from my pool
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    int getNumActive() throws UnsupportedOperationException;

    /**
     * Clears my pool, removing all pooled instances
     * (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if the pool cannot be cleared.
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    void clear() throws Exception, UnsupportedOperationException;

    /**
     * Clears the specified pool, removing all
     * pooled instances corresponding to
     * the given <i>key</i>  (optional operation).
     * Throws {@link UnsupportedOperationException}
     * if the pool cannot be cleared.
     * @param key the key to clear
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    void clear(Object key) throws Exception, UnsupportedOperationException;

    /**
     * Close this pool, and free any resources associated with it.
     */
    void close() throws Exception;

    /**
     * Sets the {@link KeyedPoolableObjectFactory factory} I use
     * to create new instances (optional operation).
     * @param factory the {@link KeyedPoolableObjectFactory} I use to create new instances.
     * @throws IllegalStateException when the factory cannot be set at this time
     * @throws UnsupportedOperationException when this implementation doesn't support the operation
     */
    void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException;
}
