package org.apache.maven.index.search.backend.indexer.internal;

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

import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.search.MAVEN;
import org.apache.maven.index.search.Record;
import org.apache.maven.index.search.SearchRequest;
import org.apache.maven.index.search.SearchResponse;
import org.apache.maven.index.search.request.Field;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.maven.index.search.request.BooleanQuery.and;
import static org.apache.maven.index.search.request.FieldQuery.fieldQuery;
import static org.apache.maven.index.search.request.Query.query;

@Ignore("This is more a showcase")
public class IndexerCoreSearchBackendImplTest
{
    private PlexusContainer plexusContainer;

    private Indexer indexer;

    private IndexUpdater indexUpdater;

    private Wagon httpWagon;

    private IndexingContext centralContext;

    private IndexerCoreSearchBackendImpl backend;

    private void dumpSingle( AtomicInteger counter, List<Record> page )
    {
        for ( Record record : page )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( record.getValue( MAVEN.GROUP_ID ) ).append( ":" ).append( record.getValue( MAVEN.ARTIFACT_ID ) )
                    .append( ":" ).append( record.getValue( MAVEN.VERSION ) );
            if ( record.hasField( MAVEN.PACKAGING ) )
            {
                if ( record.hasField( MAVEN.CLASSIFIER ) )
                {
                    sb.append( ":" ).append( record.getValue( MAVEN.CLASSIFIER ) );
                }
                sb.append( ":" ).append( record.getValue( MAVEN.PACKAGING ) );
            }

            List<String> remarks = new ArrayList<>();
            if ( record.getLastUpdated() != null )
            {
                remarks.add( "lastUpdate=" + Instant.ofEpochMilli( record.getLastUpdated() ) );
            }
            if ( record.hasField( MAVEN.VERSION_COUNT ) )
            {
                remarks.add( "versionCount=" + record.getValue( MAVEN.VERSION_COUNT ) );
            }
            if ( record.hasField( MAVEN.HAS_SOURCE ) )
            {
                remarks.add( "hasSource=" + record.getValue( MAVEN.HAS_SOURCE ) );
            }
            if ( record.hasField( MAVEN.HAS_JAVADOC ) )
            {
                remarks.add( "hasJavadoc=" + record.getValue( MAVEN.HAS_JAVADOC ) );
            }

            System.out.print( counter.incrementAndGet() + ". " + sb );
            if ( !remarks.isEmpty() )
            {
                System.out.print( " " + remarks );
            }
            System.out.println();
        }
    }

    private void dumpPage( SearchResponse searchResponse ) throws IOException
    {
        AtomicInteger counter = new AtomicInteger( 0 );
        System.out.println("QUERY: " + searchResponse.getSearchRequest().getQuery().toString());
        dumpSingle( counter, searchResponse.getPage() );
        while ( searchResponse.getCurrentHits() > 0 )
        {
            searchResponse = backend.search( searchResponse.getSearchRequest().nextPage() );
            dumpSingle( counter, searchResponse.getPage() );
            if ( counter.get() > 50 )
            {
                System.out.println( "ABORTED TO NOT SPAM" );
                break; // do not spam the SMO service
            }
        }
        System.out.println();
    }

    @Before
    public void prepareAndUpdateBackend() throws Exception
    {
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning( PlexusConstants.SCANNING_CACHE );
        this.plexusContainer = new DefaultPlexusContainer( config );

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup( Indexer.class );
        this.indexUpdater = plexusContainer.lookup( IndexUpdater.class );
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup( Wagon.class, "http" );

        // Files where local cache is (if any) and Lucene Index should be located
        File centralLocalCache = new File( "target/central-cache" );
        File centralIndexDir = new File( "target/central-index" );

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add( plexusContainer.lookup( IndexCreator.class, "min" ) );
        indexers.add( plexusContainer.lookup( IndexCreator.class, "jarContent" ) );
        indexers.add( plexusContainer.lookup( IndexCreator.class, "maven-plugin" ) );

        // Create context for central repository index
        centralContext = indexer.createIndexingContext( "central-context", "central", centralLocalCache,
                centralIndexDir, "https://repo1.maven.org/maven2", null, true, true, indexers );

        // Update the index (incremental update will happen if this is not 1st run and files are not deleted)
        // This whole block below should not be executed on every app start, but rather controlled by some configuration
        // since this block will always emit at least one HTTP GET. Central indexes are updated once a week, but
        // other index sources might have different index publishing frequency.
        // Preferred frequency is once a week.
        System.out.println( "Updating Index..." );
        System.out.println( "This might take a while on first run, so please be patient!" );
        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
        TransferListener listener = new AbstractTransferListener()
        {
            public void transferStarted( TransferEvent transferEvent )
            {
                System.out.print( "  Downloading " + transferEvent.getResource().getName() );
            }

            public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
            {
            }

            public void transferCompleted( TransferEvent transferEvent )
            {
                System.out.println( " - Done" );
            }
        };
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher( httpWagon, listener, null, null );

        Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest( centralContext, resourceFetcher );
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex( updateRequest );
        if ( updateResult.isFullUpdate() )
        {
            System.out.println( "Full update happened!" );
        }
        else if ( updateResult.getTimestamp().equals( centralContextCurrentTimestamp ) )
        {
            System.out.println( "No update needed, index is up to date!" );
        }
        else
        {
            System.out.println(
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - " + updateResult.getTimestamp() + " period." );
        }
        System.out.println();

        this.backend = new IndexerCoreSearchBackendImpl( indexer, centralContext );
    }

    @After
    public void cleanup() throws IOException
    {
        indexer.closeIndexingContext( centralContext, false );
        plexusContainer.dispose();
    }

    @Test
    public void smoke() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest( query( "smoke" ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void g() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest( fieldQuery( MAVEN.GROUP_ID, "org.apache.maven.plugins" ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void ga() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest( and( fieldQuery( MAVEN.GROUP_ID, "org.apache.maven.plugins" ),
                fieldQuery( MAVEN.ARTIFACT_ID, "maven-clean-plugin" ) ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void gav() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest( and( fieldQuery( MAVEN.GROUP_ID, "org.apache.maven.plugins" ),
                fieldQuery( MAVEN.ARTIFACT_ID, "maven-clean-plugin" ), fieldQuery( MAVEN.VERSION, "3.1.0" ) ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void sha1() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest(
                fieldQuery( MAVEN.SHA1, "8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12" ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void cn() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest( fieldQuery( MAVEN.CLASS_NAME, "MavenRepositorySystem" ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }

    @Test
    public void fqcn() throws IOException
    {
        SearchRequest searchRequest = new SearchRequest(
                fieldQuery( MAVEN.FQ_CLASS_NAME, "org.apache.maven.bridge.MavenRepositorySystem" ) );
        SearchResponse searchResponse = backend.search( searchRequest );
        System.out.println( "TOTAL HITS: " + searchResponse.getTotalHits() );
        dumpPage( searchResponse );
    }
}
