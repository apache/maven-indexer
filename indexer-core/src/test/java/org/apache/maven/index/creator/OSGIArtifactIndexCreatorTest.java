package org.apache.maven.index.creator;

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
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class OSGIArtifactIndexCreatorTest
    extends PlexusTestCase
{
    protected IndexCreator indexCreator;

    private NexusIndexer nexusIndexer;

    static final String INDEX_ID = "osgi-test1";

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        indexCreator = this.lookup( IndexCreator.class, OSGIArtifactIndexCreator.ID );

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
            new ArtifactInfo( "test", "org.apache.karaf.features", "org.apache.karaf.features.command", "2.2.2", null );

        ArtifactContext artifactContext = new ArtifactContext( pom, artifact, null, artifactInfo, null );

        indexCreator.populateArtifactInfo( artifactContext );

        assertNotNull( "bundleSymbolicName", artifactContext.getArtifactInfo().bundleSymbolicName );

        assertNotNull( "bundleVersion", artifactContext.getArtifactInfo().bundleVersion );

        assertNotNull( "bundleExportPackage", artifactContext.getArtifactInfo().bundleExportPackage );

        assertEquals( "org.apache.karaf.features.command", artifactContext.getArtifactInfo().bundleSymbolicName );

        assertEquals( "2.2.2", artifactContext.getArtifactInfo().bundleVersion );

        assertEquals(
            "org.apache.karaf.features.command.completers;uses:=\"org.apache.karaf.features,org.apache.karaf.shell.console,org.apache.karaf.shell.console.completer\";version=\"2.2.2\",org.apache.karaf.features.command;uses:=\"org.apache.felix.gogo.commands,org.apache.karaf.features,org.apache.karaf.shell.console,org.osgi.framework,org.apache.felix.service.command\";version=\"2.2.2\"",
            artifactContext.getArtifactInfo().bundleExportPackage );
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
                                         new MavenPluginArtifactInfoIndexCreator(), new OSGIArtifactIndexCreator() );

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

            assertEquals( "org.apache.karaf.features", ai.groupId );
            assertEquals( "org.apache.karaf.features.command", ai.artifactId );
            assertEquals( "2.2.2", ai.version );
            assertEquals( "org.apache.karaf.features.command", ai.bundleSymbolicName );
            assertEquals( "2.2.2", ai.bundleVersion );

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
            assertEquals( 1, response.getResults().size() );

            ArtifactInfo ai = response.getResults().iterator().next();
            System.out.println("ai " + ai );

            assertEquals( "org.apache.felix", ai.groupId );
            assertEquals( "org.apache.felix.bundlerepository", ai.artifactId );
            assertEquals( "1.6.6", ai.version );
            assertEquals( "bundle", ai.packaging );
            assertEquals( "org.apache.felix.bundlerepository", ai.bundleSymbolicName );
            assertEquals( "1.6.6", ai.bundleVersion );

        }
        finally
        {
            nexusIndexer.getIndexingContexts().get( INDEX_ID ).close( true );
        }

    }

    // Export-Service: org.apache.felix.bundlerepository.RepositoryAdmin,org.osgi.service.obr.RepositoryAdmin

}
