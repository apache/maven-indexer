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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
        return searchFlatPaged( new FlatSearchRequest( query, artifactInfoComparator, indexingContext ) ).getResults();
    }

    @Deprecated
    public Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
                                         Collection<IndexingContext> indexingContexts, Query query )
        throws IOException
    {
        return searchFlatPaged( new FlatSearchRequest( query, artifactInfoComparator ), indexingContexts ).getResults();
    }

    public FlatSearchResponse searchFlatPaged( FlatSearchRequest request )
        throws IOException
    {
        TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( request.getArtifactInfoComparator() );

        int totalHits = 0;

        for ( IndexingContext context : request.getContexts() )
        {
            totalHits +=
                searchFlat( request, result, context, request.getQuery(), request.getStart(), request.getCount() );
        }

        return new FlatSearchResponse( request.getQuery(), totalHits, result );
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

    private FlatSearchResponse searchFlatPaged( FlatSearchRequest request,
                                                Collection<IndexingContext> indexingContexts, boolean ignoreContext )
        throws IOException
    {
        TreeSet<ArtifactInfo> result = new TreeSet<ArtifactInfo>( request.getArtifactInfoComparator() );

        int totalHits = 0;

        for ( IndexingContext ctx : indexingContexts )
        {
            if ( ignoreContext || ctx.isSearchable() )
            {
                int hitCount =
                    searchFlat( request, result, ctx, request.getQuery(), request.getStart(), request.getCount() );

                if ( hitCount == AbstractSearchResponse.LIMIT_EXCEEDED )
                {
                    totalHits = hitCount;
                }
                else
                {
                    totalHits += hitCount;
                }
            }

            if ( request.isHitLimited() && ( totalHits > request.getResultHitLimit() )
                || totalHits == AbstractSearchResponse.LIMIT_EXCEEDED )
            {
                totalHits = AbstractSearchResponse.LIMIT_EXCEEDED;
                result = new TreeSet<ArtifactInfo>( request.getArtifactInfoComparator() );
                break;
            }
        }

        return new FlatSearchResponse( request.getQuery(), totalHits, result );
    }

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
        TreeMap<String, ArtifactInfoGroup> result =
            new TreeMap<String, ArtifactInfoGroup>( request.getGroupKeyComparator() );

        int totalHits = 0;

        for ( IndexingContext ctx : indexingContexts )
        {
            if ( ignoreContext || ctx.isSearchable() )
            {
                int hitCount = searchGrouped( request, result, request.getGrouping(), ctx, request.getQuery() );

                if ( hitCount == AbstractSearchResponse.LIMIT_EXCEEDED )
                {
                    totalHits = hitCount;
                }
                else
                {
                    totalHits += hitCount;
                }
            }

            if ( request.isHitLimited() && ( totalHits > request.getResultHitLimit() )
                || totalHits == AbstractSearchResponse.LIMIT_EXCEEDED )
            {
                totalHits = AbstractSearchResponse.LIMIT_EXCEEDED;
                result = new TreeMap<String, ArtifactInfoGroup>( request.getGroupKeyComparator() );
                break;
            }
        }

        return new GroupedSearchResponse( request.getQuery(), totalHits, result );
    }

    protected int searchFlat( AbstractSearchRequest req, Collection<ArtifactInfo> result, IndexingContext context,
                              Query query, int from, int aiCount )
        throws IOException
    {
        context.lock();

        try
        {
            TopScoreDocCollector collector = TopScoreDocCollector.create( req.getResultHitLimit(), true );

            context.getIndexSearcher().search( query, collector );

            if ( collector.getTotalHits() == 0 )
            {
                return 0;
            }

            if ( req.isHitLimited() && collector.getTotalHits() > req.getResultHitLimit() )
            {
                return AbstractSearchResponse.LIMIT_EXCEEDED;
            }

            ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

            int hitCount = scoreDocs.length;

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

                    result.add( artifactInfo );

                    if ( req.isHitLimited() && result.size() > req.getResultHitLimit() )
                    {
                        // we hit limit, back out now !!
                        return AbstractSearchResponse.LIMIT_EXCEEDED;
                    }
                }
            }

            return hitCount;
        }
        finally
        {
            context.unlock();
        }
    }

    protected int searchGrouped( AbstractSearchRequest req, Map<String, ArtifactInfoGroup> result, Grouping grouping,
                                 IndexingContext context, Query query )
        throws IOException
    {
        context.lock();

        try
        {
            TopScoreDocCollector collector = TopScoreDocCollector.create( req.getResultHitLimit(), true );

            context.getIndexSearcher().search( query, collector );

            if ( collector.getTotalHits() > 0 )
            {
                if ( req.isHitLimited() && collector.getTotalHits() > req.getResultHitLimit() )
                {
                    return AbstractSearchResponse.LIMIT_EXCEEDED;
                }

                ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;

                int hitCount = scoreDocs.length;

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

                return hitCount;
            }
            else
            {
                return 0;
            }
        }
        finally
        {
            context.unlock();
        }
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
        // manage defaults!
        if ( request.getStart() < 0 )
        {
            request.setStart( IteratorSearchRequest.UNDEFINED );
        }
        if ( request.getCount() < 0 )
        {
            request.setCount( IteratorSearchRequest.UNDEFINED );
        }

        try
        {
            // to not change the API all away, but we need stable ordering here
            // filter for those 1st, that take part in here
            ArrayList<IndexingContext> contexts = new ArrayList<IndexingContext>( indexingContexts.size() );

            for ( IndexingContext ctx : indexingContexts )
            {
                if ( ignoreContext || ctx.isSearchable() )
                {
                    contexts.add( ctx );

                    ctx.lock();
                }
            }

            ArrayList<IndexReader> contextsToSearch = new ArrayList<IndexReader>( contexts.size() );

            for ( IndexingContext ctx : contexts )
            {
                contextsToSearch.add( ctx.getIndexReader() );
            }

            MultiReader multiReader =
                new MultiReader( contextsToSearch.toArray( new IndexReader[contextsToSearch.size()] ) );

            IndexSearcher indexSearcher = new NexusIndexSearcher( multiReader );

            // NEXUS-3482 made us to NOT use reverse ordering (it is a fix I wanted to implement, but user contributed
            // patch
            // did come in faster! -- Thanks)
            TopScoreDocCollector hits = TopScoreDocCollector.create( request.getResultHitLimit(), true );

            indexSearcher.search( request.getQuery(), hits );

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
}
