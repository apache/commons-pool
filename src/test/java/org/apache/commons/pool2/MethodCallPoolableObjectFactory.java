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

package org.apache.commons.pool2;

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * A poolable object factory that tracks how {@link MethodCall methods are called}.
 *
 * @see MethodCall
 */
public class MethodCallPoolableObjectFactory implements PooledObjectFactory<Object> {
    private final List<MethodCall> methodCalls = new ArrayList<>();
    private int count = 0;
    private boolean valid = true;
    private boolean makeObjectFail;
    private boolean activateObjectFail;
    private boolean validateObjectFail;
    private boolean passivateObjectFail;
    private boolean destroyObjectFail;

    public void reset() {
        count = 0;
        getMethodCalls().clear();
        setMakeObjectFail(false);
        setActivateObjectFail(false);
        setValid(true);
        setValidateObjectFail(false);
        setPassivateObjectFail(false);
        setDestroyObjectFail(false);
    }

    public List<MethodCall> getMethodCalls() {
        return methodCalls;
    }

    public int getCurrentCount() {
        return count;
    }

    public void setCurrentCount(final int count) {
        this.count = count;
    }

    public boolean isMakeObjectFail() {
        return makeObjectFail;
    }

    public void setMakeObjectFail(final boolean makeObjectFail) {
        this.makeObjectFail = makeObjectFail;
    }

    public boolean isDestroyObjectFail() {
        return destroyObjectFail;
    }

    public void setDestroyObjectFail(final boolean destroyObjectFail) {
        this.destroyObjectFail = destroyObjectFail;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    public boolean isValidateObjectFail() {
        return validateObjectFail;
    }

    public void setValidateObjectFail(final boolean validateObjectFail) {
        this.validateObjectFail = validateObjectFail;
    }

    public boolean isActivateObjectFail() {
        return activateObjectFail;
    }

    public void setActivateObjectFail(final boolean activateObjectFail) {
        this.activateObjectFail = activateObjectFail;
    }

    public boolean isPassivateObjectFail() {
        return passivateObjectFail;
    }

    public void setPassivateObjectFail(final boolean passivateObjectFail) {
        this.passivateObjectFail = passivateObjectFail;
    }

    @Override
    public PooledObject<Object> makeObject() throws Exception {
        final MethodCall call = new MethodCall("makeObject");
        methodCalls.add(call);
        final int originalCount = this.count++;
        if (makeObjectFail) {
            throw new PrivateException("makeObject");
        }
        // Generate new object, don't use cache via Integer.valueOf(...)
        final Integer obj = new Integer(originalCount);
        call.setReturned(obj);
        return new DefaultPooledObject<Object>(obj);
    }

    @Override
    public void activateObject(final PooledObject<Object> obj) throws Exception {
        methodCalls.add(new MethodCall("activateObject", obj.getObject()));
        if (activateObjectFail) {
            throw new PrivateException("activateObject");
        }
    }

    @Override
    public boolean validateObject(final PooledObject<Object> obj) {
        final MethodCall call = new MethodCall("validateObject", obj.getObject());
        methodCalls.add(call);
        if (validateObjectFail) {
            throw new PrivateException("validateObject");
        }
        final boolean r = valid;
        call.returned(Boolean.valueOf(r));
        return r;
    }

    @Override
    public void passivateObject(final PooledObject<Object> obj) throws Exception {
        methodCalls.add(new MethodCall("passivateObject", obj.getObject()));
        if (passivateObjectFail) {
            throw new PrivateException("passivateObject");
        }
    }

    @Override
    public void destroyObject(final PooledObject<Object> obj) throws Exception {
        methodCalls.add(new MethodCall("destroyObject", obj.getObject()));
        if (destroyObjectFail) {
            throw new PrivateException("destroyObject");
        }
    }
}
