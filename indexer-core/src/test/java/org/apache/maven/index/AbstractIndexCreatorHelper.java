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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.codehaus.plexus.util.FileUtils;

public class AbstractIndexCreatorHelper
    extends AbstractTestSupport
{
    public List<IndexCreator> DEFAULT_CREATORS;

    public List<IndexCreator> FULL_CREATORS;

    public List<IndexCreator> MIN_CREATORS;

    Random rand = new Random();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        DEFAULT_CREATORS = new ArrayList<IndexCreator>();
        FULL_CREATORS = new ArrayList<IndexCreator>();
        MIN_CREATORS = new ArrayList<IndexCreator>();

        IndexCreator min = lookup( IndexCreator.class, MinimalArtifactInfoIndexCreator.ID );
        IndexCreator mavenPlugin = lookup( IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID );
        IndexCreator mavenArchetype = lookup( IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID );
        IndexCreator jar = lookup( IndexCreator.class, JarFileContentsIndexCreator.ID );

        MIN_CREATORS.add( min );

        DEFAULT_CREATORS.add( min );
        DEFAULT_CREATORS.add( mavenPlugin );
        DEFAULT_CREATORS.add( mavenArchetype );

        FULL_CREATORS.add( min );
        FULL_CREATORS.add( mavenPlugin );
        FULL_CREATORS.add( mavenArchetype );
        FULL_CREATORS.add( jar );
    }

    protected void deleteDirectory( File dir )
        throws IOException
    {
        FileUtils.deleteDirectory( dir );
    }

    protected File getDirectory( String name )
    {
        // pick random output location

        File outputFolder = new File( getBasedir(), "target/tests/" + name + "-" + rand.nextLong() + "/" );
        outputFolder.delete();
        assertFalse( outputFolder.exists() );
        return outputFolder;
    }

    public void testDirectory()
        throws IOException
    {
        File dir = this.getDirectory( "foo" );
        assert ( dir.getAbsolutePath().contains( "foo" ) );
        this.deleteDirectory( dir );
        assertFalse( dir.exists() );

        File dir2 = this.getDirectory( "foo" );
        assertFalse( "Directories aren't unique", dir.getCanonicalPath().equals( dir2.getCanonicalPath() ) );
    }
}
