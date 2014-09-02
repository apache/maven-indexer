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

import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class represents a mapping between repositoryId-s and their respective indexes.
 *
 * @author mtodorov
 */
@Singleton
@Component
public class RepositoryIndexManager
{

    private static final Logger logger = LoggerFactory.getLogger( RepositoryIndexManager.class );

    /**
     * K: repositoryId
     * V: index
     */
    private Map<String, RepositoryIndexer> indexes = new LinkedHashMap<>();


    public RepositoryIndexManager()
    {
    }

    /**
     * A convenience method for closing the indexes. This method will be called
     * by the Spring container and it shouldn't be invoked directly.
     * <p/>
     * This method is of particular importance, as you should close the indexes properly.
     */
    @PreDestroy
    private void close()
    {
        for ( String repositoryId : indexes.keySet() )
        {
            try
            {
                final RepositoryIndexer repositoryIndexer = indexes.get( repositoryId );

                logger.debug( "Closing indexer for " + repositoryIndexer.getRepositoryId() + "..." );

                repositoryIndexer.close();

                logger.debug( "Closed indexer for " + repositoryIndexer.getRepositoryId() + "." );
            }
            catch ( IOException e )
            {
                logger.error( e.getMessage(), e );
            }
        }
    }

    public Map<String, RepositoryIndexer> getIndexes()
    {
        return indexes;
    }

    public void setIndexes( Map<String, RepositoryIndexer> indexes )
    {
        this.indexes = indexes;
    }

    public RepositoryIndexer getRepositoryIndex( String repositoryId )
    {
        return indexes.get( repositoryId );
    }

    public RepositoryIndexer addRepositoryIndex( String repositoryId,
                                                 RepositoryIndexer value )
    {
        return indexes.put( repositoryId, value );
    }

    public RepositoryIndexer removeRepositoryIndex( String repositoryId )
    {
        return indexes.remove( repositoryId );
    }

}
