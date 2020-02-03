package org.apache.maven.index.reader.resource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.maven.index.reader.WritableResourceHandler;

/**
 * A {@link WritableResourceHandler} that represents the base of a {@link Path} hierarchy.
 */
public class PathWritableResourceHandler implements WritableResourceHandler {
  private final Path path;

  public PathWritableResourceHandler(Path path) {
    Objects.requireNonNull(path, "path cannot be null");
    this.path = path;
  }

  @Override
  public WritableResource locate(String name) {
    return new PathWritableResource(path.resolve(name));
  }

  @Override
  public void close() throws IOException {}
}
