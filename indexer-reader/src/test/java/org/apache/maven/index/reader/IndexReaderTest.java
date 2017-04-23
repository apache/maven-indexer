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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.maven.index.reader.TestUtils.expandFunction;
import static com.google.common.collect.Iterables.transform;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * UT for {@link IndexReader}
 */
public class IndexReaderTest
    extends TestSupport
{
  @Test
  public void simple() throws IOException {
    final IndexReader indexReader = new IndexReader(
        null,
        testResourceHandler("simple")
    );
    try {
      assertThat(indexReader.getIndexId(), equalTo("apache-snapshots-local"));
      assertThat(indexReader.getPublishedTimestamp().getTime(), equalTo(1243533418015L));
      assertThat(indexReader.isIncremental(), equalTo(false));
      assertThat(indexReader.getChunkNames(), equalTo(Arrays.asList("nexus-maven-repository-index.gz")));
      int chunks = 0;
      int records = 0;
      for (ChunkReader chunkReader : indexReader) {
        chunks++;
        assertThat(chunkReader.getName(), equalTo("nexus-maven-repository-index.gz"));
        assertThat(chunkReader.getVersion(), equalTo(1));
        assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
        for (Record record : transform(chunkReader, expandFunction)) {
          records++;
        }
      }

      assertThat(chunks, equalTo(1));
      assertThat(records, equalTo(5));
    }
    finally {
      indexReader.close();
    }
  }

  @Test
  public void simpleWithLocal() throws IOException {
    WritableResourceHandler writableResourceHandler = createWritableResourceHandler();
    final IndexReader indexReader = new IndexReader(
        writableResourceHandler,
        testResourceHandler("simple")
    );
    try {
      assertThat(indexReader.getIndexId(), equalTo("apache-snapshots-local"));
      assertThat(indexReader.getPublishedTimestamp().getTime(), equalTo(1243533418015L));
      assertThat(indexReader.isIncremental(), equalTo(false));
      assertThat(indexReader.getChunkNames(), equalTo(Arrays.asList("nexus-maven-repository-index.gz")));
      int chunks = 0;
      int records = 0;
      for (ChunkReader chunkReader : indexReader) {
        chunks++;
        assertThat(chunkReader.getName(), equalTo("nexus-maven-repository-index.gz"));
        assertThat(chunkReader.getVersion(), equalTo(1));
        assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
        for (Record record : transform(chunkReader, expandFunction)) {
          records++;
        }
      }

      assertThat(chunks, equalTo(1));
      assertThat(records, equalTo(5));
    }
    finally {
      indexReader.close();
    }

    assertThat(writableResourceHandler.locate("nexus-maven-repository-index.properties").read(), not(nullValue()));
  }

  @Test
  public void roundtrip() throws IOException {
    WritableResourceHandler writableResourceHandler = createWritableResourceHandler();
    Date published;
    {
      final IndexReader indexReader = new IndexReader(
          null,
          testResourceHandler("simple")
      );
      final IndexWriter indexWriter = new IndexWriter(
          writableResourceHandler,
          indexReader.getIndexId(),
          false
      );
      try {
        for (ChunkReader chunkReader : indexReader) {
          indexWriter.writeChunk(chunkReader.iterator());
        }
      }
      finally {
        indexWriter.close();
        published = indexWriter.getPublishedTimestamp();
        indexReader.close();
      }
    }

    final IndexReader indexReader = new IndexReader(
        null,
        writableResourceHandler
    );
    try {
      assertThat(indexReader.getIndexId(), equalTo("apache-snapshots-local"));
      assertThat(indexReader.getPublishedTimestamp().getTime(), equalTo(published.getTime()));
      assertThat(indexReader.isIncremental(), equalTo(false));
      assertThat(indexReader.getChunkNames(), equalTo(Arrays.asList("nexus-maven-repository-index.gz")));
      int chunks = 0;
      int records = 0;
      for (ChunkReader chunkReader : indexReader) {
        chunks++;
        assertThat(chunkReader.getName(), equalTo("nexus-maven-repository-index.gz"));
        assertThat(chunkReader.getVersion(), equalTo(1));
        // assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
        for (Record record : transform(chunkReader, expandFunction)) {
          records++;
        }
      }

      assertThat(chunks, equalTo(1));
      assertThat(records, equalTo(5));
    }
    finally {
      indexReader.close();
    }
  }

  /**
   * This UT is here for demonstration purposes only. Bashing Central is not something you want to do, and risk your
   * IP address being banned. You were warned!
   */
  @Test
  @Ignore("For eyes only")
  public void central() throws Exception {
    // local index location, against which we perform incremental updates
    final File indexDir = createTempDirectory();
    // cache of remote, to not rely on HTTP transport possible failures, or, to detect them early
    final File cacheDir = createTempDirectory();

    final PrintWriter writer = new PrintWriter(System.out, true);
    final WritableResourceHandler local = new DirectoryResourceHandler(indexDir);
    final CachingResourceHandler remote = new CachingResourceHandler(
        new DirectoryResourceHandler(cacheDir),
        new HttpResourceHandler(new URL("http://repo1.maven.org/maven2/.index/"))
    );
    final IndexReader indexReader = new IndexReader(local, remote);
    try {
      writer.println("indexRepoId=" + indexReader.getIndexId());
      writer.println("indexLastPublished=" + indexReader.getPublishedTimestamp());
      writer.println("isIncremental=" + indexReader.isIncremental());
      writer.println("indexRequiredChunkNames=" + indexReader.getChunkNames());
      for (ChunkReader chunkReader : indexReader) {
        writer.println("chunkName=" + chunkReader.getName());
        writer.println("chunkVersion=" + chunkReader.getVersion());
        writer.println("chunkPublished=" + chunkReader.getTimestamp());
        writer.println("Chunk stats:");
        Map<Type, Integer> stats = countRecordsByType(chunkReader);
        for (Map.Entry<Type, Integer> entry : stats.entrySet()) {
          writer.println(entry.getKey() + " = " + entry.getValue());
        }
        writer.println("= = = = = =");
      }
    }
    finally {
      indexReader.close();
      remote.close();
      local.close();
    }
  }
}
