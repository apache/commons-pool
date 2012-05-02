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

package org.apache.commons.pool2;

/**
 * A base implementation of <code>PoolableObjectFactory</code>.
 * <p>
 * All operations defined here are essentially no-op's.
 * <p>
 * This class is immutable, and therefore thread-safe
 * 
 * @param <T> Type of element managed in this factory.
 *
 * @see PoolableObjectFactory
 * @see BaseKeyedPoolableObjectFactory
 *
 * @author Rodney Waldhoff
 * @version $Revision$
 * @since Pool 1.0
 */
public abstract class BasePoolableObjectFactory<T> implements PoolableObjectFactory<T> {
    /**
     * {@inheritDoc}
     */
    @Override
    public abstract T makeObject() throws Exception;

    /**
     *  No-op.
     *  
     *  @param obj ignored
     */
    @Override
    public void destroyObject(T obj)
        throws Exception  {
    }

    /**
     * This implementation always returns <tt>true</tt>.
     * 
     * @param obj ignored
     * @return <tt>true</tt>
     */
    @Override
    public boolean validateObject(T obj) {
        return true;
    }

    /**
     *  No-op.
     *  
     *  @param obj ignored
     */
    @Override
    public void activateObject(T obj) throws Exception {
    }

    /**
     *  No-op.
     *  
     * @param obj ignored
     */
    @Override
    public void passivateObject(T obj)
        throws Exception {
    }
}
