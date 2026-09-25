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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Offline UT for URI composition: the base URI may or may not end with a slash.
 */
public class RemoteRepositorySearchBackendBaseUriTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://repo.example/maven2", "https://repo.example/maven2/"})
    void baseUriWithAndWithoutTrailingSlash(String baseUri) {
        List<String> requested = new ArrayList<>();
        Transport transport = new Transport() {
            @Override
            public Response get(String serviceUri, Map<String, String> headers) throws IOException {
                requested.add(serviceUri);
                throw new IOException("offline");
            }

            @Override
            public Response head(String serviceUri, Map<String, String> headers) throws IOException {
                requested.add(serviceUri);
                throw new IOException("offline");
            }
        };
        RemoteRepositorySearchBackendImpl backend = new RemoteRepositorySearchBackendImpl(
                "test", "test", baseUri, transport, new MavenCentralResponseExtractor());

        assertEquals("https://repo.example/maven2/", backend.getBaseUri());
        assertThrows(
                IOException.class,
                () -> backend.search(new SearchRequest(BooleanQuery.and(
                        FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven"),
                        FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-core")))));
        assertEquals(List.of("https://repo.example/maven2/org/apache/maven/maven-core/maven-metadata.xml"), requested);
    }
}
