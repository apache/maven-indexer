package org.apache.maven.index.reader.resource;

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
import java.io.IOException;
import java.io.OutputStream;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

/**
 * Wraps {@link WritableResource}s so that they return {@link BufferedInputStream}s and {@link
 * BufferedOutputStream}s.
 */
public class BufferedWritableResource extends BufferedResource implements WritableResource {
  private final WritableResource resource;

  public BufferedWritableResource(WritableResource resource) {
    super(resource);
    this.resource = resource;
  }

  @Override
  public OutputStream write() throws IOException {
    OutputStream out = resource.write();
    if (out == null) {
      return null;
    }
    return new BufferedOutputStream(out);
  }

  @Override
  public void close() throws IOException {
    resource.close();
  }
}
