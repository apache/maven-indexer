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

/**
 * Utility methods for basic "detection" of artifact kind in M1 repository.
 */
public class M1ArtifactRecognizer
{
    /**
     * Is this item M1 Checksum?
     */
    public static boolean isChecksum( String path )
    {
        return path.endsWith( ".sha1" ) || path.endsWith( ".md5" );
    }

    /**
     * Is this item M1 POM?
     */
    public static boolean isPom( String path )
    {
        return path.endsWith( ".pom" ) || path.endsWith( ".pom.sha1" ) || path.endsWith( ".pom.md5" );
    }

    /**
     * Is this item M1 snapshot?
     */
    public static boolean isSnapshot( String path )
    {
        return path.indexOf( "SNAPSHOT" ) != -1;
    }

    /**
     * Is this item M1 metadata? There is no such!
     */
    public static boolean isMetadata( String path )
    {
        return path.endsWith( "maven-metadata.xml" ) || path.endsWith( "maven-metadata.xml.sha1" )
            || path.endsWith( "maven-metadata.xml.md5" );
    }

}
