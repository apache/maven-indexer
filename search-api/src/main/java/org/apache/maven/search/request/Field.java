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
package org.apache.maven.search.request;

import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Field, that is used as key in record.
 */
public abstract class Field {
    private final String fieldName;

    private final boolean searchable;

    private Field(String fieldName, boolean searchable) {
        this.fieldName = requireNonNull(fieldName);
        this.searchable = searchable;
    }

    /**
     * Returns the field name.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns {@code true} if field may be used for {@link FieldQuery} (is searchable).
     */
    public boolean isSearchable() {
        return searchable;
    }

    /**
     * Returns the value of the field from given record instance, or {@code null} if field not present in record.
     * See subclasses for proper return types.
     */
    public abstract Object getFieldValue(Map<Field, Object> record);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Field fieldName1 = (Field) o;
        return Objects.equals(getFieldName(), fieldName1.getFieldName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFieldName());
    }

    @Override
    public String toString() {
        return fieldName;
    }

    public static class StringField extends Field {
        public StringField(String fieldName, boolean searchable) {
            super(fieldName, searchable);
        }

        @Override
        public String getFieldValue(Map<Field, Object> record) {
            return (String) record.get(this);
        }
    }

    public static class NumberField extends Field {
        public NumberField(String fieldName, boolean searchable) {
            super(fieldName, searchable);
        }

        @Override
        public Number getFieldValue(Map<Field, Object> record) {
            return (Number) record.get(this);
        }
    }

    public static class BooleanField extends Field {
        public BooleanField(String fieldName, boolean searchable) {
            super(fieldName, searchable);
        }

        @Override
        public Boolean getFieldValue(Map<Field, Object> record) {
            return (Boolean) record.get(this);
        }
    }
}
