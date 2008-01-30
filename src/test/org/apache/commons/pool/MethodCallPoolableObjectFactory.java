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

package org.apache.commons.pool;

import java.util.List;
import java.util.ArrayList;

/**
 * A poolable object factory that tracks how {@link MethodCall methods are called}.
 *
 * @author Sandy McArthur
 * @version $Revision$ $Date$
 * @see MethodCall
 */
public class MethodCallPoolableObjectFactory implements PoolableObjectFactory {
    private final List methodCalls = new ArrayList();
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

    public List getMethodCalls() {
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

    public Object makeObject() throws Exception {
        final MethodCall call = new MethodCall("makeObject");
        methodCalls.add(call);
        int count = this.count++;
        if (makeObjectFail) {
            throw new PrivateException("makeObject");
        }
        final Integer obj = new Integer(count);
        call.setReturned(obj);
        return obj;
    }

    public void activateObject(final Object obj) throws Exception {
        methodCalls.add(new MethodCall("activateObject", obj));
        if (activateObjectFail) {
            throw new PrivateException("activateObject");
        }
    }

    public boolean validateObject(final Object obj) {
        final MethodCall call = new MethodCall("validateObject", obj);
        methodCalls.add(call);
        if (validateObjectFail) {
            throw new PrivateException("validateObject");
        }
        final boolean r = valid;
        call.returned(new Boolean(r));
        return r;
    }

    public void passivateObject(final Object obj) throws Exception {
        methodCalls.add(new MethodCall("passivateObject", obj));
        if (passivateObjectFail) {
            throw new PrivateException("passivateObject");
        }
    }

    public void destroyObject(final Object obj) throws Exception {
        methodCalls.add(new MethodCall("destroyObject", obj));
        if (destroyObjectFail) {
            throw new PrivateException("destroyObject");
        }
    }
}
