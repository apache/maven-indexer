/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

public class AbstractSearchRequest
{
    public static final int UNDEFINED = -1;

    private Query query;

    private List<IndexingContext> contexts;

    private int start;

    /**
     * The page size, actually count of items in one page. Different than limit, because this will _cut_ the response to
     * the requested count.
     */
    private int count;

    /**
     * The limit size, the maximum possible count of items in response. Different than count above, since if this is
     * set, and the search funds more then this number, the response will be _empty_ and a flag will be set on response
     * about this.
     */
    private int resultHitLimit;

    /**
     * The filter to be used while executing the search request.
     */
    private ArtifactInfoFilter artifactInfoFilter;

    /**
     * The postprocessor to apply to hits while returning the,
     */
    private ArtifactInfoPostprocessor artifactInfoPostprocessor;

    /**
     * The highlighting requests, if any.
     */
    private List<MatchHighlightRequest> matchHighlightRequests;

    /**
     * Should Lucene Explanations be added to resulting ArtifactInfo's attributes (keyed as
     * org.apache.lucene.search.Explanation.class.getName())? Warning: calculating these are costly operation, and
     * should not be used in production systems (maybe on some "debug" like UI or so).
     */
    private boolean luceneExplain = false;

    public AbstractSearchRequest( Query query )
    {
        this( query, null );
    }

    public AbstractSearchRequest( Query query, List<IndexingContext> contexts )
    {
        this.query = query;

        if ( contexts != null )
        {
            getContexts().addAll( contexts );
        }

        this.start = UNDEFINED;

        this.count = UNDEFINED;

        this.resultHitLimit = UNDEFINED;
    }

    public Query getQuery()
    {
        return query;
    }

    public void setQuery( Query query )
    {
        this.query = query;
    }

    public List<IndexingContext> getContexts()
    {
        if ( contexts == null )
        {
            contexts = new ArrayList<IndexingContext>();
        }

        return contexts;
    }

    public void setContexts( List<IndexingContext> contexts )
    {
        this.contexts = contexts;
    }

    public boolean isHitLimited()
    {
        return getResultHitLimit() != UNDEFINED;
    }

    public int getStart()
    {
        return start;
    }

    public void setStart( int start )
    {
        this.start = start;
    }

    public int getCount()
    {
        return count;
    }

    @Deprecated
    public int getAiCount()
    {
        return getCount();
    }

    public void setCount( int count )
    {
        this.count = count;
    }

    @Deprecated
    public void setAiCount( int count )
    {
        setCount( count );
    }

    public int getResultHitLimit()
    {
        return resultHitLimit;
    }

    public void setResultHitLimit( int resultHitLimit )
    {
        this.resultHitLimit = resultHitLimit;
    }

    public ArtifactInfoFilter getArtifactInfoFilter()
    {
        return artifactInfoFilter;
    }

    public void setArtifactInfoFilter( ArtifactInfoFilter artifactInfoFilter )
    {
        this.artifactInfoFilter = artifactInfoFilter;
    }

    public ArtifactInfoPostprocessor getArtifactInfoPostprocessor()
    {
        return artifactInfoPostprocessor;
    }

    public void setArtifactInfoPostprocessor( ArtifactInfoPostprocessor artifactInfoPostprocessor )
    {
        this.artifactInfoPostprocessor = artifactInfoPostprocessor;
    }

    public List<MatchHighlightRequest> getMatchHighlightRequests()
    {
        if ( matchHighlightRequests == null )
        {
            matchHighlightRequests = new ArrayList<MatchHighlightRequest>();
        }

        return matchHighlightRequests;
    }

    public void setMatchHighlightRequests( List<MatchHighlightRequest> matchHighlightRequests )
    {
        this.matchHighlightRequests = matchHighlightRequests;
    }

    public boolean isLuceneExplain()
    {
        return luceneExplain;
    }

    public void setLuceneExplain( boolean luceneExplain )
    {
        this.luceneExplain = luceneExplain;
    }
}
