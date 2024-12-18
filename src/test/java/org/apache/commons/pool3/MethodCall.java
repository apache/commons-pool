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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds method names, parameters, and return values for tracing method calls.
 */
public class MethodCall<K, V> {
    private final String name;
    private final List<K> params;
    private V returned;

    public MethodCall(final String name) {
        this(name, Collections.emptyList());
    }

    public MethodCall(final String name, final List<K> params) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null.");
        }
        this.name = name;
        this.params = Objects.requireNonNullElse(params, Collections.emptyList());
    }

    public MethodCall(final String name, final K param) {
        this(name, Collections.singletonList(param));
    }

    public MethodCall(final String name, final K param1, final K param2) {
        this(name, Arrays.asList(param1, param2));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MethodCall<?, ?> that = (MethodCall<?, ?>)o;

        if (!Objects.equals(name, that.name)) {
            return false;
        }
        if (!Objects.equals(params, that.params)) {
            return false;
        }
        return Objects.equals(returned, that.returned);
    }

    public String getName() {
        return name;
    }

    public List<K> getParams() {
        return params;
    }

    public Object getReturned() {
        return returned;
    }

    @Override
    public int hashCode() {
        int result;
        result = name != null ? name.hashCode() : 0;
        result = 29 * result + (params != null ? params.hashCode() : 0);
        return 29 * result + (returned != null ? returned.hashCode() : 0);
    }

    public MethodCall<K, V> returned(final V obj) {
        setReturned(obj);
        return this;
    }

    public void setReturned(final V returned) {
        this.returned = returned;
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
