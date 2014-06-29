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
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.index.artifact.ArtifactPackagingMapper;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Artifact locator.
 * 
 * @author Damian Bradicich
 */
public class ArtifactLocator
    implements GavHelpedLocator
{
    private final ArtifactPackagingMapper mapper;

    public ArtifactLocator( ArtifactPackagingMapper mapper )
    {
        this.mapper = mapper;
    }

    public File locate( File source, GavCalculator gavCalculator, Gav gav )
    {
        // if we don't have this data, nothing we can do
        if ( source == null || !source.exists() || gav == null || gav.getArtifactId() == null
            || gav.getVersion() == null )
        {
            return null;
        }

        try
        {
            // need to read the pom model to get packaging
            final Model model = new MavenXpp3Reader().read( new FileInputStream( source ), false );

            if ( model == null )
            {
                return null;
            }

            // now generate the artifactname
            String artifactName =
                gav.getArtifactId() + "-" + gav.getVersion() + "."
                    + mapper.getExtensionForPackaging( model.getPackaging() );

            File artifact = new File( source.getParent(), artifactName );

            if ( !artifact.exists() )
            {
                return null;
            }

            return artifact;
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            return null;
        }
        catch ( XmlPullParserException e )
        {
            e.printStackTrace();
            return null;
        }
    }
}
