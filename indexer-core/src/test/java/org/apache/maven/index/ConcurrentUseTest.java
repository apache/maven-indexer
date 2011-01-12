package org.apache.maven.index;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexingContext;

public class ConcurrentUseTest
    extends AbstractNexusIndexerTest
{
    public static final int THREAD_COUNT = 10;

    protected File repo = new File( getBasedir(), "src/test/repo" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        // This IS concurrent test, so here, unlike all other UTs, we DO want to have async commits
        DefaultIndexingContext.BLOCKING_COMMIT = false;

        context =
            nexusIndexer.addIndexingContext( "test-default", "test", repo, indexDir, null, null, DEFAULT_CREATORS );

        assertNull( context.getTimestamp() ); // unknown upon creation

        nexusIndexer.scan( context );

        assertNotNull( context.getTimestamp() );
    }

    public void testConcurrency()
        throws Exception
    {
        IndexUserThread[] threads = new IndexUserThread[THREAD_COUNT];

        ArtifactInfo ai =
            new ArtifactInfo( "test-default", "org.apache.maven.indexer", "index-concurrent-artifact", "", null );

        for ( int i = 0; i < THREAD_COUNT; i++ )
        {
            threads[i] = new IndexUserThread( nexusIndexer, context, ai );

            threads[i].start();
        }

        Thread.sleep( 5000 );

        boolean thereWereProblems = false;

        int totalAdded = 0;

        for ( int i = 0; i < THREAD_COUNT; i++ )
        {
            threads[i].stopThread();

            threads[i].join();

            thereWereProblems = thereWereProblems || threads[i].hadProblem();

            totalAdded += threads[i].getAdded();
        }

        Assert.assertFalse( "Not all thread did clean job!", thereWereProblems );

        context.commit();

        // sleep more than bottleWarmer does, to be sure commit and reopen happened
        // BottleWarmer sleeps 1000 millis
        Thread.sleep( 2000 );

        //

        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, ai.groupId, SearchType.SCORED );

        FlatSearchResponse result = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );

        Assert.assertEquals( "All added should be found after final commit!", totalAdded, result.getTotalHits() );
    }

    // ==

    public static class IndexUserThread
        extends Thread
    {
        private static final AtomicInteger versionSource = new AtomicInteger( 1 );

        private final NexusIndexer nexusIndexer;

        private final IndexingContext indexingContext;

        private boolean stopped = false;

        private int added = 0;

        private int deleted = 0;

        private int lastSearchHitCount = 0;

        private Throwable t;

        public IndexUserThread( NexusIndexer nexusIndexer, IndexingContext indexingContext, ArtifactInfo artifactInfo )
        {
            this.nexusIndexer = nexusIndexer;

            this.indexingContext = indexingContext;
        }

        public int getAdded()
        {
            return added;
        }

        public int getDeleted()
        {
            return deleted;
        }

        public boolean hadProblem()
        {
            return t != null;
        }

        public int getLastSearchHitCount()
        {
            return lastSearchHitCount;
        }

        public void stopThread()
        {
            this.stopped = true;
        }

        public void run()
        {
            ArtifactInfo artifactInfo;

            ArtifactContext ac;

            Query q;

            while ( !stopped )
            {
                if ( System.currentTimeMillis() % 5 == 0 )
                {
                    try
                    {
                        artifactInfo =
                            new ArtifactInfo( "test-default", "org.apache.maven.indexer", "index-concurrent-artifact",
                                "1." + String.valueOf( versionSource.getAndIncrement() ), null );

                        ac = new ArtifactContext( null, null, null, artifactInfo, artifactInfo.calculateGav() );

                        nexusIndexer.addArtifactToIndex( ac, indexingContext );

                        added++;
                    }
                    catch ( Throwable e )
                    {
                        t = e;

                        throw new IllegalStateException( "error", e );
                    }
                }

                if ( System.currentTimeMillis() % 11 == 0 )
                {
                    // TODO: delete some of those already added
                    // artifactInfo.version = "1." + String.valueOf( versionSource.getAndIncrement() );
                    //
                    // ac = new ArtifactContext( null, null, null, artifactInfo, artifactInfo.calculateGav() );
                    //
                    // nexusIndexer.deleteArtifactFromIndex( ac, indexingContext );
                    //
                    // deleted++;
                }

                try
                {
                    q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org.apache.maven.indexer", SearchType.SCORED );

                    FlatSearchResponse result = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );

                    lastSearchHitCount = result.getTotalHits();
                }
                catch ( Throwable e )
                {
                    t = e;

                    throw new IllegalStateException( "error", e );
                }
            }
        }
    }
}
