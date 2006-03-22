/*
 * Copyright 2006 The Apache Software Foundation.
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

package org.apache.commons.pool.composite;

import java.io.Serializable;
import java.util.ListIterator;

/**
 * Delegates all work to another lender. Subclasses should call <code>super.method(...)</code> to access the delegates.
 *
 * @author Sandy McArthur
 * @since #.#
 * @version $Revision$ $Date$
 */
abstract class DelegateLender extends AbstractLender implements Serializable {

    private static final long serialVersionUID = -4403177642421760774L;

    /**
     * The delegate lender. This is only accessed by subclasses by calling super.method(...).
     */
    private final Lender delegate;

    /**
     * Create a lender that delegates some behavior to another lender.
     *
     * @param delegate the lender to delegate to, must not be <code>null</code>.
     * @throws IllegalArgumentException when <code>delegate</code> is <code>null</code>.
     */
    protected DelegateLender(final Lender delegate) throws IllegalArgumentException {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate Lender must not be null.");
        }
        this.delegate = delegate;
    }

    /**
     * Calls {@link AbstractLender#setCompositeObjectPool(CompositeObjectPool)} and the delegate's
     * {@link Lender#setCompositeObjectPool(CompositeObjectPool)} methods.
     *
     * @param objectPool the pool to associate with.
     */
    public void setCompositeObjectPool(final CompositeObjectPool objectPool) throws IllegalArgumentException, IllegalStateException {
        super.setCompositeObjectPool(objectPool);
        delegate.setCompositeObjectPool(getObjectPool());
    }

    /**
     * Calls the delegate's {@link Lender#borrow()} method.
     *
     * @return a previously idle object.
     */
    public Object borrow() {
        return delegate.borrow();
    }

    /**
     * Calls the delegate's {@link Lender#repay(Object)} method.
     *
     * @param obj the object to return to the idle object pool.
     */
    public void repay(final Object obj) {
        delegate.repay(obj);
    }

    /**
     * Calls the delegate's {@link Lender#listIterator()} method.
     *
     * @return a list iterator of the elements in this pool.
     */
    public ListIterator listIterator() {
        return delegate.listIterator();
    }

    /**
     * Calls the delegate's {@link Lender#size()} method.
     *
     * @return the size of the idle object pool the lender is accessing.
     */
    public int size() {
        return delegate.size();
    }

    public String toString() {
        return delegate.toString();
    }
}