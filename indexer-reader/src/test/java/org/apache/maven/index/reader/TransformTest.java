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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.maven.index.reader.Record.EntryKey;
import org.apache.maven.index.reader.Record.Type;
import org.junit.Test;

import static org.apache.maven.index.reader.TestUtils.compactFunction;
import static org.apache.maven.index.reader.TestUtils.decorate;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * UT for {@link RecordCompactor} and {@link RecordExpander}.
 */
public class TransformTest extends TestSupport {
    @Test
    public void decorateAndTransform() throws IOException {
        final String indexId = "test";
        final Record r1 = new Record(Type.ARTIFACT_ADD, artifactMap("org.apache"));
        final Record r2 = new Record(Type.ARTIFACT_ADD, artifactMap("org.foo"));
        final Record r3 = new Record(Type.ARTIFACT_ADD, artifactMap("com.bar"));

        Iterable<Map<String, String>> iterable = StreamSupport.stream(
                        decorate(Arrays.asList(r1, r2, r3), indexId).spliterator(), false)
                .map(compactFunction)
                .collect(Collectors.toList());

        try (WritableResourceHandler writableResourceHandler = createWritableResourceHandler()) {
            try (IndexWriter indexWriter = new IndexWriter(writableResourceHandler, indexId, false)) {
                indexWriter.writeChunk(iterable.iterator());
            }

            try (IndexReader indexReader = new IndexReader(null, writableResourceHandler)) {
                assertThat(indexReader.getChunkNames(), equalTo(List.of("nexus-maven-repository-index.gz")));
                ChunkReader chunkReader = indexReader.iterator().next();
                final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
                assertThat(recordTypes.get(Type.DESCRIPTOR).size(), equalTo(1));
                assertThat(recordTypes.get(Type.ROOT_GROUPS).size(), equalTo(1));
                assertThat(recordTypes.get(Type.ALL_GROUPS).size(), equalTo(1));
                assertThat(recordTypes.get(Type.ARTIFACT_ADD).size(), equalTo(3));
                assertThat(recordTypes.get(Type.ARTIFACT_REMOVE), nullValue());

                assertThat(
                        recordTypes.get(Type.ROOT_GROUPS).get(0).get(Record.ROOT_GROUPS),
                        equalTo(new String[] {"com", "org"}));
                assertThat(
                        recordTypes.get(Type.ALL_GROUPS).get(0).get(Record.ALL_GROUPS),
                        equalTo(new String[] {"com.bar", "org.apache", "org.foo"}));
            }
        }
    }

    private Map<EntryKey, Object> artifactMap(final String groupId) {
        final HashMap<EntryKey, Object> result = new HashMap<>();
        result.put(Record.GROUP_ID, groupId);
        result.put(Record.ARTIFACT_ID, "artifact");
        result.put(Record.VERSION, "1.0");
        result.put(Record.PACKAGING, "jar");
        result.put(Record.FILE_MODIFIED, System.currentTimeMillis());
        result.put(Record.FILE_SIZE, 123L);
        result.put(Record.FILE_EXTENSION, "jar");
        result.put(Record.HAS_SOURCES, Boolean.FALSE);
        result.put(Record.HAS_JAVADOC, Boolean.FALSE);
        result.put(Record.HAS_SIGNATURE, Boolean.FALSE);
        return result;
    }
}
