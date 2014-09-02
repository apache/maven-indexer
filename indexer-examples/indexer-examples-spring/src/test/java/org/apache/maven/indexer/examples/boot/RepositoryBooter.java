package org.apache.maven.indexer.examples.boot;

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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

import org.apache.maven.indexer.examples.indexing.RepositoryIndexManager;
import org.apache.maven.indexer.examples.indexing.RepositoryIndexer;
import org.apache.maven.indexer.examples.indexing.RepositoryIndexerFactory;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a dummy artifact repository creator.
 *
 * @author mtodorov
 */
@Component
public class RepositoryBooter
{

    private static final Logger logger = LoggerFactory.getLogger( RepositoryBooter.class );

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;

    @Autowired
    private RepositoryIndexerFactory repositoryIndexerFactory;


    public RepositoryBooter()
    {
    }

    @PostConstruct
    public void initialize()
            throws IOException,
                   PlexusContainerException,
                   ComponentLookupException
    {
        File repositoriesBaseDir = new File( "target/repositories" );

        if ( !lockExists( repositoriesBaseDir ) )
        {
            createLockFile( repositoriesBaseDir );
            initializeRepositories( repositoriesBaseDir );
        }
        else
        {
            logger.error( "Failed to initialize the repositories. Another JVM may have already done this." );
        }

        logger.debug( "Initialized repositories." );
    }

    private void createLockFile( File repositoriesRootDir )
            throws IOException
    {
        final File lockFile = new File( repositoriesRootDir, "repositories.lock" );
        //noinspection ResultOfMethodCallIgnored
        lockFile.getParentFile().mkdirs();
        //noinspection ResultOfMethodCallIgnored
        lockFile.createNewFile();
    }

    private boolean lockExists( File repositoriesRootDir )
            throws IOException
    {
        File lockFile = new File( repositoriesRootDir, "repositories.lock" );

        return lockFile.exists();
    }

    private void initializeRepositories( File repositoriesBaseDir )
            throws IOException,
                   PlexusContainerException,
                   ComponentLookupException
    {
        initializeRepository( repositoriesBaseDir, "releases" );
        initializeRepository( repositoriesBaseDir, "snapshots" );
    }

    private void initializeRepository( File repositoriesBaseDir,
                                       String repositoryName )
            throws IOException,
                   PlexusContainerException,
                   ComponentLookupException
    {
        createRepositoryStructure( repositoriesBaseDir.getAbsolutePath(), repositoryName );

        initializeRepositoryIndex( new File( repositoriesBaseDir.getAbsoluteFile(), repositoryName ), repositoryName );
    }

    public void createRepositoryStructure( String repositoriesBaseDir,
                                           String repositoryName )
            throws IOException
    {
        final File repositoriesBasedir = new File( repositoriesBaseDir );
        //noinspection ResultOfMethodCallIgnored
        new File( repositoriesBasedir, repositoryName ).mkdirs();
        //noinspection ResultOfMethodCallIgnored
        new File( repositoriesBasedir, repositoryName + File.separatorChar + ".index" ).mkdirs();

        logger.debug( "Created directory structure for repository '" +
                      repositoriesBasedir.getAbsolutePath() + File.separatorChar + repositoryName + "'." );
    }

    private void initializeRepositoryIndex( File repositoryBasedir,
                                            String repositoryId )
            throws PlexusContainerException,
                   ComponentLookupException,
                   IOException
    {
        final File indexDir = new File( repositoryBasedir, ".index" );

        RepositoryIndexer repositoryIndexer = repositoryIndexerFactory.createRepositoryIndexer( repositoryId,
                                                                                                repositoryBasedir,
                                                                                                indexDir );

        repositoryIndexManager.addRepositoryIndex( repositoryId, repositoryIndexer );
    }

    public RepositoryIndexManager getRepositoryIndexManager()
    {
        return repositoryIndexManager;
    }

    public void setRepositoryIndexManager( RepositoryIndexManager repositoryIndexManager )
    {
        this.repositoryIndexManager = repositoryIndexManager;
    }

}
