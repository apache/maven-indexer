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
import java.util.Collections;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.util.IndexCreatorSorter;

public class DefaultSearchEngineTest
    extends AbstractNexusIndexerTest
{

    private static class CountingIndexingContext
        extends DefaultIndexingContext
    {
        public int count;

        public CountingIndexingContext( String id, String repositoryId, File repository, Directory indexDirectory,
                                        String repositoryUrl, String indexUpdateUrl,
                                        List<? extends IndexCreator> indexCreators, boolean reclaimIndex )
            throws IOException, ExistingLuceneIndexMismatchException
        {
            super( id, repositoryId, repository, indexDirectory, repositoryUrl, indexUpdateUrl, indexCreators,
                   reclaimIndex );
        }

        public IndexSearcher acquireIndexSearcher()
            throws IOException
        {
            try
            {
                return super.acquireIndexSearcher();
            }
            finally
            {
                count++;
            }
        };

        @Override
        public void releaseIndexSearcher( IndexSearcher is )
            throws IOException
        {
            try
            {
                super.releaseIndexSearcher( is );
            }
            finally
            {
                count--;
            }
        }
    }

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        File repo = new File( getBasedir(), "src/test/repo" );
        context =
            new CountingIndexingContext( "test-minimal", "test", repo, indexDir, null, null,
                                         IndexCreatorSorter.sort( MIN_CREATORS ), false );

        nexusIndexer.scan( context );
    }

    private SearchEngine searchEngine;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        searchEngine = lookup( SearchEngine.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        searchEngine = null;
        super.tearDown();
    }

    public void testExceptionInArtifactFilter()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "com.adobe.flexunit", SearchType.EXACT );
        IteratorSearchRequest request = new IteratorSearchRequest( q );
        request.setArtifactInfoFilter( new ArtifactInfoFilter()
        {
            public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
            {
                throw new RuntimeException();
            }
        } );
        request.setArtifactInfoPostprocessor( new ArtifactInfoPostprocessor()
        {
            public void postprocess( IndexingContext ctx, ArtifactInfo ai )
            {
                throw new RuntimeException();
            }
        } );

        try
        {
            searchEngine.forceSearchIteratorPaged( request, Collections.singletonList( context ) );
        }
        catch ( RuntimeException e )
        {
            // this is the point of this test
        }

        assertEquals( 0, ( (CountingIndexingContext) context ).count );
    }
}
