package org.apache.maven.search.support;

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

import org.apache.maven.search.Record;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.SearchResponse;

import static java.util.Objects.requireNonNull;

/**
 * A search response support class.
 */
public abstract class SearchResponseSupport implements SearchResponse
{
    private final SearchRequest searchRequest;

    private final int totalHits;

    private final List<Record> page;

    protected SearchResponseSupport( SearchRequest searchRequest, int totalHits, List<Record> page )
    {
        this.searchRequest = requireNonNull( searchRequest );
        this.totalHits = totalHits;
        this.page = requireNonNull( page );
    }

    @Override
    public SearchRequest getSearchRequest()
    {
        return searchRequest;
    }

    @Override
    public int getTotalHits()
    {
        return totalHits;
    }

    @Override
    public int getCurrentHits()
    {
        return page.size();
    }

    @Override
    public List<Record> getPage()
    {
        return page;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{" + "searchRequest=" + searchRequest + ", totalHits=" + totalHits
                + ", page=" + page + '}';
    }
}
