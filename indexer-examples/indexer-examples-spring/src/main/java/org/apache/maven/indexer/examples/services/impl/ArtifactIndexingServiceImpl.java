package org.apache.maven.indexer.examples.services.impl;

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
import java.util.*;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.indexer.examples.indexing.RepositoryIndexManager;
import org.apache.maven.indexer.examples.indexing.RepositoryIndexer;
import org.apache.maven.indexer.examples.indexing.SearchRequest;
import org.apache.maven.indexer.examples.indexing.SearchResults;
import org.apache.maven.indexer.examples.services.ArtifactIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mtodorov
 */
@Component
public class ArtifactIndexingServiceImpl
        implements ArtifactIndexingService
{

    private static final Logger logger = LoggerFactory.getLogger( ArtifactIndexingServiceImpl.class );

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;


    @Override
    public void addToIndex( String repositoryId,
                            File artifactFile,
                            String groupId,
                            String artifactId,
                            String version,
                            String extension,
                            String classifier )
            throws IOException
    {
        final RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex( repositoryId );

        indexer.addArtifactToIndex( repositoryId, artifactFile, groupId, artifactId, version, extension, classifier );
    }

    @Override
    public void deleteFromIndex( String repositoryId,
                                 String groupId,
                                 String artifactId,
                                 String version,
                                 String extension,
                                 String classifier )
            throws IOException
    {
        final RepositoryIndexer indexer = repositoryIndexManager.getRepositoryIndex( repositoryId );
        if ( indexer != null )
        {
            indexer.delete( Arrays.asList( new ArtifactInfo( repositoryId,
                                                             groupId,
                                                             artifactId,
                                                             version,
                                                             classifier,
                                                             extension ) ) );
        }
    }

    @Override
    public SearchResults search( SearchRequest searchRequest )
            throws IOException, ParseException
    {
        SearchResults searchResults = new SearchResults();

        final String repositoryId = searchRequest.getRepository();

        if ( repositoryId != null && !repositoryId.isEmpty() )
        {
            logger.debug( "Repository: {}", repositoryId );

            final Map<String, Collection<ArtifactInfo>> resultsMap = getResultsMap( repositoryId,
                                                                                    searchRequest.getQuery() );

            if ( !resultsMap.isEmpty() )
            {
                searchResults.setResults( resultsMap );
            }

            if ( logger.isDebugEnabled() )
            {
                int results = resultsMap.entrySet().iterator().next().getValue().size();

                logger.debug( "Results: {}", results );
            }
        }
        else
        {
            Map<String, Collection<ArtifactInfo>> resultsMap = new LinkedHashMap<>();
            for ( String repoId : repositoryIndexManager.getIndexes().keySet() )
            {
                logger.debug( "Repository: {}", repoId );

                final RepositoryIndexer repositoryIndex = repositoryIndexManager.getRepositoryIndex( repoId );
                if ( repositoryIndex != null )
                {
                    final Set<ArtifactInfo> artifactInfoResults = repositoryIndexManager.getRepositoryIndex( repoId )
                                                                                        .search( searchRequest.getQuery() );

                    if ( !artifactInfoResults.isEmpty() )
                    {
                        resultsMap.put( repoId, artifactInfoResults );
                    }

                    logger.debug( "Results: {}", artifactInfoResults.size() );
                }
            }

            searchResults.setResults( resultsMap );
        }

        return searchResults;
    }

    @Override
    public boolean contains( SearchRequest searchRequest )
            throws IOException, ParseException
    {
        return !getResultsMap( searchRequest.getRepository(), searchRequest.getQuery() ).isEmpty();
    }

    public Map<String, Collection<ArtifactInfo>> getResultsMap( String repositoryId,
                                                                String query )
            throws IOException, ParseException
    {
        Map<String, Collection<ArtifactInfo>> resultsMap = new LinkedHashMap<>();
        final Set<ArtifactInfo> artifactInfoResults = repositoryIndexManager.getRepositoryIndex( repositoryId )
                                                                            .search( query );

        if ( !artifactInfoResults.isEmpty() )
        {
            resultsMap.put( repositoryId, artifactInfoResults );
        }

        return resultsMap;
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
