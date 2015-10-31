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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.maven.index.reader.Record.Type;

/**
 * Maven 2 Index published binary chunk reader.
 *
 * @since 5.1.2
 */
public class ChunkReader
    implements Closeable, Iterable<Record>
{
  private static final String FIELD_SEPARATOR = "|";

  private static final String NOT_AVAILABLE = "NA";

  private static final String UINFO = "u";

  private static final String INFO = "i";

  private static final Pattern FS_PATTERN = Pattern.compile(Pattern.quote(FIELD_SEPARATOR));

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
   * Returns index getVersion. All releases so far always returned {@code 1}.
   */
  public int getVersion() {
    return version;
  }

  /**
   * Returns the getTimestamp of last update of the index.
   */
  public Date getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the {@link Record} iterator.
   */
  public Iterator<Record> iterator() {
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
      implements Iterator<Record>
  {
    private final DataInputStream dataInputStream;

    private Record nextRecord;

    public IndexIterator(final DataInputStream dataInputStream) throws IOException {
      this.dataInputStream = dataInputStream;
      this.nextRecord = readRecord();
    }

    public boolean hasNext() {
      return nextRecord != null;
    }

    public Record next() {
      if (nextRecord == null) {
        throw new NoSuchElementException("chunk depleted");
      }
      Record result = nextRecord;
      try {
        nextRecord = readRecord();
        return result;
      }
      catch (IOException e) {
        throw new RuntimeException("read error", e);
      }
    }

    /**
     * Reads and returns next record from the underlying stream, or {@code null} if no more records.
     */
    private Record readRecord()
        throws IOException
    {
      int fieldCount;
      try {
        fieldCount = dataInputStream.readInt();
      }
      catch (EOFException ex) {
        return null; // no more documents
      }

      Map<String, String> recordMap = new HashMap<String, String>();
      for (int i = 0; i < fieldCount; i++) {
        readField(recordMap);
      }

      if (recordMap.containsKey("DESCRIPTOR")) {
        return new Record(Type.DESCRIPTOR, recordMap, expandDescriptor(recordMap));
      }
      else if (recordMap.containsKey("allGroups")) {
        return new Record(Type.ALL_GROUPS, recordMap, expandAllGroups(recordMap));
      }
      else if (recordMap.containsKey("rootGroups")) {
        return new Record(Type.ROOT_GROUPS, recordMap, expandRootGroups(recordMap));
      }
      else if (recordMap.containsKey("del")) {
        return new Record(Type.ARTIFACT_REMOVE, recordMap, expandDeletedArtifact(recordMap));
      }
      else {
        // Fix up UINFO field wrt MINDEXER-41
        final String uinfo = recordMap.get(UINFO);
        final String info = recordMap.get(INFO);
        if (uinfo != null && !(info == null || info.trim().length() == 0)) {
          final String[] splitInfo = FS_PATTERN.split(info);
          if (splitInfo.length > 6) {
            final String extension = splitInfo[6];
            if (uinfo.endsWith(FIELD_SEPARATOR + NOT_AVAILABLE)) {
              recordMap.put(UINFO, uinfo + FIELD_SEPARATOR + extension);
            }
          }
        }
        return new Record(Type.ARTIFACT_ADD, recordMap, expandAddedArtifact(recordMap));
      }
    }

    private void readField(final Map<String, String> record)
        throws IOException
    {
      dataInputStream.read(); // flags: neglect them
      String name = dataInputStream.readUTF();
      String value = readUTF();
      record.put(name, value);
    }

    private String readUTF()
        throws IOException
    {
      int utflen = dataInputStream.readInt();

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

      dataInputStream.readFully(bytearr, 0, utflen);

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

    private Map<String, Object> expandDescriptor(final Map<String, String> raw) {
      final Map<String, Object> result = new HashMap<String, Object>();
      String[] r = FS_PATTERN.split(raw.get("IDXINFO"));
      result.put(Record.REPOSITORY_ID, r[1]);
      return result;
    }

    private Map<String, Object> expandAllGroups(final Map<String, String> raw) {
      final Map<String, Object> result = new HashMap<String, Object>();
      putIfNotNullAsList(raw, Record.ALL_GROUPS_LIST, result, "allGroups");
      return result;
    }

    private Map<String, Object> expandRootGroups(final Map<String, String> raw) {
      final Map<String, Object> result = new HashMap<String, Object>();
      putIfNotNullAsList(raw, Record.ROOT_GROUPS_LIST, result, "rootGroups");
      return result;
    }

    private Map<String, Object> expandDeletedArtifact(final Map<String, String> raw) {
      final Map<String, Object> result = new HashMap<String, Object>();
      putIfNotNullTS(raw, "m", result, Record.REC_MODIFIED);
      if (raw.containsKey("del")) {
        expandUinfo(raw.get("del"), result);
      }
      return result;
    }

    /**
     * Expands the "encoded" Maven Indexer record by splitting the synthetic fields and applying expanded field naming.
     */
    private Map<String, Object> expandAddedArtifact(final Map<String, String> raw) {
      final Map<String, Object> result = new HashMap<String, Object>();

      // Minimal
      expandUinfo(raw.get(UINFO), result);
      final String info = raw.get(INFO);
      if (info != null) {
        String[] r = FS_PATTERN.split(info);
        result.put(Record.PACKAGING, renvl(r[0]));
        result.put(Record.FILE_MODIFIED, Long.valueOf(r[1]));
        result.put(Record.FILE_SIZE, Long.valueOf(r[2]));
        result.put(Record.HAS_SOURCES, "1".equals(r[3]) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        result.put(Record.HAS_JAVADOC, "1".equals(r[4]) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        result.put(Record.HAS_SIGNATURE, "1".equals(r[5]) ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
        if (r.length > 6) {
          result.put(Record.FILE_EXTENSION, r[6]);
        }
        else {
          final String packaging = raw.get(Record.PACKAGING);
          if (raw.get(Record.CLASSIFIER) != null
              || "pom".equals(packaging)
              || "war".equals(packaging)
              || "ear".equals(packaging)) {
            result.put(Record.FILE_EXTENSION, packaging);
          }
          else {
            result.put(Record.FILE_EXTENSION, "jar"); // best guess
          }
        }
      }
      putIfNotNullTS(raw, "m", result, Record.REC_MODIFIED);
      putIfNotNull(raw, "n", result, Record.NAME);
      putIfNotNull(raw, "d", result, Record.DESCRIPTION);
      putIfNotNull(raw, "1", result, Record.SHA1);

      // Jar file contents (optional)
      putIfNotNullAsList(raw, "classnames", result, Record.CLASSNAMES);

      // Maven Plugin (optional)
      putIfNotNull(raw, "px", result, Record.PLUGIN_PREFIX);
      putIfNotNullAsList(raw, "gx", result, Record.PLUGIN_GOALS);

      // OSGi (optional)
      putIfNotNull(raw, "Bundle-SymbolicName", result, "Bundle-SymbolicName");
      putIfNotNull(raw, "Bundle-Version", result, "Bundle-Version");
      putIfNotNull(raw, "Export-Package", result, "Export-Package");
      putIfNotNull(raw, "Export-Service", result, "Export-Service");
      putIfNotNull(raw, "Bundle-Description", result, "Bundle-Description");
      putIfNotNull(raw, "Bundle-Name", result, "Bundle-Name");
      putIfNotNull(raw, "Bundle-License", result, "Bundle-License");
      putIfNotNull(raw, "Bundle-DocURL", result, "Bundle-DocURL");
      putIfNotNull(raw, "Import-Package", result, "Import-Package");
      putIfNotNull(raw, "Require-Bundle", result, "Require-Bundle");
      putIfNotNull(raw, "Bundle-Version", result, "Bundle-Version");

      return result;
    }

    /**
     * Expands UINFO synthetic field. Handles {@code null} String inputs.
     */
    private void expandUinfo(final String uinfo, final Map<String, Object> result) {
      if (uinfo != null) {
        String[] r = FS_PATTERN.split(uinfo);
        result.put(Record.GROUP_ID, r[0]);
        result.put(Record.ARTIFACT_ID, r[1]);
        result.put(Record.VERSION, r[2]);
        String classifier = renvl(r[3]);
        if (classifier != null) {
          result.put(Record.CLASSIFIER, classifier);
          if (r.length > 4) {
            result.put(Record.FILE_EXTENSION, r[4]);
          }
        }
        else if (r.length > 4) {
          result.put(Record.PACKAGING, r[4]);
        }
      }
    }
  }

  /**
   * Helper to put a value from source map into target map, if not null.
   */
  private static void putIfNotNull(
      final Map<String, String> source,
      final String sourceName,
      final Map<String, Object> target,
      final String targetName)
  {
    String value = source.get(sourceName);
    if (value != null && value.trim().length() != 0) {
      target.put(targetName, value);
    }
  }

  /**
   * Helper to put a {@link Long} value from source map into target map, if not null.
   */
  private static void putIfNotNullTS(
      final Map<String, String> source,
      final String sourceName,
      final Map<String, Object> target,
      final String targetName)
  {
    String value = source.get(sourceName);
    if (value != null && value.trim().length() != 0) {
      target.put(targetName, Long.valueOf(value));
    }
  }

  /**
   * Helper to put a collection value from source map into target map as {@link java.util.List}, if not null.
   */
  private static void putIfNotNullAsList(
      final Map<String, String> source,
      final String sourceName,
      final Map<String, Object> target,
      final String targetName)
  {
    String value = source.get(sourceName);
    if (value != null && value.trim().length() != 0) {
      target.put(targetName, Arrays.asList(FS_PATTERN.split(value)));
    }
  }

  /**
   * Helper to translate the "NA" (not available) input into {@code null} value.
   */
  private static String renvl(final String v) {
    return NOT_AVAILABLE.equals(v) ? null : v;
  }
}
