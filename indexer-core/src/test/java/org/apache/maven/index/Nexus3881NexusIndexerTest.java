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

import junit.framework.Assert;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

public class Nexus3881NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-3881" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-3881", "nexus-3881", repo, indexDir, null, null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testRelevances()
        throws Exception
    {
        IteratorSearchRequest request = new IteratorSearchRequest( new BooleanQuery.Builder()
            .add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, "solution", SearchType.SCORED ), Occur.SHOULD )
            .add( nexusIndexer.constructQuery( MAVEN.ARTIFACT_ID, "solution", SearchType.SCORED ), Occur.SHOULD )
            .build() );

        request.setLuceneExplain( true );
        
        IteratorSearchResponse response = nexusIndexer.searchIterator( request );

        Assert.assertEquals( "All artifacts has 'solution' in their GA!", 4, response.getTotalHits() );
        

        // for (ArtifactInfo ai : response) {
        // System.out.println(ai.toString());
        // System.out.println(ai.getAttributes().get( Explanation.class.getName() ));
        // System.out.println();
        // }

        float firstRel = response.getResults().next().getLuceneScore();

        float lastRel = 0;
        for ( ArtifactInfo ai : response )
        {
            lastRel = ai.getLuceneScore();
        }

        Assert.assertTrue(
            String.format( "The relevance span should be small! (%s)",
                new Object[] { Float.valueOf( firstRel - lastRel ) } ), firstRel - lastRel < 0.35 );
    }
}
