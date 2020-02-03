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
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.maven.index.reader.ResourceHandler;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class BufferedResourceHandlerTest {
  private Mockery context = new Mockery();

  @Test
  public void locate() throws IOException {
    final Resource resource = context.mock(Resource.class);
    final ResourceHandler resourceHandler = context.mock(ResourceHandler.class);
    context.checking(new Expectations() {{
      oneOf(resource).read();
      will(returnValue(new ByteArrayInputStream(new byte[]{'a'})));
      oneOf(resourceHandler).locate("test.txt");
      will(returnValue(resource));
    }});
    InputStream in = new BufferedResourceHandler(resourceHandler).locate("test.txt").read();
    assertTrue(in instanceof BufferedInputStream);
    assertEquals('a', in.read());
    context.assertIsSatisfied();
  }

  @Test
  public void locateNull() throws IOException {
    final Resource resource = context.mock(Resource.class);
    final ResourceHandler resourceHandler = context.mock(ResourceHandler.class);
    context.checking(new Expectations() {{
      oneOf(resource).read();
      oneOf(resourceHandler).locate("test.txt");
      will(returnValue(resource));
    }});
    assertNull(new BufferedResourceHandler(resourceHandler).locate("test.txt").read());
    context.assertIsSatisfied();
  }

  @Test
  public void close() throws IOException {
    final ResourceHandler resourceHandler = context.mock(ResourceHandler.class);
    context.checking(new Expectations() {{
      oneOf(resourceHandler).close();
    }});
    new BufferedResourceHandler(resourceHandler).close();
    context.assertIsSatisfied();
  }
}
