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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.util.IndexCreatorSorter;
import org.codehaus.plexus.util.IOUtil;

/**
 * A default {@link Indexer} implementation.
 * 
 * @author Tamas Cservenak
 */
@Singleton
@Named
public class DefaultIndexer
    implements Indexer
{

    private final SearchEngine searcher;

    private final IndexerEngine indexerEngine;

    private final QueryCreator queryCreator;


    @Inject
    public DefaultIndexer( SearchEngine searcher,
                           IndexerEngine indexerEngine,
                           QueryCreator queryCreator )
    {
        this.searcher = searcher;
        this.indexerEngine = indexerEngine;
        this.queryCreator = queryCreator;
    }

    // ----------------------------------------------------------------------------
    // Contexts
    // ----------------------------------------------------------------------------

    public IndexingContext createIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                                  String repositoryUrl, String indexUpdateUrl, boolean searchable,
                                                  boolean reclaim, List<? extends IndexCreator> indexers )
        throws IOException, ExistingLuceneIndexMismatchException, IllegalArgumentException
    {
        final IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl,
                IndexCreatorSorter.sort( indexers ), reclaim );
        context.setSearchable( searchable );
        return context;
    }

    public IndexingContext createMergedIndexingContext( String id, String repositoryId, File repository,
                                                        File indexDirectory, boolean searchable,
                                                        ContextMemberProvider membersProvider )
        throws IOException
    {
        IndexingContext context =
            new MergedIndexingContext( id, repositoryId, repository, indexDirectory, searchable, membersProvider );
        return context;
    }

    public void closeIndexingContext( IndexingContext context, boolean deleteFiles )
        throws IOException
    {
        context.close( deleteFiles );
    }

    // ----------------------------------------------------------------------------
    // Modifying
    // ----------------------------------------------------------------------------

    public void addArtifactsToIndex( Collection<ArtifactContext> ac, IndexingContext context )
        throws IOException
    {
        if ( ac != null && !ac.isEmpty() )
        {
            for ( ArtifactContext actx : ac )
            {
                indexerEngine.update( context, actx );
            }

            context.commit();
        }
    }

    public void deleteArtifactsFromIndex( Collection<ArtifactContext> ac, IndexingContext context )
        throws IOException
    {
        if ( ac != null && !ac.isEmpty() )
        {
            for ( ArtifactContext actx : ac )
            {
                indexerEngine.remove( context, actx );

                context.commit();
            }
        }
    }

    // ----------------------------------------------------------------------------
    // Searching
    // ----------------------------------------------------------------------------

    public FlatSearchResponse searchFlat( FlatSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            return new FlatSearchResponse( request.getQuery(), 0, Collections.<ArtifactInfo> emptySet() );
        }
        else
        {
            return searcher.forceSearchFlatPaged( request, request.getContexts() );
        }
    }

    public IteratorSearchResponse searchIterator( IteratorSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            return IteratorSearchResponse.empty( request.getQuery() );
        }
        else
        {
            return searcher.forceSearchIteratorPaged( request, request.getContexts() );
        }
    }

    public GroupedSearchResponse searchGrouped( GroupedSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            return new GroupedSearchResponse( request.getQuery(), 0, Collections.<String, ArtifactInfoGroup> emptyMap() );
        }
        else
        {
            // search targeted
            return searcher.forceSearchGrouped( request, request.getContexts() );
        }
    }

    // ----------------------------------------------------------------------------
    // Identification
    // ----------------------------------------------------------------------------

    public Collection<ArtifactInfo> identify( final File artifact, final Collection<IndexingContext> contexts )
        throws IOException
    {
        FileInputStream is = null;
        try
        {
            final MessageDigest sha1 = MessageDigest.getInstance( "SHA-1" );
            is = new FileInputStream( artifact );
            final byte[] buff = new byte[4096];
            int n;
            while ( ( n = is.read( buff ) ) > -1 )
            {
                sha1.update( buff, 0, n );
            }
            byte[] digest = sha1.digest();
            return identify( constructQuery( MAVEN.SHA1, new SourcedSearchExpression( encode( digest ) ) ), contexts );
        }
        catch ( NoSuchAlgorithmException ex )
        {
            IOException ioe = new IOException( "Unable to calculate digest" );
            ioe.initCause( ex );
            throw ioe;
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    public Collection<ArtifactInfo> identify( Query query, Collection<IndexingContext> contexts )
        throws IOException
    {
        final IteratorSearchResponse result =
            searcher.searchIteratorPaged( new IteratorSearchRequest( query ), contexts );
        try
        {
            final ArrayList<ArtifactInfo> ais = new ArrayList<ArtifactInfo>( result.getTotalHitsCount() );
            for ( ArtifactInfo ai : result )
            {
                ais.add( ai );
            }
            return ais;
        }
        finally
        {
            result.close();
        }
    }

    // ----------------------------------------------------------------------------
    // Query construction
    // ----------------------------------------------------------------------------

    public Query constructQuery( Field field, SearchExpression expression )
        throws IllegalArgumentException
    {
        try
        {
            return queryCreator.constructQuery( field, expression );
        }
        catch ( ParseException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    // ==

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private static String encode( byte[] digest )
    {
        char[] buff = new char[digest.length * 2];

        int n = 0;

        for ( byte b : digest )
        {
            buff[n++] = DIGITS[( 0xF0 & b ) >> 4];
            buff[n++] = DIGITS[0x0F & b];
        }

        return new String( buff );
    }
}
