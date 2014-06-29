package org.apache.maven.index.updater;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.AbstractRepoNexusIndexerTest;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexDataWriter;

/**
 * @author Eugene Kuleshov
 */
public class IndexDataTest
    extends AbstractRepoNexusIndexerTest
{
    private Directory newDir;

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        indexDir = new RAMDirectory();

        context =
            nexusIndexer.addIndexingContext( "test-default", "test", repo, indexDir, null, null, DEFAULT_CREATORS );

        // assertNull( context.getTimestamp() ); // unknown upon creation

        nexusIndexer.scan( context );

        Date timestamp = context.getTimestamp();

        assertNotNull( timestamp );

        // save and restore index to be used by common tests

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        IndexDataWriter dw = new IndexDataWriter( bos );
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            dw.write( context, indexSearcher.getIndexReader(), null );
        } finally
        {
            context.releaseIndexSearcher( indexSearcher );
        }

        ByteArrayInputStream is = new ByteArrayInputStream( bos.toByteArray() );

        newDir = new RAMDirectory();

        Date newTimestamp = DefaultIndexUpdater.unpackIndexData( is, newDir, context );

        assertEquals( timestamp, newTimestamp );

        context.replace( newDir );
    }

    public void testEmptyContext()
        throws Exception
    {
        indexDir = new RAMDirectory();

        context =
            nexusIndexer.addIndexingContext( "test-default", "test", repo, indexDir, null, null, DEFAULT_CREATORS );

        assertNull( context.getTimestamp() ); // unknown upon creation

        // save and restore index to be used by common tests
        // the point is that this is virgin context, and timestamp is null,
        // and it should remain null

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        IndexDataWriter dw = new IndexDataWriter( bos );
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            dw.write( context, indexSearcher.getIndexReader(), null );
        }finally
        {
            context.releaseIndexSearcher( indexSearcher );
        }

        ByteArrayInputStream is = new ByteArrayInputStream( bos.toByteArray() );

        newDir = new RAMDirectory();

        Date newTimestamp = DefaultIndexUpdater.unpackIndexData( is, newDir, context );

        assertEquals( null, newTimestamp );

        context.replace( newDir );
    }

    public void testData()
        throws Exception
    {
        IndexReader r1 = context.acquireIndexSearcher().getIndexReader();

        Map<String, ArtifactInfo> r1map = readIndex( r1 );

        IndexReader r2 = IndexReader.open( newDir );

        Map<String, ArtifactInfo> r2map = readIndex( r2 );

        for ( Entry<String, ArtifactInfo> e : r1map.entrySet() )
        {
            String key = e.getKey();
            assertTrue( "Expected for find " + key, r2map.containsKey( key ) );
        }

        assertEquals( r1map.size(), r2map.size() );
    }

    private Map<String, ArtifactInfo> readIndex( IndexReader r1 )
        throws CorruptIndexException, IOException
    {
        Map<String, ArtifactInfo> map = new HashMap<String, ArtifactInfo>();

        for ( int i = 0; i < r1.maxDoc(); i++ )
        {
            Document document = r1.document( i );

            ArtifactInfo ai = IndexUtils.constructArtifactInfo( document, context );

            if ( ai != null )
            {
                map.put( ai.getUinfo(), ai );
            }
        }

        return map;
    }

}
