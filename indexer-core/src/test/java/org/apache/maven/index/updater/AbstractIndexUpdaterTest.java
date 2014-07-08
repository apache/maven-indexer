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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.maven.index.AbstractIndexCreatorHelper;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractIndexUpdaterTest
    extends AbstractIndexCreatorHelper
{
    File testBasedir;

    File repoDir;

    File indexDir;

    String repositoryId = "test";

    String repositoryUrl = "http://repo1.maven.org/maven2/";

    NexusIndexer indexer;

    IndexUpdater updater;

    IndexPacker packer;

    IndexingContext context;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        testBasedir = new File( getBasedir(), "/target/indexUpdater" );
        testBasedir.mkdirs();

        repoDir = new File( getBasedir(), "/target/indexUpdaterRepoDir" );
        repoDir.mkdirs();

        indexDir = super.getDirectory( "indexerUpdater" );
        indexDir.mkdirs();

        indexer = lookup( NexusIndexer.class );

        updater = lookup( IndexUpdater.class );

        packer = lookup( IndexPacker.class );

        context =
            indexer.addIndexingContext( repositoryId, repositoryId, repoDir, indexDir, repositoryUrl, null,
                MIN_CREATORS );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        // this one closes it too
        indexer.removeIndexingContext( context, true );

        FileUtils.forceDelete( testBasedir );

        FileUtils.forceDelete( repoDir );

        FileUtils.forceDelete( indexDir );
    }

    protected ArtifactContext createArtifactContext( String repositoryId, String groupId, String artifactId,
                                                     String version, String classifier )
   {
        String path = createPath( groupId, artifactId, version, classifier );
        File pomFile = new File( path + ".pom" );
        File artifact = new File( path + ".jar" );
        File metadata = null;
        ArtifactInfo artifactInfo = new ArtifactInfo( repositoryId, groupId, artifactId, version, classifier, "jar");
        Gav gav =
            new Gav( groupId, artifactId, version, classifier, "jar", null, null, artifact.getName(), false,
                null, false, null );
        return new ArtifactContext( pomFile, artifact, metadata, artifactInfo, gav );
    }

    protected String createPath( String groupId, String artifactId, String version, String classifier )
    {
        return "/" + groupId + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version
            + ( classifier == null ? "" : "-" + classifier );
    }

    protected void packIndex( File targetDir, IndexingContext context )
        throws IllegalArgumentException, IOException
    {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            IndexPackingRequest request = new IndexPackingRequest( context, indexSearcher.getIndexReader(), targetDir );
            request.setUseTargetProperties( true );
            packer.packIndex( request );
        } finally
        {
            context.releaseIndexSearcher( indexSearcher );
        }
    }

    protected void searchFor( String groupId, int expected, IndexingContext context )
        throws IOException, Exception
    {
        Query q = indexer.constructQuery( MAVEN.GROUP_ID, groupId, SearchType.EXACT );

        IteratorSearchRequest req;

        if ( context != null )
        {
            req = new IteratorSearchRequest( q, context );
        }
        else
        {
            req = new IteratorSearchRequest( q );
        }

        IteratorSearchResponse response = indexer.searchIterator( req );

        ArrayList<ArtifactInfo> ais = new ArrayList<ArtifactInfo>( response.getTotalHits() );

        for ( ArtifactInfo ai : response )
        {
            ais.add( ai );
        }

        assertEquals( ais.toString(), expected, ais.size() );
    }

}
