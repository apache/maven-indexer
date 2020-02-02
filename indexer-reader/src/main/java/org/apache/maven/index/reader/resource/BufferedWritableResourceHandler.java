package org.apache.maven.index.reader.resource;

import java.io.IOException;
import org.apache.maven.index.reader.WritableResourceHandler;

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
