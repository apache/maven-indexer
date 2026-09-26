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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Offline UT for listing versions from {@code maven-metadata.xml}.
 */
class RemoteRepositorySearchBackendMetadataTest {

    @Test
    void versionsAreFilteredLikeOtherNames() throws Exception {
        String metadata = "<metadata><groupId>org.example</groupId><artifactId>example</artifactId><versioning>"
                + "<versions><version>1.0</version><version></version><version>..</version><version>2.0</version>"
                + "</versions></versioning></metadata>";
        Transport transport = new Transport() {
            @Override
            public Response get(String serviceUri, Map<String, String> headers) {
                return new Response() {
                    @Override
                    public int getCode() {
                        return 200;
                    }

                    @Override
                    public Map<String, String> getHeaders() {
                        return Map.of();
                    }

                    @Override
                    public InputStream getBody() {
                        return new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8));
                    }

                    @Override
                    public void close() {}
                };
            }

            @Override
            public Response head(String serviceUri, Map<String, String> headers) throws IOException {
                throw new IOException("unexpected HEAD " + serviceUri);
            }
        };
        RemoteRepositorySearchBackendImpl backend = new RemoteRepositorySearchBackendImpl(
                "test", "test", "https://repo.example/maven2/", transport, new MavenCentralResponseExtractor());

        List<String> versions = backend
                .search(new SearchRequest(BooleanQuery.and(
                        FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.example"),
                        FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "example"))))
                .getPage()
                .stream()
                .map(r -> r.getValue(MAVEN.VERSION))
                .collect(Collectors.toList());

        assertEquals(List.of("1.0", "2.0"), versions);
    }
}
