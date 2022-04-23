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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class BufferedWritableResourceHandlerTest {
  private Mockery context = new Mockery();

  @Test
  public void locate() throws IOException {
    final WritableResource writableResource = context.mock(WritableResource.class);
    final WritableResourceHandler writableResourceHandler = context
        .mock(WritableResourceHandler.class);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    context.checking(new Expectations() {{
      oneOf(writableResource).write();
      will(returnValue(baos));
      oneOf(writableResourceHandler).locate("test.txt");
      will(returnValue(writableResource));
    }});
    OutputStream out = new BufferedWritableResourceHandler(writableResourceHandler)
        .locate("test.txt").write();
    assertTrue(out instanceof BufferedOutputStream);
    assertArrayEquals(new byte[]{}, baos.toByteArray());
    out.write('a');
    assertArrayEquals(new byte[]{}, baos.toByteArray());
    out.flush();
    assertArrayEquals(new byte[]{'a'}, baos.toByteArray());
    context.assertIsSatisfied();
  }

  @Test
  public void close() throws IOException {
    final WritableResourceHandler writableResourceHandler = context
        .mock(WritableResourceHandler.class);
    context.checking(new Expectations() {{
      oneOf(writableResourceHandler).close();
    }});
    new BufferedWritableResourceHandler(writableResourceHandler).close();
    context.assertIsSatisfied();
  }
}
