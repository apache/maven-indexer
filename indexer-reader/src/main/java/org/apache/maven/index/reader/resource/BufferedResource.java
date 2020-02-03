package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.maven.index.reader.ResourceHandler.Resource;

/**
 * Wraps {@link Resource}s so that they return {@link BufferedInputStream}s.
 */
public class BufferedResource implements Resource {
  private final Resource resource;

  public BufferedResource(Resource resource) {
    Objects.requireNonNull(resource, "resource cannot be null");
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
