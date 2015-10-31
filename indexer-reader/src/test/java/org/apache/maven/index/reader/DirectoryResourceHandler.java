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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A trivial {@link File} directory handler that does not perform any locking or extra bits, and just serves up files
 * by name from specified existing directory.
 */
public class DirectoryResourceHandler
    implements WritableResourceHandler
{
  private final File rootDirectory;

  public DirectoryResourceHandler(final File rootDirectory) {
    if (rootDirectory == null) {
      throw new NullPointerException("null rootDirectory");
    }
    if (!rootDirectory.isDirectory()) {
      throw new IllegalArgumentException("rootDirectory exists and is not a directory");
    }
    this.rootDirectory = rootDirectory;
  }

  public InputStream open(final String name) throws IOException {
    return new BufferedInputStream(new FileInputStream(new File(rootDirectory, name)));
  }

  public void save(final String name, final InputStream inputStream) throws IOException {
    try {
      final BufferedOutputStream outputStream = new BufferedOutputStream(
          new FileOutputStream(new File(rootDirectory, name)));
      int read;
      byte[] bytes = new byte[8192];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
      outputStream.close();
    }
    finally {
      inputStream.close();
    }
  }

  public void close() throws IOException {
    // nop
  }
}
