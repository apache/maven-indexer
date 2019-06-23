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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An artifact context used to provide information about artifact during scanning. It is passed to the
 * {@link IndexCreator}, which can populate {@link ArtifactInfo} for the given artifact.
 * 
 * @see IndexCreator#populateArtifactInfo(ArtifactContext)
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
public class ArtifactContext
{

    private static final Logger LOGGER = LoggerFactory.getLogger( ArtifactContext.class );

    private final File pom;

    private final File artifact;

    private final File metadata;

    private final ArtifactInfo artifactInfo;

    private final Gav gav;

    private final List<Exception> errors = new ArrayList<>();

    public ArtifactContext( File pom, File artifact, File metadata, ArtifactInfo artifactInfo, Gav gav )
        throws IllegalArgumentException
    {
        if ( artifactInfo == null )
        {
            throw new IllegalArgumentException( "Parameter artifactInfo must not be null." );
        }

        this.pom = pom;
        this.artifact = artifact;
        this.metadata = metadata;
        this.artifactInfo = artifactInfo;
        this.gav = gav == null ? artifactInfo.calculateGav() : gav;
    }

    public File getPom()
    {
        return pom;
    }

    public Model getPomModel()
    {
        // First check for local pom file
        File pom = getPom();
        if ( pom != null && pom.isFile() )
        {
            try ( InputStream inputStream = Files.newInputStream( pom.toPath() ) )
            {
                return new MavenXpp3Reader().read( inputStream, false );
            }
            catch ( IOException | XmlPullParserException e )
            {
                LOGGER.warn( "skip error reading pom: " + pom, e );
            }
        }
        // Otherwise, check for pom contained in maven generated artifact
        else if ( getArtifact() != null && getArtifact().isFile() )
        {
            try ( ZipFile zipFile = new ZipFile( artifact ) )
            {
                final String embeddedPomPath =
                    "META-INF/maven/" + getGav().getGroupId() + "/" + getGav().getArtifactId() + "/pom.xml";

                ZipEntry zipEntry = zipFile.getEntry( embeddedPomPath );

                if ( zipEntry != null )
                {
                    try ( InputStream inputStream = zipFile.getInputStream( zipEntry ) )
                    {
                        return new MavenXpp3Reader().read( inputStream, false );
                    }
                }
            }
            catch ( IOException | XmlPullParserException e )
            {
                LOGGER.warn( "skip error reading pom withing artifact:" + artifact, e );
            }
        }

        return null;
    }

    public File getArtifact()
    {
        return artifact;
    }

    public File getMetadata()
    {
        return metadata;
    }

    public ArtifactInfo getArtifactInfo()
    {
        return artifactInfo;
    }

    public Gav getGav()
    {
        return gav;
    }

    public List<Exception> getErrors()
    {
        return errors;
    }

    public void addError( Exception e )
    {
        errors.add( e );
    }

    /**
     * Creates Lucene Document using {@link IndexCreator}s from the given {@link IndexingContext}.
     */
    public Document createDocument( IndexingContext context )
    {
        Document doc = new Document();

        // unique key
        doc.add( new Field( ArtifactInfo.UINFO, getArtifactInfo().getUinfo(), IndexerField.KEYWORD_STORED ) );

        doc.add( new StoredField( ArtifactInfo.LAST_MODIFIED, //
            Long.toString( System.currentTimeMillis() ) ) );

        for ( IndexCreator indexCreator : context.getIndexCreators() )
        {
            try
            {
                indexCreator.populateArtifactInfo( this );
            }
            catch ( IOException ex )
            {
                addError( ex );
            }
        }

        // need a second pass in case index creators updated document attributes
        for ( IndexCreator indexCreator : context.getIndexCreators() )
        {
            indexCreator.updateDocument( getArtifactInfo(), doc );
        }

        return doc;
    }
}
