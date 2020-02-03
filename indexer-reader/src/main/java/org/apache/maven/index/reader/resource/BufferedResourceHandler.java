package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.IOException;
import org.apache.maven.index.reader.ResourceHandler;

/**
 * Wraps {@link ResourceHandler}s so that they return {@link Resource}s that return {@link
 * BufferedInputStream}s.
 */
public class BufferedResourceHandler implements ResourceHandler {
  private final ResourceHandler resourceHandler;

  public BufferedResourceHandler(ResourceHandler resourceHandler) {
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
