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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Query;

/** http://issues.sonatype.org/browse/NEXUS-13 */
public class Nexus658NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-658" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-658", "nexus-658", repo, indexDir, null, null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchFlat()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org.sonatype.nexus", SearchType.SCORED );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();
        assertEquals( r.toString(), 4, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        // g a v p c #1
        ArtifactInfo ai1 = list.get( 0 );
        assertEquals( "org.sonatype.nexus", ai1.getGroupId() );
        assertEquals( "nexus-webapp", ai1.getArtifactId() );
        assertEquals( "1.0.0-SNAPSHOT", ai1.getVersion() );
        assertEquals( "jar", ai1.getPackaging() );
        assertEquals( null, ai1.getClassifier() );
        assertEquals( ArtifactAvailability.PRESENT, ai1.getSourcesExists() );
        assertEquals( "nexus-658", ai1.getRepository() );

        // g a v p c #2
        ArtifactInfo ai2 = list.get( 1 );
        assertEquals( "org.sonatype.nexus", ai2.getGroupId() );
        assertEquals( "nexus-webapp", ai2.getArtifactId() );
        assertEquals( "1.0.0-SNAPSHOT", ai2.getVersion() );
        assertEquals( "tar.gz", ai2.getPackaging() );
        assertEquals( "bundle", ai2.getClassifier() );
        assertEquals( ArtifactAvailability.NOT_AVAILABLE, ai2.getSourcesExists() );
        assertEquals( "nexus-658", ai2.getRepository() );

        // g a v p c #3
        ArtifactInfo ai3 = list.get( 2 );
        assertEquals( "org.sonatype.nexus", ai3.getGroupId() );
        assertEquals( "nexus-webapp", ai3.getArtifactId() );
        assertEquals( "1.0.0-SNAPSHOT", ai3.getVersion() );
        assertEquals( "zip", ai3.getPackaging() );
        assertEquals( "bundle", ai3.getClassifier() );
        assertEquals( ArtifactAvailability.NOT_AVAILABLE, ai3.getSourcesExists() );
        assertEquals( "nexus-658", ai3.getRepository() );

        // g a v p c #3
        ArtifactInfo ai4 = list.get( 3 );
        assertEquals( "org.sonatype.nexus", ai4.getGroupId() );
        assertEquals( "nexus-webapp", ai4.getArtifactId() );
        assertEquals( "1.0.0-SNAPSHOT", ai4.getVersion() );
        assertEquals( "jar", ai4.getPackaging() );
        assertEquals( "sources", ai4.getClassifier() );
        assertEquals( ArtifactAvailability.NOT_AVAILABLE, ai4.getSourcesExists() );
        assertEquals( "nexus-658", ai4.getRepository() );
    }

}
