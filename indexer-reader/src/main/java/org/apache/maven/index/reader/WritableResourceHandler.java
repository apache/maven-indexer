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
import java.io.OutputStream;

/**
 * Maven 2 Index writable {@link ResourceHandler}, is capable of saving resources too. Needed only if incremental index
 * updates are wanted, to store the index state locally, and be able to calculate incremental diffs on next {@link
 * IndexReader} invocation.
 *
 * @see ResourceHandler
 * @since 5.1.2
 */
public interface WritableResourceHandler
    extends ResourceHandler
{
  /**
   * Returns the {@link OutputStream} of resource with {@code name}, never {@code null}. Closing the stream is the
   * responsibility of the caller. The stream should be buffered if possible.
   *
   * @param name Resource name, guaranteed to be non-{@code null} and is FS name and URL safe string.
   */
  OutputStream openWrite(final String name) throws IOException;
}
