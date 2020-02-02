package org.apache.maven.index.reader.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.apache.maven.index.reader.ResourceHandler.Resource;

public class UrlResource implements Resource {
  private final URL url;

  public UrlResource(URL url) {
    this.url = url;
  }

  @Override
  public InputStream read() throws IOException {
    try {
      return url.openStream();
    } catch (FileNotFoundException e) {
      return null;
    }
  }
}
