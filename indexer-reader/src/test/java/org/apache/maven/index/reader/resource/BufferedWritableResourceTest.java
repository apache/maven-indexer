package org.apache.maven.index.reader.resource;

import java.io.IOException;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

public class BufferedWritableResourceTest {
  private Mockery context = new Mockery();

  @Test
  public void close() throws IOException {
    final WritableResource resourceHandler = context.mock(WritableResource.class);
    context.checking(new Expectations() {{
      oneOf(resourceHandler).close();
    }});
    new BufferedWritableResource(resourceHandler).close();
    context.assertIsSatisfied();
  }
}