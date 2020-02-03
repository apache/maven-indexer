package org.apache.maven.index.reader.resource;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import org.apache.maven.index.reader.ResourceHandler;

/**
 * A {@link ResourceHandler} that represents the base of a {@link URI} hierarchy.
 */
public class UriResourceHandler implements ResourceHandler {
  private final URI uri;

  public UriResourceHandler(URI uri) {
    Objects.requireNonNull(uri, "uri cannot be null");
    this.uri = uri;
  }

  @Override
  public Resource locate(String name) throws IOException {
    return new UrlResource(uri.resolve(name).toURL());
  }

  @Override
  public void close() throws IOException {}
}
