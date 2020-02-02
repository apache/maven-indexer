package org.apache.maven.index.reader.resource;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.maven.index.reader.WritableResourceHandler;

public class PathWritableResourceHandler implements WritableResourceHandler {
  private final Path path;

  public PathWritableResourceHandler(Path path) {
    this.path = path;
  }

  @Override
  public WritableResource locate(String name) {
    return new PathWritableResource(path.resolve(name));
  }

  @Override
  public void close() throws IOException {}
}
