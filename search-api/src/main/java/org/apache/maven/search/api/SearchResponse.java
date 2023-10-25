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

import java.util.List;

/**
 * A search engine response.
 */
public interface SearchResponse {
    /**
     * Returns the {@link SearchRequest} used for this response, never {@code null}.
     */
    SearchRequest getSearchRequest();

    /**
     * Returns the total count of hits produced by {@link #getSearchRequest()}.
     */
    int getTotalHits();

    /**
     * Returns the count of current hits in current "page". It may be less or equal to page size of {@link
     * SearchRequest#getPaging()}.
     */
    int getCurrentHits();

    /**
     * Returns current "page" of results as list of records, never {@code null}.
     */
    List<Record> getPage();
}
