package org.apache.maven.index.reader.resource;

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
