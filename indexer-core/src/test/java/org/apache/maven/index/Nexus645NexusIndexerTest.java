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
package org.apache.maven.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Query;

/** http://issues.sonatype.org/browse/NEXUS-13 */
public class Nexus645NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-645" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-645", "nexus-645", repo, indexDir, null, null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchFlat()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org.codehaus.tycho", SearchType.SCORED );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 3, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        ArtifactInfo ai = null;

        // g a v p c #1
        ai = list.get( 0 );

        assertEquals( "org.codehaus.tycho", ai.groupId );
        assertEquals( "tycho-distribution", ai.artifactId );
        assertEquals( "0.3.0-SNAPSHOT", ai.version );
        assertEquals( "pom", ai.packaging );
        assertEquals( null, ai.classifier );
        assertEquals( "nexus-645", ai.repository );
        assertEquals( "pom", ai.fextension );

        // g a v p c #2
        ai = list.get( 1 );

        assertEquals( "org.codehaus.tycho", ai.groupId );
        assertEquals( "tycho-distribution", ai.artifactId );
        assertEquals( "0.3.0-SNAPSHOT", ai.version );
        assertEquals( "tar.gz", ai.packaging );
        assertEquals( "bin", ai.classifier );
        assertEquals( "nexus-645", ai.repository );
        assertEquals( "tar.gz", ai.fextension );

        // g a v p c #3
        ai = list.get( 2 );

        assertEquals( "org.codehaus.tycho", ai.groupId );
        assertEquals( "tycho-distribution", ai.artifactId );
        assertEquals( "0.3.0-SNAPSHOT", ai.version );
        assertEquals( "zip", ai.packaging );
        assertEquals( "bin", ai.classifier );
        assertEquals( "nexus-645", ai.repository );
        assertEquals( "zip", ai.fextension );
    }
}
