package org.apache.maven.index.reader.resource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathWritableResourceHandlerTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void locate() throws IOException {
    WritableResource test = new PathWritableResourceHandler(folder.getRoot().toPath())
        .locate("test.txt");
    assertNull(test.read());
    try (OutputStream out = test.write()) {
      out.write('a');
    }
    try (InputStream in = test.read()) {
      assertEquals('a', in.read());
    }
    assertArrayEquals(new byte[]{'a'},
        Files.readAllBytes(folder.getRoot().toPath().resolve("test.txt")));
  }
}
