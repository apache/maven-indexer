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
package org.apache.maven.search.backend.smo;

import org.apache.maven.search.api.transport.Java11HttpClientTransport;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.smo.internal.SmoSearchBackendImpl;

/**
 * The SMO search backend factory.
 */
public final class SmoSearchBackendFactory {
    public static final String SMO_BACKEND_ID = "central-smo";

    public static final String SMO_REPOSITORY_ID = "central";

    public static final String SMO_SMO_URI = "https://search.maven.org/solrsearch/select";

    public static final String CSC_BACKEND_ID = "central-csc";

    public static final String CSC_REPOSITORY_ID = "central";

    public static final String CSC_SMO_URI = "https://central.sonatype.com/solrsearch/select";

    /**
     * @deprecated Use SMO_ or CSC_ instead.
     */
    @Deprecated
    public static final String DEFAULT_BACKEND_ID = SMO_BACKEND_ID;

    /**
     * @deprecated Use SMO_ or CSC_ instead.
     */
    @Deprecated
    public static final String DEFAULT_REPOSITORY_ID = SMO_REPOSITORY_ID;

    /**
     * @deprecated Use SMO_ or CSC_ instead.
     */
    @Deprecated
    public static final String DEFAULT_SMO_URI = SMO_SMO_URI;

    private SmoSearchBackendFactory() {}

    /**
     * Creates "default" SMO search backend suitable for most use cases.
     *
     * @deprecated Use methods {@link #createSmo()} or {@link #createCsc()} instead.
     */
    @Deprecated
    public static SmoSearchBackend createDefault() {
        return create(DEFAULT_BACKEND_ID, DEFAULT_REPOSITORY_ID, DEFAULT_SMO_URI, new Java11HttpClientTransport());
    }

    /**
     * Creates SMO backend.
     */
    public static SmoSearchBackend createSmo() {
        return create(SMO_BACKEND_ID, SMO_REPOSITORY_ID, SMO_SMO_URI, new Java11HttpClientTransport());
    }

    /**
     * Creates CSC backend.
     */
    public static SmoSearchBackend createCsc() {
        return create(CSC_BACKEND_ID, CSC_REPOSITORY_ID, CSC_SMO_URI, new Java11HttpClientTransport());
    }

    /**
     * Creates SMO search backend using provided parameters.
     */
    public static SmoSearchBackend create(String backendId, String repositoryId, String smoUri, Transport transport) {
        return new SmoSearchBackendImpl(backendId, repositoryId, smoUri, transport);
    }
}
