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

import java.util.List;

import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.support.SearchResponseSupport;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchResponse;
import org.jsoup.nodes.Document;

import static java.util.Objects.requireNonNull;

public class RemoteRepositorySearchResponseImpl extends SearchResponseSupport
        implements RemoteRepositorySearchResponse {

    protected final String uri;
    protected final Document document;

    public RemoteRepositorySearchResponseImpl(
            SearchRequest searchRequest, int totalHits, List<Record> page, String uri, Document document) {
        super(searchRequest, totalHits, page);
        this.uri = requireNonNull(uri);
        this.document = document;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public Document getDocument() {
        return document;
    }
}
