package org.apache.maven.indexer.examples.indexing;

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

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

/**
 * This class provides means to index and search for artifacts in a repository on the file system.
 *
 * @author mtodorov
 */
public class RepositoryIndexer
{

    private static final Logger LOGGER = LoggerFactory.getLogger( RepositoryIndexer.class );

    private static final String[] LUCENE_FIELDS = new String[]{"g", "a", "v", "p", "c" };

    private static final WhitespaceAnalyzer LUCENE_ANALYZER = new WhitespaceAnalyzer( );

    private Indexer indexer;

    private Scanner scanner;

    private List<IndexCreator> indexers;

    private IndexingContext indexingContext;

    private String repositoryId;

    private File repositoryBasedir;

    private File indexDir;


    public RepositoryIndexer()
    {
        // no op
    }

    public void close()
        throws IOException
    {
        indexer.closeIndexingContext( indexingContext, false );
    }

    public void close( boolean deleteFiles )
        throws IOException
    {
        indexingContext.close( deleteFiles );
    }

    public void delete( final Collection<ArtifactInfo> artifacts )
        throws IOException
    {
        final List<ArtifactContext> delete = new ArrayList<>();
        for ( final ArtifactInfo artifact : artifacts )
        {
            LOGGER.debug( "Deleting artifact: {}; ctx id: {}; idx dir: {}",
                          artifact, indexingContext.getId(), indexingContext.getIndexDirectory() );

            delete.add( new ArtifactContext( null, null, null, artifact, null ) );
        }

        getIndexer().deleteArtifactsFromIndex( delete, indexingContext );
    }

    public Set<ArtifactInfo> search( final String groupId, final String artifactId, final String version,
                                     final String packaging, final String classifier )
        throws IOException
    {
        final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        if ( groupId != null )
        {
            queryBuilder.add( getIndexer().constructQuery( MAVEN.GROUP_ID,
                    new SourcedSearchExpression( groupId ) ), MUST );
        }

        if ( artifactId != null )
        {
            queryBuilder.add( getIndexer().constructQuery( MAVEN.ARTIFACT_ID,
                    new SourcedSearchExpression( artifactId ) ), MUST );
        }

        if ( version != null )
        {
            queryBuilder.add( getIndexer().constructQuery( MAVEN.VERSION,
                    new SourcedSearchExpression( version ) ), MUST );
        }

        if ( packaging != null )
        {
            queryBuilder.add( getIndexer().constructQuery( MAVEN.PACKAGING,
                    new SourcedSearchExpression( packaging ) ), MUST );
        }
        else
        {
            // Fallback to jar
            queryBuilder.add( getIndexer().constructQuery( MAVEN.PACKAGING,
                    new SourcedSearchExpression( "jar" ) ), MUST );
        }

        if ( classifier != null )
        {
            queryBuilder.add( getIndexer().constructQuery( MAVEN.CLASSIFIER,
                    new SourcedSearchExpression( classifier ) ),
                       MUST );
        }

        Query query = queryBuilder.build();

        LOGGER.debug( "Executing search query: {}; ctx id: {}; idx dir: {}",
                      query, indexingContext.getId(), indexingContext.getIndexDirectory() );

        final FlatSearchResponse response = getIndexer().searchFlat( new FlatSearchRequest( query, indexingContext ) );

        LOGGER.info( "Hit count: {}", response.getReturnedHitsCount() );

        final Set<ArtifactInfo> results = response.getResults();
        if ( LOGGER.isDebugEnabled() )
        {
            for ( final ArtifactInfo result : results )
            {
                LOGGER.debug( "Found artifact: {}", result.toString() );
            }
        }

        return results;
    }

    public Set<ArtifactInfo> search( final String queryText )
        throws ParseException, IOException
    {
        final Query query = new MultiFieldQueryParser( LUCENE_FIELDS, LUCENE_ANALYZER ).parse( queryText );

        LOGGER.debug( "Executing search query: {}; ctx id: {}; idx dir: {}",
                      query, indexingContext.getId(), indexingContext.getIndexDirectory() );

        final FlatSearchResponse response = getIndexer().searchFlat( new FlatSearchRequest( query, indexingContext ) );

        final Set<ArtifactInfo> results = response.getResults();
        if ( LOGGER.isDebugEnabled() )
        {
            LOGGER.debug( "Hit count: {}", response.getReturnedHitsCount() );

            for ( final ArtifactInfo result : results )
            {
                LOGGER.debug( "Found artifact: {}; uinfo: {}", result.toString(), result.getUinfo() );
            }
        }

        return results;
    }

    public Set<ArtifactInfo> searchBySHA1( final String checksum )
        throws IOException
    {
        final BooleanQuery query = new BooleanQuery.Builder()
                .add( getIndexer().constructQuery( MAVEN.SHA1, new SourcedSearchExpression( checksum ) ), MUST )
                .build();

        LOGGER.debug( "Executing search query: {}; ctx id: {}; idx dir: {}",
                      query, indexingContext.getId(), indexingContext.getIndexDirectory() );

        final FlatSearchResponse response = getIndexer().searchFlat( new FlatSearchRequest( query, indexingContext ) );

        LOGGER.info( "Hit count: {}", response.getReturnedHitsCount() );

        final Set<ArtifactInfo> results = response.getResults();
        if ( LOGGER.isDebugEnabled() )
        {
            for ( final ArtifactInfo result : results )
            {
                LOGGER.debug( "Found artifact: {}", result.toString() );
            }
        }

        return results;
    }

    public int index( final File startingPath )
    {
        final ScanningResult scan = getScanner().scan(
            new ScanningRequest( indexingContext, new ReindexArtifactScanningListener(),
                                 startingPath == null ? "." : startingPath.getPath() ) );
        return scan.getTotalFiles();
    }

    public void addArtifactToIndex( final File artifactFile, final ArtifactInfo artifactInfo )
        throws IOException
    {
        getIndexer().addArtifactsToIndex(
                asList( new ArtifactContext( null, artifactFile, null, artifactInfo, null ) ),
                indexingContext );
    }

    public void addArtifactToIndex( String repository, File artifactFile, String groupId, String artifactId,
                                    String version, String extension, String classifier )
        throws IOException
    {
        ArtifactInfo artifactInfo = new ArtifactInfo( repository, groupId, artifactId, version, classifier, extension );
        if ( extension != null )
        {
            artifactInfo.setFieldValue( MAVEN.EXTENSION, extension );
        }

        LOGGER.debug( "Adding artifact: {}; repo: {}; type: {}", artifactInfo.getUinfo(), repository, extension );

        getIndexer().addArtifactsToIndex(
            asList( new ArtifactContext( null, artifactFile, null, artifactInfo, artifactInfo.calculateGav() ) ),
            indexingContext );
    }

    private class ReindexArtifactScanningListener
        implements ArtifactScanningListener
    {

        int totalFiles = 0;

        private IndexingContext context;

        @Override
        public void scanningStarted( final IndexingContext context )
        {
            this.context = context;
        }

        @Override
        public void scanningFinished( final IndexingContext context, final ScanningResult result )
        {
            result.setTotalFiles( totalFiles );
            LOGGER.debug( "Scanning finished; total files: {}; has exception: {}", result.getTotalFiles(),
                          result.hasExceptions() );
        }

        @Override
        public void artifactError( final ArtifactContext ac, final Exception ex )
        {
            LOGGER.error( "Artifact error!", ex );
        }

        @Override
        public void artifactDiscovered( final ArtifactContext ac )
        {
            try
            {
                LOGGER.debug( "Adding artifact gav: {}; ctx id: {}; idx dir: {}",
                        ac.getGav(), context.getId(), context.getIndexDirectory() );

                getIndexer().addArtifactsToIndex( asList( ac ), context );
                totalFiles++;
            }
            catch ( IOException ex )
            {
                LOGGER.error( "Artifact index error", ex );
            }
        }
    }

    public Indexer getIndexer()
    {
        return indexer;
    }

    public void setIndexer( Indexer indexer )
    {
        this.indexer = indexer;
    }

    public Scanner getScanner()
    {
        return scanner;
    }

    public void setScanner( Scanner scanner )
    {
        this.scanner = scanner;
    }

    public List<IndexCreator> getIndexers()
    {
        return indexers;
    }

    public void setIndexers( List<IndexCreator> indexers )
    {
        this.indexers = indexers;
    }

    public IndexingContext getIndexingContext()
    {
        return indexingContext;
    }

    public void setIndexingContext( IndexingContext indexingContext )
    {
        this.indexingContext = indexingContext;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public File getRepositoryBasedir()
    {
        return repositoryBasedir;
    }

    public void setRepositoryBasedir( File repositoryBasedir )
    {
        this.repositoryBasedir = repositoryBasedir;
    }

    public File getIndexDir()
    {
        return indexDir;
    }

    public void setIndexDir( File indexDir )
    {
        this.indexDir = indexDir;
    }

}
