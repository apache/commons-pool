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
 * A simple base impementation of {@link ObjectPool}.
 * All optional operations are implemented as throwing
 * {@link UnsupportedOperationException}.
 *
 * @author Rodney Waldhoff
 * @version $Revision: 1.9 $ $Date: 2004/02/28 11:46:33 $
 */
public abstract class BaseKeyedObjectPool implements KeyedObjectPool {
    public abstract Object borrowObject(Object key) throws Exception;
    public abstract void returnObject(Object key, Object obj) throws Exception;
    public abstract void invalidateObject(Object key, Object obj) throws Exception;

    /**
     * Not supported in this base implementation.
     */
    public void addObject(Object key) throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumIdle(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumActive(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     */
    public int getNumIdle() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
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
