/*
 * Copyright 1999-2006 The Apache Software Foundation.
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
 * A simple base implementation of {@link ObjectPool}.
 * Optional operations are implemented to either do nothing, return a value
 * indicating it is unsupported or throw {@link UnsupportedOperationException}.
 *
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 */
public abstract class BaseKeyedObjectPool implements KeyedObjectPool {
    public abstract Object borrowObject(Object key) throws Exception;
    public abstract void returnObject(Object key, Object obj) throws Exception;
    public abstract void invalidateObject(Object key, Object obj) throws Exception;

    /**
     * Not supported in this base implementation.
     * Always throws an {@link UnsupportedOperationException},
     * subclasses should override this behavior.
     */
    public void addObject(Object key) throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported in this base implementation.
     * @return a negative value.
     */
    public int getNumIdle(Object key) throws UnsupportedOperationException {
        return -1;
    }

    /**
     * Not supported in this base implementation.
     * @return a negative value.
     */
    public int getNumActive(Object key) throws UnsupportedOperationException {
        return -1;
    }

    /**
     * Not supported in this base implementation.
     * @return a negative value.
     */
    public int getNumIdle() throws UnsupportedOperationException {
        return -1;
    }

    /**
     * Not supported in this base implementation.
     * @return a negative value.
     */
    public int getNumActive() throws UnsupportedOperationException {
        return -1;
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
    public void clear(Object key) throws Exception, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Does nothing this base implementation.
     */
    public void close() throws Exception {
    }


    /**
     * Not supported in this base implementation.
     * Always throws an {@link UnsupportedOperationException},
     * subclasses should override this behavior.
     */
    public void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
