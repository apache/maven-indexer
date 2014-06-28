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
import org.apache.maven.index.expr.UserInputSearchExpression;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Mindexer35NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/mindexer-35" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "mindexer-35", "mindexer-35", repo, indexDir, null, null, FULL_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testSearchWar()
        throws Exception
    {
        Query q = nexusIndexer.constructQuery( MAVEN.CLASSNAMES, new UserInputSearchExpression( "WebappClass" ) );
        
        FlatSearchResponse response = nexusIndexer.searchFlat( new FlatSearchRequest( q ) );
        
        Collection<ArtifactInfo> r = response.getResults();

        assertThat(r.size(), is(1));

        List<ArtifactInfo> list = new ArrayList<ArtifactInfo>( r );

        ArtifactInfo ai = null;

        // g a v p c #1
        ai = list.get( 0 );

        assertEquals( "org.apache.maven.indexer.test", ai.getGroupId() );
        assertEquals( "sample-war", ai.getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", ai.getVersion() );
        assertEquals( "war", ai.getPackaging() );
        assertEquals( null, ai.getClassifier() );
        assertEquals( "mindexer-35", ai.getRepository() );
        assertEquals( "war", ai.getFileExtension() );
    }
}
