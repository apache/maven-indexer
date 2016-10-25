package org.apache.maven.index.updater;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.IOUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.ReturnValueAction;
import org.jmock.lib.action.VoidAction;
import org.junit.Ignore;

/**
 * @author Eugene Kuleshov
 */
public class DefaultIndexUpdaterTest
    extends AbstractIndexUpdaterTest
{

    SimpleDateFormat df = new SimpleDateFormat( "yyyyMMddHHmmss.SSS Z" );

    public void testReplaceIndex()
        throws Exception
    {
        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ),
            context );

        Query q = indexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-lang", SearchType.SCORED );

        FlatSearchResponse response1 = indexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> content1 = response1.getResults();

        assertEquals( content1.toString(), 1, content1.size() );

        // updated index

        Directory tempIndexDirectory = new RAMDirectory();

        IndexingContext tempContext =
            indexer.addIndexingContext( repositoryId + "temp", repositoryId, null, tempIndexDirectory, repositoryUrl,
                null, MIN_CREATORS );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.3", null ),
            tempContext );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ),
            tempContext );

        FlatSearchResponse response2 = indexer.searchFlat( new FlatSearchRequest( q, tempContext ) );
        Collection<ArtifactInfo> tempContent = response2.getResults();
        assertEquals( tempContent.toString(), 2, tempContent.size() );

        // RAMDirectory is closed with context, forcing timestamp update
        tempContext.updateTimestamp( true );

        // A change in RAMDirectory and Directory behavior in general: it will copy the Index files ONLY
        // So we must make sure that timestamp file is transferred correctly.
        RAMDirectory tempDir2 = new RAMDirectory();
        IndexUtils.copyDirectory( tempContext.getIndexDirectory(), tempDir2 );

        Date newIndexTimestamp = tempContext.getTimestamp();

        indexer.removeIndexingContext( tempContext, false );

        context.replace( tempDir2 );

        assertEquals( newIndexTimestamp, context.getTimestamp() );

        FlatSearchResponse response3 = indexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> content2 = response3.getResults();
        assertEquals( content2.toString(), 2, content2.size() );
    }

    public void testMergeIndex()
        throws Exception
    {
        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ),
            context );

        Query q = indexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-lang", SearchType.SCORED );

        {
            FlatSearchResponse response1 = indexer.searchFlat( new FlatSearchRequest( q ) );
            Collection<ArtifactInfo> content1 = response1.getResults();

            assertEquals( content1.toString(), 1, content1.size() );
        }

        // updated index

        {
            Directory tempIndexDirectory = new RAMDirectory();

            IndexingContext tempContext =
                indexer.addIndexingContext( repositoryId + "temp", repositoryId, null, tempIndexDirectory,
                    repositoryUrl, null, MIN_CREATORS );

            // indexer.addArtifactToIndex(
            // createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ),
            // tempContext );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.3", null ), tempContext );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ), tempContext );

            FlatSearchResponse tempResponse = indexer.searchFlat( new FlatSearchRequest( q ) );
            Collection<ArtifactInfo> tempContent = tempResponse.getResults();
            assertEquals( tempContent.toString(), 3, tempContent.size() );

            RAMDirectory tempDir2 = new RAMDirectory();
            for (String file : tempContext.getIndexDirectory().listAll())
            {
                tempDir2.copyFrom(tempContext.getIndexDirectory(), file, file, IOContext.DEFAULT);
            }

            indexer.removeIndexingContext( tempContext, false );

            context.merge( tempDir2 );

            FlatSearchResponse response2 = indexer.searchFlat( new FlatSearchRequest( q ) );
            Collection<ArtifactInfo> content2 = response2.getResults();
            assertEquals( content2.toString(), 3, content2.size() );
        }
    }

    public void testMergeIndexDeletes()
        throws Exception
    {
        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ),
            context );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.3", null ),
            context );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ),
            context );

        {
            Directory tempIndexDirectory = new RAMDirectory();

            IndexingContext tempContext =
                indexer.addIndexingContext( repositoryId + "temp", repositoryId, null, tempIndexDirectory,
                    repositoryUrl, null, MIN_CREATORS );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ), tempContext );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ), tempContext );

            indexer.deleteArtifactFromIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ), tempContext );

            indexer.deleteArtifactFromIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ), tempContext );

            RAMDirectory tempDir2 = new RAMDirectory();
            for (String file : tempContext.getIndexDirectory().listAll())
            {
                tempDir2.copyFrom(tempContext.getIndexDirectory(), file, file, IOContext.DEFAULT);
            }

            indexer.removeIndexingContext( tempContext, false );

            context.merge( tempDir2 );
        }

        Query q = indexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-lang", SearchType.SCORED );

        FlatSearchResponse response = indexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> content2 = response.getResults();

        assertEquals( content2.toString(), 1, content2.size() );
    }

    public void testMergeSearch()
        throws Exception
    {
        File repo1 = new File( getBasedir(), "src/test/nexus-658" );
        Directory indexDir1 = new RAMDirectory();

        IndexingContext context1 =
            indexer.addIndexingContext( "nexus-658", "nexus-658", repo1, indexDir1, null, null, DEFAULT_CREATORS );
        indexer.scan( context1 );

        File repo2 = new File( getBasedir(), "src/test/nexus-13" );
        Directory indexDir2 = new RAMDirectory();

        IndexingContext context2 =
            indexer.addIndexingContext( "nexus-13", "nexus-13", repo2, indexDir2, null, null, DEFAULT_CREATORS );
        indexer.scan( context2 );

        context1.merge( indexDir2 );

        Query q = new TermQuery( new Term( ArtifactInfo.SHA1, "b5e9d009320d11b9859c15d3ad3603b455fa1c85" ) );
        FlatSearchRequest request = new FlatSearchRequest( q, context1 );
        FlatSearchResponse response = indexer.searchFlat( request );

        Set<ArtifactInfo> results = response.getResults();
        ArtifactInfo artifactInfo = results.iterator().next();
        assertEquals( artifactInfo.getArtifactId(), "dma.integration.tests" );
    }

    public void testMergeGroups()
        throws Exception
    {
        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.2", null ),
            context );

        indexer.addArtifactToIndex(
            createArtifactContext( repositoryId, "commons-collections", "commons-collections", "1.0", null ), context );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "org.slf4j", "slf4j-api", "1.4.2", null ),
            context );

        indexer.addArtifactToIndex( createArtifactContext( repositoryId, "org.slf4j", "slf4j-log4j12", "1.4.2", null ),
            context );

        {
            Directory tempIndexDirectory = new RAMDirectory();

            IndexingContext tempContext =
                indexer.addIndexingContext( repositoryId + "temp", repositoryId, null, tempIndexDirectory,
                    repositoryUrl, null, MIN_CREATORS );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "commons-lang", "commons-lang", "2.4", null ), tempContext );

            indexer.addArtifactToIndex( createArtifactContext( repositoryId, "junit", "junit", "3.8", null ),
                tempContext );

            indexer.addArtifactToIndex(
                createArtifactContext( repositoryId, "org.slf4j.foo", "jcl104-over-slf4j", "1.4.2", null ), context );

            RAMDirectory tempDir2 = new RAMDirectory();
            for (String file : tempContext.getIndexDirectory().listAll())
            {
                tempDir2.copyFrom(tempContext.getIndexDirectory(), file, file, IOContext.DEFAULT);
            }

            indexer.removeIndexingContext( tempContext, false );

            context.merge( tempDir2 );
        }

        Set<String> rootGroups = context.getRootGroups();

        assertEquals( rootGroups.toString(), 4, rootGroups.size() );

        Set<String> allGroups = context.getAllGroups();

        assertEquals( allGroups.toString(), 5, allGroups.size() );
    }

    public void testNoIndexUpdate()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081125010000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        final Properties localProps = new Properties();
        localProps.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "1" );
        localProps.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
        localProps.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081125010000.000 -0600" );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new IndexDirectoryFileAction( localProps, testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081125010000.000 -0600" );
                        return properties;
                    }
                } );

                allowing( tempContext ).getIndexDirectoryFile();

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    public void testFullIndexUpdate()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081125010000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new ReturnValueAction( testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                allowing( tempContext ).commit();

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081126010000.000 -0600" );
                        return properties;
                    }
                } );

                allowing( tempContext ).getIndexDirectoryFile();

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".gz" ) );
                will( returnValue( newInputStream( "/index-updater/server-root/nexus-maven-repository-index.gz" ) ) );

                oneOf( tempContext ).replace( with( any( Directory.class ) ), with( any( Set.class ) ), with( any( Set.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    public void testIncrementalIndexUpdate()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        final Properties localProps = new Properties();
        localProps.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "1" );
        localProps.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081129174241.859 -0600" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
                        properties.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "0", "3" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "1", "2" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "2", "1" );
                        return properties;
                    }
                } );

                allowing( tempContext ).getIndexDirectoryFile();
                will( new IndexDirectoryFileAction( localProps, testBasedir ) );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );
                will( returnValue( newInputStream( "/index-updater/server-root/nexus-maven-repository-index.gz" ) ) );
                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".3.gz" ) );
                will( returnValue( newInputStream( "/index-updater/server-root/nexus-maven-repository-index.gz" ) ) );
                // could create index archive there and verify that it is merged correctly

                oneOf( tempContext ).merge( with( any( Directory.class ) ) );

                oneOf( tempContext ).merge( with( any( Directory.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );
        updateRequest.setIncrementalOnly(true);

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    public void testIncrementalIndexUpdateNoCounter()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new ReturnValueAction( testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081129174241.859 -0600" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
                        properties.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "0", "3" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "1", "2" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "2", "1" );
                        return properties;
                    }
                } );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".gz" ) );
                will( returnValue( newInputStream( "/index-updater/server-root/nexus-maven-repository-index.gz" ) ) );
                // could create index archive there and verify that it is merged correctly

                oneOf( tempContext ).replace( with( any( Directory.class ) ), with( any( Set.class ) ), with( any( Set.class ) ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }
    
    public void testIncrementalOnlyIndexUpdateNoCounter()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new ReturnValueAction( testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081129174241.859 -0600" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
                        properties.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "0", "3" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "1", "2" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "2", "1" );
                        return properties;
                    }
                } );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );
        updateRequest.setIncrementalOnly(true);

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateFailed(updateResult);
    }

    public void testIncrementalIndexUpdateNoUpdateNecessary()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        final Properties localProps = new Properties();
        localProps.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
        localProps.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                allowing( tempContext ).commit();

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081129174241.859 -0600" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
                        properties.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "0", "3" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "1", "2" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "2", "1" );
                        return properties;
                    }
                } );

                allowing( tempContext ).getIndexDirectoryFile();
                will( new IndexDirectoryFileAction( localProps, testBasedir ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".gz" ) );
                // could create index archive there and verify that it is merged correctly

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".3.gz" ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                never( tempContext ).replace( with( any( Directory.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    public void testUpdateForceFullUpdate()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new ReturnValueAction( testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "20081129174241.859 -0600" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "3" );
                        properties.setProperty( IndexingContext.INDEX_CHAIN_ID, "someid" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "0", "3" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "1", "2" );
                        properties.setProperty( IndexingContext.INDEX_CHUNK_PREFIX + "2", "1" );
                        return properties;
                    }
                } );

                never( tempContext ).getIndexDirectoryFile();

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );

                never( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_FILE_PREFIX + ".3.gz" ) );

                oneOf( mockFetcher ).retrieve( with( IndexingContext.INDEX_FILE_PREFIX + ".gz" ) );
                will( returnValue( newInputStream( "/index-updater/server-root/nexus-maven-repository-index.gz" ) ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                oneOf( tempContext ).replace( with( any( Directory.class ) ), with( any( Set.class ) ), with( any( Set.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        updateRequest.setForceFullUpdate( true );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    @Ignore("Legacy format no longer supported with Lucene 4")
    public void ignoreTestUpdateForceFullUpdateNoGZ()
        throws Exception
    {
        Mockery mockery = new Mockery();

        final String indexUrl = repositoryUrl + ".index";
        final Date contextTimestamp = df.parse( "20081128000000.000 -0600" );

        final ResourceFetcher mockFetcher = mockery.mock( ResourceFetcher.class );

        final IndexingContext tempContext = mockery.mock( IndexingContext.class );

        mockery.checking( new Expectations()
        {
            {
                allowing( tempContext ).getIndexDirectoryFile();
                will( new ReturnValueAction( testBasedir ) );

                allowing( tempContext ).getTimestamp();
                will( returnValue( contextTimestamp ) );

                allowing( tempContext ).commit();

                allowing( tempContext ).getId();
                will( returnValue( repositoryId ) );

                allowing( tempContext ).getIndexUpdateUrl();
                will( returnValue( indexUrl ) );

                allowing( tempContext ).getIndexCreators();
                will( returnValue( DEFAULT_CREATORS ) );

                oneOf( mockFetcher ).connect( repositoryId, indexUrl );

                oneOf( mockFetcher ).retrieve( //
                    with( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE ) );
                will( new PropertiesAction()
                {
                    @Override
                    Properties getProperties()
                    {
                        Properties properties = new Properties();
                        properties.setProperty( IndexingContext.INDEX_ID, "central" );
                        properties.setProperty( IndexingContext.INDEX_LEGACY_TIMESTAMP, "20081129174241.859 -0600" );
                        return properties;
                    }
                } );

                never( tempContext ).getIndexDirectoryFile();

                oneOf( mockFetcher ).retrieve( with( IndexingContext.INDEX_FILE_PREFIX + ".gz" ) );

                will( throwException( new IOException() ) );

                oneOf( mockFetcher ).retrieve( with( IndexingContext.INDEX_FILE_PREFIX + ".zip" ) );

                will( returnValue( newInputStream( "/index-updater/server-root/legacy/nexus-maven-repository-index.zip" ) ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                never( tempContext ).merge( with( any( Directory.class ) ) );

                oneOf( tempContext ).replace( with( any( Directory.class ) ) );

                oneOf( mockFetcher ).disconnect();
            }
        } );

        // tempContext.updateTimestamp( true, contextTimestamp );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( tempContext, mockFetcher );

        updateRequest.setForceFullUpdate( true );

        IndexUpdateResult updateResult = updater.fetchAndUpdateIndex( updateRequest );

        mockery.assertIsSatisfied();
        assertIndexUpdateSucceeded(updateResult);
    }

    protected InputStream newInputStream( String path )
    {
        return getResourceAsStream( path );
    }

    abstract static class PropertiesAction
        extends VoidAction
    {
        @Override
        public Object invoke( Invocation invocation )
            throws Throwable
        {
            Properties properties = getProperties();

            try (ByteArrayOutputStream buf = new ByteArrayOutputStream())
            {
                properties.store( buf, null );
                buf.flush();
                return new ByteArrayInputStream( buf.toByteArray() );
            }

        }

        abstract Properties getProperties();
    }

    private static class IndexDirectoryFileAction
        extends VoidAction
    {
        File file = null;

        public IndexDirectoryFileAction( Properties properties, File basedir )
            throws Exception
        {
            basedir.mkdirs();

            this.file = new File( basedir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE );

            try ( FileOutputStream fos = new FileOutputStream( this.file ))
            {
                properties.store( fos, "" );
            }
        }

        @Override
        public Object invoke( Invocation invocation )
            throws Throwable
        {
            return this.file.getParentFile();
        }
    }
    
    private void assertIndexUpdateSucceeded(IndexUpdateResult updateResult)
    {
        assertTrue("Index update should have succeeded, but says it failed", updateResult.isSuccessful());
    }
    
    private void assertIndexUpdateFailed(IndexUpdateResult updateResult)
    {
        assertFalse("Index update should have failed, but says it succeeded", updateResult.isSuccessful());
    }
}
