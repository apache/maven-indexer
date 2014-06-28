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

public class Nexus2046NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-2046" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-2046", "nexus-2046", repo, indexDir, null, null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchFlat()
        throws Exception
    {
        // Since 4.0 the original query become illegal
        // Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "*", SearchType.SCORED );
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "org.maven.ide", SearchType.SCORED );
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        Collection<ArtifactInfo> r = response.getResults();

        assertEquals( 1, r.size() );

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        ArtifactInfo ai = null;

        // g a v p c #1
        ai = list.get( 0 );

        assertEquals( "org.maven.ide.eclipse", ai.getGroupId() );
        assertEquals( "org.maven.ide.eclipse.feature", ai.getArtifactId() );
        assertEquals( "0.9.7", ai.getVersion() );
        assertEquals( "eclipse-feature", ai.getPackaging() );
        assertEquals( null, ai.getClassifier() );
        assertEquals( "nexus-2046", ai.getRepository() );
        assertEquals( "jar", ai.getFileExtension() );
    }
}
