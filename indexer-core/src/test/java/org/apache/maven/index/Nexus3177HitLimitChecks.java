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
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.AbstractSearchResponse;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;

public class Nexus3177HitLimitChecks
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    protected Directory secondIndexDir = new RAMDirectory();

    protected IndexingContext secondContext;

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-3177", "nexus-3177", repo, indexDir, null, null, DEFAULT_CREATORS );

        secondContext =
            nexusIndexer.addIndexingContext( "nexus-3177b", "nexus-3177b", repo, secondIndexDir, null, null,
                DEFAULT_CREATORS );

        nexusIndexer.scan( context );
        nexusIndexer.scan( secondContext );
    }

    @Override
    protected void unprepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        super.unprepareNexusIndexer( nexusIndexer );

        nexusIndexer.removeIndexingContext( secondContext, false );
    }

    public void testHitLimitNotReachedSingleContext()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 5 );
        request.getContexts().add( context );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 4, r.size() );
        assertEquals( r.toString(), 4, response.getTotalHits() );
    }

    public void testHitLimitEqualSingleContext()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 4 );
        request.getContexts().add( context );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 4, r.size() );
        assertEquals( r.toString(), 4, response.getTotalHits() );
    }

    public void testHitLimitExceededSingleContext()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 3 );
        request.getContexts().add( context );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 0, r.size() );
        assertEquals( r.toString(), AbstractSearchResponse.LIMIT_EXCEEDED, response.getTotalHits() );
    }

    public void testHitLimitNotReachedMultipleContexts()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 9 );
        request.setArtifactInfoComparator( ArtifactInfo.REPOSITORY_VERSION_COMPARATOR );
        request.getContexts().add( context );
        request.getContexts().add( secondContext );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 8, r.size() );
        assertEquals( r.toString(), 8, response.getTotalHits() );
    }

    public void testHitLimitEqualMultipleContexts()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 8 );
        request.setArtifactInfoComparator( ArtifactInfo.REPOSITORY_VERSION_COMPARATOR );
        request.getContexts().add( context );
        request.getContexts().add( secondContext );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 8, r.size() );
        assertEquals( r.toString(), 8, response.getTotalHits() );
    }

    public void testHitLimitExceededMultipleContexts()
        throws Exception
    {
        WildcardQuery q = new WildcardQuery( new Term( ArtifactInfo.UINFO, "*testng*" ) );

        FlatSearchRequest request = new FlatSearchRequest( q );
        request.setResultHitLimit( 7 );
        request.setArtifactInfoComparator( ArtifactInfo.REPOSITORY_VERSION_COMPARATOR );
        request.getContexts().add( context );
        request.getContexts().add( secondContext );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Set<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 0, r.size() );
        assertEquals( r.toString(), AbstractSearchResponse.LIMIT_EXCEEDED, response.getTotalHits() );
    }
}
