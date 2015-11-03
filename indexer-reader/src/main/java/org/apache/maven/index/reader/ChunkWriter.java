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
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Maven 2 Index published binary chunk writer, it writes raw Maven Indexer records to the transport binary format.
 *
 * @since 5.1.2
 */
public class ChunkWriter
    implements Closeable
{
  private static final int F_INDEXED = 1;

  private static final int F_TOKENIZED = 2;

  private static final int F_STORED = 4;

  private final String chunkName;

  private final DataOutputStream dataOutputStream;

  private final int version;

  private final Date timestamp;

  public ChunkWriter(final String chunkName, final OutputStream outputStream, final int version, final Date timestamp)
      throws IOException
  {
    this.chunkName = chunkName.trim();
    this.dataOutputStream = new DataOutputStream(new GZIPOutputStream(outputStream, 2 * 1024));
    this.version = version;
    this.timestamp = timestamp;

    dataOutputStream.writeByte(version);
    dataOutputStream.writeLong(timestamp == null ? -1 : timestamp.getTime());
  }

  /**
   * Returns the chunk name.
   */
  public String getName() {
    return chunkName;
  }

  /**
   * Returns index version. All releases so far always returned {@code 1}.
   */
  public int getVersion() {
    return version;
  }

  /**
   * Returns the index timestamp of last update of the index.
   */
  public Date getTimestamp() {
    return timestamp;
  }

  /**
   * Writes out the record iterator and returns the written record count.
   */
  public int writeChunk(final Iterator<Map<String, String>> iterator) throws IOException {
    int written = 0;
    while (iterator.hasNext()) {
      writeRecord(iterator.next(), dataOutputStream);
      written++;
    }
    return written;
  }

  /**
   * Closes this reader and it's underlying input.
   */
  public void close() throws IOException {
    dataOutputStream.close();
  }

  private static void writeRecord(final Map<String, String> record, final DataOutput dataOutput)
      throws IOException
  {
    dataOutput.writeInt(record.size());
    for (Map.Entry<String, String> entry : record.entrySet()) {
      writeField(entry.getKey(), entry.getValue(), dataOutput);
    }
  }

  private static void writeField(final String fieldName, final String fieldValue, final DataOutput dataOutput)
      throws IOException
  {
    boolean isIndexed = !(fieldName.equals("i") || fieldName.equals("m"));
    boolean isTokenized = !(fieldName.equals("i")
        || fieldName.equals("m")
        || fieldName.equals("1")
        || fieldName.equals("px"));
    int flags = (isIndexed ? F_INDEXED : 0) + (isTokenized ? F_TOKENIZED : 0) + F_STORED;
    dataOutput.writeByte(flags);
    dataOutput.writeUTF(fieldName);
    writeUTF(fieldValue, dataOutput);
  }

  private static void writeUTF(final String str, final DataOutput dataOutput)
      throws IOException
  {
    int strlen = str.length();
    int utflen = 0;
    int c;
    // use charAt instead of copying String to char array
    for (int i = 0; i < strlen; i++) {
      c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F)) {
        utflen++;
      }
      else if (c > 0x07FF) {
        utflen += 3;
      }
      else {
        utflen += 2;
      }
    }
    dataOutput.writeInt(utflen);
    byte[] bytearr = new byte[utflen];
    int count = 0;
    int i = 0;
    for (; i < strlen; i++) {
      c = str.charAt(i);
      if (!((c >= 0x0001) && (c <= 0x007F))) {
        break;
      }
      bytearr[count++] = (byte) c;
    }
    for (; i < strlen; i++) {
      c = str.charAt(i);
      if ((c >= 0x0001) && (c <= 0x007F)) {
        bytearr[count++] = (byte) c;

      }
      else if (c > 0x07FF) {
        bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
        bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
        bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
      }
      else {
        bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
        bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
      }
    }
    dataOutput.write(bytearr, 0, utflen);
  }
}
