package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

/**
 * Wraps {@link WritableResource}s so that they return {@link BufferedInputStream}s and {@link
 * BufferedOutputStream}s.
 */
public class BufferedWritableResource extends BufferedResource implements WritableResource {
  private final WritableResource resource;

  public BufferedWritableResource(WritableResource resource) {
    super(resource);
    this.resource = resource;
  }

  @Override
  public OutputStream write() throws IOException {
    OutputStream out = resource.write();
    if (out == null) {
      return null;
    }
    return new BufferedOutputStream(out);
  }

  @Override
  public void close() throws IOException {
    resource.close();
  }
}
