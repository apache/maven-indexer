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

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusIndexMultiReader;
import org.apache.maven.index.context.NexusIndexMultiSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default search engine implementation
 * 
 * @author Eugene Kuleshov
 * @author Tamas Cservenak
 */
@Singleton
@Named
public class DefaultSearchEngine
    implements SearchEngine
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    @Deprecated
    public Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
                                         IndexingContext indexingContext, Query query )
        throws IOException
    {
        return searchFlatPaged( new FlatSearchRequest( query, artifactInfoComparator, indexingContext ),
            Arrays.asList( indexingContext ), true ).getResults();
    }

    @Deprecated
    public Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
                                         Collection<IndexingContext> indexingContexts, Query query )
        throws IOException
    {
        return searchFlatPaged( new FlatSearchRequest( query, artifactInfoComparator ), indexingContexts ).getResults();
    }

    public FlatSearchResponse searchFlatPaged( FlatSearchRequest request, Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchFlatPaged( request, indexingContexts, false );
    }

    public FlatSearchResponse forceSearchFlatPaged( FlatSearchRequest request,
                                                    Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchFlatPaged( request, indexingContexts, true );
    }

    protected FlatSearchResponse searchFlatPaged( FlatSearchRequest request,
                                                  Collection<IndexingContext> indexingContexts, boolean ignoreContext )
        throws IOException
    {
        List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );

        final TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( request.getArtifactInfoComparator() );
        return new FlatSearchResponse( request.getQuery(), searchFlat( request, result, contexts, request.getQuery() ),
            result );
    }

    // ==

    public GroupedSearchResponse searchGrouped( GroupedSearchRequest request,
                                                Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchGrouped( request, indexingContexts, false );
    }

    public GroupedSearchResponse forceSearchGrouped( GroupedSearchRequest request,
                                                     Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchGrouped( request, indexingContexts, true );
    }

    protected GroupedSearchResponse searchGrouped( GroupedSearchRequest request,
                                                   Collection<IndexingContext> indexingContexts, boolean ignoreContext )
        throws IOException
    {
        List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );

        final TreeMap<String, ArtifactInfoGroup> result =
            new TreeMap<String, ArtifactInfoGroup>( request.getGroupKeyComparator() );

        return new GroupedSearchResponse( request.getQuery(), searchGrouped( request, result, request.getGrouping(),
            contexts, request.getQuery() ), result );
    }

    // ===

    protected int searchFlat( FlatSearchRequest req, Collection<ArtifactInfo> result,
                              List<IndexingContext> participatingContexts, Query query )
        throws IOException
    {
        int hitCount = 0;
        for ( IndexingContext context : participatingContexts )
        {
            final IndexSearcher indexSearcher = context.acquireIndexSearcher();
            try
            {
                final TopScoreDocCollector collector = doSearchWithCeiling( req, indexSearcher, query );

                if ( collector.getTotalHits() == 0 )
                {
                    // context has no hits, just continue to next one
                    continue;
                }

                ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

                // uhm btw hitCount contains dups

                hitCount += collector.getTotalHits();

                int start = 0; // from == FlatSearchRequest.UNDEFINED ? 0 : from;

                // we have to pack the results as long: a) we have found aiCount ones b) we depleted hits
                for ( int i = start; i < scoreDocs.length; i++ )
                {
                    Document doc = indexSearcher.doc( scoreDocs[i].doc );

                    ArtifactInfo artifactInfo = IndexUtils.constructArtifactInfo( doc, context );

                    if ( artifactInfo != null )
                    {
                        artifactInfo.setRepository( context.getRepositoryId() );
                        artifactInfo.setContext( context.getId() );

                        if ( req.getArtifactInfoFilter() != null )
                        {
                            if ( !req.getArtifactInfoFilter().accepts( context, artifactInfo ) )
                            {
                                continue;
                            }
                        }
                        if ( req.getArtifactInfoPostprocessor() != null )
                        {
                            req.getArtifactInfoPostprocessor().postprocess( context, artifactInfo );
                        }

                        result.add( artifactInfo );
                    }
                }
            }
            finally
            {
                context.releaseIndexSearcher( indexSearcher );
            }
        }

        return hitCount;
    }

    protected int searchGrouped( GroupedSearchRequest req, Map<String, ArtifactInfoGroup> result, Grouping grouping,
                                 List<IndexingContext> participatingContexts, Query query )
        throws IOException
    {
        int hitCount = 0;

        for ( IndexingContext context : participatingContexts )
        {
            final IndexSearcher indexSearcher = context.acquireIndexSearcher();
            try
            {
                final TopScoreDocCollector collector = doSearchWithCeiling( req, indexSearcher, query );

                if ( collector.getTotalHits() > 0 )
                {
                    ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

                    hitCount += collector.getTotalHits();

                    for ( int i = 0; i < scoreDocs.length; i++ )
                    {
                        Document doc = indexSearcher.doc( scoreDocs[i].doc );

                        ArtifactInfo artifactInfo = IndexUtils.constructArtifactInfo( doc, context );

                        if ( artifactInfo != null )
                        {
                            artifactInfo.setRepository( context.getRepositoryId() );
                            artifactInfo.setContext( context.getId() );

                            if ( req.getArtifactInfoFilter() != null )
                            {
                                if ( !req.getArtifactInfoFilter().accepts( context, artifactInfo ) )
                                {
                                    continue;
                                }
                            }
                            if ( req.getArtifactInfoPostprocessor() != null )
                            {
                                req.getArtifactInfoPostprocessor().postprocess( context, artifactInfo );
                            }

                            if ( !grouping.addArtifactInfo( result, artifactInfo ) )
                            {
                                // fix the hitCount accordingly
                                hitCount--;
                            }
                        }
                    }
                }
            }
            finally
            {
                context.releaseIndexSearcher( indexSearcher );
            }
        }

        return hitCount;
    }

    // == NG Search

    public IteratorSearchResponse searchIteratorPaged( IteratorSearchRequest request,
                                                       Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchIteratorPaged( request, indexingContexts, false );
    }

    public IteratorSearchResponse forceSearchIteratorPaged( IteratorSearchRequest request,
                                                            Collection<IndexingContext> indexingContexts )
        throws IOException
    {
        return searchIteratorPaged( request, indexingContexts, true );
    }

    private IteratorSearchResponse searchIteratorPaged( IteratorSearchRequest request,
                                                        Collection<IndexingContext> indexingContexts,
                                                        boolean ignoreContext )
        throws IOException
    {
        List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );

        NexusIndexMultiReader multiReader = getMergedIndexReader( indexingContexts, ignoreContext );

        NexusIndexMultiSearcher indexSearcher = new NexusIndexMultiSearcher( multiReader );

        try
        {
            TopScoreDocCollector hits = doSearchWithCeiling( request, indexSearcher, request.getQuery() );

            return new IteratorSearchResponse( request.getQuery(), hits.getTotalHits(),
                                               new DefaultIteratorResultSet( request, indexSearcher, contexts,
                                                                             hits.topDocs() ) );
        }
        catch ( IOException e )
        {
            try
            {
                indexSearcher.release();
            }
            catch ( Exception secondary )
            {
                // do not mask original exception
            }
            throw e;
        }
        catch ( RuntimeException e )
        {
            try
            {
                indexSearcher.release();
            }
            catch ( Exception secondary )
            {
                // do not mask original exception
            }
            throw e;
        }
    }

    // ==

    protected TopScoreDocCollector doSearchWithCeiling( final AbstractSearchRequest request,
                                                        final IndexSearcher indexSearcher, final Query query )
        throws IOException
    {
        int topHitCount = getTopDocsCollectorHitNum( request, AbstractSearchRequest.UNDEFINED );

        if ( AbstractSearchRequest.UNDEFINED != topHitCount )
        {
            // count is set, simply just execute it as-is
            final TopScoreDocCollector hits = TopScoreDocCollector.create( topHitCount, true );

            indexSearcher.search( query, hits );

            return hits;
        }
        else
        {
            // set something reasonable as 1k
            topHitCount = 1000;

            // perform search
            TopScoreDocCollector hits = TopScoreDocCollector.create( topHitCount, true );
            indexSearcher.search( query, hits );

            // check total hits against, does it fit?
            if ( topHitCount < hits.getTotalHits() )
            {
                topHitCount = hits.getTotalHits();

                if ( getLogger().isDebugEnabled() )
                {
                    // warn the user and leave trace just before OOM might happen
                    // the hits.getTotalHits() might be HUUGE
                    getLogger().debug(
                        "Executing unbounded search, and fitting topHitCounts to "
                            + topHitCount
                            + ", an OOMEx might follow. To avoid OOM use narrower queries or limit your expectancy with request.setCount() method where appropriate. See MINDEXER-14 for details." );
                }

                // redo all, but this time with correct numbers
                hits = TopScoreDocCollector.create( topHitCount, true );
                indexSearcher.search( query, hits );
            }

            return hits;
        }
    }

    /**
     * Returns the list of participating contexts. Does not locks them, just builds a list of them.
     */
    protected List<IndexingContext> getParticipatingContexts( final Collection<IndexingContext> indexingContexts,
                                                              final boolean ignoreContext )
    {
        // to not change the API all away, but we need stable ordering here
        // filter for those 1st, that take part in here
        final ArrayList<IndexingContext> contexts = new ArrayList<IndexingContext>( indexingContexts.size() );

        for ( IndexingContext ctx : indexingContexts )
        {
            if ( ignoreContext || ctx.isSearchable() )
            {
                contexts.add( ctx );
            }
        }

        return contexts;
    }

    /**
     * Locks down participating contexts, and returns a "merged" reader of them. In case of error, unlocks as part of
     * cleanup and re-throws exception. Without error, it is the duty of caller to unlock contexts!
     * 
     * @param indexingContexts
     * @param ignoreContext
     * @return
     * @throws IOException
     */
    protected NexusIndexMultiReader getMergedIndexReader( final Collection<IndexingContext> indexingContexts,
                                                          final boolean ignoreContext )
        throws IOException
    {
        final List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );
        return new NexusIndexMultiReader( contexts );
    }

    protected int getTopDocsCollectorHitNum( final AbstractSearchRequest request, final int ceiling )
    {
        if ( request instanceof AbstractSearchPageableRequest )
        {
            final AbstractSearchPageableRequest prequest = (AbstractSearchPageableRequest) request;

            if ( AbstractSearchRequest.UNDEFINED != prequest.getCount() )
            {
                // easy, user knows and tells us how many results he want
                return prequest.getCount() + prequest.getStart();
            }
        }
        else
        {
            if ( AbstractSearchRequest.UNDEFINED != request.getCount() )
            {
                // easy, user knows and tells us how many results he want
                return request.getCount();
            }
        }

        return ceiling;
    }
}
