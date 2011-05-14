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

import java.util.HashMap;
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
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author Eugene Kuleshov
 */
@Component( role = ArchetypeDataSource.class, hint = "nexus" )
public class NexusArchetypeDataSource
    extends AbstractLogEnabled
    implements ArchetypeDataSource
{
    @Requirement
    private NexusIndexer indexer;

    public ArchetypeCatalog getArchetypeCatalog( Properties properties )
        throws ArchetypeDataSourceException
    {
        ArchetypeCatalog catalog = new ArchetypeCatalog();

        try
        {
            Map<String, String> repositories = getRepositoryMap();

            Query pq = indexer.constructQuery( MAVEN.PACKAGING, new SourcedSearchExpression( "maven-archetype" ) );

            FlatSearchRequest searchRequest = new FlatSearchRequest( pq );

            FlatSearchResponse searchResponse = indexer.searchFlat( searchRequest );

            for ( ArtifactInfo info : searchResponse.getResults() )
            {
                Archetype archetype = new Archetype();
                archetype.setGroupId( info.groupId );
                archetype.setArtifactId( info.artifactId );
                archetype.setVersion( info.version );
                archetype.setDescription( info.description );
                archetype.setRepository( repositories.get( info.repository ) );

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

        for ( IndexingContext context : indexer.getIndexingContexts().values() )
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
}
