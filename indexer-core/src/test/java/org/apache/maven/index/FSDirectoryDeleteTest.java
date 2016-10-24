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

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.context.IndexingContext;

public class FSDirectoryDeleteTest
    extends AbstractIndexCreatorHelper
{
    protected NexusIndexer nexusIndexer;

    protected File repo = new File( getBasedir(), "src/test/nexus-13" );

    protected IndexingContext context;

    protected File indexDirFile = super.getDirectory( "fsdirectorytest/one" );

    protected Directory indexDir;

    protected IndexingContext otherContext;

    protected File otherIndexDirFile = super.getDirectory( "fsdirectorytest/other" );

    protected Directory otherIndexDir;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        nexusIndexer = lookup( NexusIndexer.class );

        indexDir = FSDirectory.open( indexDirFile.toPath() );

        context = nexusIndexer.addIndexingContext( "one", "nexus-13", repo, indexDir, null, null, DEFAULT_CREATORS );

        nexusIndexer.scan( context );

        otherIndexDir = FSDirectory.open( otherIndexDirFile.toPath() );

        otherContext =
            nexusIndexer.addIndexingContext( "other", "nexus-13", repo, otherIndexDir, null, null, DEFAULT_CREATORS );

        nexusIndexer.scan( otherContext );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        nexusIndexer.removeIndexingContext( context, true );

        nexusIndexer.removeIndexingContext( otherContext, true );

        super.deleteDirectory( indexDirFile );

        super.deleteDirectory( otherIndexDirFile );
    }

    public void testIndexAndDelete()
        throws Exception
    {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        final IndexSearcher otherIndexSearcher = otherContext.acquireIndexSearcher();
        
        indexSearcher.getIndexReader().maxDoc();
        otherIndexSearcher.getIndexReader().maxDoc();
        
        context.releaseIndexSearcher( indexSearcher );
        otherContext.releaseIndexSearcher( otherIndexSearcher );

        context.replace( otherIndexDir );

        context.merge( otherIndexDir );
    }
}
