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
import java.util.Map;

import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchResponse;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UT for checksum parsing and validation in {@link RemoteRepositorySearchBackendImpl}: a non-checksum
 * body (for example an error page served for a missing {@code .sha1} file) must never be accepted as
 * a checksum.
 */
class RemoteRepositorySearchBackendImplChecksumTest {

    private static final String SHA1 = "2a9a736e5bd63a0865e2c795828e5a08d9d10a23";

    private static InputStream body(String payload) {
        return new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void plainChecksumIsRead() throws Exception {
        assertEquals(SHA1, RemoteRepositorySearchBackendImpl.readChecksum(body(SHA1 + "\n")));
    }

    @Test
    void checksumWithFileNameIsRead() throws Exception {
        assertEquals(SHA1, RemoteRepositorySearchBackendImpl.readChecksum(body(SHA1 + "  some-artifact-1.0.jar\n")));
    }

    @Test
    void validSha1Accepted() {
        assertTrue(RemoteRepositorySearchBackendImpl.isValidSha1(SHA1));
        assertTrue(RemoteRepositorySearchBackendImpl.isValidSha1(SHA1.toUpperCase()));
    }

    @Test
    void nonChecksumContentRejected() throws Exception {
        // first non-empty line of a typical error page must not pass as a checksum
        String errorPage = RemoteRepositorySearchBackendImpl.readChecksum(
                body("<html>\n<head><title>404 Not Found</title></head>\n</html>\n"));
        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1(errorPage));

        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1(null));
        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1(""));
        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1("not-a-checksum"));
        // right length, not hex
        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1("zz9a736e5bd63a0865e2c795828e5a08d9d10a23"));
        // truncated
        assertFalse(RemoteRepositorySearchBackendImpl.isValidSha1(SHA1.substring(0, 39)));
    }

    @Test
    void checksumFromNon200ResponseIsIgnored() throws Exception {
        assertEquals(0, searchBySha1(404).getTotalHits());
    }

    @Test
    void checksumFrom200ResponseIsUsed() throws Exception {
        assertEquals(1, searchBySha1(200).getTotalHits());
    }

    /**
     * Searches for an artifact by SHA-1 where the artifact exists, and the {@code .sha1} request answers
     * with {@code sha1Code} and a body that equals the searched checksum.
     */
    private static RemoteRepositorySearchResponse searchBySha1(int sha1Code) throws IOException {
        Transport transport = new Transport() {
            @Override
            public Response get(String serviceUri, Map<String, String> headers) {
                assertTrue(serviceUri.endsWith(".jar.sha1"), serviceUri);
                return response(sha1Code, SHA1);
            }

            @Override
            public Response head(String serviceUri, Map<String, String> headers) {
                return response(200, "");
            }
        };
        RemoteRepositorySearchBackendImpl backend = new RemoteRepositorySearchBackendImpl(
                "test", "test", "https://repo.example/maven2/", transport, new MavenCentralResponseExtractor());
        return backend.search(new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.example"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "example"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "1.0"),
                FieldQuery.fieldQuery(MAVEN.FILE_EXTENSION, "jar"),
                FieldQuery.fieldQuery(MAVEN.SHA1, SHA1))));
    }

    private static Transport.Response response(int code, String payload) {
        return new Transport.Response() {
            @Override
            public int getCode() {
                return code;
            }

            @Override
            public Map<String, String> getHeaders() {
                return Map.of();
            }

            @Override
            public InputStream getBody() {
                return body(payload);
            }

            @Override
            public void close() {}
        };
    }
}
