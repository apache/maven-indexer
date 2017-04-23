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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * UT for {@link ChunkReader}
 */
public class ChunkReaderTest
    extends TestSupport
{
  @Test
  public void simple() throws IOException {
    final ChunkReader chunkReader = new ChunkReader(
        "full",
        testResourceHandler("simple").locate("nexus-maven-repository-index.gz").read()
    );
    final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
    assertThat(recordTypes.get(Type.DESCRIPTOR).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ROOT_GROUPS).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ALL_GROUPS).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ARTIFACT_ADD).size(), equalTo(2));
    assertThat(recordTypes.get(Type.ARTIFACT_REMOVE), nullValue());
  }

  @Test
  public void roundtrip() throws IOException {
    final Date published;
    File tempChunkFile = createTempFile("nexus-maven-repository-index.gz");
    {
      final Resource resource = testResourceHandler("simple").locate("nexus-maven-repository-index.gz");
      final ChunkReader chunkReader = new ChunkReader(
          "full",
          resource.read()
      );
      final ChunkWriter chunkWriter = new ChunkWriter(
          chunkReader.getName(),
          new FileOutputStream(tempChunkFile), 1, new Date()
      );
      try {
        chunkWriter.writeChunk(chunkReader.iterator());
      }
      finally {
        chunkWriter.close();
        chunkReader.close();
      }
      published = chunkWriter.getTimestamp();
    }

    final ChunkReader chunkReader = new ChunkReader(
        "full",
        new FileInputStream(tempChunkFile)
    );
    assertThat(chunkReader.getVersion(), equalTo(1));
    assertThat(chunkReader.getTimestamp().getTime(), equalTo(published.getTime()));
    final Map<Type, List<Record>> recordTypes = loadRecordsByType(chunkReader);
    assertThat(recordTypes.get(Type.DESCRIPTOR).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ROOT_GROUPS).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ALL_GROUPS).size(), equalTo(1));
    assertThat(recordTypes.get(Type.ARTIFACT_ADD).size(), equalTo(2));
    assertThat(recordTypes.get(Type.ARTIFACT_REMOVE), nullValue());
  }
}
