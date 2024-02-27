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

/**
 * Object pooling API.
 * <p>
 * The <code>org.apache.commons.pool3</code> package defines a simple interface for a pool of object instances, and a handful of base classes that may be useful
 * when creating pool implementations.
 * </p>
 * <p>
 * The <code>pool</code> package itself doesn't define a specific object pooling implementation, but rather a contract that implementations may support in order
 * to be fully interchangeable.
 * </p>
 * <p>
 * The <code>pool</code> package separates the way in which instances are pooled from the way in which they are created, resulting in a pair of interfaces:
 * </p>
 * <dl>
 * <dt>{@link org.apache.commons.pool3.ObjectPool ObjectPool}</dt>
 * <dd>defines a simple object pooling interface, with methods for borrowing instances from and returning them to the pool.</dd>
 * <dt>{@link org.apache.commons.pool3.PooledObjectFactory PooledObjectFactory}</dt>
 * <dd>defines lifecycle methods for object instances contained within a pool. By associating a factory with a pool, the pool can create new object instances as
 * needed.</dd>
 * </dl>
 * <p>
 * The <code>pool</code> package also provides a keyed pool interface, which pools instances of multiple types, accessed according to an arbitrary key. See
 * {@link org.apache.commons.pool3.KeyedObjectPool KeyedObjectPool} and {@link org.apache.commons.pool3.KeyedPooledObjectFactory KeyedPooledObjectFactory}.
 * </p>
 */
package org.apache.commons.pool3;
