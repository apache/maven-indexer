package org.apache.maven.index.reader.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import org.apache.maven.index.reader.ResourceHandler.Resource;

/**
 * A {@link Resource} that represents a {@link URL}.
 */
public class UrlResource implements Resource {
  private final URL url;

  public UrlResource(URL url) {
    Objects.requireNonNull(url, "url cannot be null");
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
