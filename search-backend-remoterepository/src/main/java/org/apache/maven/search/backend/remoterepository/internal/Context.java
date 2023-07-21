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
package org.apache.maven.search.backend.remoterepository.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.search.MAVEN;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.request.BooleanQuery;
import org.apache.maven.search.request.Field;
import org.apache.maven.search.request.FieldQuery;
import org.apache.maven.search.request.Query;

import static java.util.Objects.requireNonNull;

public class Context {
    private final SearchRequest searchRequest;

    private final Map<Field, String> fields;

    public Context(SearchRequest searchRequest) {
        this.searchRequest = requireNonNull(searchRequest);
        this.fields = new HashMap<>();
        populateFields(searchRequest.getQuery());
    }

    private void populateFields(Query query) {
        if (query instanceof BooleanQuery) {
            populateFields(((BooleanQuery) query).getLeft());
            populateFields(((BooleanQuery) query).getRight());
        } else if (query instanceof FieldQuery) {
            fields.put(((FieldQuery) query).getField(), query.getValue());
        } else {
            throw new IllegalArgumentException("Unsupported Query type: " + query.getClass());
        }
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }

    public String getFieldValue(Field field) {
        return fields.get(field);
    }

    public String getGroupId() {
        return getFieldValue(MAVEN.GROUP_ID);
    }

    public String getArtifactId() {
        return getFieldValue(MAVEN.ARTIFACT_ID);
    }

    public String getVersion() {
        return getFieldValue(MAVEN.VERSION);
    }

    public String getClassifier() {
        return getFieldValue(MAVEN.CLASSIFIER);
    }

    public String getFileExtension() {
        return getFieldValue(MAVEN.FILE_EXTENSION);
    }

    public String getSha1() {
        return getFieldValue(MAVEN.SHA1);
    }
}
