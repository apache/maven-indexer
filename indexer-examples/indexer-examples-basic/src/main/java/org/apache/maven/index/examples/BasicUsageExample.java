package org.apache.maven.index.examples;

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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiBits;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.Field;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Grouping;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.expr.UserInputSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Collection of some use cases.
 */
public class BasicUsageExample
{
    public static void main( String[] args )
        throws Exception
    {
        new BasicUsageExample().perform();
    }

    // ==

    private final Injector injector;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;

    public BasicUsageExample()
    {
        final Module app = Main.wire(
                BeanScanning.INDEX
        );
        injector = Guice.createInjector( app );

        // lookup the indexer components from plexus
        this.indexer = injector.getInstance( Indexer.class );
        this.indexUpdater = injector.getInstance( IndexUpdater.class );
        // lookup wagon used to remotely fetch index
        this.httpWagon = new HttpWagon();

    }

    public void perform()
        throws IOException, InvalidVersionSpecificationException
    {
        // Files where local cache is (if any) and Lucene Index should be located
        File centralLocalCache = new File( "target/central-cache" );
        File centralIndexDir = new File( "target/central-index" );

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add( injector.getInstance( Key.get( IndexCreator.class, Names.named( "min" ) ) ) );
        indexers.add( injector.getInstance( Key.get( IndexCreator.class, Names.named( "jarContent" ) ) ) );
        indexers.add( injector.getInstance( Key.get( IndexCreator.class, Names.named( "maven-plugin" ) ) ) );

        // Create context for central repository index
        centralContext =
            indexer.createIndexingContext( "central-context", "central", centralLocalCache, centralIndexDir,
                                           "https://repo1.maven.org/maven2", null, true, true, indexers );

        // Update the index (incremental update will happen if this is not 1st run and files are not deleted)
        // This whole block below should not be executed on every app start, but rather controlled by some configuration
        // since this block will always emit at least one HTTP GET. Central indexes are updated once a week, but
        // other index sources might have different index publishing frequency.
        // Preferred frequency is once a week.
        if ( true )
        {
            Instant updateStart = Instant.now();
            System.out.println( "Updating Index..." );
            System.out.println( "This might take a while on first run, so please be patient!" );
            // Create ResourceFetcher implementation to be used with IndexUpdateRequest
            // Here, we use Wagon based one as shorthand, but all we need is a ResourceFetcher implementation
            TransferListener listener = new AbstractTransferListener()
            {
                Instant start;
                public void transferStarted( TransferEvent transferEvent )
                {
                    start = Instant.now();
                    System.out.print( "  Downloading " + transferEvent.getResource().getName() );
                }

                public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
                {
                }

                public void transferCompleted( TransferEvent transferEvent )
                {
                    System.out.println( " - Done in "
                            + Duration.between( start, Instant.now() ).getSeconds() + " sec" );
                }
            };
            ResourceFetcher resourceFetcher = new WagonFetcher( httpWagon, listener, null, null );

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
                    "Incremental update happened, change covered " + centralContextCurrentTimestamp + " - "
                        + updateResult.getTimestamp() + " period." );
            }

            System.out.println( "Finished in " + Duration.between( updateStart, Instant.now() ).getSeconds() + " sec" );
            System.out.println();
        }

        System.out.println();
        System.out.println( "Using index" );
        System.out.println( "===========" );
        System.out.println();

        // ====
        // Case:
        // dump all the GAVs
        // NOTE: will not actually execute do this below, is too long to do (Central is HUGE), but is here as code
        // example
        if ( false )
        {
            final IndexSearcher searcher = centralContext.acquireIndexSearcher();
            try
            {
                final IndexReader ir = searcher.getIndexReader();
                Bits liveDocs = MultiBits.getLiveDocs( ir );
                for ( int i = 0; i < ir.maxDoc(); i++ )
                {
                    if ( liveDocs == null || liveDocs.get( i ) )
                    {
                        final Document doc = ir.document( i );
                        final ArtifactInfo ai = IndexUtils.constructArtifactInfo( doc, centralContext );
                        System.out.println( ai.getGroupId() + ":" + ai.getArtifactId() + ":" + ai.getVersion() + ":"
                                                + ai.getClassifier() + " (sha1=" + ai.getSha1() + ")" );
                    }
                }
            }
            finally
            {
                centralContext.releaseIndexSearcher( searcher );
            }
        }

        // ====
        // Case:
        // Search for all GAVs with known G and A and having version greater than V

        final GenericVersionScheme versionScheme = new GenericVersionScheme();
        final String versionString = "3.1.0";
        final Version version = versionScheme.parseVersion( versionString );

        // construct the query for known GA
        final Query groupIdQ =
            indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.maven" ) );
        final Query artifactIdQ =
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( "maven-plugin-api" ) );

        final BooleanQuery query = new BooleanQuery.Builder()
            .add( groupIdQ, Occur.MUST )
            .add( artifactIdQ, Occur.MUST )
            // we want "jar" artifacts only
            .add( indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "jar" ) ), Occur.MUST )
            // we want main artifacts only (no classifier)
            // Note: this below is unfinished API, needs fixing
            .add( indexer.constructQuery( MAVEN.CLASSIFIER,
                    new SourcedSearchExpression( Field.NOT_PRESENT ) ), Occur.MUST_NOT )
            .build();

        // construct the filter to express "V greater than"
        final ArtifactInfoFilter versionFilter = ( ctx, ai ) ->
        {
            try
            {
                final Version aiV = versionScheme.parseVersion( ai.getVersion() );
                // Use ">=" if you are INCLUSIVE
                return aiV.compareTo( version ) > 0;
            }
            catch ( InvalidVersionSpecificationException e )
            {
                // do something here? be safe and include?
                return true;
            }
        };

        System.out.println(
            "Searching for all GAVs with org.apache.maven:maven-plugin-api having V greater than 3.1.0" );
        final IteratorSearchRequest request =
            new IteratorSearchRequest( query, Collections.singletonList( centralContext ), versionFilter );
        final IteratorSearchResponse response = indexer.searchIterator( request );
        for ( ArtifactInfo ai : response )
        {
            System.out.println( ai.toString() );
        }

        // Case:
        // Use index
        // Searching for some artifact
        Query gidQ =
            indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.maven.indexer" ) );
        Query aidQ = indexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( "indexer-core" ) );

        BooleanQuery bq = new BooleanQuery.Builder()
                .add( gidQ, Occur.MUST )
                .add( aidQ, Occur.MUST )
                .build();

        searchAndDump( indexer, "all artifacts under GA org.apache.maven.indexer:indexer-core", bq );

        // Searching for some main artifact
        bq = new BooleanQuery.Builder()
                .add( gidQ, Occur.MUST )
                .add( aidQ, Occur.MUST )
                .add( indexer.constructQuery( MAVEN.CLASSIFIER, new SourcedSearchExpression( "*" ) ), Occur.MUST_NOT )
                .build();

        searchAndDump( indexer, "main artifacts under GA org.apache.maven.indexer:indexer-core", bq );

        // doing sha1 search
        searchAndDump( indexer, "SHA1 7ab67e6b20e5332a7fb4fdf2f019aec4275846c2",
                       indexer.constructQuery( MAVEN.SHA1,
                                               new SourcedSearchExpression( "7ab67e6b20e5332a7fb4fdf2f019aec4275846c2" )
                       )
        );

        searchAndDump( indexer, "SHA1 7ab67e6b20 (partial hash)",
                       indexer.constructQuery( MAVEN.SHA1, new UserInputSearchExpression( "7ab67e6b20" ) ) );

        // doing classname search (incomplete classname)
        searchAndDump( indexer, "classname DefaultNexusIndexer (note: Central does not publish classes in the index)",
                       indexer.constructQuery( MAVEN.CLASSNAMES,
                                               new UserInputSearchExpression( "DefaultNexusIndexer" ) ) );

        // doing search for all "canonical" maven plugins latest versions
        bq = new BooleanQuery.Builder()
            .add( indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "maven-plugin" ) ), Occur.MUST )
            .add( indexer.constructQuery( MAVEN.GROUP_ID,
                    new SourcedSearchExpression( "org.apache.maven.plugins" ) ), Occur.MUST )
            .build();

        searchGroupedAndDumpFlat( indexer, "all \"canonical\" maven plugins", bq, new GAGrouping() );

        // doing search for all archetypes latest versions
        searchGroupedAndDump( indexer, "all maven archetypes (latest versions)",
                              indexer.constructQuery( MAVEN.PACKAGING,
                                                      new SourcedSearchExpression( "maven-archetype" ) ),
                              new GAGrouping() );

        // close cleanly
        indexer.closeIndexingContext( centralContext, false );
    }

    public void searchAndDump( Indexer nexusIndexer, String descr, Query q )
        throws IOException
    {
        System.out.println( "Searching for " + descr );

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q, centralContext ) );

        for ( ArtifactInfo ai : response.getResults() )
        {
            System.out.println( ai.toString() );
        }

        System.out.println( "------" );
        System.out.println( "Total: " + response.getTotalHitsCount() );
        System.out.println();
    }

    private static final int MAX_WIDTH = 60;

    public void searchGroupedAndDumpFlat( Indexer nexusIndexer, String descr, Query q, Grouping g )
            throws IOException
    {
        System.out.println( "Searching for " + descr );

        GroupedSearchResponse response = nexusIndexer.searchGrouped( new GroupedSearchRequest( q, g, centralContext ) );

        for ( Map.Entry<String, ArtifactInfoGroup> entry : response.getResults().entrySet() )
        {
            ArtifactInfo ai = entry.getValue().getArtifactInfos().iterator().next();
            System.out.println( "* " + ai.getGroupId() + ":" + ai.getArtifactId() + ":" + ai.getVersion() );
        }

        System.out.println( "------" );
        System.out.println( "Total record hits: " + response.getTotalHitsCount() );
        System.out.println();
    }

    public void searchGroupedAndDump( Indexer nexusIndexer, String descr, Query q, Grouping g )
        throws IOException
    {
        System.out.println( "Searching for " + descr );

        GroupedSearchResponse response = nexusIndexer.searchGrouped( new GroupedSearchRequest( q, g, centralContext ) );

        for ( Map.Entry<String, ArtifactInfoGroup> entry : response.getResults().entrySet() )
        {
            ArtifactInfo ai = entry.getValue().getArtifactInfos().iterator().next();
            System.out.println( "* Entry " + ai );
            System.out.println( "  Latest version:  " + ai.getVersion() );
            System.out.println( StringUtils.isBlank( ai.getDescription() )
                                    ? "No description in plugin's POM."
                                    : StringUtils.abbreviate( ai.getDescription(), MAX_WIDTH ) );
            System.out.println();
        }

        System.out.println( "------" );
        System.out.println( "Total record hits: " + response.getTotalHitsCount() );
        System.out.println();
    }

    public static class WagonFetcher
            implements ResourceFetcher
    {
        private final TransferListener listener;

        private final AuthenticationInfo authenticationInfo;

        private final ProxyInfo proxyInfo;

        private final Wagon wagon;

        public WagonFetcher( final Wagon wagon, final TransferListener listener,
                             final AuthenticationInfo authenticationInfo, final ProxyInfo proxyInfo )
        {
            this.wagon = wagon;
            this.listener = listener;
            this.authenticationInfo = authenticationInfo;
            this.proxyInfo = proxyInfo;
        }

        public void connect( final String id, final String url )
                throws IOException
        {
            Repository repository = new Repository( id, url );

            try
            {
                // wagon = wagonManager.getWagon( repository );

                if ( listener != null )
                {
                    wagon.addTransferListener( listener );
                }

                // when working in the context of Maven, the WagonManager is already
                // populated with proxy information from the Maven environment

                if ( authenticationInfo != null )
                {
                    if ( proxyInfo != null )
                    {
                        wagon.connect( repository, authenticationInfo, proxyInfo );
                    }
                    else
                    {
                        wagon.connect( repository, authenticationInfo );
                    }
                }
                else
                {
                    if ( proxyInfo != null )
                    {
                        wagon.connect( repository, proxyInfo );
                    }
                    else
                    {
                        wagon.connect( repository );
                    }
                }
            }
            catch ( AuthenticationException ex )
            {
                String msg = "Authentication exception connecting to " + repository;
                logError( msg, ex );
                throw new IOException( msg, ex );
            }
            catch ( WagonException ex )
            {
                String msg = "Wagon exception connecting to " + repository;
                logError( msg, ex );
                throw new IOException( msg, ex );
            }
        }

        public void disconnect()
                throws IOException
        {
            if ( wagon != null )
            {
                try
                {
                    wagon.disconnect();
                }
                catch ( ConnectionException ex )
                {
                    throw new IOException( ex.toString(), ex );
                }
            }
        }

        public InputStream retrieve( String name )
                throws IOException, FileNotFoundException
        {
            final File target = Files.createTempFile( name, "tmp" ).toFile();
            target.deleteOnExit();
            retrieve( name, target );
            return new FileInputStream( target )
            {
                @Override
                public void close()
                        throws IOException
                {
                    super.close();
                    target.delete();
                }
            };
        }

        public void retrieve( final String name, final File targetFile )
                throws IOException, FileNotFoundException
        {
            try
            {
                wagon.get( name, targetFile );
            }
            catch ( AuthorizationException e )
            {
                targetFile.delete();
                String msg = "Authorization exception retrieving " + name;
                logError( msg, e );
                throw new IOException( msg, e );
            }
            catch ( ResourceDoesNotExistException e )
            {
                targetFile.delete();
                String msg = "Resource " + name + " does not exist";
                logError( msg, e );
                FileNotFoundException fileNotFoundException = new FileNotFoundException( msg );
                fileNotFoundException.initCause( e );
                throw fileNotFoundException;
            }
            catch ( WagonException e )
            {
                targetFile.delete();
                String msg = "Transfer for " + name + " failed";
                logError( msg, e );
                throw new IOException( msg + "; " + e.getMessage(), e );
            }
        }

        private void logError( final String msg, final Exception ex )
        {
            if ( listener != null )
            {
                listener.debug( msg + "; " + ex.getMessage() );
            }
        }
    }
}
