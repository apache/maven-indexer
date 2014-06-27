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

import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.NexusIndexer;

/**
 * @author Jason van Zyl
 * @author Eugene Kuleshov
 */
public class MinimalIndexNexusIndexerTest
    extends AbstractRepoNexusIndexerTest
{
    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "test-minimal", "test", repo, indexDir, null, null, MIN_CREATORS );

        nexusIndexer.scan( context );
    }

    public void testNEXUS2712()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.GROUP_ID, "com.adobe.flexunit", SearchType.EXACT );// WAS SCORED

        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );

        Set<ArtifactInfo> r = response.getResults();

        assertEquals( 1, r.size() );

        ArtifactInfo ai = r.iterator().next();

        assertEquals( "com.adobe.flexunit", ai.getGroupId() );
        assertEquals( "flexunit", ai.getArtifactId() );
        assertEquals( "0.90", ai.getVersion() );
        assertEquals( null, ai.getClassifier() );
        assertEquals( "swc", ai.getPackaging() );

        assertEquals( "swc", ai.getFileExtension() );
    }
}
