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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
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
        for (Record record : Iterables.transform(chunkReader, new RecordExpander())) {
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
        for (Record record : Iterables.transform(chunkReader, new RecordExpander())) {
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
  @Ignore("Here for example but test depending on external resource is not nice thing to have")
  public void central() throws Exception {
    final File tempDir = File.createTempFile("index-reader", "tmp");
    tempDir.mkdirs();
    final Writer writer = new OutputStreamWriter(System.out);
    final IndexReader indexReader = new IndexReader(
        new DirectoryResourceHandler(tempDir),
        new HttpResourceHandler(new URL("http://repo1.maven.org/maven2/.index/"))
    );
    try {
      writer.write("indexRepoId=" + indexReader.getIndexId() + "\n");
      writer.write("indexLastPublished=" + indexReader.getPublishedTimestamp() + "\n");
      writer.write("isIncremental=" + indexReader.isIncremental() + "\n");
      writer.write("indexRequiredChunkNames=" + indexReader.getChunkNames() + "\n");
      for (ChunkReader chunkReader : indexReader) {
        writer.write("chunkName=" + chunkReader.getName() + "\n");
        writer.write("chunkVersion=" + chunkReader.getVersion() + "\n");
        writer.write("chunkPublished=" + chunkReader.getTimestamp() + "\n");
        writer.write("= = = = = = \n");
        for (Record record : Iterables.transform(chunkReader, new RecordExpander())) {
          writer.write(record.getExpanded() + "\n");
        }
      }
    }
    finally {
      indexReader.close();
      writer.close();
    }
  }
}
