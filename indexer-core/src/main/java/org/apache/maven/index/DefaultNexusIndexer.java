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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.context.StaticContextMemberProvider;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

/**
 * A default {@link NexusIndexer} implementation.
 * 
 * @author Tamas Cservenak
 * @author Eugene Kuleshov
 */
@Component( role = NexusIndexer.class )
public class DefaultNexusIndexer
    extends AbstractLogEnabled
    implements NexusIndexer
{

    @Requirement
    private Scanner scanner;

    @Requirement
    private SearchEngine searcher;

    @Requirement
    private IndexerEngine indexerEngine;

    @Requirement
    private QueryCreator queryCreator;

    private Map<String, IndexingContext> indexingContexts;

    public DefaultNexusIndexer()
    {
        this.indexingContexts = new ConcurrentHashMap<String, IndexingContext>();
    }

    // ----------------------------------------------------------------------------
    // Contexts
    // ----------------------------------------------------------------------------

    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                               String repositoryUrl, String indexUpdateUrl,
                                               List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl,
                indexers, false );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public IndexingContext addIndexingContextForced( String id, String repositoryId, File repository,
                                                     File indexDirectory, String repositoryUrl, String indexUpdateUrl,
                                                     List<? extends IndexCreator> indexers )
        throws IOException
    {
        IndexingContext context = null;

        try
        {
            context =
                new DefaultIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl,
                    indexUpdateUrl, indexers, true );

            indexingContexts.put( context.getId(), context );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            // will not be thrown
        }

        return context;
    }

    @Deprecated
    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                               String repositoryUrl, String indexUpdateUrl,
                                               List<? extends IndexCreator> indexers, boolean reclaimIndexOwnership )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl,
                indexers, reclaimIndexOwnership );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, Directory directory,
                                               String repositoryUrl, String indexUpdateUrl,
                                               List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, directory, repositoryUrl, indexUpdateUrl,
                indexers, false );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public IndexingContext addIndexingContextForced( String id, String repositoryId, File repository,
                                                     Directory directory, String repositoryUrl, String indexUpdateUrl,
                                                     List<? extends IndexCreator> indexers )
        throws IOException
    {
        IndexingContext context = null;

        try
        {
            context =
                new DefaultIndexingContext( id, repositoryId, repository, directory, repositoryUrl, indexUpdateUrl,
                    indexers, true );

            indexingContexts.put( context.getId(), context );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            // will not be thrown
        }

        return context;
    }

    @Deprecated
    public IndexingContext addIndexingContext( String id, String repositoryId, File repository, Directory directory,
                                               String repositoryUrl, String indexUpdateUrl,
                                               List<? extends IndexCreator> indexers, boolean reclaimIndexOwnership )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        IndexingContext context =
            new DefaultIndexingContext( id, repositoryId, repository, directory, repositoryUrl, indexUpdateUrl,
                indexers, reclaimIndexOwnership );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
                                                     boolean searchable, Collection<IndexingContext> contexts )
        throws IOException
    {
        IndexingContext context =
            new MergedIndexingContext( id, repositoryId, repository, searchable, new StaticContextMemberProvider(
                contexts ) );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
                                                     boolean searchable, ContextMemberProvider membersProvider )
        throws IOException
    {
        IndexingContext context = new MergedIndexingContext( id, repositoryId, repository, searchable, membersProvider );

        indexingContexts.put( context.getId(), context );

        return context;
    }

    public void removeIndexingContext( IndexingContext context, boolean deleteFiles )
        throws IOException
    {
        if ( indexingContexts.containsKey( context.getId() ) )
        {
            indexingContexts.remove( context.getId() );
            context.close( deleteFiles );
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
        File repositoryDirectory = context.getRepository();

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
        File indexDir = context.getIndexDirectoryFile();
        File dir = null;
        if ( indexDir != null )
        {
            dir = indexDir.getParentFile();
        }

        File tmpFile = File.createTempFile( context.getId() + "-tmp", "", dir );
        File tmpDir = new File( tmpFile.getParentFile(), tmpFile.getName() + ".dir" );
        if ( !tmpDir.mkdirs() )
        {
            throw new IOException( "Cannot create temporary directory: " + tmpDir );
        }

        IndexingContext tmpContext = null;
        try
        {
            FSDirectory directory = FSDirectory.getDirectory( tmpDir );

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
                new DefaultScannerListener( tmpContext, indexerEngine, update, listener ), fromPath ) );

            tmpContext.updateTimestamp( true );
            context.replace( tmpContext.getIndexDirectory() );

            removeIndexingContext( tmpContext, true );
        }
        catch ( Exception ex )
        {
            throw (IOException) new IOException( "Error scanning context " + context.getId() + ": " + ex ).initCause( ex );
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
        if ( ac != null )
        {
            indexerEngine.update( context, ac );

            context.commit();
        }
    }

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

    /**
     * Delegates to the {@link IndexerEngine} to remove artifact from the index
     */
    public void deleteArtifactFromIndex( ArtifactContext ac, IndexingContext context )
        throws IOException
    {
        if ( ac != null )
        {
            indexerEngine.remove( context, ac );

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

    /**
     * @deprecated use {@link #searchFlat(FlatSearchRequest)} instead
     */
    @Deprecated
    public Collection<ArtifactInfo> searchFlat( Query query )
        throws IOException
    {
        return searchFlat( ArtifactInfo.VERSION_COMPARATOR, query );
    }

    /**
     * @deprecated use {@link #searchFlat(FlatSearchRequest)} instead
     */
    @Deprecated
    public Collection<ArtifactInfo> searchFlat( Query query, IndexingContext context )
        throws IOException
    {
        return searchFlat( ArtifactInfo.VERSION_COMPARATOR, query, context );
    }

    /**
     * @deprecated use {@link #searchFlat(FlatSearchRequest)} instead
     */
    @Deprecated
    public Collection<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator, Query query )
        throws IOException
    {
        return searcher.searchFlat( artifactInfoComparator, indexingContexts.values(), query );
    }

    /**
     * @deprecated use {@link #searchFlat(FlatSearchRequest)} instead
     */
    @Deprecated
    public Collection<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator, Query query,
                                                IndexingContext context )
        throws IOException
    {
        return searcher.searchFlat( artifactInfoComparator, context, query );
    }

    public FlatSearchResponse searchFlat( FlatSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            return searcher.searchFlatPaged( request, indexingContexts.values() );
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
            return searcher.searchIteratorPaged( request, indexingContexts.values() );
        }
        else
        {
            return searcher.forceSearchIteratorPaged( request, request.getContexts() );
        }
    }

    /**
     * @deprecated use {@link #searchGrouped(GroupedSearchRequest)

     */
    @Deprecated
    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Query query )
        throws IOException
    {
        return searchGrouped( grouping, String.CASE_INSENSITIVE_ORDER, query );
    }

    /**
     * @deprecated use {@link #searchGrouped(GroupedSearchRequest)

     */
    @Deprecated
    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Query query, IndexingContext context )
        throws IOException
    {
        return searchGrouped( grouping, String.CASE_INSENSITIVE_ORDER, query, context );
    }

    /**
     * @deprecated use {@link #searchGrouped(GroupedSearchRequest)

     */
    @Deprecated
    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Comparator<String> groupKeyComparator,
                                                         Query query )
        throws IOException
    {
        return searcher.searchGrouped( new GroupedSearchRequest( query, grouping, groupKeyComparator ),
            indexingContexts.values() ).getResults();
    }

    /**
     * @deprecated use {@link #searchGrouped(GroupedSearchRequest)

     */
    @Deprecated
    public Map<String, ArtifactInfoGroup> searchGrouped( Grouping grouping, Comparator<String> groupKeyComparator,
                                                         Query query, IndexingContext context )
        throws IOException
    {
        return searcher.searchGrouped( new GroupedSearchRequest( query, grouping, groupKeyComparator ),
            Arrays.asList( new IndexingContext[] { context } ) ).getResults();
    }

    public GroupedSearchResponse searchGrouped( GroupedSearchRequest request )
        throws IOException
    {
        if ( request.getContexts().isEmpty() )
        {
            // search all
            return searcher.searchGrouped( request, indexingContexts.values() );
        }
        else
        {
            // search targeted
            return searcher.forceSearchGrouped( request, request.getContexts() );
        }
    }

    // ----------------------------------------------------------------------------
    // Query construction
    // ----------------------------------------------------------------------------

    public Query constructQuery( String field, String query )
    {
        return queryCreator.constructQuery( field, query );
    }

    public Query constructQuery( Field field, String query, SearchType type )
    {
        return queryCreator.constructQuery( field, query, type );
    }

    // ----------------------------------------------------------------------------
    // Identification
    // ----------------------------------------------------------------------------

    @Deprecated
    public ArtifactInfo identify( String field, String query )
        throws IOException
    {
        return identify( new TermQuery( new Term( field, query ) ) );
    }

    public ArtifactInfo identify( Field field, String query )
        throws IOException
    {
        return identify( constructQuery( field, query, SearchType.EXACT ) );
    }

    public ArtifactInfo identify( File artifact )
        throws IOException
    {
        return identify( artifact, indexingContexts.values() );
    }

    public ArtifactInfo identify( File artifact, Collection<IndexingContext> contexts )
        throws IOException
    {
        FileInputStream is = null;

        try
        {
            MessageDigest sha1 = MessageDigest.getInstance( "SHA-1" );

            is = new FileInputStream( artifact );

            byte[] buff = new byte[4096];

            int n;

            while ( ( n = is.read( buff ) ) > -1 )
            {
                sha1.update( buff, 0, n );
            }

            byte[] digest = sha1.digest();

            Query q = constructQuery( MAVEN.SHA1, encode( digest ), SearchType.EXACT );

            return identify( q, contexts );
        }
        catch ( NoSuchAlgorithmException ex )
        {
            throw new IOException( "Unable to calculate digest" );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    public ArtifactInfo identify( Query query )
        throws IOException
    {
        return identify( query, indexingContexts.values() );
    }

    public ArtifactInfo identify( Query query, Collection<IndexingContext> contexts )
        throws IOException
    {
        IteratorSearchResponse result = searcher.searchIteratorPaged( new IteratorSearchRequest( query ), contexts );

        try
        {
            // TODO: this implementation is flakey: case a) 0 hits is okay, b) 1 hit is okay, c1) >1 hits and all same
            // GAVs
            // -- okay but which source repo will be used? c2) >1 hits, and different GAVs --- huh?
            if ( result.getTotalHits() > 0 )
            {
                return result.getResults().next();
            }
            else
            {
                return null;
            }
        }
        finally
        {
            result.close();
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
