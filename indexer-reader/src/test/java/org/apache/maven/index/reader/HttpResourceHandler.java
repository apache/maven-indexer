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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A trivial HTTP {@link ResourceHandler} that uses {@link URL} to fetch remote content. This implementation does not
 * handle any advanced cases, like redirects, authentication, etc.
 */
public class HttpResourceHandler
    implements ResourceHandler
{
  private final URI root;

  public HttpResourceHandler(final URL root) throws URISyntaxException {
    if (root == null) {
      throw new NullPointerException("root URL null");
    }
    this.root = root.toURI();
  }

  public Resource locate(final String name) throws IOException {
    return new HttpResource(name);
  }

  private class HttpResource
      implements Resource
  {
    private final String name;

    private HttpResource(final String name) {
      this.name = name;
    }

    public InputStream read() throws IOException {
      URL target = root.resolve(name).toURL();
      HttpURLConnection conn = (HttpURLConnection) target.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", "ASF Maven-Indexer-Reader/1.0");
      return new BufferedInputStream(conn.getInputStream());
    }

    public void close() throws IOException {
      // nop
    }
  }

  public void close() throws IOException {
    // nop
  }
}
