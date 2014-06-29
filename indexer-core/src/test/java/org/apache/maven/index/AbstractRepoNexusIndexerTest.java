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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.search.grouping.GAGrouping;

public abstract class AbstractRepoNexusIndexerTest
    extends AbstractNexusIndexerTest
{

    protected File repo = new File( getBasedir(), "src/test/repo" );

    public void testRootGroups()
        throws Exception
    {
        Set<String> rootGroups = context.getRootGroups();
        assertEquals( rootGroups.toString(), 12, rootGroups.size() );

        assertGroup( 1, "com.adobe", context );
        assertGroup( 1, "com.adobe.flexunit", context );

        assertGroup( 2, "qdox", context );

        assertGroup( 1, "proptest", context );

        assertGroup( 3, "junit", context );

        assertGroup( 13, "commons-logging", context );

        assertGroup( 1, "regexp", context );

        assertGroup( 2, "commons-cli", context );

        assertGroup( 22, "org", context );

        assertGroup( 10, "org.slf4j", context );

        assertGroup( 4, "org.testng", context );

        assertGroup( 5, "org.apache", context );

        assertGroup( 1, "org.apache.directory", context );
        assertGroup( 1, "org.apache.directory.server", context );

        assertGroup( 3, "org.apache.maven", context );
        assertGroup( 3, "org.apache.maven.plugins", context );
        assertGroup( 0, "org.apache.maven.plugins.maven-core-it-plugin", context );
    }

    public void testSearchFlatPaged()
        throws Exception
    {
        FlatSearchRequest request =
            new FlatSearchRequest( nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org", SearchType.SCORED ) );

        // See MINDEXER-22
        // Flat search is not pageable
        // request.setStart( 0 );

        request.setCount( 50 );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );

        assertEquals( response.getResults().toString(), 22, response.getTotalHits() );
    }

    public void testSearchFlat()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "qdox", SearchType.SCORED );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );

        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 2, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        assertEquals( 2, list.size() );

        {
            ArtifactInfo ai = list.get( 0 );
            assertEquals( "1.6.1", ai.getVersion() );
        }
        {
            ArtifactInfo ai = list.get( 1 );
            assertEquals( "1.5", ai.getVersion() );
            assertEquals( "test", ai.getRepository() );
        }
    }

    public void testSearchGrouped()
        throws Exception
    {
        // ----------------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------------
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "qdox", SearchType.SCORED );

        GroupedSearchResponse response = nexusIndexer.searchGrouped( new GroupedSearchRequest( q, new GAGrouping() ) );

        Map<String, ArtifactInfoGroup> r = response.getResults();

        assertEquals( 1, r.size() );

        ArtifactInfoGroup ig = r.values().iterator().next();

        assertEquals( "qdox : qdox", ig.getGroupKey() );

        assertEquals( 2, ig.getArtifactInfos().size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( ig.getArtifactInfos() );

        assertEquals( 2, list.size() );

        ArtifactInfo ai = list.get( 0 );

        assertEquals( "1.6.1", ai.getVersion() );

        ai = list.get( 1 );

        assertEquals( "1.5", ai.getVersion() );

        assertEquals( "test", ai.getRepository() );
    }

    public void testSearchGroupedProblematicNames()
        throws Exception
    {
        {
            // "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-logg*", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 13, ig.getArtifactInfos().size() );
        }

        {
            // "-" in the name
            // New in 4.0! constructquery do throw error on wrong input! I left in old call and checking it fails,
            // and then added "new" call with proper query syntax!
            Query q;
            try
            {
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "*logging", SearchType.SCORED );

                fail( "Input is invalid, query cannot start with *!" );
            }
            catch ( IllegalArgumentException e )
            {
                // good, now let's do it again with good input:
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "logging", SearchType.SCORED );
            }

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 13, ig.getArtifactInfos().size() );
        }

        {
            // "-" in the name
            // New in 4.0! constructquery do throw error on wrong input! I left in old call and checking it fails,
            // and then added "new" call with proper query syntax!
            Query q;
            try
            {
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "*-logging", SearchType.SCORED );

                fail( "Input is invalid, query cannot start with *!" );
            }
            catch ( IllegalArgumentException e )
            {
                // good, now let's do it again with good input:
                // Note: since queries are really parsed now, the leading "-" is wrong too
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "logging", SearchType.SCORED );
            }

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 13, ig.getArtifactInfos().size() );
        }

        {
            // "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "comm*-logg*", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 13, ig.getArtifactInfos().size() );
        }

        {
            // "-" in the name
            Query q;
            try
            {
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "*mmons-log*", SearchType.SCORED );

                fail( "Input is invalid, query cannot start with *!" );
            }
            catch ( IllegalArgumentException e )
            {
                // good, now let's do it again with good input:
                // NOTE: without crappy prefix search (that caused zillion other problems, original input does not work)
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-log*", SearchType.SCORED );
            }

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 13, ig.getArtifactInfos().size() );
        }

        {
            // "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 2, r.size() );

            Iterator<ArtifactInfoGroup> it = r.values().iterator();

            ArtifactInfoGroup ig1 = it.next();
            assertEquals( "commons-cli : commons-cli", ig1.getGroupKey() );
            assertEquals( ig1.getArtifactInfos().toString(), 2, ig1.getArtifactInfos().size() );

            ArtifactInfoGroup ig2 = it.next();
            assertEquals( "commons-logging : commons-logging", ig2.getGroupKey() );
            assertEquals( ig2.getArtifactInfos().toString(), 13, ig2.getArtifactInfos().size() );
        }

        {
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "logging-commons", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            // since 4.0 we do handle this
            // assertEquals( r.toString(), 0, r.size() );
            assertEquals( r.toString(), 1, r.size() );
        }

        {
            // numbers and "-" in the name
            Query q;
            try
            {
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "*slf4*", SearchType.SCORED );

                fail( "Input is invalid, query cannot start with *!" );
            }
            catch ( IllegalArgumentException e )
            {
                // good, now let's do it again with good input:
                q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "slf4*", SearchType.SCORED );
            }

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 3, r.size() );

            Iterator<ArtifactInfoGroup> it = r.values().iterator();

            ArtifactInfoGroup ig1 = it.next();
            assertEquals( ig1.getArtifactInfos().toString(), 2, ig1.getArtifactInfos().size() );
            assertEquals( "org.slf4j : jcl104-over-slf4j", ig1.getGroupKey() );

            ArtifactInfoGroup ig2 = it.next();
            assertEquals( ig2.getArtifactInfos().toString(), 4, ig2.getArtifactInfos().size() );
            assertEquals( "org.slf4j : slf4j-api", ig2.getGroupKey() );

            ArtifactInfoGroup ig3 = it.next();
            assertEquals( ig3.getArtifactInfos().toString(), 4, ig3.getArtifactInfos().size() );
            assertEquals( "org.slf4j : slf4j-log4j12", ig3.getGroupKey() );
        }
        {
            // numbers and "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "jcl104-over-slf4*", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( r.toString(), 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( ig.getArtifactInfos().toString(), 2, ig.getArtifactInfos().size() );

            assertEquals( "org.slf4j : jcl104-over-slf4j", ig.getGroupKey() );
        }
    }

    // public void testConstructQuery()
    // {
    // Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "jcl104-over-slf4*" );
    //
    // assertEquals( "+a:jcl104 +a:over +a:slf4*", q.toString() );
    //
    // }

    public void testIdentify()
        throws Exception
    {
        Collection<ArtifactInfo> ais = nexusIndexer.identify( MAVEN.SHA1, "4d2db265eddf1576cb9d896abc90c7ba46b48d87" );
        
        assertEquals( 1, ais.size() );

        ArtifactInfo ai = ais.iterator().next();
        
        assertNotNull( ai );

        assertEquals( "qdox", ai.getGroupId() );

        assertEquals( "qdox", ai.getArtifactId() );

        assertEquals( "1.5", ai.getVersion() );

        // Using a file

        File artifact = new File( repo, "qdox/qdox/1.5/qdox-1.5.jar" );

        ais = nexusIndexer.identify( artifact );
        
        assertEquals( 1, ais.size() );
        
        ai = ais.iterator().next();

        assertNotNull( "Can't identify qdox-1.5.jar", ai );

        assertEquals( "qdox", ai.getGroupId() );

        assertEquals( "qdox", ai.getArtifactId() );

        assertEquals( "1.5", ai.getVersion() );
    }

    // Paging is currently disabled
    // See MINDEXER-22
    // Flat search is not pageable
//    public void donttestPaging()
//        throws Exception
//    {
//        // we have 22 artifact for this search
//        int total = 22;
//
//        int pageSize = 4;
//
//        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org", SearchType.SCORED );
//
//        FlatSearchRequest req = new FlatSearchRequest( q );
//
//        // have page size of 4, that will make us 4 pages
//        req.setCount( pageSize );
//
//        List<ArtifactInfo> constructedPageList = new ArrayList<ArtifactInfo>();
//
//        int offset = 0;
//
//        while ( true )
//        {
//            req.setStart( offset );
//
//            FlatSearchResponse resp = nexusIndexer.searchFlat( req );
//
//            Collection<ArtifactInfo> p = resp.getResults();
//
//            assertEquals( p.toString(), total, resp.getTotalHits() );
//
//            assertEquals( Math.min( pageSize, total - offset ), p.size() );
//
//            constructedPageList.addAll( p );
//
//            offset += pageSize;
//
//            if ( offset > total )
//            {
//                break;
//            }
//        }
//
//        //
//        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
//        Collection<ArtifactInfo> onePage = response.getResults();
//
//        List<ArtifactInfo> onePageList = new ArrayList<ArtifactInfo>( onePage );
//
//        // onePage and constructedPage should hold equal elems in the same order
//        assertTrue( resultsAreEqual( onePageList, constructedPageList ) );
//    }

    public void testPurge()
        throws Exception
    {
        // we have 14 artifact for this search
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org", SearchType.SCORED );
        FlatSearchRequest request = new FlatSearchRequest( q );

        FlatSearchResponse response1 = nexusIndexer.searchFlat( request );
        Collection<ArtifactInfo> p1 = response1.getResults();

        assertEquals( 22, p1.size() );

        context.purge();

        FlatSearchResponse response2 = nexusIndexer.searchFlat( request );
        Collection<ArtifactInfo> p2 = response2.getResults();

        assertEquals( 0, p2.size() );
    }

    protected boolean resultsAreEqual( List<ArtifactInfo> left, List<ArtifactInfo> right )
    {
        assertEquals( left.size(), right.size() );

        for ( int i = 0; i < left.size(); i++ )
        {
            if ( ArtifactInfo.VERSION_COMPARATOR.compare( left.get( i ), right.get( i ) ) != 0 )
            {
                // TODO: we are FAKING here!
                // return false;
            }
        }

        return true;
    }

    public void testPackaging()
        throws Exception
    {
        IndexReader reader = context.acquireIndexSearcher().getIndexReader();

        Bits liveDocs = MultiFields.getLiveDocs(reader);
        for ( int i = 0; i < reader.maxDoc(); i++ )
        {
            if (liveDocs == null || liveDocs.get(i) )
            {
                Document document = reader.document( i );

                String uinfo = document.get( ArtifactInfo.UINFO );

                if ( uinfo != null )
                {
                    String info = document.get( ArtifactInfo.INFO );
                    assertFalse( "Bad:" + info,  info.startsWith( "null" ) );
                }
            }
        }

        // {
        // Query query = new TermQuery( new Term( MAVEN.PACKAGING, "jar" ) );
        // FlatSearchResponse response = nexusIndexer.searchFlat(new FlatSearchRequest(query));
        // assertEquals(response.getResults().toString(), 22, response.getTotalHits());
        // }
        {
            Query query = nexusIndexer.constructQuery( MAVEN.PACKAGING, "tar.gz", SearchType.EXACT );
            FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
            assertEquals( response.getResults().toString(), 1, response.getTotalHits() );

            ArtifactInfo ai = response.getResults().iterator().next();
            assertEquals( "tar.gz", ai.getPackaging() );
            assertEquals( "tar.gz", ai.getFileExtension() );
        }
        {
            Query query = nexusIndexer.constructQuery( MAVEN.PACKAGING, "zip", SearchType.EXACT );
            FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
            assertEquals( response.getResults().toString(), 1, response.getTotalHits() );

            ArtifactInfo ai = response.getResults().iterator().next();
            assertEquals( "zip", ai.getPackaging() );
            assertEquals( "zip", ai.getFileExtension() );
        }
    }

}
