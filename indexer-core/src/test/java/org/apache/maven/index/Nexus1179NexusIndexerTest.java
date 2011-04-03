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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.NexusIndexer;

public class Nexus1179NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-1179" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-1179", "nexus-1179", repo, indexDir, null, null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchFlat()
        throws Exception
    {
        // Since 4.0 this query become illegal
        // This test only performs search and expects to have all the "problematic" ones found too, to prove
        // they are indexed
        // Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "*", SearchType.SCORED );
        // So, we found the "common denominator" and thats version
        Query q = nexusIndexer.constructQuery( MAVEN.VERSION, "1", SearchType.SCORED );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 4, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        ArtifactInfo ai = null;

        // g a v p c #1
        ai = list.get( 0 );

        assertEquals( "ant", ai.groupId );
        assertEquals( "ant", ai.artifactId );
        assertEquals( "1.6.5", ai.version );
        assertEquals( "jar", ai.packaging );
        assertEquals( null, ai.classifier );
        assertEquals( "nexus-1179", ai.repository );
        assertEquals( "jar", ai.fextension );

        // g a v p c #2
        ai = list.get( 1 );

        assertEquals( "ant", ai.groupId );
        assertEquals( "ant", ai.artifactId );
        assertEquals( "1.5.1", ai.version );
        assertEquals( null, ai.packaging );
        assertEquals( null, ai.classifier );
        assertEquals( "nexus-1179", ai.repository );
        assertEquals( "pom", ai.fextension );

        // g a v p c #3
        ai = list.get( 2 );

        assertEquals( "asm", ai.groupId );
        assertEquals( "asm-commons", ai.artifactId );
        assertEquals( "3.1", ai.version );
        assertEquals( "jar", ai.packaging );
        assertEquals( null, ai.classifier );
        assertEquals( "nexus-1179", ai.repository );
        assertEquals( "pom", ai.fextension );

        // g a v p c #4
        ai = list.get( 3 );

        assertEquals( "org", ai.groupId );
        assertEquals( "test", ai.artifactId );
        assertEquals( "1.0", ai.version );
        assertEquals( null, ai.packaging );
        assertEquals( null, ai.classifier );
        assertEquals( "nexus-1179", ai.repository );
        assertEquals( "pom", ai.fextension );
    }
}
