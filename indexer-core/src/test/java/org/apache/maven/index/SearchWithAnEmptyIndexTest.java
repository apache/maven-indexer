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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.StringSearchExpression;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class SearchWithAnEmptyIndexTest
    extends AbstractTestSupport
{
    protected List<IndexCreator> indexCreators;

    private NexusIndexer nexusIndexer;

    static final String INDEX_ID1 = "osgi-test1";

    static final String INDEX_ID2 = "empty-repo";

    private IndexPacker indexPacker;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        indexCreators = this.getContainer().lookupList( IndexCreator.class );

        nexusIndexer = this.lookup( NexusIndexer.class );

        indexPacker = this.lookup( IndexPacker.class );

        if ( !nexusIndexer.getIndexingContexts().isEmpty() )
        {
            for ( IndexingContext context : nexusIndexer.getIndexingContexts().values() )
            {
                nexusIndexer.removeIndexingContext( context, true );
            }
        }
    }

    public void testWithTwoContextWithOneEmptyFirstInContextsListSearchFlat()
        throws Exception
    {

        String repoPath = "target/test/empty-repo-for-searchtest";

        File emptyRepo = new File( getBasedir(), repoPath );

        if ( emptyRepo.exists() )
        {
            FileUtils.deleteDirectory( emptyRepo );
        }

        emptyRepo.mkdirs();

        //createIndex( "/src/test/repo", repoPath + "/.index", INDEX_ID2 );
        createIndex( repoPath, repoPath, INDEX_ID2 );

        createIndex( "src/test/repo-with-osgi", "target/test/repo-with-osgi/", INDEX_ID1 );

        try
        {
            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                                new StringSearchExpression( "org.apache.karaf.features.command" ) ),
                   BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            assertEquals( 2, nexusIndexer.getIndexingContexts().values().size() );
            request.setContexts( Arrays.asList( nexusIndexer.getIndexingContexts().get( INDEX_ID2 ),
                                                nexusIndexer.getIndexingContexts().get( INDEX_ID1 ) ) );

            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            assertEquals( 1, response.getResults().size() );

            q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                                new StringSearchExpression( "org.apache.karaf.features.core" ) ),
                   BooleanClause.Occur.MUST );

            request = new FlatSearchRequest( q );
            request.setContexts( new ArrayList( nexusIndexer.getIndexingContexts().values() ) );

            response = nexusIndexer.searchFlat( request );

            assertEquals( 2, response.getResults().size() );

            String term = "org.apache.karaf.features";

            q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( term ) ),
                   BooleanClause.Occur.SHOULD );
            q.add( nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( term ) ),
                   BooleanClause.Occur.SHOULD );
            q.add( nexusIndexer.constructQuery( MAVEN.VERSION, new StringSearchExpression( term ) ),
                   BooleanClause.Occur.SHOULD );
            q.add( nexusIndexer.constructQuery( MAVEN.PACKAGING, new StringSearchExpression( term ) ),
                   BooleanClause.Occur.SHOULD );
            q.add( nexusIndexer.constructQuery( MAVEN.CLASSNAMES, new StringSearchExpression( term ) ),
                   BooleanClause.Occur.SHOULD );

            request = new FlatSearchRequest( q );
            request.setContexts( new ArrayList( nexusIndexer.getIndexingContexts().values() ) );

            response = nexusIndexer.searchFlat( request );

            System.out.println( " result size with term usage " + response.getResults().size() );

            assertEquals( 3, response.getResults().size() );

        }
        finally
        {
            closeAllIndexs();
        }
    }

    /**
     * both repos contains commons-cli so ensure we don't return duplicates
     */
    public void testSearchNoDuplicateArtifactInfo()
        throws Exception
    {

        String repoPathIndex = "target/test/repo-for-searchdupe";

        File emptyRepo = new File( getBasedir(), repoPathIndex );

        if ( emptyRepo.exists() )
        {
            FileUtils.deleteDirectory( emptyRepo );
        }

        emptyRepo.mkdirs();

        //createIndex( "/src/test/repo", repoPath + "/.index", INDEX_ID2 );
        createIndex( "/src/test/repo", repoPathIndex, INDEX_ID2 );

        createIndex( "src/test/repo-with-osgi", "target/test/repo-with-osgi/", INDEX_ID1 );

        try
        {
            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "commons-cli" ) ),
                   BooleanClause.Occur.MUST );

            q.add( nexusIndexer.constructQuery( MAVEN.PACKAGING, new StringSearchExpression( "jar" ) ),
                   BooleanClause.Occur.MUST );

            q.add( nexusIndexer.constructQuery( MAVEN.CLASSIFIER, new StringSearchExpression( "sources" ) ),
                   BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            assertEquals( 2, nexusIndexer.getIndexingContexts().values().size() );
            request.setContexts( Arrays.asList( nexusIndexer.getIndexingContexts().get( INDEX_ID2 ),
                                                nexusIndexer.getIndexingContexts().get( INDEX_ID1 ) ) );

            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            assertEquals( 1, response.getResults().size() );

        }
        finally
        {
            closeAllIndexs();
        }
    }

    private void closeAllIndexs()
        throws Exception
    {
        for ( IndexingContext context : nexusIndexer.getIndexingContexts().values() )
        {
            context.close( true );
        }
    }

    private void createIndex( String filePath, String repoIndex, String contextId )
        throws Exception
    {

        File repo = new File( getBasedir(), filePath );

        File repoIndexDir = new File( getBasedir(), repoIndex + "/.index" );

        if ( repoIndexDir.exists() )
        {
            FileUtils.deleteDirectory( repoIndexDir );
        }

        repoIndexDir.mkdirs();

        System.out.println(
            "creating Index with id " + contextId + " path : " + filePath + " , indexPath " + repoIndex );

        IndexingContext indexingContext =
            nexusIndexer.addIndexingContext( contextId, contextId, repo, repoIndexDir, "http://www.apache.org",
                                             "http://www.apache.org/.index", indexCreators );
        indexingContext.setSearchable( true );
        nexusIndexer.scan( indexingContext, false );

        indexingContext.optimize();

        File managedRepository = new File( repoIndex );
        final IndexSearcher indexSearcher = indexingContext.acquireIndexSearcher();
        try
        {
            final File indexLocation = new File( managedRepository, ".index" );
            IndexPackingRequest request = new IndexPackingRequest( indexingContext, indexSearcher.getIndexReader(), indexLocation );
            indexPacker.packIndex( request );
        } finally {
            indexingContext.releaseIndexSearcher( indexSearcher );
        }

    }
}
