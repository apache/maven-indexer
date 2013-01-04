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
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.updater.fixtures.ServerTestFixture;
import org.apache.maven.index.updater.fixtures.TransferListenerFixture;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.events.TransferEvent;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;

public class DefaultIndexUpdaterEmbeddingIT
    extends TestCase
{
    private String baseUrl;

    private PlexusContainer container;

    private ServerTestFixture server;

    private IndexUpdater updater;

    private WagonHelper wagonHelper;

    public void testBasicIndexRetrieval()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture(), null,
                    null ) );

            updater.fetchAndUpdateIndex( updateRequest );
            
            ctx.close( false );
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

    public void testBasicAuthenticatedIndexRetrieval()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "protected/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture(),
                    new AuthenticationInfo()
                    {
                        private static final long serialVersionUID = 1L;

                        {
                            setUserName( "user" );
                            setPassword( "password" );
                        }
                    }, null ) );

            updater.fetchAndUpdateIndex( updateRequest );
            
            ctx.close( false );
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

    public void testAuthenticatedIndexRetrieval_LongAuthorizationHeader()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "protected/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture(),
                    new AuthenticationInfo()
                    {
                        private static final long serialVersionUID = 1L;

                        {
                            setUserName( "longuser" );
                            setPassword( ServerTestFixture.LONG_PASSWORD );
                        }
                    }, null ) );

            updater.fetchAndUpdateIndex( updateRequest );
            
            ctx.close( false );
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

    public void testBasicHighLatencyIndexRetrieval()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "slow/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture(),
                    new AuthenticationInfo()
                    {
                        private static final long serialVersionUID = 1L;

                        {
                            setUserName( "user" );
                            setPassword( "password" );
                        }
                    }, null ) );

            updater.fetchAndUpdateIndex( updateRequest );
            
            ctx.close( false );
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

    // Disabled, since with Wagon you cannot set timeout as it was possible with Jetty client
    public void OFFtestHighLatencyIndexRetrieval_LowConnectionTimeout()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "slow/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture()
                {
                    @Override
                    public void transferError( final TransferEvent transferEvent )
                    {
                    }
                }, new AuthenticationInfo()
                {
                    private static final long serialVersionUID = 1L;

                    {
                        setUserName( "user" );
                        setPassword( "password" );
                    }
                }, null ) );

            // ResourceFetcher fetcher =
            // new JettyResourceFetcher().setConnectionTimeoutMillis( 100 ).setAuthenticationInfo(
            // updateRequest.getAuthenticationInfo() ).addTransferListener( updateRequest.getTransferListener() );

            try
            {
                updater.fetchAndUpdateIndex( updateRequest );
                fail( "Should timeout and throw IOException." );
            }
            catch ( IOException e )
            {
                System.out.println( "Operation timed out due to short connection timeout, as expected." );
            }
            
            ctx.close( false );
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

    // Disabled, since with Wagon you cannot set timeout as it was possible with Jetty client
    public void OFFtestHighLatencyIndexRetrieval_LowTransactionTimeout()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "slow/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture()
                {
                    @Override
                    public void transferError( final TransferEvent transferEvent )
                    {
                    }
                }, new AuthenticationInfo()
                {
                    private static final long serialVersionUID = 1L;

                    {
                        setUserName( "user" );
                        setPassword( "password" );
                    }
                }, null ) );

            // ResourceFetcher fetcher =
            // new JettyResourceFetcher().setTransactionTimeoutMillis( 100 ).setAuthenticationInfo(
            // updateRequest.getAuthenticationInfo() ).addTransferListener( updateRequest.getTransferListener() );

            try
            {
                updater.fetchAndUpdateIndex( updateRequest );
                fail( "Should timeout and throw IOException." );
            }
            catch ( IOException e )
            {
                System.out.println( "Operation timed out due to short transaction timeout, as expected." );
            }
            
            ctx.close( false );
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

    public void testIndexRetrieval_InfiniteRedirection()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, baseUrl + "redirect-trap/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture()
                {
                    @Override
                    public void transferError( final TransferEvent transferEvent )
                    {
                    }
                }, null, null ) );

            try
            {
                updater.fetchAndUpdateIndex( updateRequest );
                fail( "Should throw IOException from too many redirects." );
            }
            catch ( IOException e )
            {
                System.out.println( "Operation timed out due to too many redirects, as expected." );
            }
            
            ctx.close( false );
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

    public void testIndexRetrieval_BadHostname()
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        File basedir = File.createTempFile( "nexus-indexer.", ".dir" );
        basedir.delete();
        basedir.mkdirs();

        try
        {
            IndexingContext ctx = newTestContext( basedir, "http://dummy/" );

            IndexUpdateRequest updateRequest =
                new IndexUpdateRequest( ctx, wagonHelper.getWagonResourceFetcher( new TransferListenerFixture()
                {
                    @Override
                    public void transferError( final TransferEvent transferEvent )
                    {
                    }
                }, new AuthenticationInfo()
                {
                    private static final long serialVersionUID = 1L;

                    {
                        setUserName( "user" );
                        setPassword( "password" );
                    }
                }, null ) );

            try
            {
                updater.fetchAndUpdateIndex( updateRequest );
                fail( "Should timeout and throw IOException." );
            }
            catch ( Exception e )
            {
                System.out.println( "Connection failed as expected." );
            }
            
            ctx.close( false );
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

    private IndexingContext newTestContext( final File basedir, final String baseUrl )
        throws IOException, UnsupportedExistingLuceneIndexException, ComponentLookupException
    {
        IndexCreator min = container.lookup( IndexCreator.class, "min" );
        IndexCreator jar = container.lookup( IndexCreator.class, "jarContent" );

        List<IndexCreator> creators = new ArrayList<IndexCreator>();
        creators.add( min );
        creators.add( jar );

        String repositoryId = "test";

        return new DefaultIndexingContext( repositoryId, repositoryId, basedir, basedir, baseUrl, baseUrl, creators,
            true );
    }

    @Override
    public void setUp()
        throws Exception
    {

        server = new ServerTestFixture( 0 );
        container = new DefaultPlexusContainer();

        baseUrl = "http://127.0.0.1:" + server.getPort() + "/";

        updater = container.lookup( IndexUpdater.class, "default" );

        wagonHelper = new WagonHelper( container );
    }

    @Override
    public void tearDown()
        throws Exception
    {
        container.release( updater );
        container.dispose();
        server.stop();
    }
}