package org.apache.maven.index.creator;

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
import java.util.List;

import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.AbstractTestSupport;

/**
 * @author juven
 */
public class MavenPluginArtifactInfoIndexCreatorTest
    extends AbstractTestSupport
{
    protected IndexCreator indexCreator;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        indexCreator = this.lookup( IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID );
    }

    public void testMavenPluginInfo()
        throws Exception
    {
        File artifact =
            new File( getBasedir(),
                "src/test/repo-creator/org/apache/maven/plugins/maven-dependency-plugin/2.0/maven-dependency-plugin-2.0.jar" );

        File pom =
            new File( getBasedir(),
                "src/test/repo-creator/org/apache/maven/plugins/maven-dependency-plugin/2.0/maven-dependency-plugin-2.0.pom" );

        ArtifactInfo artifactInfo =
            new ArtifactInfo( "test", "org.apache.maven.plugins", "maven-dependency-plugin", "2.0", null, "jar" );

        artifactInfo.setPackaging( "maven-plugin" );
        artifactInfo.setFileExtension( "jar" );

        ArtifactContext artifactContext = new ArtifactContext( pom, artifact, null, artifactInfo, null );

        indexCreator.populateArtifactInfo( artifactContext );

        assertEquals( "dependency", artifactContext.getArtifactInfo().getPrefix() );

        List<String> goals = new ArrayList<String>( 16 );
        goals.add( "analyze-dep-mgt" );
        goals.add( "analyze" );
        goals.add( "analyze-only" );
        goals.add( "analyze-report" );
        goals.add( "build-classpath" );
        goals.add( "copy-dependencies" );
        goals.add( "copy" );
        goals.add( "unpack" );
        goals.add( "list" );
        goals.add( "purge-local-repository" );
        goals.add( "go-offline" );
        goals.add( "resolve" );
        goals.add( "sources" );
        goals.add( "resolve-plugins" );
        goals.add( "tree" );
        goals.add( "unpack-dependencies" );

        assertEquals( goals, artifactContext.getArtifactInfo().getGoals() );

    }
}
