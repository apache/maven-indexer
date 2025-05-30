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
package org.apache.maven.search.api;

import org.apache.maven.search.api.request.Paging;
import org.apache.maven.search.api.request.Query;

import static java.util.Objects.requireNonNull;

/**
 * A search request to perform search: defines paging and query.
 */
public class SearchRequest {
    protected final Paging paging;

    protected final Query query;

    /**
     * Creates a request with given {@link Query} instance and default page size of 50.
     */
    public SearchRequest(Query query) {
        this(new Paging(50), query);
    }

    /**
     * Creates a request with given {@link Query} and {@link Paging}.
     */
    public SearchRequest(Paging paging, Query query) {
        this.paging = requireNonNull(paging);
        this.query = requireNonNull(query);
    }

    /**
     * The {@link Paging} of this request: defines page size and page offset, never {@code null}.
     */
    public Paging getPaging() {
        return paging;
    }

    /**
     * The {@link Query} of this request, never {@code null}.
     */
    public Query getQuery() {
        return query;
    }

    /**
     * Returns a new {@link SearchRequest} instance for "next page" relative to this instance, never {@code null}.
     */
    public SearchRequest nextPage() {
        return new SearchRequest(paging.nextPage(), query);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "paging=" + paging + ", query=" + query + '}';
    }
}
