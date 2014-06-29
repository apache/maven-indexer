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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.DefaultIndexPacker;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;

/** http://issues.sonatype.org/browse/NEXUS-13 */
public class Nexus13NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-13" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "nexus-13", "nexus-13", repo, indexDir, null, null, FULL_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchGroupedClasses()
        throws Exception
    {
        {
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "cisco", SearchType.SCORED );

            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 4, r.size() );

            assertTrue( r.containsKey( "cisco.infra.dft : dma.plugin.utils" ) );
            assertTrue( r.containsKey( "cisco.infra.dft : dma.pom.enforcer" ) );
            assertTrue( r.containsKey( "cisco.infra.dft : maven-dma-mgmt-plugin" ) );
            assertTrue( r.containsKey( "cisco.infra.dft : maven-dma-plugin" ) );
        }

        {
            Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, "dft.plugin.utils", SearchType.SCORED );
            GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
            GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
            Map<String, ArtifactInfoGroup> r = response.getResults();
            assertEquals( r.toString(), 1, r.size() );

            assertTrue( r.containsKey( "cisco.infra.dft : dma.plugin.utils" ) );
            assertEquals( "cisco.infra.dft : dma.plugin.utils",
                r.get( "cisco.infra.dft : dma.plugin.utils" ).getGroupKey() );
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
        assertEquals( r.toString(), 1, r.size() );

        ArtifactInfo ai = r.iterator().next();
        assertEquals( "cisco.infra.dft", ai.getGroupId() );
        assertEquals( "archetype.sdf", ai.getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );
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

        Directory indexDir = new RAMDirectory();

        IndexingContext newContext =
            nexusIndexer.addIndexingContext( "test-new", "nexus-13", null, indexDir, null, null, DEFAULT_CREATORS );

        final IndexUpdater indexUpdater = lookup( IndexUpdater.class );
        final IndexUpdateRequest updateRequest = new IndexUpdateRequest( newContext, new DefaultIndexUpdater.FileFetcher( targetDir ) );
        indexUpdater.fetchAndUpdateIndex( updateRequest );

        assertEquals( 0, newContext.getTimestamp().getTime() - context.getTimestamp().getTime() );

        assertEquals( context.getTimestamp(), newContext.getTimestamp() );

        // make sure context has the same artifacts

        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "cisco", SearchType.SCORED );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q, newContext ) );
        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 10, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        assertEquals( 10, list.size() );

        ArtifactInfo ai = list.get( 0 );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );

        ai = list.get( 1 );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );

        assertEquals( "nexus-13", ai.getRepository() );

        newContext.close( true );
    }

    public void testRootGroups()
        throws Exception
    {
        Set<String> rootGroups = context.getRootGroups();
        assertEquals( rootGroups.toString(), 1, rootGroups.size() );

        assertGroup( 10, "cisco", context );
    }

    public void testSearchFlat()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "cisco.infra", SearchType.SCORED );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 10, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        assertEquals( 10, list.size() );

        ArtifactInfo ai = list.get( 0 );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );

        ai = list.get( 1 );

        assertEquals( "nexus-13", ai.getRepository() );

    }

    public void testSearchGrouped()
        throws Exception
    {
        // ----------------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------------
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "cisco.infra", SearchType.SCORED );

        GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
        GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
        Map<String, ArtifactInfoGroup> r = response.getResults();
        assertEquals( 8, r.size() );

        ArtifactInfoGroup ig = r.values().iterator().next();

        assertEquals( "cisco.infra.dft : archetype.sdf", ig.getGroupKey() );

        assertEquals( 1, ig.getArtifactInfos().size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( ig.getArtifactInfos() );

        assertEquals( 1, list.size() );

        ArtifactInfo ai = list.get( 0 );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );
    }

    public void testSearchGroupedProblematicNames()
        throws Exception
    {

        // ----------------------------------------------------------------------------
        // Artifacts with "problematic" names
        // ----------------------------------------------------------------------------

        Query q = nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "dma.integr*", SearchType.SCORED );

        GroupedSearchRequest request = new GroupedSearchRequest( q, new GAGrouping() );
        GroupedSearchResponse response = nexusIndexer.searchGrouped( request );
        Map<String, ArtifactInfoGroup> r = response.getResults();

        assertEquals( 1, r.size() );

        ArtifactInfoGroup ig = r.values().iterator().next();

        assertEquals( "cisco.infra.dft : dma.integration.tests", ig.getGroupKey() );

        assertEquals( 1, ig.getArtifactInfos().size() );
    }

    public void testIdentify()
        throws Exception
    {
        Collection<ArtifactInfo> ais = nexusIndexer.identify( MAVEN.SHA1, "c8a2ef9d92a4b857eae0f36c2e01481787c5cbf8" );

        assertEquals( 1, ais.size() );

        ArtifactInfo ai = ais.iterator().next();

        assertNotNull( ai );

        assertEquals( "cisco.infra.dft", ai.getGroupId() );

        assertEquals( "dma.plugin.utils", ai.getArtifactId() );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );

        // Using a file

        File artifact =
            new File( repo,
                "cisco/infra/dft/maven-dma-mgmt-plugin/1.0-SNAPSHOT/maven-dma-mgmt-plugin-1.0-20080409.022326-2.jar" );

        ais = nexusIndexer.identify( artifact );
        
        assertEquals( 1, ais.size() );

        ai = ais.iterator().next();

        assertNotNull( ai );

        assertEquals( "cisco.infra.dft", ai.getGroupId() );

        assertEquals( "maven-dma-mgmt-plugin", ai.getArtifactId() );

        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );
    }
}
