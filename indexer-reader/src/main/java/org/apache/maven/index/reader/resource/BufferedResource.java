package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.maven.index.reader.ResourceHandler.Resource;

public class BufferedResource implements Resource {
  private final Resource resource;

  public BufferedResource(Resource resource) {
    this.resource = resource;
  }

  @Override
  public InputStream read() throws IOException {
    InputStream in = resource.read();
    if (in == null) {
      return null;
    }
    return new BufferedInputStream(in);
  }
}
