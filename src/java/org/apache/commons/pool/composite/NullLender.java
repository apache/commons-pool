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

package org.apache.commons.pool.composite;

import java.io.Serializable;
import java.util.Collections;
import java.util.ListIterator;

/**
 * Neither borrow nor return objects.
 *
 * @see BorrowPolicy#NULL
 * @author Sandy McArthur
 * @since Pool 2.0
 * @version $Revision$ $Date$
 */
final class NullLender extends AbstractLender implements Serializable {

    private static final long serialVersionUID = -135471856936204860L;

    /**
     * Return <code>null</code>.
     *
     * @return <code>null</code>.
     */
    public Object borrow() {
        return null;
    }

    /**
     * Discards the object.
     *
     * @param obj the object to be discarded.
     */
    public void repay(final Object obj) {
        // nothing
    }

    /**
     * Returns a list iterator from an empty list.
     *
     * @return a list iterator from an empty list.
     */
    public ListIterator listIterator() {
        return Collections.EMPTY_LIST.listIterator();
    }

    /**
     * Return <code>0</code>.
     *
     * @return <code>0</code>.
     */
    public int size() {
        return 0;
    }

    public String toString() {
        return "NULL";
    }
}