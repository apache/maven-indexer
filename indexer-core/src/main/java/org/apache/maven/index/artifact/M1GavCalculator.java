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

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An M1 <code>GavCalculator</code>. Heavily under-maintained.
 * 
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
@Singleton
@Named ("maven1")
public class M1GavCalculator
    implements GavCalculator
{

    private static final Pattern pat1 = Pattern.compile( "^([^0-9]+)-([0-9].+)\\.([^0-9]+)(\\.md5|\\.sha1){0,1}$" );

    private static final Pattern pat2 = Pattern.compile( "^([a-z0-9-_]+)-([0-9-].+)\\.([^0-9]+)(\\.md5|\\.sha1){0,1}$" );

    public Gav pathToGav( String str )
    {
        try
        {
            String s = str.startsWith( "/" ) ? str.substring( 1 ) : str;

            int n1 = s.lastIndexOf( '/' );

            if ( n1 == -1 )
            {
                return null;
            }

            int n2 = s.lastIndexOf( '/', n1 - 1 );

            if ( n2 == -1 )
            {
                return null;
            }

            String g = s.substring( 0, n2 ).replace( '/', '.' );
            String middle = s.substring( n2 + 1, n1 );
            String n = s.substring( n1 + 1 );

            String classifier = null;
            if ( "java-sources".equals( middle ) )
            {
                classifier = "sources";
            }
            else if ( "javadocs".equals( middle ) )
            {
                classifier = "javadoc";
            }
            else if ( "ejbs".equals( middle )
                && ( n.endsWith( "client.jar" ) || n.endsWith( "client.jar.sha1" ) || n.endsWith( "client.jar.md5" ) ) )
            {
                classifier = "client";
            }

            boolean checksum = false;
            Gav.HashType checksumType = null;
            if ( s.endsWith( ".md5" ) )
            {
                checksum = true;
                checksumType = Gav.HashType.md5;
                s = s.substring( 0, s.length() - 4 );
            }
            else if ( s.endsWith( ".sha1" ) )
            {
                checksum = true;
                checksumType = Gav.HashType.sha1;
                s = s.substring( 0, s.length() - 5 );
            }

            if ( s.endsWith( "maven-metadata.xml" ) )
            {
                return null;
            }

            String ext = s.substring( s.lastIndexOf( '.' ) + 1 );

            Matcher m = pat1.matcher( n );
            if ( m.matches() )
            {
                String a = m.group( 1 );
                String version = m.group( 2 );
                if ( classifier != null )
                {
                    version = version.substring( 0, version.length() - ( classifier.length() + 1 ) );
                }

                return new Gav( g, a, version, classifier, ext, null, null, n, checksum, checksumType, false,
                    null );
            }
            else
            {
                m = pat2.matcher( n );
                if ( m.matches() )
                {
                    String a = m.group( 1 );
                    String version = m.group( 2 );
                    if ( classifier != null )
                    {
                        version = version.substring( 0, version.length() - ( classifier.length() + 1 ) );
                    }

                    return new Gav( g, a, version, classifier, ext, null, null, n, checksum, checksumType,
                        false, null );
                }
                else
                {
                    return null;
                }
            }
        }
        catch ( StringIndexOutOfBoundsException e )
        {
            return null;
        }
        catch ( IndexOutOfBoundsException e )
        {
            return null;
        }
    }

    /**
     * // XXX this is not accurate, m1 is using packaging as an artifact folder name.
     * 
     * @see org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout#pathOf(org.apache.maven.artifact.Artifact)
     * @see org.apache.maven.artifact.handler.DefaultArtifactHandler#getDirectory()
     */
    public String gavToPath( Gav gav )
    {
        StringBuilder path = new StringBuilder( "/" );

        path.append( gav.getGroupId() );

        path.append( "/" );

        if ( gav.getClassifier() == null )
        {
            path.append( gav.getExtension() );

            path.append( "s" );
        }
        else
        {
            if ( gav.getClassifier().startsWith( "source" ) )
            {
                path.append( "java-source" );
            }
            else if ( gav.getClassifier().startsWith( "client" ) )
            {
                path.append( "ejb" );
            }
            else
            {
                path.append( gav.getClassifier() );
            }
            path.append( "s" );
        }

        path.append( "/" );

        path.append( gav.getArtifactId() );

        path.append( "-" );

        path.append( gav.getVersion() );

        if ( gav.getClassifier() != null )
        {
            path.append( "-" );

            path.append( gav.getClassifier() );
        }

        path.append( "." );

        path.append( gav.getExtension() );

        if ( gav.isHash() )
        {
            path.append( "." );

            path.append( gav.getHashType().toString() );
        }

        return path.toString();
    }

}
