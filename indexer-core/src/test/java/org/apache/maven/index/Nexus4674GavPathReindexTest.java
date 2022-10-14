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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Nexus4674GavPathReindexTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "test-minimal", "test", repo, indexDir, null, null, MIN_CREATORS );

        nexusIndexer.scan( context, "/org/slf4j/slf4j-api", null, false );
        nexusIndexer.scan( context, "/org/slf4j/slf4j-api/1.4.1", null, true );
    }

    @Test
    public void testRootGroups()
        throws Exception
    {
        Set<String> rootGroups = context.getRootGroups();
        assertEquals( rootGroups.toString(), 1, rootGroups.size() );

        assertGroup( 4, "org", context );

        assertGroup( 4, "org.slf4j", context );
    }

    @Test
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