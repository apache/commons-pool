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
 * An interface defining life-cycle methods for
 * instances to be served by an {@link ObjectPool}.
 * <p>
 * By contract, when an {@link ObjectPool}
 * delegates to a {@link PoolableObjectFactory},
 * <ol>
 *  <li>
 *   {@link #makeObject}
 *   is called whenever a new instance is needed.
 *  </li>
 *  <li>
 *   {@link #activateObject}
 *   is invoked on every instance that has been
 *   {@link #passivateObject passivated} before it is
 *   {@link ObjectPool#borrowObject borrowed} from the pool.
 *  </li>
 *  <li>
 *   {@link #validateObject}
 *   is invoked on {@link #activateObject activated} instances to make sure
 *   they can be {@link ObjectPool#borrowObject borrowed} from the pool.
 *   {@link #validateObject} <strong>may</strong> also be used to test an
 *   instance being {@link ObjectPool#returnObject returned} to the pool
 *   before it is {@link #passivateObject passivated}. It will only be invoked
 *   on an {@link #activateObject activated} instance.
 *  </li>
 *  <li>
 *   {@link #passivateObject}
 *   is invoked on every instance when it is returned to the pool.
 *  </li>
 *  <li>
 *   {@link #destroyObject}
 *   is invoked on every instance when it is being "dropped" from the
 *   pool (whether due to the response from {@link #validateObject},
 *   or for reasons specific to the pool implementation.) There is no
 *   guarantee that the instance being {@link #destroyObject destroyed} will
 *   be considered active or passive.
 *  </li>
 * </ol>
 *
 * <p>
 * {@link PoolableObjectFactory} must be thread-safe. The only promise
 * an {@link ObjectPool} makes is that the same instance of an object will not
 * be passed to more than one method of a {@link PoolableObjectFactory}
 * at a time.
 *
 * @see ObjectPool
 *
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$ 
 */
public interface PoolableObjectFactory {
  /**
   * Creates an instance that can be returned by the pool.
   * @return an instance that can be returned by the pool.
   */
  Object makeObject() throws Exception;

  /**
   * Destroys an instance no longer needed by the pool.
   * @param obj the instance to be destroyed
   */
  void destroyObject(Object obj) throws Exception;

  /**
   * Ensures that the instance is safe to be returned by the pool.
   * Returns <tt>false</tt> if this object should be destroyed.
   * @param obj the instance to be validated
   * @return <tt>false</tt> if this <i>obj</i> is not valid and should
   *         be dropped from the pool, <tt>true</tt> otherwise.
   */
  boolean validateObject(Object obj);

  /**
   * Reinitialize an instance to be returned by the pool.
   * @param obj the instance to be activated
   */
  void activateObject(Object obj) throws Exception;

  /**
   * Uninitialize an instance to be returned to the pool.
   * @param obj the instance to be passivated
   */
  void passivateObject(Object obj) throws Exception;
}
