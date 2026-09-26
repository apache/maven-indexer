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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.maven.index.reader.Record.Type;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UT for {@link ChunkReader}
 */
class ChunkReaderTest extends TestSupport {
    @Test
    void simple() throws Exception {
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
    void roundtrip() throws Exception {
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
            assertEquals(published.getTime(), chunkReader.getTimestamp().getTime());
            final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
            assertEquals(1, recordTypes.get(Type.DESCRIPTOR).size());
            assertEquals(1, recordTypes.get(Type.ROOT_GROUPS).size());
            assertEquals(1, recordTypes.get(Type.ALL_GROUPS).size());
            assertEquals(2, recordTypes.get(Type.ARTIFACT_ADD).size());
            assertNull(recordTypes.get(Type.ARTIFACT_REMOVE));
        }
    }

    @Test
    void streamEndingEarlyIsAnError() throws Exception {
        byte[] data;
        try (WritableResourceHandler handler = testResourceHandler("simple")) {
            data = handler.locate("nexus-maven-repository-index.gz").read().readAllBytes();
        }
        byte[] truncated = Arrays.copyOf(data, data.length - 4);

        try (ChunkReader chunkReader = new ChunkReader("full", new ByteArrayInputStream(truncated))) {
            assertThrows(RuntimeException.class, () -> chunkReader.forEach(record -> {}));
        }
    }

    @Test
    void negativeValueLengthIsAnError() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(1); // version
            out.writeLong(0L); // timestamp
            out.writeInt(1); // field count
            out.writeByte(0); // flags
            out.writeUTF("name");
            out.writeInt(-1); // value length
        }

        try (ChunkReader chunkReader = new ChunkReader("full", new ByteArrayInputStream(bytes.toByteArray()))) {
            RuntimeException e = assertThrows(RuntimeException.class, () -> chunkReader.forEach(record -> {}));
            assertEquals(IOException.class, e.getCause().getClass());
        }
    }
}
