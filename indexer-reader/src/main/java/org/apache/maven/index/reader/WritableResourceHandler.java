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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Maven 2 Index writable {@link ResourceHandler}, is capable of saving resources too. Needed only if incremental index
 * updates are wanted, to store the index state locally, and be able to calculate incremental diffs on next {@link
 * IndexReader} invocation. Is used by single thread only.
 *
 * @see ResourceHandler
 * @since 5.1.2
 */
public interface WritableResourceHandler
    extends ResourceHandler
{
  interface WritableResource
      extends Resource, Closeable
  {
    /**
     * Returns the {@link OutputStream} stream of the resource, if exists, it will replace the existing content, or if
     * not exists, the resource will be created. The stream should be closed by caller, otherwise resource leaks might
     * be introduced. How and when content is written is left to implementation, but it is guaranteed that this method
     * is called only once, and will be followed by {@link #close()} on the resource itself. Implementation does not
     * have to be "read consistent", in a way to worry what subsequent {@link #read()} method will return, as mixed
     * calls will not happen on same instance of resource.
     */
    OutputStream write() throws IOException;
  }

  /**
   * Returns the {@link WritableResource} with {@code name}. Returned locator should be handled as
   * resource, is {@link Closeable}.
   *
   * @param name Resource name, guaranteed to be non-{@code null} and is FS and URL safe string.
   */
  WritableResource locate(String name) throws IOException;
}
