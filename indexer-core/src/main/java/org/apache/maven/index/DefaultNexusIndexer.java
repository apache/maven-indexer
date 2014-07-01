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
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.context.StaticContextMemberProvider;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.util.IndexCreatorSorter;
import org.codehaus.plexus.util.FileUtils;

/**
 * A default {@link NexusIndexer} implementation.
 *
 * @author Tamas Cservenak
 * @author Eugene Kuleshov
 * @deprecated Use {@link Indexer} instead. Discouraged from further use, as it suffers from multiple synchronization
 *             and other problems. As it tries to serve as "context registry" too, but it does not synchronize the
 *             access to it, but also, introduces some extras (like "targeted" vs "non targeted" search), that makes
 *             it's behavior less intuitive.
 */
@Deprecated
@Singleton
@Named
public class DefaultNexusIndexer
    implements NexusIndexer
{

    private final Indexer indexer;

    private final Scanner scanner;

    private final IndexerEngine indexerEngine;

    private final QueryCreator queryCreator;

    private final Map<String, IndexingContext> indexingContexts = new ConcurrentHashMap<String, IndexingContext>();


    @Inject
    public DefaultNexusIndexer( Indexer indexer,
                                Scanner scanner,
                                IndexerEngine indexerEngine,
                                QueryCreator queryCreator )
    {
        this.indexer = indexer;
        this.scanner = scanner;
        this.indexerEngine = indexerEngine;
        this.queryCreator = queryCreator;
    }

    // ----------------------------------------------------------------------------
    // Contexts
    // ----------------------------------------------------------------------------

    public void addIndexingContext( IndexingContext context )
    {
        indexingContexts.put( context.getId(), context );
    }

    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
        String repositoryUrl, String indexUpdateUrl,
        List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        try
        {
            IndexingContext context =
                indexer.createIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl,
                                               indexUpdateUrl, true, false, indexers );
            indexingContexts.put( context.getId(), context );
            return context;
        }
        catch ( ExistingLuceneIndexMismatchException e )
        {
            throw new UnsupportedExistingLuceneIndexException( e.getMessage(), e );
        }
    }

    public IndexingContext addIndexingContextForced( String id, String repositoryId, File repository,
        File indexDirectory, String repositoryUrl, String indexUpdateUrl,
        List<? extends IndexCreator> indexers )
        throws IOException
    {
        IndexingContext context =
            indexer.createIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl,
                                           true, true, indexers );
        indexingContexts.put( context.getId(), context );
        return context;
    }

    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, Directory directory,
        String repositoryUrl, String indexUpdateUrl,
        List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        try
        {
            IndexingContext context =
                new DefaultIndexingContext( id, repositoryId, repository, directory, repositoryUrl, indexUpdateUrl,
                                            IndexCreatorSorter.sort( indexers ), false );
            indexingContexts.put( context.getId(), context );
            return context;
        }
        catch ( ExistingLuceneIndexMismatchException e )
        {
            throw new UnsupportedExistingLuceneIndexException( e.getMessage(), e );
        }
    }

    public IndexingContext addIndexingContextForced( String id, String repositoryId, File repository,
        Directory directory, String repositoryUrl, String indexUpdateUrl,
        List<? extends IndexCreator> indexers )
        throws IOException
    {
        IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, directory, repositoryUrl, indexUpdateUrl,
                                        IndexCreatorSorter.sort( indexers ), true );
        indexingContexts.put( context.getId(), context );
        return context;
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
        File indexDirectory, boolean searchable,
        Collection<IndexingContext> contexts )
        throws IOException
    {
        return addMergedIndexingContext( id, repositoryId, repository, indexDirectory, searchable,
                                         new StaticContextMemberProvider( contexts ) );
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
        File indexDirectory, boolean searchable,
        ContextMemberProvider membersProvider )
        throws IOException
    {
        IndexingContext context =
            indexer.createMergedIndexingContext( id, repositoryId, repository, indexDirectory, searchable,
                                                 membersProvider );
        indexingContexts.put( context.getId(), context );
        return context;
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
        Directory indexDirectory, boolean searchable,
        Collection<IndexingContext> contexts )
        throws IOException
    {
        IndexingContext context =
            new MergedIndexingContext( id, repositoryId, repository, indexDirectory, searchable,
                                       new StaticContextMemberProvider( contexts ) );
        indexingContexts.put( context.getId(), context );
        return context;
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
        Directory indexDirectory, boolean searchable,
        ContextMemberProvider membersProvider )
        throws IOException
    {
        IndexingContext context =
            new MergedIndexingContext( id, repositoryId, repository, indexDirectory, searchable, membersProvider );
        indexingContexts.put( context.getId(), context );
        return context;
    }

    public void removeIndexingContext( IndexingContext context, boolean deleteFiles )
        throws IOException
    {
        if ( indexingContexts.containsKey( context.getId() ) )
        {
            indexingContexts.remove( context.getId() );
            indexer.closeIndexingContext( context, deleteFiles );
        }
    }

    public Map<String, IndexingContext> getIndexingContexts()
    {
        return Collections.unmodifiableMap( indexingContexts );
    }

    // ----------------------------------------------------------------------------
    // Scanning
    // ----------------------------------------------------------------------------

    public void scan( final IndexingContext context )
        throws IOException
    {
        scan( context, null );
    }

    public void scan( final IndexingContext context, boolean update )
        throws IOException
    {
        scan( context, null, update );
    }

    public void scan( final IndexingContext context, final ArtifactScanningListener listener )
        throws IOException
    {
        scan( context, listener, false );
    }

    public void scan( final IndexingContext context, final ArtifactScanningListener listener, final boolean update )
        throws IOException
    {
        scan( context, null, listener, update );
    }

    /**
     * Uses {@link Scanner} to scan repository content. A {@link ArtifactScanningListener} is used to process found
     * artifacts and to add them to the index using
     * {@link NexusIndexer#artifactDiscovered(ArtifactContext, IndexingContext)}.
     *
     * @see DefaultScannerListener
     * @see #artifactDiscovered(ArtifactContext, IndexingContext)
     */
    public void scan( final IndexingContext context, final String fromPath, final ArtifactScanningListener listener,
        final boolean update )
        throws IOException
    {
        final File repositoryDirectory = context.getRepository();
        if ( repositoryDirectory == null )
        {
            // nothing to scan
            return;
        }

        if ( !repositoryDirectory.exists() )
        {
            throw new IOException( "Repository directory " + repositoryDirectory + " does not exist" );
        }

        // always use temporary context when reindexing
        final File tmpFile = File.createTempFile( context.getId() + "-tmp", "" );
        final File tmpDir = new File( tmpFile.getParentFile(), tmpFile.getName() + ".dir" );
        if ( !tmpDir.mkdirs() )
        {
            throw new IOException( "Cannot create temporary directory: " + tmpDir );
        }

        IndexingContext tmpContext = null;
        try
        {
            final FSDirectory directory = FSDirectory.open( tmpDir );
            if ( update )
            {
                IndexUtils.copyDirectory( context.getIndexDirectory(), directory );
            }
            tmpContext = new DefaultIndexingContext( context.getId() + "-tmp", //
                                                     context.getRepositoryId(), //
                                                     context.getRepository(), //
                                                     directory, //
                                                     context.getRepositoryUrl(), //
                                                     context.getIndexUpdateUrl(), //
                                                     context.getIndexCreators(), //
                                                     true );

            scanner.scan( new ScanningRequest( tmpContext, //
                                               new DefaultScannerListener( tmpContext, indexerEngine,
                                                                           update, listener ), fromPath ) );

            tmpContext.updateTimestamp( true );
            context.replace( tmpContext.getIndexDirectory() );
        }
        catch ( Exception ex )
        {
            throw (IOException) new IOException( "Error scanning context " + context.getId() + ": " + ex ).initCause(
                ex );
        }
        finally
        {
            if ( tmpContext != null )
            {
                tmpContext.close( true );
            }

            if ( tmpFile.exists() )
            {
                tmpFile.delete();
            }

            FileUtils.deleteDirectory( tmpDir );
        }
    }

    /**
     * Delegates to the {@link IndexerEngine} to add a new artifact to the index
     */
    public void artifactDiscovered( ArtifactContext ac, IndexingContext context )
        throws IOException
    {
        if ( ac != null )
        {
            indexerEngine.index( context, ac );
        }
    }

    // ----------------------------------------------------------------------------
    // Modifying
    // ----------------------------------------------------------------------------

    /**
     * Delegates to the {@link IndexerEngine} to update artifact to the index
     */
    public void addArtifactToIndex( ArtifactContext ac, IndexingContext context )
        throws IOException
    {
        indexer.addArtifactsToIndex( Collections.singleton( ac ), context );
    }

    public void addArtifactsToIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException
    {
        indexer.addArtifactsToIndex( acs, context );
    }

    /**
     * Delegates to the {@link IndexerEngine} to remove artifact from the index
     */
    public void deleteArtifactFromIndex( ArtifactContext ac, IndexingContext context )
        throws IOException
    {
        indexer.deleteArtifactsFromIndex( Collections.singleton( ac ), context );
    }

    public void deleteArtifactsFromIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException
    {
        indexer.deleteArtifactsFromIndex( acs, context );
    }

    // ----------------------------------------------------------------------------
    // Searching
    // ----------------------------------------------------------------------------

    public FlatSearchResponse searchFlat( FlatSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            request.getContexts().addAll( getIndexingContexts().values() );
        }
        return indexer.searchFlat( request );
    }

    public IteratorSearchResponse searchIterator( IteratorSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            request.getContexts().addAll( getIndexingContexts().values() );
        }
        return indexer.searchIterator( request );
    }

    public GroupedSearchResponse searchGrouped( GroupedSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            request.getContexts().addAll( getIndexingContexts().values() );
        }
        return indexer.searchGrouped( request );
    }

    // ----------------------------------------------------------------------------
    // Query construction
    // ----------------------------------------------------------------------------

    @Deprecated
    public Query constructQuery( Field field, String query, SearchType type )
        throws IllegalArgumentException
    {
        try
        {
            return queryCreator.constructQuery( field, query, type );
        }
        catch ( ParseException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    public Query constructQuery( Field field, SearchExpression expression )
        throws IllegalArgumentException
    {
        return indexer.constructQuery( field, expression );
    }

    // ----------------------------------------------------------------------------
    // Identification
    // ----------------------------------------------------------------------------

    public Collection<ArtifactInfo> identify( Field field, String query )
        throws IllegalArgumentException, IOException
    {
        return identify( constructQuery( field, query, SearchType.EXACT ) );
    }

    public Collection<ArtifactInfo> identify( File artifact )
        throws IOException
    {
        return identify( artifact, indexingContexts.values() );
    }

    public Collection<ArtifactInfo> identify( File artifact, Collection<IndexingContext> contexts )
        throws IOException
    {
        return indexer.identify( artifact, contexts );
    }

    public Collection<ArtifactInfo> identify( Query query )
        throws IOException
    {
        return identify( query, indexingContexts.values() );
    }

    public Collection<ArtifactInfo> identify( Query query, Collection<IndexingContext> contexts )
        throws IOException
    {
        return indexer.identify( query, contexts );
    }
}
