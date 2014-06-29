package org.apache.maven.index.packer;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.AbstractNexusIndexerTest;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.packer.IndexPackingRequest.IndexFormat;
import org.apache.maven.index.updater.IndexDataReader;
import org.codehaus.plexus.util.StringUtils;

public class NEXUS4149TransferFormatTest
    extends AbstractNexusIndexerTest
{
    protected File reposBase = new File( getBasedir(), "src/test/nexus-4149" );

    protected File idxsBase = new File( getBasedir(), "target/index/nexus-4149" );

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        IndexingContext ctx1 =
            nexusIndexer.addIndexingContext( "repo1", "repo1", new File( reposBase, "repo1" ), new File( idxsBase,
                "repo1" ), null, null, MIN_CREATORS );
        nexusIndexer.scan( ctx1 );

        IndexingContext ctx2 =
            nexusIndexer.addIndexingContext( "repo2", "repo2", new File( reposBase, "repo2" ), new File( idxsBase,
                "repo2" ), null, null, MIN_CREATORS );
        nexusIndexer.scan( ctx2 );

        IndexingContext ctx3 =
            nexusIndexer.addIndexingContext( "repo3", "repo3", new File( reposBase, "repo3" ), new File( idxsBase,
                "repo3" ), null, null, MIN_CREATORS );
        nexusIndexer.scan( ctx3 );

        IndexingContext ctx4 =
            nexusIndexer.addIndexingContext( "repo4", "repo4", new File( reposBase, "repo4" ), new File( idxsBase,
                "repo4" ), null, null, MIN_CREATORS );
        nexusIndexer.scan( ctx4 );

        context =
            nexusIndexer.addMergedIndexingContext( "ctx", "ctx", new File( reposBase, "merged" ), new File( idxsBase,
                "merged" ), false, Arrays.asList( ctx1, ctx2, ctx3, ctx4 ) );

        context.getIndexDirectoryFile().mkdirs();
    }

    @Override
    protected void unprepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        // remove the merged
        nexusIndexer.removeIndexingContext( context, true );

        // remove members
        MergedIndexingContext mctx = (MergedIndexingContext) context;

        for ( IndexingContext member : mctx.getMembers() )
        {
            nexusIndexer.removeIndexingContext( member, true );
        }
    }

    @Override
    public void testDirectory()
        throws IOException
    {
        // we use no directory
    }

    public void testMembersAndMergedRootGroups()
        throws Exception
    {
        MergedIndexingContext mctx = (MergedIndexingContext) context;

        for ( IndexingContext member : mctx.getMembers() )
        {
            Assert.assertEquals( "Members should have one root group!", 1, member.getRootGroups().size() );
        }

        Assert.assertEquals( "Merged should have one root multiply members count!", mctx.getMembers().size(),
            mctx.getRootGroups().size() );
    }

    public void testTransportFile()
        throws Exception
    {
        File packTargetDir = new File( getBasedir(), "target/nexus-4149/packed" );

        IndexPacker packer = lookup( IndexPacker.class );

        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            IndexPackingRequest request = new IndexPackingRequest( context, indexSearcher.getIndexReader(), packTargetDir );
            request.setCreateIncrementalChunks( false );
            request.setFormats( Arrays.asList( IndexFormat.FORMAT_V1 ) );

            packer.packIndex( request );
        } finally {
            context.releaseIndexSearcher( indexSearcher );
        }

        // read it up and verify, but stay "low level", directly consume the GZ file and count
        FileInputStream fis = new FileInputStream( new File( packTargetDir, "nexus-maven-repository-index.gz" ) );
        IndexDataReader reader = new IndexDataReader( fis );
        try
        {
            // read header and neglect it
            reader.readHeader();

            // read docs
            int totalDocs = 0;
            int specialDocs = 0;
            int artifactDocs = 0;
            String allGroups = null;
            String rootGroups = null;
            Document doc;
            while ( ( doc = reader.readDocument() ) != null )
            {
                totalDocs++;
                if ( doc.getField( "DESCRIPTOR" ) != null || doc.getField( ArtifactInfo.ALL_GROUPS ) != null
                    || doc.getField( ArtifactInfo.ROOT_GROUPS ) != null )
                {
                    specialDocs++;

                    if ( doc.get( ArtifactInfo.ALL_GROUPS ) != null )
                    {
                        allGroups = doc.get( ArtifactInfo.ALL_GROUPS_LIST );
                    }
                    if ( doc.get( ArtifactInfo.ROOT_GROUPS ) != null )
                    {
                        rootGroups = doc.get( ArtifactInfo.ROOT_GROUPS_LIST );
                    }
                }
                else
                {
                    artifactDocs++;
                }
            }

            Assert.assertNotNull( "Group transport file should contain allGroups!", allGroups );
            Assert.assertNotNull( "Group transport file should contain rootGroups!", rootGroups );
            checkListOfStringDoesNotContainEmptyString( ArtifactInfo.str2lst( allGroups ) );
            checkListOfStringDoesNotContainEmptyString( ArtifactInfo.str2lst( rootGroups ) );

            Assert.assertEquals( 15, totalDocs );
            // 1 descriptor + 1 allGroups + 1 rootGroups
            Assert.assertEquals( 3, specialDocs );
            // repo1 has 1 artifact, repo2 has 1 artifact and repo3 has 10 artifact
            Assert.assertEquals( 12, artifactDocs );

        }
        finally
        {
            fis.close();
        }
    }

    protected void checkListOfStringDoesNotContainEmptyString( List<String> lst )
    {
        if ( lst != null )
        {
            for ( String str : lst )
            {
                if ( StringUtils.isBlank( str ) )
                {
                    throw new IllegalArgumentException( "List " + lst + " contains empty string!" );
                }
            }
        }
    }
}
