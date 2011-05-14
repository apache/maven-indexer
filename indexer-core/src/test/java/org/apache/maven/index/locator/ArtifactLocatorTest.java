package org.apache.maven.index.locator;

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

import org.apache.maven.index.AbstractNexusIndexerTest;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.artifact.ArtifactPackagingMapper;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.artifact.M2GavCalculator;

public class ArtifactLocatorTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    private ArtifactContextProducer artifactContextProducer;

    private ArtifactPackagingMapper artifactPackagingMapper;

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "al-test", "al-test", repo, indexDir, null, null, FULL_CREATORS );

        nexusIndexer.scan( context );

        artifactContextProducer = lookup( ArtifactContextProducer.class );

        artifactPackagingMapper = lookup( ArtifactPackagingMapper.class );
    }

    public void testContextProducer()
    {
        final File pomFile =
            getTestFile( "src/test/repo/ch/marcus-schulte/maven/hivedoc-plugin/1.0.0/hivedoc-plugin-1.0.0.pom" );

        final ArtifactContext ac = artifactContextProducer.getArtifactContext( context, pomFile );

        assertTrue( "Artifact file was not found!", ac.getArtifact() != null );
        assertTrue( "Artifact file was not found!", ac.getArtifact().exists() );
    }

    public void testArtifactLocator()
    {
        ArtifactLocator al = new ArtifactLocator( artifactPackagingMapper );

        final M2GavCalculator gavCalculator = new M2GavCalculator();

        final File pomFile =
            getTestFile( "src/test/repo/ch/marcus-schulte/maven/hivedoc-plugin/1.0.0/hivedoc-plugin-1.0.0.pom" );

        final Gav gav =
            gavCalculator.pathToGav( "/ch/marcus-schulte/maven/hivedoc-plugin/1.0.0/hivedoc-plugin-1.0.0.pom" );

        File artifactFile = al.locate( pomFile, gavCalculator, gav );

        assertTrue( "Artifact file was not located!", artifactFile != null );
        assertTrue( "Artifact file was not located!", artifactFile.exists() );
    }
}
