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

package org.apache.commons.pool2.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.reference.SoftReferenceObjectPoolMBean;

/**
 * A {@link java.lang.ref.SoftReference SoftReference} based
 * {@link ObjectPool}.
 *
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 1.0
 */
public class SoftReferenceObjectPool<T> extends AbstractReferenceObjectPool<T, SoftReference<T>> implements SoftReferenceObjectPoolMBean {

    /**
     * Create a <code>SoftReferenceObjectPool</code> with the specified factory.
     *
     * @param factory object factory to use, not {@code null}
     * @throws IllegalArgumentException if the factory is null
     */
    public SoftReferenceObjectPool(PoolableObjectFactory<T> factory) {
        super(factory);
    }

    /**
     * {@inheritDoc}
     */
    protected SoftReference<T> createReference(T referent, ReferenceQueue<? super T> referenceQueue) {
        return new SoftReference<T>(referent, referenceQueue);
    }

}
