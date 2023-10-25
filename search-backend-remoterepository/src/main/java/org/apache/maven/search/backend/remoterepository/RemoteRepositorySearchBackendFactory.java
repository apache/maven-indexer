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

import org.apache.maven.search.api.transport.Java11HttpClientTransport;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.apache.maven.search.backend.remoterepository.extractor.Nx2ResponseExtractor;
import org.apache.maven.search.backend.remoterepository.internal.RemoteRepositorySearchBackendImpl;

/**
 * The remote repository search backend factory.
 */
public final class RemoteRepositorySearchBackendFactory {
    public static final String BACKEND_ID = "search-rr";

    public static final String CENTRAL_REPOSITORY_ID = "central";

    public static final String CENTRAL_URI = "https://repo.maven.apache.org/maven2/";

    public static final String RAO_RELEASES_REPOSITORY_ID = "apache.releases.https";

    public static final String RAO_RELEASES_URI = "https://repository.apache.org/content/repositories/releases/";

    private RemoteRepositorySearchBackendFactory() {}

    /**
     * Creates "default" RR search backend against Maven Central suitable for most use cases.
     */
    public static RemoteRepositorySearchBackend createDefaultMavenCentral() {
        return create(
                BACKEND_ID,
                CENTRAL_REPOSITORY_ID,
                CENTRAL_URI,
                new Java11HttpClientTransport(),
                new MavenCentralResponseExtractor());
    }

    /**
     * Creates "default" RR search backend against repository.apache.org releases repository suitable for most use cases.
     */
    public static RemoteRepositorySearchBackend createDefaultRAOReleases() {
        return create(
                BACKEND_ID,
                RAO_RELEASES_REPOSITORY_ID,
                RAO_RELEASES_URI,
                new Java11HttpClientTransport(),
                new Nx2ResponseExtractor());
    }

    /**
     * Creates RR search backend using provided parameters.
     */
    public static RemoteRepositorySearchBackend create(
            String backendId,
            String repositoryId,
            String baseUri,
            Transport transport,
            ResponseExtractor responseExtractor) {
        return new RemoteRepositorySearchBackendImpl(backendId, repositoryId, baseUri, transport, responseExtractor);
    }
}
