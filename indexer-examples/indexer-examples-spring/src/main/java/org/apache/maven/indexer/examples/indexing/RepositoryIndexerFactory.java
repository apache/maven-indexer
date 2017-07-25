package org.apache.maven.indexer.examples.indexing;

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

import org.apache.maven.index.Indexer;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * A factory for pre-configured RepositoryIndexers.
 *
 * @author mtodorov
 */
@Singleton
public class RepositoryIndexerFactory
{

    private IndexerConfiguration indexerConfiguration;


    @Inject
    public RepositoryIndexerFactory( IndexerConfiguration indexerConfiguration )
    {
        this.indexerConfiguration = indexerConfiguration;
    }

    public RepositoryIndexer createRepositoryIndexer( String repositoryId, File repositoryBasedir, File indexDir )
        throws IOException
    {
        RepositoryIndexer repositoryIndexer = new RepositoryIndexer();
        repositoryIndexer.setRepositoryId( repositoryId );
        repositoryIndexer.setRepositoryBasedir( repositoryBasedir );
        repositoryIndexer.setIndexDir( indexDir );
        repositoryIndexer.setIndexingContext( createIndexingContext( repositoryId, repositoryBasedir, indexDir ) );
        repositoryIndexer.setIndexer( indexerConfiguration.getIndexer() );
        repositoryIndexer.setScanner( indexerConfiguration.getScanner() );

        return repositoryIndexer;
    }

    private IndexingContext createIndexingContext( String repositoryId, File repositoryBasedir, File indexDir )
        throws IOException
    {
        return getIndexer().createIndexingContext( repositoryId + "/ctx", repositoryId, repositoryBasedir, indexDir,
                                                   null, null, true,
                                                   // if context should be searched in non-targeted mode.
                                                   true, // if indexDirectory is known to contain (or should contain)
                                                   // valid Maven Indexer lucene index, and no checks needed to be
                                                   // performed, or, if we want to "stomp" over existing index
                                                   // (unsafe to do!).
                                                   indexerConfiguration.getIndexersAsList() );
    }

    public IndexerConfiguration getIndexerConfiguration()
    {
        return indexerConfiguration;
    }

    public void setIndexerConfiguration( IndexerConfiguration indexerConfiguration )
    {
        this.indexerConfiguration = indexerConfiguration;
    }

    public Indexer getIndexer()
    {
        return indexerConfiguration.getIndexer();
    }

    public Scanner getScanner()
    {
        return indexerConfiguration.getScanner();
    }

    public Map<String, IndexCreator> getIndexers()
    {
        return indexerConfiguration.getIndexers();
    }

}
