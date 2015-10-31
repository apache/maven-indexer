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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * UT for {@link ChunkReader}
 */
public class ChunkReaderTest
{
  @Test
  public void simple() throws IOException {
    final Map<Type, Integer> recordTypes = new HashMap<Type, Integer>();
    recordTypes.put(Type.DESCRIPTOR, 0);
    recordTypes.put(Type.ROOT_GROUPS, 0);
    recordTypes.put(Type.ALL_GROUPS, 0);
    recordTypes.put(Type.ARTIFACT_ADD, 0);
    recordTypes.put(Type.ARTIFACT_REMOVE, 0);

    final ChunkReader chunkReader = new ChunkReader("full",
        new FileInputStream("src/test/resources/nexus-maven-repository-index.gz"));
    try {
      assertThat(chunkReader.getVersion(), equalTo(1));
      assertThat(chunkReader.getTimestamp().getTime(), equalTo(1243533418015L));
      for (Record record : chunkReader) {
        recordTypes.put(record.getType(), recordTypes.get(record.getType()) + 1);
      }
    }
    finally {
      chunkReader.close();
    }

    assertThat(recordTypes.get(Type.DESCRIPTOR), equalTo(1));
    assertThat(recordTypes.get(Type.ROOT_GROUPS), equalTo(1));
    assertThat(recordTypes.get(Type.ALL_GROUPS), equalTo(1));
    assertThat(recordTypes.get(Type.ARTIFACT_ADD), equalTo(2));
    assertThat(recordTypes.get(Type.ARTIFACT_REMOVE), equalTo(0));
  }
}
