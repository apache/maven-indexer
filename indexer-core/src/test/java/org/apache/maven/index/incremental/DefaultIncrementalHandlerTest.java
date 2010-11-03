/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index.incremental;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.index.AbstractIndexCreatorHelper;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.incremental.IncrementalHandler;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class DefaultIncrementalHandlerTest
    extends AbstractIndexCreatorHelper
{
    IncrementalHandler handler = null;
    NexusIndexer indexer = null;
    IndexingContext context = null;
    File indexDir = null;
    File repoDir = null;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        indexer = lookup( NexusIndexer.class );
        handler = lookup( IncrementalHandler.class );
        
        indexDir = super.getDirectory("index/nexus-incremental-test" );
        repoDir = super.getDirectory("repos/nexus-incremental-test" );
        super.deleteDirectory( indexDir );
        super.deleteDirectory( repoDir );
        
        context = indexer.addIndexingContext( "test", "test", repoDir, indexDir, null, null, DEFAULT_CREATORS );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        
       try
       { 
        indexer.removeIndexingContext( context, true );
       }
       catch (IOException e)
       {
    	   //ignore this, the folders are unique. This can cause weird issues on windows.
       }
    }
    
    public void testUpdateInvalidProperties()
        throws Exception
    {
        Properties properties = new Properties();
        
        IndexPackingRequest request = new IndexPackingRequest( context, indexDir );
        
        //No properties definite fail
        assertNull( handler.getIncrementalUpdates( request, properties ) );
        
        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "junk" );
        
        //property set, but invalid 
        assertNull( handler.getIncrementalUpdates( request, properties ) );
        
        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "19991112182432.432 -0600" );
        
        List<Integer> updates = handler.getIncrementalUpdates( request, properties );
        
        assertEquals( updates.size(), 0 );
    }
    
    public void testUpdateValid()
        throws Exception
    {
        Properties properties = new Properties();
        
        IndexPackingRequest request = new IndexPackingRequest( context, indexDir );
        
        properties.setProperty( IndexingContext.INDEX_TIMESTAMP, "19991112182432.432 -0600" );
        
        FileUtils.copyDirectoryStructure( new File( getBasedir(), "src/test/repo/ch" ), new File( repoDir, "ch" ) );
        
        indexer.scan( context );
        
        List<Integer> updates = handler.getIncrementalUpdates( request, properties );
        
        assertEquals( updates.size(), 1 );
    }
    
    public void testRemoteUpdatesInvalidProperties()
        throws Exception
    {
        IndexUpdateRequest request = new IndexUpdateRequest( context );
        
        Properties localProperties = new Properties();
        Properties remoteProperties = new Properties();
        
        List<String> filenames = handler.loadRemoteIncrementalUpdates( request, localProperties, remoteProperties );
        
        assertNull( filenames );
    }
}
