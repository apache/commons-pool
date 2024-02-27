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

/**
 * A base class for common functionality.
 *
 * @since 2.4.3
 */
public abstract class BaseObject {

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        toStringAppendFields(builder);
        builder.append("]");
        return builder.toString();
    }

    /**
     * Used by sub-classes to include the fields defined by the sub-class in the
     * {@link #toString()} output.
     *
     * @param builder Field names and values are appended to this object
     */
    protected void toStringAppendFields(final StringBuilder builder) {
        // do nothing by default, needed for b/w compatibility.
    }
}
