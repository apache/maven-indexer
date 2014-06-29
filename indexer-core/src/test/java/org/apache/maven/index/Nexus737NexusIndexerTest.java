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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;

/** http://issues.sonatype.org/browse/NEXUS-737 */
public class Nexus737NexusIndexerTest
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

    public void testValidateUINFOs()
        throws Exception
    {
        IndexReader reader = context.acquireIndexSearcher().getIndexReader();
        Bits liveDocs = MultiFields.getLiveDocs(reader);

        int foundCount = 0;

        for ( int i = 0; i < reader.maxDoc(); i++ )
        {
            if (liveDocs == null || liveDocs.get(i) )
            {
                Document document = reader.document( i );

                String uinfo = document.get( ArtifactInfo.UINFO );

                if ( "org.sonatype.nexus|nexus-webapp|1.0.0-SNAPSHOT|NA|jar".equals( uinfo )
                    || "org.sonatype.nexus|nexus-webapp|1.0.0-SNAPSHOT|bundle|zip".equals( uinfo )
                    || "org.sonatype.nexus|nexus-webapp|1.0.0-SNAPSHOT|bundle|tar.gz".equals( uinfo ) )
                {
                    foundCount++;
                }
            }
        }

        assertEquals( foundCount, 3 );
    }
}
