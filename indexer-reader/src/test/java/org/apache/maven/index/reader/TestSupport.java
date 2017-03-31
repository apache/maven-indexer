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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test support.
 */
public class TestSupport
{
  @Rule
  public TestName testName = new TestName();

  private File tempDir;

  private List<DirectoryResourceHandler> directoryResourceHandlers;

  /**
   * Creates the temp directory and list for resource handlers.
   */
  @Before
  public void setup() throws IOException {
    this.tempDir = new File("target/tmp-" + getClass().getSimpleName());
    this.tempDir.delete();
    this.tempDir.mkdirs();
    this.directoryResourceHandlers = new ArrayList<DirectoryResourceHandler>();
  }

  /**
   * Closes all the registered resources handlers and deletes the temp directory.
   */
  @After
  public void cleanup() throws IOException {
    for (DirectoryResourceHandler directoryResourceHandler : directoryResourceHandlers) {
      directoryResourceHandler.close();
    }
    // delete(tempDir);
  }

  /**
   * Creates a temp file within {@link #tempDir}.
   */
  protected File createTempFile() throws IOException {
    return File.createTempFile(testName.getMethodName() + "-file", "", tempDir);
  }


  /**
   * Creates a temp file within {@link #tempDir} with given name.
   */
  protected File createTempFile(final String name) throws IOException {
    return new File(tempDir, name);
  }

  /**
   * Creates a temp directory within {@link #tempDir}.
   */
  protected File createTempDirectory() throws IOException {
    File result = File.createTempFile(testName.getMethodName() + "-dir", "", tempDir);
    result.delete();
    result.mkdirs();
    return result;
  }

  /**
   * Creates an empty {@link DirectoryResourceHandler}.
   */
  protected WritableResourceHandler createWritableResourceHandler() throws IOException {
    DirectoryResourceHandler result = new DirectoryResourceHandler(createTempDirectory());
    directoryResourceHandlers.add(result);
    return result;
  }

  /**
   * Creates a "test" {@link ResourceHandler} that contains predefined files, is mapped to test resources under given
   * name.
   */
  protected ResourceHandler testResourceHandler(final String name) throws IOException {
    DirectoryResourceHandler result = new DirectoryResourceHandler(new File("src/test/resources/" + name));
    directoryResourceHandlers.add(result);
    return result;
  }

  /**
   * Consumes {@link ChunkReader} and creates a map "by type" with records.
   */
  protected Map<Type, List<Record>> loadRecordsByType(final ChunkReader chunkReader) throws IOException {
    HashMap<Type, List<Record>> stat = new HashMap<Type, List<Record>>();
    try {
      assertThat(chunkReader.getVersion(), equalTo(1));
      final RecordExpander recordExpander = new RecordExpander();
      for (Map<String, String> rec : chunkReader) {
        final Record record = recordExpander.apply(rec);
        if (!stat.containsKey(record.getType())) {
          stat.put(record.getType(), new ArrayList<Record>());
        }
        stat.get(record.getType()).add(record);
      }
    }
    finally {
      chunkReader.close();
    }
    return stat;
  }


  /**
   * Consumes {@link ChunkReader} and creates a map "by type" with record type counts.
   */
  protected Map<Type, Integer> countRecordsByType(final ChunkReader chunkReader) throws IOException {
    HashMap<Type, Integer> stat = new HashMap<Type, Integer>();
    try {
      assertThat(chunkReader.getVersion(), equalTo(1));
      final RecordExpander recordExpander = new RecordExpander();
      for (Map<String, String> rec : chunkReader) {
        final Record record = recordExpander.apply(rec);
        if (!stat.containsKey(record.getType())) {
          stat.put(record.getType(), 0);
        }
        stat.put(record.getType(), stat.get(record.getType()) + 1);
      }
    }
    finally {
      chunkReader.close();
    }
    return stat;
  }

  /**
   * Delete recursively.
   */
  private static boolean delete(final File file) {
    if (file == null) {
      return false;
    }
    if (!file.exists()) {
      return true;
    }
    if (file.isDirectory()) {
      String[] list = file.list();
      if (list != null) {
        for (int i = 0; i < list.length; i++) {
          File entry = new File(file, list[i]);
          if (!delete(entry)) {
            return false;
          }
        }
      }
    }
    return file.delete();
  }
}
