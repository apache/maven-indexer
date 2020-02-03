package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import org.apache.maven.index.reader.WritableResourceHandler;

/**
 * Wraps {@link WritableResourceHandler}s so that they return {@link WritableResource}s that return
 * {@link BufferedInputStream}s and {@link BufferedOutputStream}s.
 */
public class BufferedWritableResourceHandler implements WritableResourceHandler {
  private final WritableResourceHandler writableResourceHandler;

  public BufferedWritableResourceHandler(WritableResourceHandler writableResourceHandler) {
    this.writableResourceHandler = writableResourceHandler;
  }

  @Override
  public WritableResource locate(String name) throws IOException {
    return new BufferedWritableResource(writableResourceHandler.locate(name));
  }

  @Override
  public void close() throws IOException {
    writableResourceHandler.close();
  }
}
