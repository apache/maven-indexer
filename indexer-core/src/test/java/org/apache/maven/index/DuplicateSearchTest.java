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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;

public class DuplicateSearchTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    protected IndexingContext context1;

    protected Directory contextDir1 = new RAMDirectory();

    protected IndexingContext context2;

    protected Directory contextDir2 = new RAMDirectory();

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        // we have a context with ID "repo1-ctx" that contains index of repository with ID "repo1"
        context = nexusIndexer.addIndexingContext( "repo1-ctx", "repo1", repo, indexDir, null, null, FULL_CREATORS );
        // we have a context with ID "repo2-ctx" that contains index of repository with ID "repo2"
        context1 = nexusIndexer.addIndexingContext( "repo2-ctx", "repo2", repo, contextDir1, null, null, FULL_CREATORS );
        // we have a context with ID "repo3-ctx" that contains index of repository with ID "repo2"
        context2 = nexusIndexer.addIndexingContext( "repo3-ctx", "repo2", repo, contextDir2, null, null, FULL_CREATORS );

        // note: those three contexts, while representing different entities are actually indexing the same repository
        // directory, hence, will have exactly same content! Also, context1 and context2 do say, they both index
        // repository with ID "repo2"!

        nexusIndexer.scan( context );
        nexusIndexer.scan( context1 );
        nexusIndexer.scan( context2 );

        assertNotNull( context.getTimestamp() );
        assertNotNull( context1.getTimestamp() );
        assertNotNull( context2.getTimestamp() );
    }

    // a bit of explanation:
    // we focus on a G "org.slf4j". The given section (subdir tree) looks like this (simplified):
    // ├── org
    //    ├── slf4j
    //       ├── jcl104-over-slf4j
    //       │   └── 1.4.2
    //       │   ├── jcl104-over-slf4j-1.4.2-sources.jar
    //       │   ├── jcl104-over-slf4j-1.4.2-sources.jar.sha1
    //       │   ├── jcl104-over-slf4j-1.4.2.jar
    //       │   ├── jcl104-over-slf4j-1.4.2.jar.sha1
    //       │   ├── jcl104-over-slf4j-1.4.2.pom
    //       │   └── jcl104-over-slf4j-1.4.2.pom.sha1
    //       ├── slf4j-api
    //       │   ├── 1.4.1
    //       │   │   ├── slf4j-api-1.4.1-sources.jar
    //       │   │   ├── slf4j-api-1.4.1-sources.jar.sha1
    //       │   │   ├── slf4j-api-1.4.1.jar
    //       │   │   ├── slf4j-api-1.4.1.jar.sha1
    //       │   │   ├── slf4j-api-1.4.1.pom
    //       │   │   └── slf4j-api-1.4.1.pom.sha1
    //       │   └── 1.4.2
    //       │   ├── slf4j-api-1.4.2-sources.jar
    //       │   ├── slf4j-api-1.4.2-sources.jar.sha1
    //       │   ├── slf4j-api-1.4.2.jar
    //       │   ├── slf4j-api-1.4.2.jar.sha1
    //       │   ├── slf4j-api-1.4.2.pom
    //       │   └── slf4j-api-1.4.2.pom.sha1
    //       └── slf4j-log4j12
    //       └── 1.4.1
    //       ├── slf4j-log4j12-1.4.1-bin.tar.gz
    //       ├── slf4j-log4j12-1.4.1-bin.zip
    //       ├── slf4j-log4j12-1.4.1-sources.jar
    //       ├── slf4j-log4j12-1.4.1-sources.jar.sha1
    //       ├── slf4j-log4j12-1.4.1.jar
    //       ├── slf4j-log4j12-1.4.1.jar.sha1
    //       ├── slf4j-log4j12-1.4.1.pom
    //       └── slf4j-log4j12-1.4.1.pom.sha1
    //
    // Records on index are created as: each main and each "classified" artifact is one Document.
    // Meaning, with structure above, for groupId "org.slf4j" we have 10 records:
    // G:A:V
    // org.slf4j:jcl104-over-slf4j:1.4.2:jar
    // org.slf4j:jcl104-over-slf4j:1.4.2:jar:sources
    // org.slf4j:slf4j-api:1.4.1:jar
    // org.slf4j:slf4j-api:1.4.1:jar:sources
    // org.slf4j:slf4j-api:1.4.2:jar
    // org.slf4j:slf4j-api:1.4.2:jar:sources
    // org.slf4j:slf4j-log4j12:1.4.1:jar
    // org.slf4j:slf4j-log4j12:1.4.1:jar:sources
    // org.slf4j:slf4j-log4j12:1.4.1:zip:bin
    // org.slf4j:slf4j-log4j12:1.4.1:tar.gz:bin
    //
    // ArtifactInfo, along with GAV carries contextId and repositoryId too!

    public void testProveSvnRev1158917IsWrong()
        throws IOException
    {
        // change is SVN Rev1158917 (http://svn.apache.org/viewvc?view=revision&revision=1158917) is wrong (and is
        // undone)
        // because after removing it, we still dont have GAV dupes in results, here is a proof:

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.slf4j" ) );
        FlatSearchRequest fsReq = new FlatSearchRequest( query );
        fsReq.getContexts().add( context );
        fsReq.getContexts().add( context1 );
        fsReq.getContexts().add( context2 );

        FlatSearchResponse fsResp = nexusIndexer.searchFlat( fsReq );

        Assert.assertEquals( "We have 10 GAVs coming from three contextes", 10, fsResp.getResults().size() );

        // Why? Look at the FlatSearchRequest default comparator it uses, it is ArtifactInfo.VERSION_COMPARATOR
        // that neglects contextId and repositoryId and compares GAVs only, and the Collection fixed in SVN Rev1158917
        // is actually a Set<ArtifactInfo with proper comparator set.
    }

    public void testHowUniqueSearchShouldBeDone()
        throws IOException
    {
        // my use case: I am searching for duplicates in given two contexts belonging to given groupId "org.slf4j"
        // I expect to find intersection of two reposes, since both of those indexes/reposes contains that

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.slf4j" ) );

        FlatSearchRequest fsReq = new FlatSearchRequest( query );
        fsReq.setArtifactInfoComparator( ArtifactInfo.CONTEXT_VERSION_COMPARATOR );
        fsReq.getContexts().add( context );
        fsReq.getContexts().add( context1 );
        fsReq.getContexts().add( context2 );

        FlatSearchResponse fsResp = nexusIndexer.searchFlat( fsReq );

        Assert.assertEquals( "We have 10 GAVs coming from three contextes, it is 30", 30, fsResp.getResults().size() );

        // Why? We set explicitly the comparator to CONTEXT_VERSION_COMPARATOR, that compares GAV+contextId, hence,
        // will return all hits from all participating contexts.
    }

    public void testHowtoPerformAggregatedSearch()
        throws IOException
    {
        // Note: currently this is implemented for IteratorSearches only! TBD for Flat and Grouped searches

        // my use case: searching across multiple contexts, querying how many combinations of GAs exists in groupId
        // "org.slf4j".

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.slf4j" ) );

        IteratorSearchRequest isReq = new IteratorSearchRequest( query );

        // so, how many different GA combinations exists, this is almost equal to SQLs group by "groupId, artifactId"
        isReq.setArtifactInfoFilter( new UniqueArtifactFilterPostprocessor( new HashSet<Field>( Arrays.asList(
            MAVEN.GROUP_ID, MAVEN.ARTIFACT_ID ) ) ) );
        isReq.getContexts().add( context );
        isReq.getContexts().add( context1 );
        isReq.getContexts().add( context2 );

        // Note: iteratorSearch is completely different beast that flat or grouped searches. While it excels in
        // low memory consumption and extra features (like presented here), it needs special care: you have to handle it
        // as resource, since lazy loading requires context locking, and if you forget to do so, you will end up with a
        // flaky
        // application that will most probably fail (by deadlocking itself or thrashing indexes).

        IteratorSearchResponse isResp = null;
        int actualResultCount = 0;

        try
        {
            isResp = nexusIndexer.searchIterator( isReq );

            // consume the iterator to count actual result set size
            for ( ArtifactInfo ai : isResp )
            {
                actualResultCount++;
            }
        }
        finally
        {
            if ( isResp != null )
            {
                isResp.close();
            }
        }

        Assert.assertEquals( "Iterator delivered to us 3 results, since we have 3 GA combinations", 3,
            actualResultCount );
        Assert.assertEquals(
            "IteratorSearch is strange beast, due to it's nature, it cannot say how many elements it (will) return in advance, due to filtering, postprocessing, etc",
            -1, isResp.getReturnedHitsCount() );
        Assert.assertEquals(
            "The processing/search tackled 10 GAVs coming from three contextes, it is 30. This is the record count that were hit by processing of this search, but IS NOT the count results (it depends on filtering, comparators, etc)!",
            30, isResp.getTotalHitsCount() );
    }
}
