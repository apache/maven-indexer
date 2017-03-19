package org.apache.maven.index.reader;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

import static org.apache.maven.index.reader.Utils.loadProperties;
import static org.apache.maven.index.reader.Utils.storeProperties;

/**
 * Maven 2 Index writer that writes chunk and maintains published property file.
 * <p/>
 * <strong>Currently no incremental update is supported, as the deleteion states should be maintained by
 * caller</strong>. Hence, this writer will always produce the "main" chunk only.
 *
 * @since 5.1.2
 */
public class IndexWriter
    implements Closeable
{
  private static final int INDEX_V1 = 1;

  private final WritableResourceHandler local;

  private final Properties localIndexProperties;

  private final boolean incremental;

  private final String nextChunkCounter;

  private final String nextChunkName;

  public IndexWriter(final WritableResourceHandler local, final String indexId, final boolean incrementalSupported)
      throws IOException
  {
    if (local == null) {
      throw new NullPointerException("local resource handler null");
    }
    if (indexId == null) {
      throw new NullPointerException("indexId null");
    }
    this.local = local;
    Properties indexProperties = loadProperties(local.locate(Utils.INDEX_FILE_PREFIX + ".properties"));
    if (incrementalSupported && indexProperties != null) {
      this.localIndexProperties = indexProperties;
      // existing index, this is incremental publish, and we will add new chunk
      String localIndexId = localIndexProperties.getProperty("nexus.index.id");
      if (localIndexId == null || !localIndexId.equals(indexId)) {
        throw new IllegalArgumentException(
            "index already exists and indexId mismatch or unreadable: " + localIndexId + ", " +
                indexId);
      }
      this.incremental = true;
      this.nextChunkCounter = calculateNextChunkCounter();
      this.nextChunkName = Utils.INDEX_FILE_PREFIX + "." + nextChunkCounter + ".gz";
    }
    else {
      // non-existing index, create published index from scratch
      this.localIndexProperties = new Properties();
      this.localIndexProperties.setProperty("nexus.index.id", indexId);
      this.localIndexProperties.setProperty("nexus.index.chain-id", UUID.randomUUID().toString());
      this.incremental = false;
      this.nextChunkCounter = null;
      this.nextChunkName = Utils.INDEX_FILE_PREFIX + ".gz";
    }
  }

  /**
   * Returns the index context ID that published index has set.
   */
  public String getIndexId() {
    return localIndexProperties.getProperty("nexus.index.id");
  }

  /**
   * Returns the {@link Date} when index was last published or {@code null} if this is first publishing. In other
   * words,returns {@code null} when {@link #isIncremental()} returns {@code false}. After this writer is closed, the
   * return value is updated to "now" (in {@link #close() method}.
   */
  public Date getPublishedTimestamp() {
    try {
      String timestamp = localIndexProperties.getProperty("nexus.index.timestamp");
      if (timestamp != null) {
        return Utils.INDEX_DATE_FORMAT.parse(timestamp);
      }
      return null;
    }
    catch (ParseException e) {
      throw new RuntimeException("Corrupt date", e);
    }
  }

  /**
   * Returns {@code true} if incremental publish is about to happen.
   */
  public boolean isIncremental() {
    return incremental;
  }

  /**
   * Returns the chain id of published index. If {@link #isIncremental()} is {@code false}, this is the newly generated
   * chain ID.
   */
  public String getChainId() {
    return localIndexProperties.getProperty("nexus.index.chain-id");
  }

  /**
   * Returns the next chunk name about to be published.
   */
  public String getNextChunkName() {
    return nextChunkName;
  }

  /**
   * Writes out the record iterator and returns the written record count.
   */
  public int writeChunk(final Iterator<Map<String, String>> iterator) throws IOException {
    int written;
    WritableResource writableResource = local.locate(nextChunkName);
    try {
      final ChunkWriter chunkWriter = new ChunkWriter(nextChunkName, writableResource.write(), INDEX_V1, new Date());
      try {
        written = chunkWriter.writeChunk(iterator);
      }
      finally {
        chunkWriter.close();
      }
      if (incremental) {
        // TODO: update main gz file
      }
      return written;
    }
    finally {
      writableResource.close();
    }
  }

  /**
   * Closes the underlying {@link ResourceHandler} and synchronizes published index properties, so remote clients
   * becomes able to consume newly published index. If sync is not desired (ie. due to aborted publish), then this
   * method should NOT be invoked, but rather the {@link ResourceHandler} that caller provided in constructor of
   * this class should be closed manually.
   */
  public void close() throws IOException {
    try {
      if (incremental) {
        localIndexProperties.setProperty("nexus.index.last-incremental", nextChunkCounter);
      }
      localIndexProperties.setProperty("nexus.index.timestamp", Utils.INDEX_DATE_FORMAT.format(new Date()));
      storeProperties(local.locate(Utils.INDEX_FILE_PREFIX + ".properties"), localIndexProperties);
    }
    finally {
      local.close();
    }
  }

  /**
   * Calculates the chunk names that needs to be fetched.
   */
  private String calculateNextChunkCounter() {
    String lastChunkCounter = localIndexProperties.getProperty("nexus.index.last-incremental");
    if (lastChunkCounter != null) {
      return String.valueOf(Integer.parseInt(lastChunkCounter) + 1);
    }
    else {
      return "1";
    }
  }
}
