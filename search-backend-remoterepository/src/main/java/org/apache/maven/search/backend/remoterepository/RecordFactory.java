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
package org.apache.maven.search.backend.remoterepository;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.search.MAVEN;
import org.apache.maven.search.Record;
import org.apache.maven.search.request.Field;

import static java.util.Objects.requireNonNull;

/**
 * Helper class that creates record instances for provided backend.
 */
public final class RecordFactory {

    private final RemoteRepositorySearchBackend backend;

    public RecordFactory(RemoteRepositorySearchBackend backend) {
        this.backend = requireNonNull(backend);
    }

    /**
     * Creates {@link Record} on behalf of backed. Only {@code groupId} is mandatory, all the other values are optional.
     */
    public Record create(String groupId, String artifactId, String version, String classifier, String fileExtension) {
        requireNonNull(groupId);
        HashMap<Field, Object> result = new HashMap<>();
        mayPut(result, MAVEN.GROUP_ID, groupId);
        mayPut(result, MAVEN.ARTIFACT_ID, artifactId);
        mayPut(result, MAVEN.VERSION, version);
        mayPut(result, MAVEN.CLASSIFIER, classifier);
        mayPut(result, MAVEN.FILE_EXTENSION, fileExtension);
        return new Record(backend.getBackendId(), backend.getRepositoryId(), null, null, result);
    }

    private static void mayPut(Map<Field, Object> result, Field fieldName, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String && ((String) value).isBlank()) {
            return;
        }
        result.put(fieldName, value);
    }
}
