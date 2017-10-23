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
import java.util.Collections;
import java.util.Arrays;

/**
 * Holds method names, parameters, and return values for tracing method calls.
 */
public class MethodCall {
    private final String name;
    private final List<Object> params;
    private Object returned;

    public MethodCall(final String name) {
        this(name, null);
    }

    public MethodCall(final String name, final Object param) {
        this(name, Collections.singletonList(param));
    }

    public MethodCall(final String name, final Object param1, final Object param2) {
        this(name, Arrays.asList(new Object[] {param1, param2}));
    }

    public MethodCall(final String name, final List<Object> params) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null.");
        }
        this.name = name;
        if (params != null) {
            this.params = params;
        } else {
            this.params = Collections.emptyList();
        }
    }

    public String getName() {
        return name;
    }

    public List<Object> getParams() {
        return params;
    }

    public Object getReturned() {
        return returned;
    }

    public void setReturned(final Object returned) {
        this.returned = returned;
    }

    public MethodCall returned(final Object obj) {
        setReturned(obj);
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MethodCall that = (MethodCall)o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (params != null ? !params.equals(that.params) : that.params != null) {
            return false;
        }
        if (returned != null ? !returned.equals(that.returned) : that.returned != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = name != null ? name.hashCode() : 0;
        result = 29 * result + (params != null ? params.hashCode() : 0);
        result = 29 * result + (returned != null ? returned.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MethodCall");
        sb.append("{name='").append(name).append('\'');
        if (!params.isEmpty()) {
            sb.append(", params=").append(params);
        }
        if (returned != null) {
            sb.append(", returned=").append(returned);
        }
        sb.append('}');
        return sb.toString();
    }
}
