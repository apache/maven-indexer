package org.apache.maven.index.reader;

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
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.maven.index.reader.Record.Type;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.maven.index.reader.TestUtils.expandFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * UT for {@link IndexReader}
 */
public class IndexReaderTest
        extends TestSupport
{
    @Test
    public void simple() throws IOException
    {
        try ( IndexReader indexReader = new IndexReader( null, testResourceHandler( "simple" ) ) )
        {
            assertThat( indexReader.getIndexId(), equalTo( "apache-snapshots-local" ) );
            assertThat( indexReader.getPublishedTimestamp().getTime(), equalTo( 1243533418015L ) );
            assertThat( indexReader.isIncremental(), equalTo( false ) );
            assertThat( indexReader.getChunkNames(), equalTo(
                    Collections.singletonList( "nexus-maven-repository-index.gz" ) ) );
            int chunks = 0;
            int records = 0;

            Iterator<ChunkReader> chunkReaders = indexReader.iterator();
            while ( chunkReaders.hasNext() )
            {
                try ( ChunkReader chunkReader = chunkReaders.next() )
                {
                    chunks++;
                    assertThat( chunkReader.getName(), equalTo( "nexus-maven-repository-index.gz" ) );
                    assertThat( chunkReader.getVersion(), equalTo( 1 ) );
                    assertThat( chunkReader.getTimestamp().getTime(), equalTo( 1243533418015L ) );
                    for ( Record ignored : StreamSupport.stream( chunkReader.spliterator(), false )
                            .map( expandFunction )
                            .collect( Collectors.toList() ) )
                    {
                        records++;
                    }
                }
            }

            assertThat( chunks, equalTo( 1 ) );
            assertThat( records, equalTo( 5 ) );
        }
    }

    @Test
    public void simpleWithLocal() throws IOException
    {
        try ( WritableResourceHandler writableResourceHandler = createWritableResourceHandler() )
        {
            try ( IndexReader indexReader = new IndexReader( writableResourceHandler,
                    testResourceHandler( "simple" ) ) )
            {
                assertThat( indexReader.getIndexId(), equalTo( "apache-snapshots-local" ) );
                assertThat( indexReader.getPublishedTimestamp().getTime(), equalTo( 1243533418015L ) );
                assertThat( indexReader.isIncremental(), equalTo( false ) );
                assertThat( indexReader.getChunkNames(),
                        equalTo( Collections.singletonList( "nexus-maven-repository-index.gz" ) ) );
                int chunks = 0;
                int records = 0;

                Iterator<ChunkReader> chunkReaders = indexReader.iterator();
                while ( chunkReaders.hasNext() )
                {
                    try ( ChunkReader chunkReader = chunkReaders.next() )
                    {
                        chunks++;
                        assertThat( chunkReader.getName(), equalTo( "nexus-maven-repository-index.gz" ) );
                        assertThat( chunkReader.getVersion(), equalTo( 1 ) );
                        assertThat( chunkReader.getTimestamp().getTime(), equalTo( 1243533418015L ) );
                        for ( Record ignored : StreamSupport.stream( chunkReader.spliterator(), false )
                                .map( expandFunction )
                                .collect( Collectors.toList() ) )
                        {
                            records++;
                        }
                    }
                }

                assertThat( chunks, equalTo( 1 ) );
                assertThat( records, equalTo( 5 ) );
            }
            assertThat( writableResourceHandler.locate( "nexus-maven-repository-index.properties" ).read(),
                    not( nullValue() ) );
        }
    }

    @Test
    public void roundtrip() throws IOException
    {
        try ( WritableResourceHandler writableResourceHandler = createWritableResourceHandler() )
        {
            Date published;
            {
                try ( ResourceHandler resourceHandler = testResourceHandler( "simple" );
                      IndexReader indexReader = new IndexReader( null, resourceHandler );
                      IndexWriter indexWriter = new IndexWriter( writableResourceHandler, indexReader.getIndexId(),
                              false ) )
                {
                    Iterator<ChunkReader> chunkReaders = indexReader.iterator();
                    while ( chunkReaders.hasNext() )
                    {
                        try ( ChunkReader chunkReader = chunkReaders.next() )
                        {
                            indexWriter.writeChunk( chunkReader.iterator() );
                        }
                    }
                    indexWriter.close(); // must close to store properties, so next call returns non-null
                    published = indexWriter.getPublishedTimestamp();
                }
            }

            try ( IndexReader indexReader = new IndexReader( null, writableResourceHandler ) )
            {
                assertThat( indexReader.getIndexId(), equalTo( "apache-snapshots-local" ) );
                assertThat( indexReader.getPublishedTimestamp().getTime(), equalTo( published.getTime() ) );
                assertThat( indexReader.isIncremental(), equalTo( false ) );
                assertThat( indexReader.getChunkNames(), equalTo(
                        Collections.singletonList( "nexus-maven-repository-index.gz" ) ) );
                int chunks = 0;
                AtomicInteger records = new AtomicInteger( 0 );

                Iterator<ChunkReader> chunkReaders = indexReader.iterator();
                while ( chunkReaders.hasNext() )
                {
                    try ( ChunkReader chunkReader = chunkReaders.next() )
                    {
                        chunks++;
                        assertThat( chunkReader.getName(), equalTo( "nexus-maven-repository-index.gz" ) );
                        assertThat( chunkReader.getVersion(), equalTo( 1 ) );
                        chunkReader.forEach( r -> records.incrementAndGet() );
                    }
                }

                assertThat( chunks, equalTo( 1 ) );
                assertThat( records.get(), equalTo( 5 ) );
            }
        }
    }

    /**
     * This UT is here for demonstration purposes only. Bashing Central is not something you want to do, and risk your
     * IP address being banned. You were warned!
     */
    @Test
    @Ignore( "For eyes only" )
    public void central() throws Exception
    {
        // local index location, against which we perform incremental updates
        final File indexDir = createTempDirectory();
        // cache of remote, to not rely on HTTP transport possible failures, or, to detect them early
        final File cacheDir = createTempDirectory();

        final PrintWriter writer = new PrintWriter( System.out, true );
        final WritableResourceHandler local = new DirectoryResourceHandler( indexDir );
        final CachingResourceHandler remote = new CachingResourceHandler(
                new DirectoryResourceHandler( cacheDir ),
                new HttpResourceHandler( new URL( "http://repo1.maven.org/maven2/.index/" ) )
        );
        final IndexReader indexReader = new IndexReader( local, remote );
        try
        {
            writer.println( "indexRepoId=" + indexReader.getIndexId() );
            writer.println( "indexLastPublished=" + indexReader.getPublishedTimestamp() );
            writer.println( "isIncremental=" + indexReader.isIncremental() );
            writer.println( "indexRequiredChunkNames=" + indexReader.getChunkNames() );

            Iterator<ChunkReader> chunkReaders = indexReader.iterator();
            while ( chunkReaders.hasNext() )
            {
                try ( ChunkReader chunkReader = chunkReaders.next() )
                {
                    writer.println( "chunkName=" + chunkReader.getName() );
                    writer.println( "chunkVersion=" + chunkReader.getVersion() );
                    writer.println( "chunkPublished=" + chunkReader.getTimestamp() );
                    writer.println( "Chunk stats:" );
                    Map<Type, Integer> stats = countRecordsByType( chunkReader );
                    for ( Map.Entry<Type, Integer> entry : stats.entrySet() )
                    {
                        writer.println( entry.getKey() + " = " + entry.getValue() );
                    }
                    writer.println( "= = = = = =" );
                }
            }
        }
        finally
        {
            indexReader.close();
            remote.close();
            local.close();
        }
    }
}
