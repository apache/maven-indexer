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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * UT for {@link IndexWriter}
 */
public class IndexWriterTest
    extends TestSupport
{
  @Test
  public void roundtrip() throws IOException {
    IndexReader indexReader;
    IndexWriter indexWriter;
    WritableResourceHandler writableResourceHandler = createWritableResourceHandler();

    // write it once
    indexReader = new IndexReader(
        null,
        testResourceHandler("simple")
    );
    indexWriter = new IndexWriter(
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
      indexReader.close();
    }

    // read what we wrote out
    indexReader = new IndexReader(
        null,
        writableResourceHandler
    );
    try {
      assertThat(indexReader.getIndexId(), equalTo("apache-snapshots-local"));
      // assertThat(indexReader.getPublishedTimestamp().getTime(), equalTo(published.getTime()));
      assertThat(indexReader.isIncremental(), equalTo(false));
      assertThat(indexReader.getChunkNames(), equalTo(Arrays.asList("nexus-maven-repository-index.gz")));
      int chunks = 0;
      int records = 0;
      for (ChunkReader chunkReader : indexReader) {
        chunks++;
        assertThat(chunkReader.getName(), equalTo("nexus-maven-repository-index.gz"));
        assertThat(chunkReader.getVersion(), equalTo(1));
        // assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
        for (Record record : Transform.transform(chunkReader, new RecordExpander())) {
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
}
