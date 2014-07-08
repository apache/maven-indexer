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

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.util.zip.ZipFacade;
import org.apache.maven.index.util.zip.ZipHandle;

/**
 * A Maven Archetype index creator used to detect and correct the artifact packaging to "maven-archetype" if the
 * inspected JAR is an Archetype. Since packaging is already handled by Minimal creator, this Creator only alters the
 * supplied ArtifactInfo packaging field during processing, but does not interferes with Lucene document fill-up or the
 * ArtifactInfo fill-up (the update* methods are empty).
 * 
 * @author cstamas
 */
@Singleton
@Named (MavenArchetypeArtifactInfoIndexCreator.ID)
public class MavenArchetypeArtifactInfoIndexCreator
    extends AbstractIndexCreator
{
    public static final String ID = "maven-archetype";

    private static final String MAVEN_ARCHETYPE_PACKAGING = "maven-archetype";

    private static final String[] ARCHETYPE_XML_LOCATIONS = { "META-INF/maven/archetype.xml", "META-INF/archetype.xml",
        "META-INF/maven/archetype-metadata.xml" };

    public MavenArchetypeArtifactInfoIndexCreator()
    {
        super( ID, Arrays.asList( MinimalArtifactInfoIndexCreator.ID ) );
    }

    public void populateArtifactInfo( ArtifactContext ac )
    {
        File artifact = ac.getArtifact();

        ArtifactInfo ai = ac.getArtifactInfo();

        // we need the file to perform these checks, and those may be only JARs
        if ( artifact != null && artifact.isFile() && !MAVEN_ARCHETYPE_PACKAGING.equals( ai.getPackaging() )
            && artifact.getName().endsWith( ".jar" ) )
        {
            // TODO: recheck, is the following true? "Maven plugins and Maven Archetypes can be only JARs?"

            // check for maven archetype, since Archetypes seems to not have consistent packaging,
            // and depending on the contents of the JAR, this call will override the packaging to "maven-archetype"!
            checkMavenArchetype( ai, artifact );
        }
    }

    /**
     * Archetypes that are added will have their packaging types set correctly (to maven-archetype)
     * 
     * @param ai
     * @param artifact
     */
    private void checkMavenArchetype( ArtifactInfo ai, File artifact )
    {
        ZipHandle handle = null;

        try
        {
            handle = ZipFacade.getZipHandle( artifact );

            for ( String path : ARCHETYPE_XML_LOCATIONS )
            {
                if ( handle.hasEntry( path ) )
                {
                    ai.setPackaging( MAVEN_ARCHETYPE_PACKAGING );

                    return;
                }
            }
        }
        catch ( Exception e )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().info(
                    "Failed to parse Maven artifact " + artifact.getAbsolutePath() + " due to exception:", e );
            }
            else
            {
                getLogger().info(
                    "Failed to parse Maven artifact " + artifact.getAbsolutePath() + " due to " + e.getMessage() );
            }
        }
        finally
        {
            try
            {
                ZipFacade.close( handle );
            }
            catch ( IOException ex )
            {
            }
        }
    }

    public void updateDocument( ArtifactInfo ai, Document doc )
    {
        // nothing to update, minimal will maintain it.
    }

    public boolean updateArtifactInfo( Document doc, ArtifactInfo ai )
    {
        // nothing to update, minimal will maintain it.

        return false;
    }

    // ==

    @Override
    public String toString()
    {
        return ID;
    }

    public Collection<IndexerField> getIndexerFields()
    {
        // it does not "add" any new field, it actually updates those already maintained by minimal creator.
        return Collections.emptyList();
    }
}
