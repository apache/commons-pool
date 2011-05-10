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
package org.apache.commons.pool2.impl;

/**
 * Provides the possible states that a {@link PooledObject} may be in.
 * <ul>
 * <li>{@link #IDLE}: In queue, not in use.</li>
 * <li>{@link #ALLOCATED}: In use.</li>
 * <li>{@link #MAINTAIN_EVICTION}: In queue, currently being tested for possible
 *     eviction.</li>
 * <li>{@link #MAINTAIN_EVICTION_RETURN_TO_HEAD}: Not in queue, currently being
 *     tested for possible eviction. An attempt to borrow the object was made
 *     while being tested which removed it from the queue. It should be returned
 *     to the head of the queue once testing completes.</li>
 * <li>{@link #MAINTAIN_VALIDATION}: In queue, currently being validated.</li>
 * <li>{@link #MAINTAIN_VALIDATION_PREALLOCATED}: Not in queue, currently being
 *     validated. The object was borrowed while being validated which removed it
 *     from the queue. It should be allocated once validation completes.</li>
 * <li>{@link #MAINTAIN_VALIDATION_RETURN_TO_HEAD}: Not in queue, currently
 *     being validated. An attempt to borrow the object was made while
 *     previously being tested for eviction which removed it from the queue. It
 *     should be returned to the head of the queue once validation completes.
 *     </li>
 * <li>{@link #INVALID}: Failed maintenance (e.g. eviction test or validation)
 *     and will be / has been destroyed.</li>
 * </ul>
 * 
 * TODO: Find shorter names for these states without loss of meaning.
 */
public enum PooledObjectState {
    IDLE,
    ALLOCATED,
    MAINTAIN_EVICTION,
    MAINTAIN_EVICTION_RETURN_TO_HEAD,
    MAINTAIN_VALIDATION,
    MAINTAIN_VALIDATION_PREALLOCATED,
    MAINTAIN_VALIDATION_RETURN_TO_HEAD,
    INVALID
}
