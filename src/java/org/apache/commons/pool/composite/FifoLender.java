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
import java.util.LinkedList;
import java.util.List;

/**
 * A First In First Out (FIFO) {@link Lender}.
 *
 * @see BorrowPolicy#FIFO
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @since Pool 2.0
 */
final class FifoLender extends AbstractLender implements Serializable {

    private static final long serialVersionUID = 8586173154375349810L;

    public Object borrow() {
        final List pool = getObjectPool().getPool();
        assert Thread.holdsLock(pool);
        if (pool instanceof LinkedList) {
            return ((LinkedList)pool).removeFirst();
        } else {
            return pool.remove(0);
        }
    }

    public String toString() {
        return "FIFO";
    }
}