package org.apache.maven.index;

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

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.search.Query;

public class AbstractSearchResponse
    implements Closeable
{
    private final Query query;

    private final int totalHitsCount;

    private final int returnedHitsCount;

    public AbstractSearchResponse( final Query query, final int totalHitsCount, final int returnedHitsCount )
    {
        this.query = query;

        this.totalHitsCount = totalHitsCount;

        this.returnedHitsCount = returnedHitsCount;
    }

    public Query getQuery()
    {
        return query;
    }

    /**
     * Returns the number of total hits found. This may be different that actual hits returned (is usually more).
     * 
     * @return
     * @deprecated use {@link #getTotalHitsCount()} instead.
     */
    public int getTotalHits()
    {
        return getTotalHitsCount();
    }

    /**
     * Returns the number of total hits found by this query (total number of potential hits as reported by Lucene
     * index). This is the number of existing AIs matching your query, and does not represent the count of hits
     * delivered, which is returned by {@link #getReturnedHitsCount()}.
     * 
     * @return
     */
    public int getTotalHitsCount()
    {
        return totalHitsCount;
    }

    /**
     * Returns the number of hits returned by this search response. This number is affected by various input parameters
     * (like count set on request) and filtering, paging, etc. Warning: this number's meaning depends on actual search
     * response (for flat response number of actual AIs, for grouped response number of actual groups), and also, might
     * be not precise at all (see {@link IteratorSearchResponse}).
     * 
     * @return
     */
    public int getReturnedHitsCount()
    {
        return returnedHitsCount;
    }

    /**
     * Returns true if hit limit exceeded.
     * 
     * @return
     * @deprecated always returns false, since 4.1.0 there is no notion of hit limit
     * @see http://jira.codehaus.org/browse/MINDEXER-14
     */
    public boolean isHitLimitExceeded()
    {
        return false;
    }

    /**
     * Frees any resource associated with this response. Should be called as last method on this response, when it's not
     * used anymore.
     * 
     * @throws IOException
     */
    public void close()
        throws IOException
    {
        // noop
    }
}
