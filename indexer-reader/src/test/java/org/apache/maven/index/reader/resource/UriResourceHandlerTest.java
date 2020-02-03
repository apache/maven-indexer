package org.apache.maven.index.reader.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UriResourceHandlerTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void locate() throws IOException {
    Resource test = new UriResourceHandler(folder.getRoot().toURI())
        .locate("test.txt");
    assertNull(test.read());
    Files.write(folder.getRoot().toPath().resolve("test.txt"), new byte[]{'a'});
    try (InputStream in = test.read()) {
      assertEquals('a', in.read());
    }
  }
}