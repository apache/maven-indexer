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

import java.io.IOException;
import java.io.InputStream;

/**
 * A trivial caching {@link ResourceHandler} that caches forever during single session (existence of the instance).
 */
public class CachingResourceHandler
    implements ResourceHandler
{
  private final DirectoryResourceHandler local;

  private final ResourceHandler remote;

  public CachingResourceHandler(final DirectoryResourceHandler local, final ResourceHandler remote) {
    if (local == null || remote == null) {
      throw new NullPointerException("null resource handler");
    }
    this.local = local;
    this.remote = remote;
  }

  public InputStream open(final String name) throws IOException {
    InputStream inputStream = local.open(name);
    if (inputStream != null) {
      return inputStream;
    }
    inputStream = remote.open(name);
    if (inputStream == null) {
      return null;
    }
    local.save(name, inputStream);
    return local.open(name);
  }

  public void close() throws IOException {
    remote.close();
    local.close();
  }
}
