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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;

public class DefaultIndexNexusIndexerTest
    extends MinimalIndexNexusIndexerTest
{
    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "test-default", "test", repo, indexDir, null, null, DEFAULT_CREATORS );

        assertNull( context.getTimestamp() ); // unknown upon creation

        nexusIndexer.scan( context );

        assertNotNull( context.getTimestamp() );
    }

    public void testPlugin()
        throws Exception
    {
        // String term = "plugin";
        // String term = "maven-core-it-plugin";
        String term = "org.apache.maven.plugins";

        // Query bq = new TermQuery(new Term(ArtifactInfo.GROUP_ID, "org.apache.maven.plugins"));
        // Query bq = new TermQuery(new Term(ArtifactInfo.ARTIFACT_ID, term));
        Query bq = new PrefixQuery( new Term( ArtifactInfo.GROUP_ID, term ) );
        // BooleanQuery bq = new BooleanQuery();
        // bq.add(new PrefixQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
        // bq.add(new PrefixQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
        TermQuery tq = new TermQuery( new Term( ArtifactInfo.PACKAGING, "maven-plugin" ) );
        Query query = new FilteredQuery( tq, new QueryWrapperFilter( bq ) );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );

        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( r.toString(), 1, r.size() );

        ArtifactInfo ai = r.iterator().next();

        assertEquals( "org.apache.maven.plugins", ai.getGroupId() );
        assertEquals( "maven-core-it-plugin", ai.getArtifactId() );
        assertEquals( "core-it", ai.getPrefix() );

        List<String> goals = ai.getGoals();
        assertEquals( 14, goals.size() );
        assertEquals( "catch", goals.get( 0 ) );
        assertEquals( "fork", goals.get( 1 ) );
        assertEquals( "fork-goal", goals.get( 2 ) );
        assertEquals( "touch", goals.get( 3 ) );
        assertEquals( "setter-touch", goals.get( 4 ) );
        assertEquals( "generate-envar-properties", goals.get( 5 ) );
        assertEquals( "generate-properties", goals.get( 6 ) );
        assertEquals( "loadable", goals.get( 7 ) );
        assertEquals( "light-touch", goals.get( 8 ) );
        assertEquals( "package", goals.get( 9 ) );
        assertEquals( "reachable", goals.get( 10 ) );
        assertEquals( "runnable", goals.get( 11 ) );
        assertEquals( "throw", goals.get( 12 ) );
        assertEquals( "tricky-params", goals.get( 13 ) );
    }

    public void testPluginPackaging()
        throws Exception
    {
        Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "maven-plugin" ) );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
        // repo contains 3 artifacts with packaging "maven-plugin", but one of the is actually an archetype!
        assertEquals( response.getResults().toString(), 2, response.getTotalHits() );
    }

    public void testSearchArchetypes()
        throws Exception
    {
        // TermQuery tq = new TermQuery(new Term(ArtifactInfo.PACKAGING, "maven-archetype"));
        // BooleanQuery bq = new BooleanQuery();
        // bq.add(new WildcardQuery(new Term(ArtifactInfo.GROUP_ID, term + "*")), Occur.SHOULD);
        // bq.add(new WildcardQuery(new Term(ArtifactInfo.ARTIFACT_ID, term + "*")), Occur.SHOULD);
        // FilteredQuery query = new FilteredQuery(tq, new QueryWrapperFilter(bq));

        Query q = new TermQuery( new Term( ArtifactInfo.PACKAGING, "maven-archetype" ) );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 4, r.size() );

        Iterator<ArtifactInfo> it = r.iterator();
        {
            ArtifactInfo ai = it.next();
            assertEquals( "org.apache.directory.server", ai.getGroupId() );
            assertEquals( "apacheds-schema-archetype", ai.getArtifactId() );
            assertEquals( "1.0.2", ai.getVersion() );
        }
        {
            ArtifactInfo ai = it.next();
            assertEquals( "org.apache.servicemix.tooling", ai.getGroupId() );
            assertEquals( "servicemix-service-engine", ai.getArtifactId() );
            assertEquals( "3.1", ai.getVersion() );
        }
        {
            ArtifactInfo ai = it.next();
            assertEquals( "org.terracotta.maven.archetypes", ai.getGroupId() );
            assertEquals( "pojo-archetype", ai.getArtifactId() );
            assertEquals( "1.0.3", ai.getVersion() );
        }
        {
            ArtifactInfo ai = it.next();
            assertEquals( "proptest", ai.getGroupId() );
            assertEquals( "proptest-archetype", ai.getArtifactId() );
            assertEquals( "1.0", ai.getVersion() );
        }
    }

    public void testIndexTimestamp()
        throws Exception
    {
        final File targetDir = File.createTempFile( "testIndexTimestamp", "ut-tmp" );
        targetDir.delete();
        targetDir.mkdirs();

        final IndexPacker indexPacker = lookup( IndexPacker.class );
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            final IndexPackingRequest request =
                new IndexPackingRequest( context, indexSearcher.getIndexReader(), targetDir );
            indexPacker.packIndex( request );
        }
        finally
        {
            context.releaseIndexSearcher( indexSearcher );
        }

        Thread.sleep( 1000L );

        File newIndex = new File( getBasedir(), "target/test-new" );

        Directory newIndexDir = FSDirectory.open( newIndex );

        IndexingContext newContext =
            nexusIndexer.addIndexingContext( "test-new", "test", null, newIndexDir, null, null, DEFAULT_CREATORS );

        final IndexUpdater indexUpdater = lookup( IndexUpdater.class );
        indexUpdater.fetchAndUpdateIndex( new IndexUpdateRequest( newContext, new DefaultIndexUpdater.FileFetcher( targetDir ) ) );

        assertEquals( context.getTimestamp().getTime(), newContext.getTimestamp().getTime() );

        assertEquals( context.getTimestamp(), newContext.getTimestamp() );

        // make sure context has the same artifacts

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "qdox", SearchType.SCORED );

        FlatSearchRequest request = new FlatSearchRequest( query, newContext );
        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        Collection<ArtifactInfo> r = response.getResults();

        System.out.println(r);

        assertEquals( 2, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        assertEquals( 2, list.size() );

        ArtifactInfo ai = list.get( 0 );

        assertEquals( "1.6.1", ai.getVersion() );

        ai = list.get( 1 );

        assertEquals( "1.5", ai.getVersion() );

        assertEquals( "test", ai.getRepository() );

        Date timestamp = newContext.getTimestamp();

        newContext.close( false );

        newIndexDir = FSDirectory.open( newIndex );

        newContext =
            nexusIndexer.addIndexingContext( "test-new", "test", null, newIndexDir, null, null, DEFAULT_CREATORS );

        indexUpdater.fetchAndUpdateIndex( new IndexUpdateRequest( newContext, new DefaultIndexUpdater.FileFetcher( targetDir ) ) );

        assertEquals( timestamp, newContext.getTimestamp() );

        newContext.close( true );

        assertFalse( new File( newIndex, "timestamp" ).exists() );
    }

    public void testArchetype()
        throws Exception
    {
        String term = "proptest";

        Query bq = new PrefixQuery( new Term( ArtifactInfo.GROUP_ID, term ) );
        TermQuery tq = new TermQuery( new Term( ArtifactInfo.PACKAGING, "maven-archetype" ) );
        Query query = new FilteredQuery( tq, new QueryWrapperFilter( bq ) );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );

        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( r.toString(), 1, r.size() );
    }

    public void testArchetypePackaging()
        throws Exception
    {
        Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "maven-archetype" ) );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
        assertEquals( response.getResults().toString(), 4, response.getTotalHits() );
    }

    public void testBrokenJar()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "brokenjar", SearchType.SCORED );

        FlatSearchRequest searchRequest = new FlatSearchRequest( q );

        FlatSearchResponse response = nexusIndexer.searchFlat( searchRequest );

        Set<ArtifactInfo> r = response.getResults();

        assertEquals( r.toString(), 1, r.size() );

        ArtifactInfo ai = r.iterator().next();

        assertEquals( "brokenjar", ai.getGroupId() );
        assertEquals( "brokenjar", ai.getArtifactId() );
        assertEquals( "1.0", ai.getVersion() );
        assertEquals( null, ai.getClassNames() );
    }

    public void testMissingPom()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "missingpom", SearchType.SCORED );

        FlatSearchRequest searchRequest = new FlatSearchRequest( q );

        FlatSearchResponse response = nexusIndexer.searchFlat( searchRequest );

        Set<ArtifactInfo> r = response.getResults();

        assertEquals( r.toString(), 1, r.size() );

        ArtifactInfo ai = r.iterator().next();

        assertEquals( "missingpom", ai.getGroupId() );
        assertEquals( "missingpom", ai.getArtifactId() );
        assertEquals( "1.0", ai.getVersion() );
        // See Nexus 2318. It should be null for a jar without classes
        assertNull( ai.getClassNames() );
    }

}
