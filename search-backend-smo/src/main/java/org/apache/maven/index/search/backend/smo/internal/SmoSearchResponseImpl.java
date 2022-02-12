package org.apache.maven.index.search.backend.smo.internal;

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

import java.util.List;

import org.apache.maven.index.search.Record;
import org.apache.maven.index.search.SearchRequest;
import org.apache.maven.index.search.backend.smo.SmoSearchResponse;
import org.apache.maven.index.search.support.SearchResponseSupport;

import static java.util.Objects.requireNonNull;

public class SmoSearchResponseImpl extends SearchResponseSupport implements SmoSearchResponse
{
    private final String searchUri;

    private final String rawJsonResponse;

    public SmoSearchResponseImpl( SearchRequest searchRequest, int totalHits, List<Record> page,
                                  String searchUri, String rawJsonResponse )
    {
        super( searchRequest, totalHits, page );
        this.searchUri = requireNonNull( searchUri );
        this.rawJsonResponse = requireNonNull( rawJsonResponse );
    }

    @Override
    public String getSearchUri()
    {
        return searchUri;
    }

    @Override
    public String getRawJsonResponse()
    {
        return rawJsonResponse;
    }
}
