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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UT for {@link ChunkReader}
 */
public class ChunkReaderTest extends TestSupport {
    @Test
    public void simple() throws IOException {
        try (WritableResourceHandler handler = testResourceHandler("simple");
                ChunkReader chunkReader = new ChunkReader(
                        "full",
                        handler.locate("nexus-maven-repository-index.gz").read())) {
            final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
            assertEquals(1, recordTypes.get(Type.DESCRIPTOR).size());
            assertEquals(1, recordTypes.get(Type.ROOT_GROUPS).size());
            assertEquals(1, recordTypes.get(Type.ALL_GROUPS).size());
            assertEquals(2, recordTypes.get(Type.ARTIFACT_ADD).size());
            assertNull(recordTypes.get(Type.ARTIFACT_REMOVE));
        }
    }

    @Test
    public void roundtrip() throws IOException {
        final Date published;
        Path tempChunkFile = createTempFile("nexus-maven-repository-index.gz");
        {
            try (WritableResourceHandler resource = testResourceHandler("simple");
                    ChunkReader chunkReader = new ChunkReader(
                            "full",
                            resource.locate("nexus-maven-repository-index.gz").read());
                    ChunkWriter chunkWriter = new ChunkWriter(
                            chunkReader.getName(), Files.newOutputStream(tempChunkFile), 1, new Date())) {
                chunkWriter.writeChunk(chunkReader.iterator());
                published = chunkWriter.getTimestamp();
            }
        }

        try (ChunkReader chunkReader = new ChunkReader("full", Files.newInputStream(tempChunkFile))) {
            assertEquals(1, chunkReader.getVersion());
            assertEquals(chunkReader.getTimestamp().getTime(), published.getTime());
            final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
            assertEquals(1, recordTypes.get(Type.DESCRIPTOR).size());
            assertEquals(1, recordTypes.get(Type.ROOT_GROUPS).size());
            assertEquals(1, recordTypes.get(Type.ALL_GROUPS).size());
            assertEquals(2, recordTypes.get(Type.ARTIFACT_ADD).size());
            assertNull(recordTypes.get(Type.ARTIFACT_REMOVE));
        }
    }
}
