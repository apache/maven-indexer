package org.apache.maven.index.cli;

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
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import org.apache.lucene.search.Query;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractNexusIndexerCliTest
        extends InjectedTest
{

    private static final long rand = new Random().nextLong();

    /*
     * private static final String DEST_DIR = new File( getBasedir(), "target/tests/clitest/output-"+rand
     * ).getAbsolutePath(); private static final String INDEX_DIR = new File( getBasedir(),
     * "target/tests/clitest/index-"+rand ).getAbsolutePath(); private static final String UNPACK_DIR = new File(
     * getBasedir(), "target/tests/clitest/unpack-"+rand ).getAbsolutePath(); private static final String TEST_REPO =
     * new File( getBasedir(), "src/test/repo" ).getAbsolutePath();
     */
    private final String DEST_DIR =
        new File( getBasedir(), "target/tests/clitest-" + rand + "/output" ).getAbsolutePath();

    private final String INDEX_DIR =
        new File( getBasedir(), "target/tests/clitest-" + rand + "/index" ).getAbsolutePath();

    private final String UNPACK_DIR =
        new File( getBasedir(), "target/tests/clitest-" + rand + "/unpack" ).getAbsolutePath();

    private final String TEST_REPO = new File( getBasedir(), "src/test/repo" ).getAbsolutePath();

    protected OutputStream out;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        out = new OutputStream()
        {

            private StringBuilder buf = new StringBuilder();

            @Override
            public void write( int b )
                throws IOException
            {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) b;
                buf.append( new String( bytes ) );
            }

            @Override
            public String toString()
            {
                String string = buf.toString();
                buf = new StringBuilder();
                return string;
            }
        };

        FileUtils.deleteDirectory( INDEX_DIR );
        FileUtils.deleteDirectory( DEST_DIR );
        FileUtils.deleteDirectory( UNPACK_DIR );

    }

    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();

    }

    protected File getTestFile( String path )
    {
        return new File( new File( getBasedir() ), path );
    }

    @Test
    public void testNoArgs()
    {
        int code = execute();
        String output = out.toString();
        assertEquals( output, 1, code );
        assertTrue( "Should print usage", output.contains( "usage: nexus-indexer [options]" ) );
    }

    @Test
    public void testRequiredArgs()
        throws Exception
    {
        int code = execute( "--repository", TEST_REPO, "--index", INDEX_DIR, "-d", DEST_DIR );
        String output = out.toString();
        assertEquals( output, 0, code );
        assertIndexFiles();
    }

    @Test
    public void testUnpack()
        throws Exception
    {
        // first create an index, in the destination dir
        execute( "--repository", TEST_REPO, "--index", INDEX_DIR, "-d", DEST_DIR );
        // then unpack it
        int code = execute( "--unpack", "--index", DEST_DIR, "-d", UNPACK_DIR );
        String output = out.toString();
        assertEquals( output, 0, code );
        
        //FIXME: Looks strange that a newly generated index can not be reopened.
        //assertIndexFiles( UNPACK_DIR );
    }

    @Test
    public void testMissingArgs()
        throws IOException
    {
        String usage = "usage: nexus-indexer";

        int code = execute( "--repository", "--index", INDEX_DIR, "-d", DEST_DIR );
        String output = out.toString();
        assertEquals( output, 1, code );
        assertTrue( "Should print bad usage", output.contains( usage ) );

        code = execute( "--repository", TEST_REPO, "--index", "-d", DEST_DIR );
        output = out.toString();
        assertEquals( output, 1, code );
        assertTrue( "Should print bad usage", output.contains( usage ) );

        code = execute( "--repository", TEST_REPO, "--index", INDEX_DIR, "-d" );
        output = out.toString();
        assertEquals( output, 1, code );
        assertTrue( "Should print bad usage", output.contains( usage ) );

        code = execute( "--repository", "--index", "-d" );
        output = out.toString();
        assertEquals( output, 1, code );
        assertTrue( "Should print bad usage but '" + output + "'", output.contains( usage ) );

        assertFalse( "Index file was generated", new File( INDEX_DIR ).exists() );
    }

    @Test
    public void testAbrvsRequiredArgs()
        throws Exception
    {
        int code = execute( "-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR );
        String output = out.toString();
        assertEquals( output, 0, code );
        assertIndexFiles();
    }

    @Test
    public void testLoggingLevel()
        throws Exception
    {
        int code = execute( "-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR );
        String normal = out.toString();
        assertEquals( normal, 0, code );
        assertIndexFiles();

        setUp();

        code = execute( "-q", "-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR );
        String quiet = out.toString();
        assertEquals( quiet, 0, code );
        assertFalse( "Expected an different output on quiet mode:\n" + normal, normal.equals( quiet ) );
        assertIndexFiles();

        setUp();

        code = execute( "-X", "-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR );
        String debug = out.toString();
        assertEquals( debug, 0, code );
        assertFalse( "Expected an different output on debug mode:\n" + normal, normal.equals( debug ) );
        assertIndexFiles();

        setUp();

        code = execute( "-e", "-r", TEST_REPO, "-i", INDEX_DIR, "-d", DEST_DIR );
        String error = out.toString();
        assertEquals( error, 0, code );
        assertFalse( "Expected an different output on error mode:\n" + normal, normal.equals( error ) );
        assertIndexFiles();
    }

    @Test
    public void testInvalidRepo()
        throws Exception
    {
        int code =
            execute( "-r", new File( "target/undexinting/repo/to/try/what/will/happen/here" ).getCanonicalPath(), "-i",
                     INDEX_DIR, "-d", DEST_DIR );
        String output = out.toString();
        assertEquals( output, 1, code );
    }

    private void assertIndexFiles()
        throws Exception
    {
        //FIXME: Looks strange that a newly generated index can not be reopened.
        //assertIndexFiles( INDEX_DIR );
    }

    @Test
    @Ignore("Old lucene format not supported")
    public void ignoreAssertIndexFiles( final String indexDir )
        throws Exception
    {
        IndexingContext context = null;
        NexusIndexer indexer = lookup( NexusIndexer.class );
        try
        {
            List<IndexCreator> indexCreators = null; /*lookup( List<IndexCreator>.class );*/

            context =
                indexer.addIndexingContext( "index", "index", new File( TEST_REPO ), new File( indexDir ), null, null,
                                            indexCreators );

            assertFalse( "No index file was generated", new File( indexDir ).list().length == 0 );

            Query query = indexer.constructQuery( MAVEN.GROUP_ID, "ch.marcus-schulte.maven", SearchType.SCORED );

            FlatSearchRequest request = new FlatSearchRequest( query );
            FlatSearchResponse response = indexer.searchFlat( request );
            assertEquals( response.getResults().toString(), 1, response.getTotalHits() );
        }
        finally
        {
            if ( context != null )
            {
                indexer.removeIndexingContext( context, true );
            }
        }
    }

    protected abstract int execute( String... args );

}