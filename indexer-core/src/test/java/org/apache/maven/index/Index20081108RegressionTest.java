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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.junit.Ignore;

/**
 * @author Eugene Kuleshov
 */
@Ignore("Index format too old for Lucene 4")
public class Index20081108RegressionTest
    extends AbstractRepoNexusIndexerTest
{
    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContextForced( "test", "test", null, indexDir, null, null, DEFAULT_CREATORS );

        InputStream is = new FileInputStream( getBasedir() + //
            File.separator + "src" + //
            File.separator + "test" + //
            File.separator + "nexus-maven-repository-index.20081108.zip" );

        Directory tempDir = new RAMDirectory();
        DefaultIndexUpdater.unpackIndexArchive( is, tempDir, context );
        context.replace( tempDir );
    }

    public void testExtension()
        throws Exception
    {
        assertEquals( 31, context.getSize() );

        {
            Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "qdox", SearchType.SCORED );
            FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
            assertEquals( response.getResults().toString(), 2, response.getTotalHits() );

            List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( response.getResults() );
            assertEquals( 2, list.size() );

            {
                ArtifactInfo ai = list.get( 0 );
                assertEquals( "1.6.1", ai.version );
                assertEquals( "jar", ai.fextension );
                assertEquals( "jar", ai.packaging );
            }
            {
                ArtifactInfo ai = list.get( 1 );
                assertEquals( "1.5", ai.version );
                assertEquals( "jar", ai.fextension );
                assertEquals( "jar", ai.packaging );
            }
        }
        {
            Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "tar.gz" ) );
            FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
            assertEquals( response.getResults().toString(), 1, response.getTotalHits() );

            ArtifactInfo ai = response.getResults().iterator().next();
            assertEquals( "tar.gz", ai.packaging );
            assertEquals( "tar.gz", ai.fextension );
        }
        {
            Query query = new TermQuery( new Term( ArtifactInfo.PACKAGING, "zip" ) );
            FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( query ) );
            assertEquals( response.getResults().toString(), 1, response.getTotalHits() );

            ArtifactInfo ai = response.getResults().iterator().next();
            assertEquals( "zip", ai.packaging );
            assertEquals( "zip", ai.fextension );
        }
    }

    @Override
    public void testRootGroups()
        throws Exception
    {
        Set<String> rootGroups = context.getRootGroups();
        assertEquals( rootGroups.toString(), 8, rootGroups.size() );

        assertGroup( 2, "qdox", context );

        assertGroup( 1, "proptest", context );

        assertGroup( 1, "junit", context );

        assertGroup( 6, "commons-logging", context );

        assertGroup( 1, "regexp", context );

        assertGroup( 1, "commons-cli", context );

        assertGroup( 15, "org", context );

        assertGroup( 6, "org.slf4j", context );

        assertGroup( 3, "org.testng", context );

        assertGroup( 3, "org.apache", context );

        assertGroup( 1, "org.apache.directory", context );
        assertGroup( 1, "org.apache.directory.server", context );

        assertGroup( 1, "org.apache.maven", context );
        assertGroup( 1, "org.apache.maven.plugins", context );
        assertGroup( 0, "org.apache.maven.plugins.maven-core-it-plugin", context );
    }

    @Override
    public void testSearchFlatPaged()
        throws Exception
    {
        FlatSearchRequest request = new FlatSearchRequest( nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org", SearchType.SCORED ) );

        // See MINDEXER-22
        // Flat search is not pageable
        // request.setStart( 0 );

        request.setCount( 50 );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );

        assertEquals( response.getResults().toString(), 15, response.getTotalHits() );
    }

    @Override
    public void testSearchGroupedProblematicNames()
        throws Exception
    {

        // ----------------------------------------------------------------------------
        // Artifacts with "problematic" names
        // ----------------------------------------------------------------------------
        {
            // "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "commons-logg*", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( "commons-logging : commons-logging", ig.getGroupKey() );

            assertEquals( ig.getArtifactInfos().toString(), 6, ig.getArtifactInfos().size() );
        }

        {
            // numbers and "-" in the name
            Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "jcl104-over-slf4*", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );

            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();

            assertEquals( 1, r.size() );

            ArtifactInfoGroup ig = r.values().iterator().next();

            assertEquals( ig.getArtifactInfos().toString(), 1, ig.getArtifactInfos().size() );

            assertEquals( "org.slf4j : jcl104-over-slf4j", ig.getGroupKey() );
        }
    }

    // See MINDEXER-22
    // Flat search is not pageable
//    @Override
//    public void donttestPaging()
//        throws Exception
//    {
//        // we have 15 artifact for this search
//        int total = 15;
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

    @Override
    public void testPurge()
        throws Exception
    {
        // we have 14 artifact for this search
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org", SearchType.SCORED );
        FlatSearchRequest request = new FlatSearchRequest( q );

        FlatSearchResponse response1 = nexusIndexer.searchFlat( request );
        Collection<ArtifactInfo> p1 = response1.getResults();

        assertEquals( 15, p1.size() );

        context.purge();

        FlatSearchResponse response2 = nexusIndexer.searchFlat( request );
        Collection<ArtifactInfo> p2 = response2.getResults();

        assertEquals( 0, p2.size() );
    }

    @Override
    public void testIdentify()
    {
        // skip test (sha1 field wasn't stored in the old index format)
    }

}
