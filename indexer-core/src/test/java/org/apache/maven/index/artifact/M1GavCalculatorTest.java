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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class M1GavCalculatorTest
{
    private M1GavCalculator gavCalculator = new M1GavCalculator();

    private SimpleDateFormat formatter = new SimpleDateFormat( "yyyyMMdd.HHmmss" );


    protected Long parseTimestamp( String timeStamp )
        throws ParseException
    {
        if ( timeStamp == null )
        {
            return null;
        }
        else
        {
            return formatter.parse( timeStamp ).getTime();
        }
    }

    @Test
    public void testGav()
        throws Exception
    {
        Gav gav = gavCalculator.pathToGav( "/org.jruby/javadocs/jruby-1.0RC1-SNAPSHOT-javadoc.jar" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( "javadoc", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0RC1-SNAPSHOT-javadoc.jar", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        String path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/javadocs/jruby-1.0RC1-SNAPSHOT-javadoc.jar", path );

        gav = gavCalculator.pathToGav( "/org.jruby/jars/jruby-1.0RC1-SNAPSHOT.jar" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0RC1-SNAPSHOT.jar", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/jars/jruby-1.0RC1-SNAPSHOT.jar", path );

        gav = gavCalculator.pathToGav( "/org.jruby/jars/jruby-1.0RC1-SNAPSHOT.jar.md5" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.0RC1-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0RC1-SNAPSHOT.jar.md5", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( true, gav.isHash() );
        assertEquals( Gav.HashType.md5, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/jars/jruby-1.0RC1-SNAPSHOT.jar.md5", path );

        gav = gavCalculator.pathToGav( "/org.jruby/javadocs/jruby-1.0-javadoc.jar" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0", gav.getVersion() );
        assertEquals( "1.0", gav.getBaseVersion() );
        assertEquals( "javadoc", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0-javadoc.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/javadocs/jruby-1.0-javadoc.jar", path );

        gav = gavCalculator.pathToGav( "/org.jruby/javadocs/jruby-1.0-javadoc.jar.sha1" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0", gav.getVersion() );
        assertEquals( "1.0", gav.getBaseVersion() );
        assertEquals( "javadoc", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0-javadoc.jar.sha1", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( true, gav.isHash() );
        assertEquals( Gav.HashType.sha1, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/javadocs/jruby-1.0-javadoc.jar.sha1", path );

        gav = gavCalculator.pathToGav( "/org.jruby/jars/jruby-1.0.jar" );

        assertEquals( "org.jruby", gav.getGroupId() );
        assertEquals( "jruby", gav.getArtifactId() );
        assertEquals( "1.0", gav.getVersion() );
        assertEquals( "1.0", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "jruby-1.0.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.jruby/jars/jruby-1.0.jar", path );

        gav = gavCalculator.pathToGav( "/maven/jars/dom4j-1.7-20060614.jar" );

        assertEquals( "maven", gav.getGroupId() );
        assertEquals( "dom4j", gav.getArtifactId() );
        assertEquals( "1.7-20060614", gav.getVersion() );
        assertEquals( "1.7-20060614", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "dom4j-1.7-20060614.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/maven/jars/dom4j-1.7-20060614.jar", path );

        gav = gavCalculator.pathToGav( "maven/java-sources/velocity-1.5-SNAPSHOT-sources.jar" );

        assertEquals( "maven", gav.getGroupId() );
        assertEquals( "velocity", gav.getArtifactId() );
        assertEquals( "1.5-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.5-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( "sources", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "velocity-1.5-SNAPSHOT-sources.jar", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/maven/java-sources/velocity-1.5-SNAPSHOT-sources.jar", path );

        gav = gavCalculator.pathToGav( "castor/jars/castor-0.9.9-xml.jar" );

        assertEquals( "castor", gav.getGroupId() );
        assertEquals( "castor", gav.getArtifactId() );
        assertEquals( "0.9.9-xml", gav.getVersion() );
        assertEquals( "0.9.9-xml", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "castor-0.9.9-xml.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/castor/jars/castor-0.9.9-xml.jar", path );

        gav = gavCalculator.pathToGav( "/org.slf4j/poms/slf4j-log4j12-1.4.3.pom" );

        assertEquals( "org.slf4j", gav.getGroupId() );
        assertEquals( "slf4j-log4j12", gav.getArtifactId() );
        assertEquals( "1.4.3", gav.getVersion() );
        assertEquals( "1.4.3", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "pom", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "slf4j-log4j12-1.4.3.pom", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/org.slf4j/poms/slf4j-log4j12-1.4.3.pom", path );

        // TODO: fix this!
        /* There is an "Oh" letter at the end, not a zero! */
        gav = gavCalculator.pathToGav( "/xpp3/poms/xpp3_min-1.1.3.4.O.pom" );

        assertEquals( "xpp3", gav.getGroupId() );
        assertEquals( "xpp3_min", gav.getArtifactId() );
        assertEquals( "1.1.3.4.O", gav.getVersion() );
        assertEquals( "1.1.3.4.O", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "pom", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "xpp3_min-1.1.3.4.O.pom", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/xpp3/poms/xpp3_min-1.1.3.4.O.pom", path );
    }

    @Test
    public void testNEXUS1336()
        throws Exception
    {
        Gav gav = gavCalculator.pathToGav( "/castor/ejbs/castor-ejb-1.0.7-SNAPSHOT-client.jar" );

        assertEquals( "castor", gav.getGroupId() );
        assertEquals( "castor-ejb", gav.getArtifactId() );
        assertEquals( "1.0.7-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.0.7-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( "client", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "castor-ejb-1.0.7-SNAPSHOT-client.jar", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        String path = gavCalculator.gavToPath( gav );
        assertEquals( "/castor/ejbs/castor-ejb-1.0.7-SNAPSHOT-client.jar", path );

        gav = gavCalculator.pathToGav( "/castor/ejbs/castor-ejb-1.0.7.jar" );

        assertEquals( "castor", gav.getGroupId() );
        assertEquals( "castor-ejb", gav.getArtifactId() );
        assertEquals( "1.0.7", gav.getVersion() );
        assertEquals( "1.0.7", gav.getBaseVersion() );
        assertEquals( null, gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "castor-ejb-1.0.7.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/castor/jars/castor-ejb-1.0.7.jar", path );

        gav = gavCalculator.pathToGav( "/castor/ejbs/castor-ejb-1.0.7-SNAPSHOT-client.jar.sha1" );

        assertEquals( "castor", gav.getGroupId() );
        assertEquals( "castor-ejb", gav.getArtifactId() );
        assertEquals( "1.0.7-SNAPSHOT", gav.getVersion() );
        assertEquals( "1.0.7-SNAPSHOT", gav.getBaseVersion() );
        assertEquals( "client", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "castor-ejb-1.0.7-SNAPSHOT-client.jar.sha1", gav.getName() );
        assertEquals( true, gav.isSnapshot() );
        assertEquals( true, gav.isHash() );
        assertEquals( Gav.HashType.sha1, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/castor/ejbs/castor-ejb-1.0.7-SNAPSHOT-client.jar.sha1", path );

        gav = gavCalculator.pathToGav( "/castor/ejbs/castor-ejb-1.0.7-client.jar" );

        assertEquals( "castor", gav.getGroupId() );
        assertEquals( "castor-ejb", gav.getArtifactId() );
        assertEquals( "1.0.7", gav.getVersion() );
        assertEquals( "1.0.7", gav.getBaseVersion() );
        assertEquals( "client", gav.getClassifier() );
        assertEquals( "jar", gav.getExtension() );
        assertEquals( null, gav.getSnapshotBuildNumber() );
        assertEquals( null, gav.getSnapshotTimeStamp() );
        assertEquals( "castor-ejb-1.0.7-client.jar", gav.getName() );
        assertEquals( false, gav.isSnapshot() );
        assertEquals( false, gav.isHash() );
        assertEquals( null, gav.getHashType() );

        path = gavCalculator.gavToPath( gav );
        assertEquals( "/castor/ejbs/castor-ejb-1.0.7-client.jar", path );
    }

    @Test
    public void testGavExtreme()
        throws Exception
    {
        Gav gav = gavCalculator.pathToGav( "/" );
        assertEquals( null, gav );

        gav = gavCalculator.pathToGav( "/some/stupid/path" );
        assertEquals( null, gav );

        gav = gavCalculator.pathToGav( "/some/stupid/path/more/in/it" );
        assertEquals( null, gav );

        gav = gavCalculator.pathToGav( "/something/that/looks/" );
        assertEquals( null, gav );

        gav = gavCalculator.pathToGav( "/something/that/like-an-artifact.blah" );
        assertEquals( null, gav );
        // assertEquals( false, gav.isChecksum() );
        // assertEquals( false, gav.isPrimary() );
        // assertEquals( false, gav.isSnapshot() );

        gav = gavCalculator.pathToGav( "/something/that/like-an-artifact.pom" );
        assertEquals( null, gav );
        // assertEquals( false, gav.isChecksum() );
        // assertEquals( false, gav.isPrimary() );
        // assertEquals( false, gav.isSnapshot() );

        gav = gavCalculator.pathToGav( "/something/that/maven-metadata.xml" );
        assertEquals( null, gav );
        // assertEquals( false, gav.isChecksum() );
        // assertEquals( false, gav.isPrimary() );
        // assertEquals( false, gav.isSnapshot() );

        gav = gavCalculator.pathToGav( "/something/that/like-SNAPSHOT/maven-metadata.xml" );
        assertEquals( null, gav );

    }
}
