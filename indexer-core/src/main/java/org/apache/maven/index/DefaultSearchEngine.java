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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusIndexSearcher;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * A default search engine implementation
 * 
 * @author Eugene Kuleshov
 * @author Tamas Cservenak
 */
@Component( role = SearchEngine.class )
public class DefaultSearchEngine
    extends AbstractLogEnabled
    implements SearchEngine
{
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

        try
        {
            final TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( request.getArtifactInfoComparator() );

            for ( IndexingContext ctx : contexts )
            {
                ctx.lock();
            }

            return new FlatSearchResponse( request.getQuery(), searchFlat( request, result, contexts,
                request.getQuery() ), result );
        }
        finally
        {
            for ( IndexingContext ctx : contexts )
            {
                ctx.unlock();
            }
        }
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

        try
        {
            final TreeMap<String, ArtifactInfoGroup> result =
                new TreeMap<String, ArtifactInfoGroup>( request.getGroupKeyComparator() );

            for ( IndexingContext ctx : contexts )
            {
                ctx.lock();
            }

            return new GroupedSearchResponse( request.getQuery(), searchGrouped( request, result,
                request.getGrouping(), contexts, request.getQuery() ), result );

        }
        finally
        {
            for ( IndexingContext ctx : contexts )
            {
                ctx.unlock();
            }
        }
    }

    // ===

    protected int searchFlat( FlatSearchRequest req, Collection<ArtifactInfo> result,
                              List<IndexingContext> participatingContexts, Query query )
        throws IOException
    {
        int hitCount = 0;

        Set<ArtifactInfo> nonDuplicateResults = new TreeSet<ArtifactInfo>( ArtifactInfo.VERSION_COMPARATOR );


        for ( IndexingContext context : participatingContexts )
        {
            final TopScoreDocCollector collector = doSearchWithCeiling( req, context.getIndexSearcher(), query );

            // olamy if the first context used doesn't find the other are not used for search
            // so the result can probably returns duplicate as artifactInfo doesn't implements hashCode/equals
            // so implements this in nonDuplicateResults
            /*
            if ( collector.getTotalHits() == 0 )
            {
                return 0;
            }
            */

            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

            // uhm btw hitCount contains dups

            hitCount += collector.getTotalHits();

            int start = 0; // from == FlatSearchRequest.UNDEFINED ? 0 : from;

            // we have to pack the results as long: a) we have found aiCount ones b) we depleted hits
            for ( int i = start; i < scoreDocs.length; i++ )
            {
                Document doc = context.getIndexSearcher().doc( scoreDocs[i].doc );

                ArtifactInfo artifactInfo = IndexUtils.constructArtifactInfo( doc, context );

                if ( artifactInfo != null )
                {
                    artifactInfo.repository = context.getRepositoryId();

                    artifactInfo.context = context.getId();

                    nonDuplicateResults.add( artifactInfo );

                }
            }
        }

        result.addAll( nonDuplicateResults );

        return hitCount;
    }

    protected int searchGrouped( GroupedSearchRequest req, Map<String, ArtifactInfoGroup> result, Grouping grouping,
                                 List<IndexingContext> participatingContexts, Query query )
        throws IOException
    {
        int hitCount = 0;

        for ( IndexingContext context : participatingContexts )
        {
            final TopScoreDocCollector collector = doSearchWithCeiling( req, context.getIndexSearcher(), query );

            if ( collector.getTotalHits() > 0 )
            {
                ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

                hitCount += collector.getTotalHits();

                for ( int i = 0; i < scoreDocs.length; i++ )
                {
                    Document doc = context.getIndexSearcher().doc( scoreDocs[i].doc );

                    ArtifactInfo artifactInfo = IndexUtils.constructArtifactInfo( doc, context );

                    if ( artifactInfo != null )
                    {
                        artifactInfo.repository = context.getRepositoryId();

                        artifactInfo.context = context.getId();

                        if ( !grouping.addArtifactInfo( result, artifactInfo ) )
                        {
                            // fix the hitCount accordingly
                            hitCount--;
                        }
                    }
                }
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
        try
        {
            List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );

            IndexReader multiReader = getMergedIndexReader( indexingContexts, ignoreContext );

            IndexSearcher indexSearcher = new NexusIndexSearcher( multiReader );

            TopScoreDocCollector hits = doSearchWithCeiling( request, indexSearcher, request.getQuery() );

            return new IteratorSearchResponse( request.getQuery(), hits.getTotalHits(), new DefaultIteratorResultSet(
                request, indexSearcher, contexts, hits.topDocs() ) );
        }
        catch ( Throwable e )
        {
            // perform cleaup, otherwise DefaultIteratorResultSet will do it
            for ( IndexingContext ctx : indexingContexts )
            {
                if ( ignoreContext || ctx.isSearchable() )
                {
                    ctx.unlock();
                }
            }

            if ( e instanceof IOException )
            {
                throw (IOException) e;
            }
            else
            {
                // wrap it
                IOException ex = new IOException( e.getMessage() );
                ex.initCause( e );
                throw ex;
            }
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
    protected IndexReader getMergedIndexReader( final Collection<IndexingContext> indexingContexts,
                                                final boolean ignoreContext )
        throws IOException
    {
        final List<IndexingContext> contexts = getParticipatingContexts( indexingContexts, ignoreContext );

        try
        {
            final ArrayList<IndexReader> contextsToSearch = new ArrayList<IndexReader>( contexts.size() );

            for ( IndexingContext ctx : contexts )
            {
                ctx.lock();

                contextsToSearch.add( ctx.getIndexReader() );
            }

            MultiReader multiReader =
                new MultiReader( contextsToSearch.toArray( new IndexReader[contextsToSearch.size()] ) );

            return multiReader;
        }
        catch ( Throwable e )
        {
            // perform cleaup, otherwise DefaultIteratorResultSet will do it
            for ( IndexingContext ctx : contexts )
            {
                ctx.unlock();
            }

            if ( e instanceof IOException )
            {
                throw (IOException) e;
            }
            else
            {
                // wrap it
                IOException ex = new IOException( e.getMessage() );
                ex.initCause( e );
                throw ex;
            }
        }
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
