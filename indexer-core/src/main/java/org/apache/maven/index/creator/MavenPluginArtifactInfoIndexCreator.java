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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.util.zip.ZipFacade;
import org.apache.maven.index.util.zip.ZipHandle;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

/**
 * A Maven Plugin index creator used to provide information about Maven Plugins. It will collect the plugin prefix and
 * the goals the plugin provides. Also, the Lucene document and the returned ArtifactInfo will be correctly filled with
 * these information.
 * 
 * @author cstamas
 */
@Component( role = IndexCreator.class, hint = MavenPluginArtifactInfoIndexCreator.ID )
public class MavenPluginArtifactInfoIndexCreator
    extends AbstractIndexCreator
{
    public static final String ID = "maven-plugin";

    private static final String MAVEN_PLUGIN_PACKAGING = "maven-plugin";

    public static final IndexerField FLD_PLUGIN_PREFIX = new IndexerField( MAVEN.PLUGIN_PREFIX, IndexerFieldVersion.V1,
        "px", "MavenPlugin prefix (as keyword, stored)", Store.YES, Index.NOT_ANALYZED );

    public static final IndexerField FLD_PLUGIN_GOALS = new IndexerField( MAVEN.PLUGIN_GOALS, IndexerFieldVersion.V1,
        "gx", "MavenPlugin goals (as keyword, stored)", Store.YES, Index.ANALYZED );

    public MavenPluginArtifactInfoIndexCreator()
    {
        super( ID, Arrays.asList( MinimalArtifactInfoIndexCreator.ID ) );
    }

    public void populateArtifactInfo( ArtifactContext ac )
    {
        File artifact = ac.getArtifact();

        ArtifactInfo ai = ac.getArtifactInfo();

        // we need the file to perform these checks, and those may be only JARs
        if ( artifact != null && MAVEN_PLUGIN_PACKAGING.equals( ai.packaging ) && artifact.getName().endsWith( ".jar" ) )
        {
            // TODO: recheck, is the following true? "Maven plugins and Maven Archetypes can be only JARs?"

            // 1st, check for maven plugin
            checkMavenPlugin( ai, artifact );
        }
    }

    private void checkMavenPlugin( ArtifactInfo ai, File artifact )
    {
        ZipHandle handle = null;

        try
        {
            handle = ZipFacade.getZipHandle( artifact );

            final String pluginDescriptorPath = "META-INF/maven/plugin.xml";

            if ( handle.hasEntry( pluginDescriptorPath ) )
            {
                InputStream is = new BufferedInputStream( handle.getEntryContent( pluginDescriptorPath ) );

                try
                {
                    // here the reader is closed
                    PlexusConfiguration plexusConfig =
                        new XmlPlexusConfiguration( Xpp3DomBuilder.build( new InputStreamReader( is ) ) );

                    ai.prefix = plexusConfig.getChild( "goalPrefix" ).getValue();

                    ai.goals = new ArrayList<String>();

                    PlexusConfiguration[] mojoConfigs = plexusConfig.getChild( "mojos" ).getChildren( "mojo" );

                    for ( PlexusConfiguration mojoConfig : mojoConfigs )
                    {
                        ai.goals.add( mojoConfig.getChild( "goal" ).getValue() );
                    }
                }
                finally
                {
                    is.close();
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
            catch ( IOException e )
            {
            }
        }
    }

    public void updateDocument( ArtifactInfo ai, Document doc )
    {
        if ( ai.prefix != null )
        {
            doc.add( FLD_PLUGIN_PREFIX.toField( ai.prefix ) );
        }

        if ( ai.goals != null )
        {
            doc.add( FLD_PLUGIN_GOALS.toField( ArtifactInfo.lst2str( ai.goals ) ) );
        }
    }

    public boolean updateArtifactInfo( Document doc, ArtifactInfo ai )
    {
        boolean res = false;

        if ( "maven-plugin".equals( ai.packaging ) )
        {
            ai.prefix = doc.get( ArtifactInfo.PLUGIN_PREFIX );

            String goals = doc.get( ArtifactInfo.PLUGIN_GOALS );

            if ( goals != null )
            {
                ai.goals = ArtifactInfo.str2lst( goals );
            }

            res = true;
        }

        return res;
    }

    @Override
    public String toString()
    {
        return ID;
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_PLUGIN_GOALS, FLD_PLUGIN_PREFIX );
    }
}
