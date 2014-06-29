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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.DefaultIndexPacker;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.search.grouping.GGrouping;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FullIndexNexusIndexerTest
    extends DefaultIndexNexusIndexerTest
{
    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "test-default", "test", repo, indexDir, null, null, FULL_CREATORS );

        assertNull( context.getTimestamp() ); // unknown upon creation

        nexusIndexer.scan( context );

        assertNotNull( context.getTimestamp() );
    }

    public void testSearchGroupedClasses()
        throws Exception
    {
        {
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "com/thoughtworks/qdox", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 2, r.size() ); // qdox and testng

            assertTrue( r.containsKey( "qdox : qdox" ) );
            assertTrue( r.containsKey( "org.testng : testng" ) );
            assertEquals( "qdox : qdox", r.get( "qdox : qdox" ).getGroupKey() );
            assertEquals( "org.testng : testng", r.get( "org.testng : testng" ).getGroupKey() );
        }

        {
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "com.thoughtworks.qdox", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() );

            assertTrue( r.containsKey( "qdox : qdox" ) );
            assertTrue( r.containsKey( "org.testng : testng" ) );
            assertEquals( "qdox : qdox", r.get( "qdox : qdox" ).getGroupKey() );
            assertEquals( "org.testng : testng", r.get( "org.testng : testng" ).getGroupKey() );
        }

        {
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "thoughtworks", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() );
            assertTrue( r.containsKey( "qdox : qdox" ) );
            assertTrue( r.containsKey( "org.testng : testng" ) );
            assertEquals( "qdox : qdox", r.get( "qdox : qdox" ).getGroupKey() );
            assertEquals( "org.testng : testng", r.get( "org.testng : testng" ).getGroupKey() );
        }

        {
            // an implicit class name wildcard
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "Logger", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertThat(r.toString(), r.size(), is(2));

            Iterator<ArtifactInfoGroup> it = r.values().iterator();

            ArtifactInfoGroup ig1 = it.next();
            assertEquals( r.toString(), "org.slf4j", ig1.getGroupKey() );

            ArtifactInfoGroup ig2 = it.next();
            assertEquals( r.toString(), "org.testng", ig2.getGroupKey() );
        }

        {
            // a lower case search
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "logger", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() );

            Iterator<ArtifactInfoGroup> it = r.values().iterator();

            ArtifactInfoGroup ig1 = it.next();
            assertEquals( r.toString(), "org.slf4j", ig1.getGroupKey() );

            ArtifactInfoGroup ig2 = it.next();
            assertEquals( r.toString(), "org.testng", ig2.getGroupKey() );
        }

        {
            // explicit class name wildcard without terminator
            // Since 4.0 query starting with * is illegal
            // Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "*.Logger", SearchType.SCORED );
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, ".Logger", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() );
            Iterator<ArtifactInfoGroup> it = r.values().iterator();
            ArtifactInfoGroup ig1 = it.next();
            assertEquals( r.toString(), "org.slf4j", ig1.getGroupKey() );
            ArtifactInfoGroup ig2 = it.next();
            assertEquals( r.toString(), "org.testng", ig2.getGroupKey() );
        }

        {
            // explicit class name wildcard with terminator
            // Since 4.0 query starting with * is illegal
            // Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "*.Logger ", SearchType.SCORED );
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, ".Logger ", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() );
            Iterator<ArtifactInfoGroup> it = r.values().iterator();
            ArtifactInfoGroup ig1 = it.next();
            assertEquals( r.toString(), "org.slf4j", ig1.getGroupKey() );
            ArtifactInfoGroup ig2 = it.next();
            assertEquals( r.toString(), "org.testng", ig2.getGroupKey() );
        }

        {
            // a class name wildcard
            // Since 4.0 query starting with * is illegal
            // Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "*Logger", SearchType.SCORED );
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "Logger", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            // Results are less, since no PREFIX searches anymore!
            // assertEquals( r.toString(), 3, r.size() );
            assertEquals( r.toString(), 2, r.size() );

            Iterator<ArtifactInfoGroup> it = r.values().iterator();

            // Results are less, since no PREFIX searches anymore!
            // ArtifactInfoGroup ig1 = it.next();
            // assertEquals( r.toString(), "commons-logging", ig1.getGroupKey() ); // Jdk14Logger and LogKitLogger

            ArtifactInfoGroup ig2 = it.next();
            assertEquals( r.toString(), "org.slf4j", ig2.getGroupKey() );

            ArtifactInfoGroup ig3 = it.next();
            assertEquals( r.toString(), "org.testng", ig3.getGroupKey() );
        }

        {
            // exact class name
            Query q =
                nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "org/apache/commons/logging/LogConfigurationException",
                    SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() ); // jcl104-over-slf4j and commons-logging
        }

        {
            // implicit class name pattern
            Query q =
                nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "org.apache.commons.logging.LogConfigurationException",
                    SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() ); // jcl104-over-slf4j and commons-logging
        }

        {
            // exact class name
            Query q =
                nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "org.apache.commons.logging.LogConfigurationException",
                    SearchType.EXACT );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() ); // jcl104-over-slf4j and commons-logging
        }

        {
            // package name prefix
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "org.apache.commons.logging", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 2, r.size() ); // jcl104-over-slf4j and commons-logging
        }

        {
            // Since 4.0, queries cannot start with '*'
            // Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "*slf4j*Logg*", SearchType.SCORED );
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "slf4j.Logg*", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();
            // Error is fixed, see below
            assertEquals( r.toString(), 1, r.size() );

            {
                ArtifactInfoGroup ig = r.values().iterator().next();
                ArrayList<ArtifactInfo> list1 = new ArrayList<ArtifactInfo>( ig.getArtifactInfos() );
                assertEquals( r.toString(), 2, list1.size() );

                ArtifactInfo ai1 = list1.get( 0 );
                assertEquals( "org.slf4j", ai1.getGroupId() );
                assertEquals( "slf4j-api", ai1.getArtifactId() );
                assertEquals( "1.4.2", ai1.getVersion() );
                ArtifactInfo ai2 = list1.get( 1 );
                assertEquals( "org.slf4j", ai2.getGroupId() );
                assertEquals( "slf4j-api", ai2.getArtifactId() );
                assertEquals( "1.4.1", ai2.getVersion() );
            }

            // this is fixed now, no more false hit
            // {
            // // This was error, since slf4j-log4j12 DOES NOT HAVE any class for this search!
            // ArtifactInfoGroup ig = r.get( "org.slf4j : slf4j-log4j12" );
            // ArrayList<ArtifactInfo> list = new ArrayList<ArtifactInfo>( ig.getArtifactInfos() );
            // assertEquals( list.toString(), 1, list.size() );
            //
            // ArtifactInfo ai = list.get( 0 );
            // assertEquals( "org.slf4j", ai.groupId );
            // assertEquals( "slf4j-log4j12", ai.artifactId );
            // assertEquals( "1.4.1", ai.version );
            // }
        }
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

    // ==

    protected IteratorSearchRequest createHighlightedRequest( Field field, String text, SearchType type )
        throws ParseException
    {
        Query q = nexusIndexer.constructQuery( field, text, type );

        IteratorSearchRequest request = new IteratorSearchRequest( q );

        request.getMatchHighlightRequests().add( new MatchHighlightRequest( field, q, MatchHighlightMode.HTML ) );

        return request;
    }

    public void testClassnameSearchNgWithHighlighting()
        throws Exception
    {
        IteratorSearchRequest request = createHighlightedRequest( MAVEN.CLASSNAMES, "Logger", SearchType.SCORED );

        IteratorSearchResponse response = nexusIndexer.searchIterator( request );

        for ( ArtifactInfo ai : response )
        {
            for ( MatchHighlight mh : ai.getMatchHighlights() )
            {
                for ( String highlighted : mh.getHighlightedMatch() )
                {
                    // Logger and LoggerFactory
                    assertTrue( "Class name should be highlighted", highlighted.contains( "<B>Logger" ) );
                    assertFalse( "Class name should not contain \"/\" alone (but okay within HTML, see above!)",
                        highlighted.matches( "\\p{Lower}/\\p{Upper}" ) );
                    assertFalse( "Class name should not begin with \".\" or \"/\"", highlighted.startsWith( "." )
                        || highlighted.startsWith( "/" ) );
                }
            }
        }
        
        assertThat(response.getTotalHitsCount(), is(5));

        assertEquals( "found in jcl104-over-slf4j and commons-logging", 5, response.getTotalHits() );
    }

    public void testGAVSearchNgWithHighlighting()
        throws Exception
    {
        IteratorSearchRequest request = createHighlightedRequest( MAVEN.GROUP_ID, "commons", SearchType.SCORED );

        IteratorSearchResponse response = nexusIndexer.searchIterator( request );

        for ( ArtifactInfo ai : response )
        {
            for ( MatchHighlight mh : ai.getMatchHighlights() )
            {
                assertTrue(
                    "Group ID should be highlighted",
                    mh.getHighlightedMatch().contains( "<B>commons</B>-logging" )
                        || mh.getHighlightedMatch().contains( "<B>commons</B>-cli" ) );
            }
        }

        assertEquals( "found in commons-logging and commons-cli", 15, response.getTotalHits() );
    }
}
