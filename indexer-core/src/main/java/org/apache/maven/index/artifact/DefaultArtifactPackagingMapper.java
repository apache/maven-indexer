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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple artifact packaging mapper, that has everything for quick-start wired in this class. Also, it takes into
 * account the "${nexus-work}/conf/packaging2extension-mapping.properties" file into account if found. To override the
 * "defaults" in this class, simply add lines to properties file with same keys.
 *
 * @author cstamas
 */
@Singleton
@Named
public class DefaultArtifactPackagingMapper
    implements ArtifactPackagingMapper
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    public static final String MAPPING_PROPERTIES_FILE = "packaging2extension-mapping.properties";

    private File propertiesFile;

    private volatile Map<String, String> packaging2extensionMapping;

    private static final Map<String, String> DEFAULTS;

    static
    {
        DEFAULTS = new HashMap<String, String>();
        DEFAULTS.put( "ejb-client", "jar" );
        DEFAULTS.put( "ejb", "jar" );
        DEFAULTS.put( "rar", "jar" );
        DEFAULTS.put( "par", "jar" );
        DEFAULTS.put( "maven-plugin", "jar" );
        DEFAULTS.put( "maven-archetype", "jar" );
        DEFAULTS.put( "plexus-application", "jar" );
        DEFAULTS.put( "eclipse-plugin", "jar" );
        DEFAULTS.put( "eclipse-feature", "jar" );
        DEFAULTS.put( "eclipse-application", "zip" );
        DEFAULTS.put( "nexus-plugin", "jar" );
        DEFAULTS.put( "java-source", "jar" );
        DEFAULTS.put( "javadoc", "jar" );
        DEFAULTS.put( "test-jar", "jar" );
    }

    public void setPropertiesFile( File propertiesFile )
    {
        this.propertiesFile = propertiesFile;
        this.packaging2extensionMapping = null;
    }

    public Map<String, String> getPackaging2extensionMapping()
    {
        if ( packaging2extensionMapping == null )
        {
            synchronized ( this )
            {
                if ( packaging2extensionMapping == null )
                {
                    packaging2extensionMapping = new HashMap<String, String>();

                    // merge defaults
                    packaging2extensionMapping.putAll( DEFAULTS );

                    if ( propertiesFile != null && propertiesFile.exists() )
                    {
                        getLogger().info( "Found user artifact packaging mapping file, applying it..." );

                        Properties userMappings = new Properties();

                        try ( FileInputStream fis = new FileInputStream( propertiesFile ) )
                        {
                            userMappings.load( fis );

                            if ( userMappings.keySet().size() > 0 )
                            {
                                for ( Object key : userMappings.keySet() )
                                {
                                    packaging2extensionMapping.put( key.toString(),
                                                                    userMappings.getProperty( key.toString() ) );
                                }

                                getLogger().info(
                                    propertiesFile.getAbsolutePath()
                                        + " user artifact packaging mapping file contained "
                                        + userMappings.keySet().size() + " mappings, applied them all succesfully." );
                            }
                        }
                        catch ( IOException e )
                        {
                            getLogger().warn(
                                "Got IO exception during read of file: " + propertiesFile.getAbsolutePath() );
                        }

                    }
                    else
                    {
                        // make it silent if using defaults
                        getLogger().debug(
                            "User artifact packaging mappings file not found, will work with defaults..." );
                    }
                }
            }
        }

        return packaging2extensionMapping;
    }

    public void setPackaging2extensionMapping( Map<String, String> packaging2extensionMapping )
    {
        this.packaging2extensionMapping = packaging2extensionMapping;
    }

    public Map<String, String> getDefaults()
    {
        return DEFAULTS;
    }

    public String getExtensionForPackaging( String packaging )
    {
        if ( packaging == null )
        {
            return "jar";
        }

        if ( getPackaging2extensionMapping().containsKey( packaging ) )
        {
            return getPackaging2extensionMapping().get( packaging );
        }
        else
        {
            // default's to packaging name, ie. "jar", "war", "pom", etc.
            return packaging;
        }
    }
}
