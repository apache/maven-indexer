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
import java.io.IOException;
import java.util.Objects;
import org.apache.maven.index.reader.ResourceHandler;

/**
 * Wraps {@link ResourceHandler}s so that they return {@link Resource}s that return {@link
 * BufferedInputStream}s.
 */
public class BufferedResourceHandler implements ResourceHandler {
  private final ResourceHandler resourceHandler;

  public BufferedResourceHandler(ResourceHandler resourceHandler) {
    Objects.requireNonNull(resourceHandler, "resourceHandler cannot be null");
    this.resourceHandler = resourceHandler;
  }

  @Override
  public Resource locate(String name) throws IOException {
    return new BufferedResource(resourceHandler.locate(name));
  }

  @Override
  public void close() throws IOException {
    resourceHandler.close();
  }
}
