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
package org.apache.maven.search.backend.smo.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.apache.maven.search.api.transport.Transport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Offline UT for reading SMO responses.
 */
class SmoSearchBackendResponseTest {

    @Test
    void responseIsRead() throws IOException {
        byte[] json = "{\"response\":{}}".getBytes(StandardCharsets.UTF_8);

        assertEquals(
                "{\"response\":{}}",
                backend(new ByteArrayInputStream(json)).fetch("https://search.example/", Map.of()));
    }

    @Test
    void endlessResponseIsRejected() {
        InputStream endless = new InputStream() {
            @Override
            public int read() {
                return ' ';
            }

            @Override
            public int read(byte[] b, int off, int len) {
                Arrays.fill(b, off, off + len, (byte) ' ');
                return len;
            }
        };

        assertThrows(IOException.class, () -> backend(endless).fetch("https://search.example/", Map.of()));
    }

    private static SmoSearchBackendImpl backend(InputStream body) {
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
                        return body;
                    }

                    @Override
                    public void close() {}
                };
            }

            @Override
            public Response head(String serviceUri, Map<String, String> headers) throws IOException {
                throw new IOException("offline");
            }
        };
        return new SmoSearchBackendImpl("smo", "central", "https://search.example/solrsearch/select", transport);
    }
}
