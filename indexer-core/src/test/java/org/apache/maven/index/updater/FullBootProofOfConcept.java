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
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.updater.WagonHelper.WagonFetcher;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;

public class FullBootProofOfConcept
{

    private static final int ONE_MEGABYTE = 1024 * 1024;

    public static void main( final String[] args )
        throws IOException
    {
        for ( int i = 0; i < 1; i++ )
        {
            File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
            basedir.deleteOnExit();

            try
            {
                run( basedir );
            }
            catch ( UnsupportedExistingLuceneIndexException e )
            {
                e.printStackTrace();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            catch ( ComponentLookupException e )
            {
                e.printStackTrace();
            }
            catch ( PlexusContainerException e )
            {
                e.printStackTrace();
            }
            catch ( ParseException e )
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    FileUtils.forceDelete( basedir );
                }
                catch ( IOException e )
                {
                }
            }
        }
    }

    public static void run( final File basedir )
        throws IOException, ComponentLookupException, PlexusContainerException, ParseException,
        UnsupportedExistingLuceneIndexException
    {
        try
        {
            FileUtils.forceDelete( basedir );
        }
        catch ( IOException e )
        {
            // just do your best to delete this.
        }

        basedir.mkdirs();

        PlexusContainer container = new DefaultPlexusContainer();

        IndexCreator min = container.lookup( IndexCreator.class, "min" );
        IndexCreator jar = container.lookup( IndexCreator.class, "jarContent" );

        List<IndexCreator> creators = new ArrayList<IndexCreator>();
        creators.add( min );
        creators.add( jar );

        String repositoryId = "test";
        String repositoryUrl = "http://localhost:8081/nexus/content/groups/sonatype-public/";
        // String repositoryUrl = "http://repository.sonatype.org/content/groups/public/";
        // String repositoryUrl = "http://repository.sonatype.org/content/groups/sonatype/";
        String indexUrl = repositoryUrl + ".index";

        IndexingContext ctx =
            new DefaultIndexingContext( repositoryId, repositoryId, basedir, basedir, repositoryUrl, indexUrl,
                creators, true );

        // craft the Wagon based Resource

        TransferListener tl = new TransferListener()
        {

            private int col = 0;

            private int count = 0;

            private int mb = 0;

            public void transferStarted( final TransferEvent transferEvent )
            {
                System.out.println( "Started transfer: " + transferEvent.getResource().getName() );
            }

            public void transferProgress( final TransferEvent transferEvent, final byte[] buffer, final int length )
            {
                if ( buffer == null )
                {
                    return;
                }

                count += buffer.length;

                if ( ( count / ONE_MEGABYTE ) > mb )
                {
                    if ( col > 80 )
                    {
                        System.out.println();
                        col = 0;
                    }

                    System.out.print( '.' );
                    col++;
                    mb++;
                }
            }

            public void transferInitiated( final TransferEvent transferEvent )
            {
            }

            public void transferError( final TransferEvent transferEvent )
            {
                System.out.println( "[ERROR]: " + transferEvent.getException().getLocalizedMessage() );
                transferEvent.getException().printStackTrace();
            }

            public void transferCompleted( final TransferEvent transferEvent )
            {
                System.out.println( "\nCompleted transfer: " + transferEvent.getResource().getName() + " ("
                    + (double) ( count / ONE_MEGABYTE ) + " MB)" );
            }

            public void debug( final String message )
            {
                System.out.println( "[DEBUG]: " + message );
            }
        };

        WagonHelper wh = new WagonHelper( container );

        WagonFetcher wf = wh.getWagonResourceFetcher( tl, null, null );

        IndexUpdateRequest updateRequest = new IndexUpdateRequest( ctx, wf );

        container.lookup( IndexUpdater.class ).fetchAndUpdateIndex( updateRequest );
    }

}