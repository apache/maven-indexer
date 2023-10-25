/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.search.api.request;

import static java.util.Objects.requireNonNull;

/**
 * Field query.
 */
public class FieldQuery extends Query {
    private final Field field;

    protected FieldQuery(Field field, String queryString) {
        super(queryString);
        this.field = requireNonNull(field);
    }

    /**
     * Returns the field, never {@code null}.
     */
    public Field getField() {
        return field;
    }

    @Override
    public String toString() {
        return getField().getFieldName() + ":" + getValue();
    }

    /**
     * Creates a field query using given {@link Field} and query string.
     */
    public static FieldQuery fieldQuery(Field fieldName, String query) {
        return new FieldQuery(fieldName, query);
    }
}
