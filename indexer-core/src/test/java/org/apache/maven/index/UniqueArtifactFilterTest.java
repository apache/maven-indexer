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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.codehaus.plexus.util.FileUtils;

public class UniqueArtifactFilterTest
    extends AbstractIndexCreatorHelper
{
    private IndexingContext context;

    public void testSearchIterator()
        throws Exception
    {
        NexusIndexer indexer = prepare();

        Query q = indexer.constructQuery( MAVEN.GROUP_ID, "qdox", SearchType.SCORED );

        IteratorSearchRequest request = new IteratorSearchRequest( q );

        IteratorSearchResponse response = indexer.searchIterator( request );

        assertEquals( 2, response.getTotalHits() );

        for ( ArtifactInfo ai : response.getResults() )
        {
            assertEquals( "GroupId must match \"qdox\"!", "qdox", ai.getGroupId() );
        }
    }

    public void testSearchIteratorWithFilter()
        throws Exception
    {
        NexusIndexer indexer = prepare();

        Query q = indexer.constructQuery( MAVEN.GROUP_ID, "commons", SearchType.SCORED );

        UniqueArtifactFilterPostprocessor filter = new UniqueArtifactFilterPostprocessor();
        filter.addField( MAVEN.GROUP_ID );
        filter.addField( MAVEN.ARTIFACT_ID );

        IteratorSearchRequest request = new IteratorSearchRequest( q, filter );

        IteratorSearchResponse response = indexer.searchIterator( request );

        assertEquals( "15 total hits (before filtering!)", 15, response.getTotalHits() );

        ArtifactInfo ai = response.getResults().next();
        assertTrue( "Iterator has to have next (2 should be returned)", ai != null );

        ai = response.getResults().next();
        assertTrue( "Iterator has to have next (2 should be returned)", ai != null );

        assertEquals( "Property that is not unique has to have \"COLLAPSED\" value!",
            UniqueArtifactFilterPostprocessor.COLLAPSED, ai.getVersion() );
        assertEquals( "Property that is not unique has to have \"COLLAPSED\" value!",
            UniqueArtifactFilterPostprocessor.COLLAPSED, ai.getPackaging() );
        assertEquals( "Property that is not unique has to have \"COLLAPSED\" value!",
            UniqueArtifactFilterPostprocessor.COLLAPSED, ai.getClassifier() );
    }

    // ==

    private NexusIndexer prepare()
        throws Exception, IOException, UnsupportedExistingLuceneIndexException
    {
        NexusIndexer indexer = lookup( NexusIndexer.class );

        // Directory indexDir = new RAMDirectory();
        File indexDir = new File( getBasedir(), "target/index/test-" + Long.toString( System.currentTimeMillis() ) );
        FileUtils.deleteDirectory( indexDir );

        File repo = new File( getBasedir(), "src/test/repo" );

        context = indexer.addIndexingContext( "test", "test", repo, indexDir, null, null, DEFAULT_CREATORS );

        indexer.scan( context );

        return indexer;
    }
}
