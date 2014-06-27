package org.apache.maven.index.artifact;

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

import org.apache.maven.index.artifact.M2ArtifactRecognizer;

import junit.framework.TestCase;

public class MavenArtifactRecognizerTest
    extends TestCase
{

    public void testIsPom()
    {
        assertEquals( true, M2ArtifactRecognizer.isPom( "aaa.pom" ) );
        assertEquals( true, M2ArtifactRecognizer.isPom( "zxc-1-2-3.pom" ) );
        assertEquals( false, M2ArtifactRecognizer.isPom( "aaa.jar" ) );
        assertEquals( false, M2ArtifactRecognizer.isPom( "aaa.pom-a" ) );
    }

    public void testIsSnapshot1()
    {
        // NEXUS-3148
        assertEquals( true, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0SNAPSHOT/aid-1.0SNAPSHOT.jar" ) );

        assertEquals( true, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.jar" ) );
        assertEquals( true, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.pom" ) );
        assertEquals( true, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0-SNAPSHOT/aid-1.2.3-.pom" ) );
        assertEquals( false, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0/xsd-SNAPsHOT.jar" ) );
        assertEquals( false, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0/xsd-SNAPHOT.pom" ) );
        assertEquals( false, M2ArtifactRecognizer.isSnapshot( "/org/somewhere/aid/1.0/a/b/c/xsd-1.2.3NAPSHOT.pom" ) );
        assertEquals( false, M2ArtifactRecognizer.isSnapshot( "/javax/mail/mail/1.4/mail-1.4.jar" ) );
    }

    public void testIsSnapshot2()
    {
        assertEquals(
            true,
            M2ArtifactRecognizer.isSnapshot( "/org/somewhere/appassembler-maven-plugin/1.0-SNAPSHOT/appassembler-maven-plugin-1.0-20060714.142547-1.pom" ) );
        assertEquals(
            false,
            M2ArtifactRecognizer.isSnapshot( "/org/somewhere/appassembler-maven-plugin/1.0/appassembler-maven-plugin-1.0-20060714.142547-1.pom" ) );
    }

    public void testIsMetadata()
    {
        assertEquals( true, M2ArtifactRecognizer.isMetadata( "maven-metadata.xml" ) );
        assertEquals( false, M2ArtifactRecognizer.isMetadata( "aven-metadata.xml" ) );
        assertEquals( false, M2ArtifactRecognizer.isMetadata( "/javax/mail/mail/1.4/mail-1.4.jar" ) );
    }

}
