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
package org.apache.maven.index.reader;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import static org.apache.maven.index.reader.TestUtils.expandFunction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * UT for {@link IndexWriter}
 */
public class IndexWriterTest extends TestSupport {
    @Test
    public void roundtrip() throws IOException {
        try (WritableResourceHandler writableResourceHandler = createWritableResourceHandler()) {
            try (IndexReader indexReader = new IndexReader(null, testResourceHandler("simple"));
                    IndexWriter indexWriter =
                            new IndexWriter(writableResourceHandler, indexReader.getIndexId(), false)) {
                for (ChunkReader chunkReader : indexReader) {
                    try (chunkReader) {
                        indexWriter.writeChunk(chunkReader.iterator());
                    }
                }
            }

            // read what we wrote out
            try (IndexReader indexReader = new IndexReader(null, writableResourceHandler)) {
                assertThat(indexReader.getIndexId(), equalTo("apache-snapshots-local"));
                // assertThat(indexReader.getPublishedTimestamp().getTime(), equalTo(published.getTime()));
                assertThat(indexReader.isIncremental(), equalTo(false));
                assertThat(indexReader.getChunkNames(), equalTo(List.of("nexus-maven-repository-index.gz")));
                int chunks = 0;
                int records = 0;
                for (ChunkReader chunkReader : indexReader) {
                    chunks++;
                    assertThat(chunkReader.getName(), equalTo("nexus-maven-repository-index.gz"));
                    assertThat(chunkReader.getVersion(), equalTo(1));
                    // assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
                    records = (int) StreamSupport.stream(chunkReader.spliterator(), false)
                            .map(expandFunction)
                            .count();
                }

                assertThat(chunks, equalTo(1));
                assertThat(records, equalTo(5));
            }
        }
    }
}
