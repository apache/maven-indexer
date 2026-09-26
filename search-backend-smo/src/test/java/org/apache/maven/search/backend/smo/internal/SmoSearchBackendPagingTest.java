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

import java.io.IOException;
import java.util.Map;

import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.Paging;
import org.apache.maven.search.api.transport.Transport;
import org.junit.jupiter.api.Test;

import static org.apache.maven.search.api.request.Query.query;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Offline UT for the SMO paging parameters.
 */
public class SmoSearchBackendPagingTest {

    @Test
    void startIsComputedWithoutIntOverflow() {
        Transport transport = new Transport() {
            @Override
            public Response get(String serviceUri, Map<String, String> headers) throws IOException {
                throw new IOException("offline");
            }

            @Override
            public Response head(String serviceUri, Map<String, String> headers) throws IOException {
                throw new IOException("offline");
            }
        };
        SmoSearchBackendImpl backend =
                new SmoSearchBackendImpl("smo", "central", "https://search.example/solrsearch/select", transport);

        String uri = backend.toURI(new SearchRequest(new Paging(1 << 30, 3), query("junit")));

        assertTrue(uri.contains("&start=3221225472&rows=1073741824"), uri);
    }
}
