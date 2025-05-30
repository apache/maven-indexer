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
package org.apache.maven.search.api.support;

import org.apache.maven.search.api.SearchBackend;

import static java.util.Objects.requireNonNull;

/**
 * A search backend support class.
 */
public abstract class SearchBackendSupport implements SearchBackend {
    protected final String backendId;

    protected final String repositoryId;

    protected SearchBackendSupport(String backendId, String repositoryId) {
        this.backendId = requireNonNull(backendId);
        this.repositoryId = requireNonNull(repositoryId);
    }

    @Override
    public String getBackendId() {
        return backendId;
    }

    @Override
    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public void close() {
        // override if needed
    }
}
