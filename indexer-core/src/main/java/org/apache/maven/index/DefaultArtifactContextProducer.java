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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

import org.apache.maven.index.artifact.ArtifactPackagingMapper;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.locator.ArtifactLocator;
import org.apache.maven.index.locator.GavHelpedLocator;
import org.apache.maven.index.locator.Locator;
import org.apache.maven.index.locator.MetadataLocator;
import org.apache.maven.index.locator.PomLocator;
import org.codehaus.plexus.util.StringUtils;

/**
 * A default implementation of the {@link ArtifactContextProducer}.
 * 
 * @author Tamas Cservenak
 * @author Eugene Kuleshov
 */
@Singleton
@Named
public class DefaultArtifactContextProducer
    implements ArtifactContextProducer
{

    private final ArtifactPackagingMapper mapper;

    private GavHelpedLocator pl = new PomLocator();

    private Locator ml = new MetadataLocator();


    @Inject
    public DefaultArtifactContextProducer( ArtifactPackagingMapper mapper )
    {
        this.mapper = mapper;
    }

    /**
     * Get ArtifactContext for given pom or artifact (jar, war, etc). A file can be
     */
    public ArtifactContext getArtifactContext( IndexingContext context, File file )
    {
        // TODO shouldn't this use repository layout instead?

        String repositoryPath = context.getRepository().getAbsolutePath();
        String artifactPath = file.getAbsolutePath();

        // protection from IndexOutOfBounds
        if ( artifactPath.length() <= repositoryPath.length() )
        {
            return null; // not an artifact
        }

        if ( !isIndexable( file ) )
        {
            return null; // skipped
        }

        Gav gav = getGavFromPath( context, repositoryPath, artifactPath );

        if ( gav == null )
        {
            return null; // not an artifact, but rather metadata
        }

        File pom;
        File artifact;

        if ( file.getName().endsWith( ".pom" ) )
        {
            ArtifactLocator al = new ArtifactLocator( mapper );
            artifact = al.locate( file, context.getGavCalculator(), gav );

            // If we found the matching artifact, switch over to indexing that, instead of the pom
            if ( artifact != null )
            {
                gav = getGavFromPath( context, repositoryPath, artifact.getAbsolutePath() );
            }

            pom = file;
        }
        else
        {
            artifact = file;
            pom = pl.locate( file, context.getGavCalculator(), gav );
        }

        String groupId = gav.getGroupId();

        String artifactId = gav.getArtifactId();

        String version = gav.getBaseVersion();

        String classifier = gav.getClassifier();

        ArtifactInfo ai = new ArtifactInfo( context.getRepositoryId(), groupId, artifactId, version, classifier, gav.getExtension() );

        // store extension if classifier is not empty
        if ( !StringUtils.isEmpty( ai.getClassifier() ) )
        {
            ai.setPackaging( gav.getExtension() );
        }

        ai.setFileName( file.getName() );
        ai.setFileExtension( gav.getExtension() );

        File metadata = ml.locate( pom );

        return new ArtifactContext( pom, artifact, metadata, ai, gav );
    }

    private boolean isIndexable( File file )
    {
        if ( file == null )
        {
            return false;
        }

        String filename = file.getName();

        if ( filename.equals( "maven-metadata.xml" )
        // || filename.endsWith( "-javadoc.jar" )
        // || filename.endsWith( "-javadocs.jar" )
        // || filename.endsWith( "-sources.jar" )
            || filename.endsWith( ".properties" )
            // || filename.endsWith( ".xml" ) // NEXUS-3029
            || filename.endsWith( ".asc" ) || filename.endsWith( ".md5" ) || filename.endsWith( ".sha1" ) )
        {
            return false;
        }

        return true;
    }

    private Gav getGavFromPath( IndexingContext context, String repositoryPath, String artifactPath )
    {
        String path = artifactPath.substring( repositoryPath.length() + 1 ).replace( '\\', '/' );

        return context.getGavCalculator().pathToGav( path );
    }

}
