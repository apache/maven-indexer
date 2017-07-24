package org.apache.maven.index.updater;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.FileUtils;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;

public class DownloadRemoteIndexerManagerTest
    extends AbstractIndexUpdaterTest
{
    private Server server;

    private File fakeCentral;

    private IndexingContext centralContext;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        fakeCentral = new File( getBasedir(), "target/repos/fake-central" );
        fakeCentral.mkdirs();

        // create proxy server
        ServerSocket s = new ServerSocket( 0 );
        int port = s.getLocalPort();
        s.close();

        server = new Server( port );

        ResourceHandler resource_handler = new ResourceHandler()
        {
            @Override
            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
//                System.out.print( "JETTY: " + target );
                super.handle( target, request, response, dispatch );
//                System.out.println( "  ::  " + ( (Response) response ).getStatus() );
            }
        };
        resource_handler.setResourceBase( fakeCentral.getAbsolutePath() );
        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[] { resource_handler, new DefaultHandler() } );
        server.setHandler( handlers );

//        System.out.print( "JETTY Started on port: " + port );
        server.start();

        // make context "fake central"
        centralContext =
            indexer.addIndexingContext( "central", "central", fakeCentral, getDirectory( "central" ),
                "http://localhost:" + port, null, MIN_CREATORS );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        server.stop();

        FileUtils.forceDelete( fakeCentral );

        super.tearDown();
    }

    public void testRepoReindex()
        throws Exception
    {
        IndexUpdateRequest iur;

        File index1 = new File( getBasedir(), "src/test/resources/repo-index/index" );
        File index2 = new File( getBasedir(), "src/test/resources/repo-index/index2" );
        File centralIndex = new File( fakeCentral, ".index" );

        // copy index 02
        overwriteIndex( index2, centralIndex );

        iur =
            new IndexUpdateRequest( centralContext, new WagonHelper( getContainer() ).getWagonResourceFetcher( null ) );
        iur.setForceFullUpdate( true );

        updater.fetchAndUpdateIndex( iur );

        searchFor( "org.sonatype.nexus", 8, centralContext );

        // copy index 01
        overwriteIndex( index1, centralIndex );

        iur =
            new IndexUpdateRequest( centralContext, new WagonHelper( getContainer() ).getWagonResourceFetcher( null ) );
        iur.setForceFullUpdate( true );
        // just a dummy filter to invoke filtering! -- this is what I broke unnoticing it
        iur.setDocumentFilter( new DocumentFilter()
        {
            public boolean accept( Document doc )
            {
                return true;
            }
        });

        updater.fetchAndUpdateIndex( iur );

        searchFor( "org.sonatype.nexus", 1, centralContext );

        // copy index 02
        overwriteIndex( index2, centralIndex );

        iur =
            new IndexUpdateRequest( centralContext, new WagonHelper( getContainer() ).getWagonResourceFetcher( null ) );
        iur.setForceFullUpdate( true );

        updater.fetchAndUpdateIndex( iur );

        searchFor( "org.sonatype.nexus", 8, centralContext );
    }

    private void overwriteIndex( File source, File destination )
        throws Exception
    {
        File indexFile = new File( destination, "nexus-maven-repository-index.gz" );
        File indexProperties = new File( destination, "nexus-maven-repository-index.properties" );

        long lastMod = -1;
        if ( destination.exists() )
        {
            FileUtils.forceDelete( destination );
            lastMod = indexFile.lastModified();
        }
        FileUtils.copyDirectory( source, destination );
        long lastMod2 = indexFile.lastModified();
        assertTrue( lastMod < lastMod2 );

        Properties p = new Properties();
        try (InputStream input = Files.newInputStream( indexProperties.toPath() ))
        {
            p.load( input );
        }

        p.setProperty( "nexus.index.time", format( new Date() ) );
        p.setProperty( "nexus.index.timestamp", format( new Date() ) );

        try (OutputStream output = Files.newOutputStream( indexProperties.toPath() ))
        {
            p.store( output, null );
        }
    }

    private String format( Date d )
    {
        SimpleDateFormat df = new SimpleDateFormat( IndexingContext.INDEX_TIME_FORMAT );
        df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        return df.format( d );
    }
}
