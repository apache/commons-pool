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
 * Object pooling proxy implementations.
 * <p>
 * The {@code org.apache.commons.pool2.proxy} package defines a
 * object pool that wraps all objects returned to clients. This allows it
 * to disable those proxies when the objects are returned thereby enabling
 * the continued use of those objects by clients to be detected.
 * </p>
 * <p>
 * Support is provided for {@code java.lang.reflect.Proxy} and for
 * {@code net.sf.cglib.proxy} based proxies. The latter, requires the
 * additional of the optional Code Generation Library (GCLib).
 * </p>
 * <p>
 * cglib is unmaintained and does not work well (or possibly at all?) in newer JDKs, particularly JDK17+; see https://github.com/cglib/cglib
 * </p>
 */
package org.apache.commons.pool2.proxy;
