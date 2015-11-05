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
import java.io.InputStream;

/**
 * Maven 2 Index resource abstraction, that should be handled as a resource (is {@link Closeable}. That means, that
 * implementations could perform any extra activity as FS locking or so (if uses FS as backing store). Is used by single
 * thread only.
 *
 * @since 5.1.2
 */
public interface ResourceHandler
    extends Closeable
{
  interface Resource
  {
    /**
     * Returns the {@link InputStream} stream of the resource, if exists, {@code null} otherwise. The stream should
     * be closed by caller, otherwise resource leaks might be introduced.
     */
    InputStream read() throws IOException;
  }

  /**
   * Returns the {@link Resource} with {@code name}, non {@code null}.
   *
   * @param name Resource name, guaranteed to be non-{@code null} and is FS and URL safe string.
   */
  Resource locate(String name) throws IOException;
}
