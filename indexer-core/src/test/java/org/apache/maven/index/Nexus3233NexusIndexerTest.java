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

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.NexusIndexer;

/** http://issues.sonatype.org/browse/NEXUS-3233 */
public class Nexus3233NexusIndexerTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/nexus-3233" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context =
            nexusIndexer.addIndexingContext( "nexus-3233", "nexus-3233", repo, indexDir, null, null, FULL_CREATORS );
        nexusIndexer.scan( context );
    }

    public void testIdentifyPomPackagingArtifacts()
        throws Exception
    {
        // POM1
        ArtifactInfo ai = nexusIndexer.identify( ArtifactInfo.SHA1, "741ea3998e6db3ce202d8b88aa53889543f050cc" );

        assertNotNull( ai );

        assertEquals( "cisco.infra.dft", ai.groupId );

        assertEquals( "dma.maven.plugins", ai.artifactId );

        assertEquals( "1.0-SNAPSHOT", ai.version );

        // POM2
        ai = nexusIndexer.identify( ArtifactInfo.SHA1, "efb52d4ef65452b4e575fc2e7709595915775857" );

        assertNotNull( ai );

        assertEquals( "cisco.infra.dft", ai.groupId );

        assertEquals( "parent.pom", ai.artifactId );

        assertEquals( "1.0-SNAPSHOT", ai.version );
    }
}
