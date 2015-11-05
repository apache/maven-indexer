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
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

/**
 * A trivial caching {@link ResourceHandler} that caches forever during single session (existence of the instance).
 */
public class CachingResourceHandler
    implements ResourceHandler
{
  private static final Resource NOT_EXISTING_RESOURCE = new Resource()
  {
    public InputStream read() throws IOException {
      return null;
    }

    public void close() throws IOException {
      // nop
    }
  };

  private final WritableResourceHandler local;

  private final ResourceHandler remote;

  private final Set<String> notFoundResources;

  public CachingResourceHandler(final WritableResourceHandler local, final ResourceHandler remote) {
    if (local == null || remote == null) {
      throw new NullPointerException("null resource handler");
    }
    this.local = local;
    this.remote = remote;
    this.notFoundResources = new HashSet<String>();
  }

  public Resource locate(final String name) throws IOException {
    if (notFoundResources.contains(name)) {
      return NOT_EXISTING_RESOURCE;
    }
    else {
      return new CachingResource(name);
    }
  }

  private class CachingResource
      implements Resource
  {
    private final String name;

    private CachingResource(final String name) {
      this.name = name;
    }

    public InputStream read() throws IOException {
      InputStream inputStream = local.locate(name).read();
      if (inputStream != null) {
        return inputStream;
      }
      if (cacheLocally(name)) {
        return local.locate(name).read();
      }
      notFoundResources.add(name);
      return null;
    }

    private boolean cacheLocally(final String name) throws IOException {
      final Resource remoteResource = remote.locate(name);
      final WritableResource localResource = local.locate(name);
      try {
        final InputStream inputStream = remoteResource.read();
        if (inputStream != null) {
          final OutputStream outputStream = localResource.write();
          try {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
              outputStream.write(bytes, 0, read);
            }
            outputStream.flush();
            return true;
          }
          finally {
            outputStream.close();
            inputStream.close();
          }
        }
        return false;
      }
      finally {
        localResource.close();
      }
    }

    public void close() throws IOException {
      // nop
    }
  }

  public void close() throws IOException {
    remote.close();
    local.close();
  }
}
