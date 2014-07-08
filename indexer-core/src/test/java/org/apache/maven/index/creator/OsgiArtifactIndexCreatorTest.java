package org.apache.maven.index.creator;

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
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.OSGI;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.StringSearchExpression;
import org.apache.maven.index.AbstractTestSupport;
import org.codehaus.plexus.util.FileUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Olivier Lamy
 */
public class OsgiArtifactIndexCreatorTest
    extends AbstractTestSupport
{
    protected IndexCreator indexCreator;

    private NexusIndexer nexusIndexer;

    static final String INDEX_ID = "osgi-test1";

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        indexCreator = this.lookup( IndexCreator.class, OsgiArtifactIndexCreator.ID );

        nexusIndexer = this.lookup( NexusIndexer.class );
    }

    public void testAssertIndexCreatorComponentExists()
        throws Exception
    {
        assertNotNull( indexCreator );
    }

    public void testPopulateArtifactInfo()
        throws Exception
    {
        File artifact = new File( getBasedir(),
                                  "src/test/repo-with-osgi/org/apache/karaf/features/org.apache.karaf.features.command/2.2.2/org.apache.karaf.features.command-2.2.2.jar" );

        File pom = new File( getBasedir(),
                             "src/test/repo-with-osgi/org/apache/karaf/features/org.apache.karaf.features.command/2.2.2/org.apache.karaf.features.command-2.2.2.pom" );

        ArtifactInfo artifactInfo =
            new ArtifactInfo( "test", "org.apache.karaf.features", "org.apache.karaf.features.command", "2.2.2", null, "jar" );

        ArtifactContext artifactContext = new ArtifactContext( pom, artifact, null, artifactInfo, null );

        indexCreator.populateArtifactInfo( artifactContext );

        assertNotNull( "bundleSymbolicName", artifactContext.getArtifactInfo().getBundleSymbolicName() );

        assertNotNull( "bundleVersion", artifactContext.getArtifactInfo().getBundleVersion() );

        assertNotNull( "bundleExportPackage", artifactContext.getArtifactInfo().getBundleExportPackage() );

        assertEquals( "org.apache.karaf.features.command", artifactContext.getArtifactInfo().getBundleSymbolicName() );

        assertEquals( "2.2.2", artifactContext.getArtifactInfo().getBundleVersion() );

        assertEquals(
            "org.apache.karaf.features.command.completers;uses:=\"org.apache.karaf.features,org.apache.karaf.shell.console,org.apache.karaf.shell.console.completer\";version=\"2.2.2\",org.apache.karaf.features.command;uses:=\"org.apache.felix.gogo.commands,org.apache.karaf.features,org.apache.karaf.shell.console,org.osgi.framework,org.apache.felix.service.command\";version=\"2.2.2\"",
            artifactContext.getArtifactInfo().getBundleExportPackage() );

        ArtifactInfo ai = artifactContext.getArtifactInfo();

        assertEquals( "This bundle provides the Karaf shell commands to manipulate features.", ai.getBundleDescription() );
        assertEquals( "Apache Karaf :: Features :: Command", ai.getBundleName() );
        assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.txt", ai.getBundleLicense() );
        assertEquals( "http://www.apache.org/", ai.getBundleDocUrl() );

        assertEquals(
            "javax.management,javax.management.loading,org.apache.felix.gogo.commands;version=\"[0.6,1)\",org.apache.felix.service.command;status=provisional;version=\"[0.6,1)\",org.apache.karaf.features;version=\"[2.2,3)\",org.apache.karaf.shell.console;version=\"[2.2,3)\",org.apache.karaf.shell.console.completer;version=\"[2.2,3)\",org.osgi.framework;version=\"[1.5,2)\",org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\"",
            ai.getBundleImportPackage() );
    }


    private void indexOSGIRepo()
        throws Exception
    {

        File repo = new File( getBasedir(), "src/test/repo-with-osgi" );

        File repoIndexDir = new File( getBasedir(), "target/test/repo-with-osgi/.index/" );

        if ( repoIndexDir.exists() )
        {
            FileUtils.deleteDirectory( repoIndexDir );
        }

        repoIndexDir.mkdirs();

        List<IndexCreator> indexCreators =
            Arrays.<IndexCreator>asList( new MinimalArtifactInfoIndexCreator(), new JarFileContentsIndexCreator(),
                                         new MavenPluginArtifactInfoIndexCreator(), new OsgiArtifactIndexCreator() );

        IndexingContext indexingContext =
            nexusIndexer.addIndexingContext( INDEX_ID, INDEX_ID, repo, repoIndexDir, "http://www.apache.org",
                                             "http://www.apache.org/.index", indexCreators );
        indexingContext.setSearchable( true );
        nexusIndexer.scan( indexingContext, false );


    }

    public void testIndexOSGIRepoThenSearch()
        throws Exception
    {

        try
        {
            indexOSGIRepo();

            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                                new StringSearchExpression( "org.apache.karaf.features.command" ) ),
                   BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            // here only one results !
            assertEquals( 1, response.getResults().size() );

            q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                                new StringSearchExpression( "org.apache.karaf.features.core" ) ),
                   BooleanClause.Occur.MUST );

            request = new FlatSearchRequest( q );
            response = nexusIndexer.searchFlat( request );

            // here two results !
            assertEquals( 2, response.getResults().size() );
        }
        finally
        {
            nexusIndexer.getIndexingContexts().get( INDEX_ID ).close( true );
        }
    }

    public void testIndexOSGIRepoThenSearchWithVersion()
        throws Exception
    {

        indexOSGIRepo();

        try
        {

            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.SYMBOLIC_NAME,
                                                new StringSearchExpression( "org.apache.karaf.features.core" ) ),
                   BooleanClause.Occur.MUST );

            q.add( nexusIndexer.constructQuery( OSGI.VERSION, new StringSearchExpression( "2.2.1" ) ),
                   BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            // here only one results as we use version
            assertEquals( 1, response.getResults().size() );
        }
        finally
        {
            nexusIndexer.getIndexingContexts().get( INDEX_ID ).close( true );
        }

    }

    public void testIndexOSGIRepoThenSearchWithExportPackage()
        throws Exception
    {

        indexOSGIRepo();

        try
        {

            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.EXPORT_PACKAGE, new StringSearchExpression(
                "org.apache.karaf.features.command.completers" ) ), BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            //System.out.println("results with export package query " + response.getResults() );
            assertEquals( 1, response.getResults().size() );

            ArtifactInfo ai = response.getResults().iterator().next();

            assertEquals( "org.apache.karaf.features", ai.getGroupId() );
            assertEquals( "org.apache.karaf.features.command", ai.getArtifactId() );
            assertEquals( "2.2.2", ai.getVersion() );
            assertEquals( "org.apache.karaf.features.command", ai.getBundleSymbolicName() );
            assertEquals( "2.2.2", ai.getBundleVersion() );

            assertEquals( "This bundle provides the Karaf shell commands to manipulate features.",
                          ai.getBundleDescription() );
            assertEquals( "Apache Karaf :: Features :: Command", ai.getBundleName() );
            assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.txt", ai.getBundleLicense() );
            assertEquals( "http://www.apache.org/", ai.getBundleDocUrl() );

            assertEquals(
                "javax.management,javax.management.loading,org.apache.felix.gogo.commands;version=\"[0.6,1)\",org.apache.felix.service.command;status=provisional;version=\"[0.6,1)\",org.apache.karaf.features;version=\"[2.2,3)\",org.apache.karaf.shell.console;version=\"[2.2,3)\",org.apache.karaf.shell.console.completer;version=\"[2.2,3)\",org.osgi.framework;version=\"[1.5,2)\",org.osgi.service.blueprint;version=\"[1.0.0,2.0.0)\"",
                ai.getBundleImportPackage() );

        }
        finally
        {
            nexusIndexer.getIndexingContexts().get( INDEX_ID ).close( true );
        }

    }

    public void testIndexOSGIRepoThenSearchWithExportService()
        throws Exception
    {

        indexOSGIRepo();

        try
        {

            BooleanQuery q = new BooleanQuery();

            q.add( nexusIndexer.constructQuery( OSGI.EXPORT_SERVICE, new StringSearchExpression(
                "org.apache.felix.bundlerepository.RepositoryAdmin" ) ), BooleanClause.Occur.MUST );

            FlatSearchRequest request = new FlatSearchRequest( q );
            FlatSearchResponse response = nexusIndexer.searchFlat( request );

            //System.out.println("results with export package query " + response.getResults() );
            assertThat(response.getResults().size(), is(1));

            ArtifactInfo ai = response.getResults().iterator().next();
            System.out.println( "ai " + ai );

            assertEquals( "org.apache.felix", ai.getGroupId() );
            assertEquals( "org.apache.felix.bundlerepository", ai.getArtifactId() );
            assertEquals( "1.6.6", ai.getVersion() );
            assertEquals( "bundle", ai.getPackaging() );
            assertEquals( "org.apache.felix.bundlerepository", ai.getBundleSymbolicName() );
            assertEquals( "1.6.6", ai.getBundleVersion() );

        }
        finally
        {
            nexusIndexer.getIndexingContexts().get( INDEX_ID ).close( true );
        }

    }

    // Export-Service: org.apache.felix.bundlerepository.RepositoryAdmin,org.osgi.service.obr.RepositoryAdmin

}
