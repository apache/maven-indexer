package org.apache.maven.index.archetype;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.Query;
import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.archetype.source.ArchetypeDataSourceException;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractArchetypeDataSource
    implements ArchetypeDataSource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private final Indexer indexer;


    @Inject
    protected AbstractArchetypeDataSource( Indexer indexer )
    {
        this.indexer = indexer;
    }

    public ArchetypeCatalog getArchetypeCatalog( final Properties properties )
        throws ArchetypeDataSourceException
    {
        final ArchetypeCatalog catalog = new ArchetypeCatalog();
        try
        {
            final Map<String, String> repositories = getRepositoryMap();
            final Query pq = indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "maven-archetype" ) );
            final FlatSearchRequest searchRequest = new FlatSearchRequest( pq );
            searchRequest.setContexts( getIndexingContexts() );
            final FlatSearchResponse searchResponse = indexer.searchFlat( searchRequest );
            for ( ArtifactInfo info : searchResponse.getResults() )
            {
                Archetype archetype = new Archetype();
                archetype.setGroupId( info.getGroupId() );
                archetype.setArtifactId( info.getArtifactId() );
                archetype.setVersion( info.getVersion() );
                archetype.setDescription( info.getDescription() );
                archetype.setRepository( repositories.get( info.getRepository() ) );
                catalog.addArchetype( archetype );
            }
        }
        catch ( Exception ex )
        {
            getLogger().error( "Unable to retrieve archetypes", ex );
        }

        return catalog;
    }

    private Map<String, String> getRepositoryMap()
    {
        // can't cache this because indexes can be changed
        Map<String, String> repositories = new HashMap<String, String>();

        for ( IndexingContext context : getIndexingContexts() )
        {
            String repositoryUrl = context.getRepositoryUrl();
            if ( repositoryUrl != null )
            {
                repositories.put( context.getId(), repositoryUrl );
            }
        }

        return repositories;
    }

    public void updateCatalog( Properties properties, Archetype archetype )
    {
        // TODO maybe update index
    }

    // ==

    protected abstract List<IndexingContext> getIndexingContexts();
}
