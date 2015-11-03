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
import java.io.OutputStream;

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

  public File getRootDirectory() {
    return rootDirectory;
  }

  public WritableResource locate(final String name) throws IOException {
    return new FileResource(new File(rootDirectory, name));
  }

  public void close() throws IOException {
    // nop
  }

  private class FileResource
      implements WritableResource
  {
    private final File file;

    private FileResource(final File file) {
      this.file = file;
    }

    public InputStream read() throws IOException {
      if (file.isFile()) {
        return new BufferedInputStream(new FileInputStream(file));
      }
      return null;
    }

    public OutputStream write() throws IOException {
      return new BufferedOutputStream(new FileOutputStream(file));
    }

    public void close() throws IOException {
      // nop
    }
  }

}
