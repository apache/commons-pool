/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Object pooling API implementations.
 * <p>
 * {@link org.apache.commons.pool3.impl.GenericObjectPool GenericObjectPool} ({@link org.apache.commons.pool3.impl.GenericKeyedObjectPool
 * GenericKeyedObjectPool}) provides a more robust (but also more complicated) implementation of {@link org.apache.commons.pool3.ObjectPool ObjectPool}
 * ({@link org.apache.commons.pool3.KeyedObjectPool KeyedObjectPool}).
 * </p>
 * <p>
 * {@link org.apache.commons.pool3.impl.SoftReferenceObjectPool SoftReferenceObjectPool} provides a {@link java.lang.ref.SoftReference SoftReference} based
 * {@link org.apache.commons.pool3.ObjectPool ObjectPool}.
 * </p>
 * <p>
 * See also the {@link org.apache.commons.pool3} package.
 * </p>
 */
package org.apache.commons.pool3.impl;
