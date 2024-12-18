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

package org.apache.commons.pool3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.pool3.impl.DefaultPooledObject;

/**
 * A poolable object factory that tracks how {@link MethodCall methods are called}.
 *
 * @see MethodCall
 */
public class MethodCallPoolableObjectFactory <K> implements PooledObjectFactory<K, PrivateException> {
    private final List<MethodCall<K, K>> methodCalls = new ArrayList<>();
    private boolean valid = true;
    private boolean makeObjectFail;
    private boolean activateObjectFail;
    private boolean validateObjectFail;
    private boolean passivateObjectFail;
    private boolean destroyObjectFail;
    private final Supplier<K> supplier;

    public MethodCallPoolableObjectFactory(final Supplier<K> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void activateObject(final PooledObject<K> obj) {
        methodCalls.add(new MethodCall<>("activateObject", obj.getObject()));
        if (activateObjectFail) {
            throw new PrivateException("activateObject");
        }
    }

    @Override
    public void destroyObject(final PooledObject<K> obj) {
        methodCalls.add(new MethodCall<>("destroyObject", obj.getObject()));
        if (destroyObjectFail) {
            throw new PrivateException("destroyObject");
        }
    }

    public List<MethodCall<K, K>> getMethodCalls() {
        return methodCalls;
    }

    public boolean isActivateObjectFail() {
        return activateObjectFail;
    }

    public boolean isDestroyObjectFail() {
        return destroyObjectFail;
    }

    public boolean isMakeObjectFail() {
        return makeObjectFail;
    }

    public boolean isPassivateObjectFail() {
        return passivateObjectFail;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isValidateObjectFail() {
        return validateObjectFail;
    }

    @Override
    public PooledObject<K> makeObject() {
        final MethodCall<K, K> call = new MethodCall<>("makeObject");
        methodCalls.add(call);
        if (makeObjectFail) {
            throw new PrivateException("makeObject");
        }
        // Generate new object, don't use cache via Integer.valueOf(...)
        final K obj = supplier.get();
        call.setReturned(obj);
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void passivateObject(final PooledObject<K> obj) {
        methodCalls.add(new MethodCall<>("passivateObject", obj.getObject()));
        if (passivateObjectFail) {
            throw new PrivateException("passivateObject");
        }
    }

    public void reset() {
        getMethodCalls().clear();
        setMakeObjectFail(false);
        setActivateObjectFail(false);
        setValid(true);
        setValidateObjectFail(false);
        setPassivateObjectFail(false);
        setDestroyObjectFail(false);
    }

    public void setActivateObjectFail(final boolean activateObjectFail) {
        this.activateObjectFail = activateObjectFail;
    }

    public void setDestroyObjectFail(final boolean destroyObjectFail) {
        this.destroyObjectFail = destroyObjectFail;
    }

    public void setMakeObjectFail(final boolean makeObjectFail) {
        this.makeObjectFail = makeObjectFail;
    }

    public void setPassivateObjectFail(final boolean passivateObjectFail) {
        this.passivateObjectFail = passivateObjectFail;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public void setValidateObjectFail(final boolean validateObjectFail) {
        this.validateObjectFail = validateObjectFail;
    }

    @Override
    public boolean validateObject(final PooledObject<K> obj) {
        final MethodCall<K, K> call = new MethodCall<>("validateObject", obj.getObject());
        methodCalls.add(call);
        if (validateObjectFail) {
            throw new PrivateException("validateObject");
        }
        final boolean r = valid;
        call.returned(obj.getObject());
        return r;
    }
}
