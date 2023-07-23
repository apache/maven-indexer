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

import org.apache.maven.search.backend.remoterepository.internal.Java11HttpClientRemoteRepositorySearchTransport;
import org.apache.maven.search.backend.remoterepository.internal.RemoteRepositorySearchBackendImpl;

/**
 * The remote repository search backend factory.
 */
public class RemoteRepositorySearchBackendFactory {
    public static final String DEFAULT_BACKEND_ID = "central-rr";

    public static final String DEFAULT_REPOSITORY_ID = "central";

    public static final String DEFAULT_URI = "https://repo.maven.apache.org/maven2/";

    /**
     * Creates "default" RR search backend against Maven Central suitable for most use cases.
     */
    public static RemoteRepositorySearchBackend createDefault() {
        return create(
                DEFAULT_BACKEND_ID,
                DEFAULT_REPOSITORY_ID,
                DEFAULT_URI,
                new Java11HttpClientRemoteRepositorySearchTransport());
    }

    /**
     * Creates RR search backend using provided parameters.
     */
    public static RemoteRepositorySearchBackend create(
            String backendId, String repositoryId, String baseUri, RemoteRepositorySearchTransport transport) {
        return new RemoteRepositorySearchBackendImpl(backendId, repositoryId, baseUri, transport);
    }
}
