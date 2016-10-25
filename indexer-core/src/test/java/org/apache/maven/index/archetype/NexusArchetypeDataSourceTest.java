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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.index.AbstractIndexCreatorHelper;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;

public class NexusArchetypeDataSourceTest
    extends AbstractIndexCreatorHelper
{
    private IndexingContext context;

    private NexusIndexer nexusIndexer;

    // private IndexUpdater indexUpdater;

    private NexusArchetypeDataSource nexusArchetypeDataSource;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        prepare( true );
    }

    private void prepare( boolean inRam )
        throws Exception, IOException, UnsupportedExistingLuceneIndexException
    {
        nexusIndexer = lookup( NexusIndexer.class );

        // indexUpdater = lookup( IndexUpdater.class );

        Directory indexDir = null;

        if ( inRam )
        {
            indexDir = new RAMDirectory();
        }
        else
        {
            File indexDirFile = super.getDirectory( "index/test" );

            super.deleteDirectory( indexDirFile );

            indexDir = FSDirectory.open( indexDirFile.toPath() );
        }

        File repo = new File( getBasedir(), "src/test/repo" );

        context =
            nexusIndexer.addIndexingContext( "test", "public", repo, indexDir,
                "http://repository.sonatype.org/content/groups/public/", null, DEFAULT_CREATORS );
        nexusIndexer.scan( context );

        // to update, uncomment this
        // IndexUpdateRequest updateRequest = new IndexUpdateRequest( context );
        // indexUpdater.fetchAndUpdateIndex( updateRequest );

        nexusArchetypeDataSource = (NexusArchetypeDataSource) lookup( ArchetypeDataSource.class, "nexus" );
    }

    public void testArchetype()
        throws Exception
    {
        ArchetypeCatalog catalog = nexusArchetypeDataSource.getArchetypeCatalog( null );

        assertEquals( "Not correct numbers of archetypes in catalog!", 4, catalog.getArchetypes().size() );
    }

}
