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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

/**
 * Maven 2 Index published binary chunk reader, it reads raw Maven Indexer records from the transport binary format.
 *
 * @since 5.1.2
 */
public class ChunkReader
    implements Closeable, Iterable<Map<String, String>>
{
  private final String chunkName;

  private final DataInputStream dataInputStream;

  private final int version;

  private final Date timestamp;

  public ChunkReader(final String chunkName, final InputStream inputStream) throws IOException
  {
    this.chunkName = chunkName.trim();
    this.dataInputStream = new DataInputStream(new GZIPInputStream(inputStream, 2 * 1024));
    this.version = ((int) dataInputStream.readByte()) & 0xff;
    this.timestamp = new Date(dataInputStream.readLong());
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
   * Returns the {@link Record} iterator.
   */
  public Iterator<Map<String, String>> iterator() {
    try {
      return new IndexIterator(dataInputStream);
    }
    catch (IOException e) {
      throw new RuntimeException("error", e);
    }
  }

  /**
   * Closes this reader and it's underlying input.
   */
  public void close() throws IOException {
    dataInputStream.close();
  }

  /**
   * Low memory footprint index iterator that incrementally parses the underlying stream.
   */
  private static class IndexIterator
      implements Iterator<Map<String, String>>
  {
    private final DataInputStream dataInputStream;

    private Map<String, String> nextRecord;

    public IndexIterator(final DataInputStream dataInputStream) throws IOException {
      this.dataInputStream = dataInputStream;
      this.nextRecord = nextRecord();
    }

    public boolean hasNext() {
      return nextRecord != null;
    }

    public Map<String, String> next() {
      if (nextRecord == null) {
        throw new NoSuchElementException("chunk depleted");
      }
      Map<String, String> result = nextRecord;
      nextRecord = nextRecord();
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException("remove");
    }

    private Map<String, String> nextRecord() {
      try {
        return readRecord(dataInputStream);
      }
      catch (IOException e) {
        throw new RuntimeException("read error", e);
      }
    }
  }

  /**
   * Reads and returns next record from the underlying stream, or {@code null} if no more records.
   */
  private static Map<String, String> readRecord(final DataInput dataInput)
      throws IOException
  {
    int fieldCount;
    try {
      fieldCount = dataInput.readInt();
    }
    catch (EOFException ex) {
      return null; // no more documents
    }

    Map<String, String> recordMap = new HashMap<String, String>();
    for (int i = 0; i < fieldCount; i++) {
      readField(recordMap, dataInput);
    }
    return recordMap;
  }

  private static void readField(final Map<String, String> record, final DataInput dataInput)
      throws IOException
  {
    dataInput.readByte(); // flags: neglect them
    String name = dataInput.readUTF();
    String value = readUTF(dataInput);
    record.put(name, value);
  }

  private static String readUTF(final DataInput dataInput)
      throws IOException
  {
    int utflen = dataInput.readInt();

    byte[] bytearr;
    char[] chararr;

    try {
      bytearr = new byte[utflen];
      chararr = new char[utflen];
    }
    catch (OutOfMemoryError e) {
      IOException ioex = new IOException("Index data content is corrupt");
      ioex.initCause(e);
      throw ioex;
    }

    int c, char2, char3;
    int count = 0;
    int chararr_count = 0;

    dataInput.readFully(bytearr, 0, utflen);

    while (count < utflen) {
      c = bytearr[count] & 0xff;
      if (c > 127) {
        break;
      }
      count++;
      chararr[chararr_count++] = (char) c;
    }

    while (count < utflen) {
      c = bytearr[count] & 0xff;
      switch (c >> 4) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
                    /* 0xxxxxxx */
          count++;
          chararr[chararr_count++] = (char) c;
          break;

        case 12:
        case 13:
                    /* 110x xxxx 10xx xxxx */
          count += 2;
          if (count > utflen) {
            throw new UTFDataFormatException("malformed input: partial character at end");
          }
          char2 = bytearr[count - 1];
          if ((char2 & 0xC0) != 0x80) {
            throw new UTFDataFormatException("malformed input around byte " + count);
          }
          chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
          break;

        case 14:
                    /* 1110 xxxx 10xx xxxx 10xx xxxx */
          count += 3;
          if (count > utflen) {
            throw new UTFDataFormatException("malformed input: partial character at end");
          }
          char2 = bytearr[count - 2];
          char3 = bytearr[count - 1];
          if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
            throw new UTFDataFormatException("malformed input around byte " + (count - 1));
          }
          chararr[chararr_count++] =
              (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F));
          break;

        default:
                    /* 10xx xxxx, 1111 xxxx */
          throw new UTFDataFormatException("malformed input around byte " + count);
      }
    }

    // The number of chars produced may be less than utflen
    return new String(chararr, 0, chararr_count);
  }
}
