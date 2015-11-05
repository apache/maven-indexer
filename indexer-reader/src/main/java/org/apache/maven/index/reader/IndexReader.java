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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.index.reader.ResourceHandler.Resource;

import static org.apache.maven.index.reader.Utils.loadProperties;
import static org.apache.maven.index.reader.Utils.storeProperties;

/**
 * Maven 2 Index reader that handles incremental updates if possible and provides one or more {@link ChunkReader}s, to
 * read all the required records.
 *
 * @since 5.1.2
 */
public class IndexReader
    implements Iterable<ChunkReader>, Closeable
{
  private final WritableResourceHandler local;

  private final ResourceHandler remote;

  private final Properties localIndexProperties;

  private final Properties remoteIndexProperties;

  private final String indexId;

  private final Date publishedTimestamp;

  private final boolean incremental;

  private final List<String> chunkNames;

  public IndexReader(final WritableResourceHandler local, final ResourceHandler remote) throws IOException {
    if (remote == null) {
      throw new NullPointerException("remote resource handler null");
    }
    this.local = local;
    this.remote = remote;
    remoteIndexProperties = loadProperties(remote.locate(Utils.INDEX_FILE_PREFIX + ".properties"));
    if (remoteIndexProperties == null) {
      throw new IllegalArgumentException("Non-existent remote index");
    }
    try {
      if (local != null) {
        Properties localProperties = loadProperties(local.locate(Utils.INDEX_FILE_PREFIX + ".properties"));
        if (localProperties != null) {
          this.localIndexProperties = localProperties;
          String remoteIndexId = remoteIndexProperties.getProperty("nexus.index.id");
          String localIndexId = localIndexProperties.getProperty("nexus.index.id");
          if (remoteIndexId == null || localIndexId == null || !remoteIndexId.equals(localIndexId)) {
            throw new IllegalArgumentException(
                "local and remote index IDs does not match or is null: " + localIndexId + ", " +
                    remoteIndexId);
          }
          this.indexId = localIndexId;
          this.incremental = canRetrieveAllChunks();
        }
        else {
          localIndexProperties = null;
          this.indexId = remoteIndexProperties.getProperty("nexus.index.id");
          this.incremental = false;
        }
      }
      else {
        localIndexProperties = null;
        this.indexId = remoteIndexProperties.getProperty("nexus.index.id");
        this.incremental = false;
      }
      this.publishedTimestamp = Utils.INDEX_DATE_FORMAT.parse(remoteIndexProperties.getProperty("nexus.index.timestamp"));
      this.chunkNames = calculateChunkNames();
    }
    catch (ParseException e) {
      IOException ex = new IOException("Index properties corrupted");
      ex.initCause(e);
      throw ex;
    }
  }

  /**
   * Returns the index context ID that published index has set. Usually it is equal to "repository ID" used in {@link
   * Record.Type#DESCRIPTOR} but does not have to be.
   */
  public String getIndexId() {
    return indexId;
  }

  /**
   * Returns the {@link Date} when remote index was last published.
   */
  public Date getPublishedTimestamp() {
    return publishedTimestamp;
  }

  /**
   * Returns {@code true} if incremental update is about to happen. If incremental update, the {@link #iterator()} will
   * return only the diff from the last update.
   */
  public boolean isIncremental() {
    return incremental;
  }

  /**
   * Returns unmodifiable list of actual chunks that needs to be pulled from remote {@link ResourceHandler}. Those are
   * incremental chunks or the big main file, depending on result of {@link #isIncremental()}. Empty list means local
   * index is up to date, and {@link #iterator()} will return empty iterator.
   */
  public List<String> getChunkNames() {
    return chunkNames;
  }

  /**
   * Closes the underlying {@link ResourceHandler}s. In case of incremental update use, it also assumes that user
   * consumed all the iterator and integrated it, hence, it will update the {@link WritableResourceHandler} contents to
   * prepare it for future incremental update. If this is not desired (ie. due to aborted update), then this method
   * should NOT be invoked, but rather the {@link ResourceHandler}s that caller provided in constructor of
   * this class should be closed manually.
   */
  public void close() throws IOException {
    remote.close();
    if (local != null) {
      try {
        syncLocalWithRemote();
      }
      finally {
        local.close();
      }
    }
  }

  /**
   * Returns an {@link Iterator} of {@link ChunkReader}s, that if read in sequence, provide all the (incremental)
   * updates from the index. It is caller responsibility to either consume fully this iterator, or to close current
   * {@link ChunkReader} if aborting.
   */
  public Iterator<ChunkReader> iterator() {
    return new ChunkReaderIterator(remote, chunkNames.iterator());
  }

  /**
   * Stores the remote index properties into local index properties, preparing local {@link WritableResourceHandler}
   * for future incremental updates.
   */
  private void syncLocalWithRemote() throws IOException {
    storeProperties(local.locate(Utils.INDEX_FILE_PREFIX + ".properties"), remoteIndexProperties);
  }

  /**
   * Calculates the chunk names that needs to be fetched.
   */
  private List<String> calculateChunkNames() {
    if (incremental) {
      ArrayList<String> chunkNames = new ArrayList<String>();
      int maxCounter = Integer.parseInt(remoteIndexProperties.getProperty("nexus.index.last-incremental"));
      int currentCounter = Integer.parseInt(localIndexProperties.getProperty("nexus.index.last-incremental"));
      currentCounter++;
      while (currentCounter <= maxCounter) {
        chunkNames.add(Utils.INDEX_FILE_PREFIX + "." + currentCounter++ + ".gz");
      }
      return Collections.unmodifiableList(chunkNames);
    }
    else {
      return Collections.singletonList(Utils.INDEX_FILE_PREFIX + ".gz");
    }
  }

  /**
   * Verifies incremental update is possible, as all the diff chunks we need are still enlisted in remote properties.
   */
  private boolean canRetrieveAllChunks()
  {
    String localChainId = localIndexProperties.getProperty("nexus.index.chain-id");
    String remoteChainId = remoteIndexProperties.getProperty("nexus.index.chain-id");

    // If no chain id, or not the same, do full update
    if (localChainId == null || remoteChainId == null || !localChainId.equals(remoteChainId)) {
      return false;
    }

    try {
      int localLastIncremental = Integer.parseInt(localIndexProperties.getProperty("nexus.index.last-incremental"));
      String currentLocalCounter = String.valueOf(localLastIncremental);
      String nextLocalCounter = String.valueOf(localLastIncremental + 1);
      // check remote props for existence of current or next chunk after local
      for (Object key : remoteIndexProperties.keySet()) {
        String sKey = (String) key;
        if (sKey.startsWith("nexus.index.incremental-")) {
          String value = remoteIndexProperties.getProperty(sKey);
          if (currentLocalCounter.equals(value) || nextLocalCounter.equals(value)) {
            return true;
          }
        }
      }
    }
    catch (NumberFormatException e) {
      // fall through
    }
    return false;
  }

  /**
   * Internal iterator implementation that lazily opens and closes the returned {@link ChunkReader}s as this iterator
   * is being consumed.
   */
  private static class ChunkReaderIterator
      implements Iterator<ChunkReader>
  {
    private final ResourceHandler resourceHandler;

    private final Iterator<String> chunkNamesIterator;

    private Resource currentResource;

    private ChunkReader currentChunkReader;

    private ChunkReaderIterator(final ResourceHandler resourceHandler, final Iterator<String> chunkNamesIterator) {
      this.resourceHandler = resourceHandler;
      this.chunkNamesIterator = chunkNamesIterator;
    }

    public boolean hasNext() {
      return chunkNamesIterator.hasNext();
    }

    public ChunkReader next() {
      String chunkName = chunkNamesIterator.next();
      try {
        if (currentChunkReader != null) {
          currentChunkReader.close();
        }
        currentResource = resourceHandler.locate(chunkName);
        currentChunkReader = new ChunkReader(chunkName, currentResource.read());
        return currentChunkReader;
      }
      catch (IOException e) {
        throw new RuntimeException("IO problem while switching chunk readers", e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }
  }
}
