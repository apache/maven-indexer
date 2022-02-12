package org.apache.maven.index.search;

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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.maven.index.search.request.Field;

import static java.util.Objects.requireNonNull;

/**
 * A search response record.
 */
public final class Record
{
    private final String backendId;

    private final String repositoryId;

    private final String uid;

    private final Long lastUpdated;

    private final Map<Field, Object> fields;

    public Record( String backendId,
                   String repositoryId,
                   String uid,
                   Long lastUpdated,
                   Map<Field, Object> fields )
    {
        this.backendId = requireNonNull( backendId );
        this.repositoryId = requireNonNull( repositoryId );
        this.uid = uid;
        this.lastUpdated = lastUpdated;
        this.fields = Collections.unmodifiableMap( fields );
    }

    /**
     * Returns {@link SearchBackend#getBackendId()} of originating search backend. Never {@code null}.
     */
    public String getBackendId()
    {
        return backendId;
    }

    /**
     * Returns {@link SearchBackend#getRepositoryId()}) of originating search backend. Never {@code null}.
     */
    public String getRepositoryId()
    {
        return repositoryId;
    }

    /**
     * Returns UID (unique if combined with {@link #getBackendId()}) of search result record, if provided by backend.
     * May be {@code null} if not provided.
     */
    public String getUid()
    {
        return uid;
    }

    /**
     * Returns {@link Long}, representing "last updated" timestamp as epoch millis if provided by backend. May be
     * {@code null} if not provided.
     */
    public Long getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Returns unmodifiable map of all values keyed by {@link Field} backing this record.
     */
    public Map<Field, Object> getFields()
    {
        return fields;
    }

    /**
     * Returns unmodifiable set of present fields in this record, never {@code null}.
     */
    public Set<Field> fieldSet()
    {
        return fields.keySet();
    }

    /**
     * Returns {@code true} if given field is present in this record.
     */
    public boolean hasField( Field field )
    {
        return fields.containsKey( field );
    }

    /**
     * Returns the value belonging to given field in this record, or {@code null} if field not present.
     */
    public String getValue( Field.StringField field )
    {
        return field.getFieldValue( fields );
    }

    /**
     * Returns the value belonging to given field in this record, or {@code null} if field not present.
     */
    public Number getValue( Field.NumberField field )
    {
        return field.getFieldValue( fields );
    }

    /**
     * Returns the value belonging to given field in this record, or {@code null} if field not present.
     */
    public Boolean getValue( Field.BooleanField field )
    {
        return field.getFieldValue( fields );
    }
}
