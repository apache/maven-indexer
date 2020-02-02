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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UriResourceHandlerTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void locate() throws IOException {
    Resource test = new UriResourceHandler(folder.getRoot().toURI())
        .locate("test.txt");
    assertNull(test.read());
    Files.write(folder.getRoot().toPath().resolve("test.txt"), new byte[]{'a'});
    try (InputStream in = test.read()) {
      assertEquals('a', in.read());
    }
  }
}
