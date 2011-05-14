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
import java.util.Collection;
import java.util.Set;

public class SubPathReindexTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "test-minimal", "test", repo, indexDir, null, null, MIN_CREATORS );

        nexusIndexer.scan( context, "/org/slf4j/slf4j-api", null, false );
    }

    public void testRootGroups()
        throws Exception
    {
        Set<String> rootGroups = context.getRootGroups();
        assertEquals( rootGroups.toString(), 1, rootGroups.size() );

        assertGroup( 0, "com.adobe", context );
        assertGroup( 0, "com.adobe.flexunit", context );

        assertGroup( 0, "qdox", context );

        assertGroup( 0, "proptest", context );

        assertGroup( 0, "junit", context );

        assertGroup( 0, "commons-logging", context );

        assertGroup( 0, "regexp", context );

        assertGroup( 0, "commons-cli", context );

        assertGroup( 4, "org", context );

        assertGroup( 4, "org.slf4j", context );

        assertGroup( 0, "org.testng", context );

        assertGroup( 0, "org.apache", context );

        assertGroup( 0, "org.apache.directory", context );
        assertGroup( 0, "org.apache.directory.server", context );

        assertGroup( 0, "org.apache.maven", context );
        assertGroup( 0, "org.apache.maven.plugins", context );
        assertGroup( 0, "org.apache.maven.plugins.maven-core-it-plugin", context );
    }

    public void testIdentify()
        throws Exception
    {
        Collection<ArtifactInfo> ais;
        File artifact;

        // Using a file: this one should be unknown
        artifact = new File( repo, "qdox/qdox/1.5/qdox-1.5.jar" );

        ais = nexusIndexer.identify( artifact );

        assertTrue( "Should not be able to identify it!", ais.isEmpty() );

        // Using a file: this one should be known
        artifact = new File( repo, "org/slf4j/slf4j-api/1.4.2/slf4j-api-1.4.2.jar" );

        ais = nexusIndexer.identify( artifact );

        assertEquals( "Should not be able to identify it!", 1, ais.size() );
    }

}