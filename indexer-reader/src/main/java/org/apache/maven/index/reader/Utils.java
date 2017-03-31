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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.maven.index.reader.Record.EntryKey;
import org.apache.maven.index.reader.Record.Type;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;

/**
 * Reusable code snippets and constants.
 *
 * @since 5.1.2
 */
public final class Utils
{
  private Utils() {
    // nothing
  }

  public static final String INDEX_FILE_PREFIX = "nexus-maven-repository-index";

  public static final DateFormat INDEX_DATE_FORMAT;

  static {
    INDEX_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss.SSS Z");
    INDEX_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public static final String FIELD_SEPARATOR = "|";

  public static final String NOT_AVAILABLE = "NA";

  public static final String UINFO = "u";

  public static final String INFO = "i";

  public static final Pattern FS_PATTERN = Pattern.compile(Pattern.quote(FIELD_SEPARATOR));

  /**
   * Creates and loads {@link Properties} from provided {@link InputStream} and closes the stream.
   */
  public static Properties loadProperties(final InputStream inputStream) throws IOException {
    try {
      final Properties properties = new Properties();
      properties.load(inputStream);
      return properties;
    }
    finally {
      inputStream.close();
    }
  }

  /**
   * Creates and loads {@link Properties} from provided {@link Resource} if exists, and closes the resource. If not
   * exists, returns {@code null}.
   */
  public static Properties loadProperties(final Resource resource) throws IOException {
    final InputStream inputStream = resource.read();
    if (inputStream == null) {
      return null;
    }
    return loadProperties(resource.read());
  }

  /**
   * Saves {@link Properties} to provided {@link OutputStream} and closes the stream.
   */
  public static void storeProperties(final OutputStream outputStream, final Properties properties) throws IOException {
    try {
      properties.store(outputStream, "Maven Indexer Writer");
    }
    finally {
      outputStream.close();
    }
  }


  /**
   * Saves {@link Properties} to provided {@link WritableResource} and closes the resource.
   */
  public static void storeProperties(final WritableResource writableResource, final Properties properties) throws IOException {
    try {
      storeProperties(writableResource.write(), properties);
    }
    finally {
      writableResource.close();
    }
  }

  /**
   * Creates a record of type {@link Type#DESCRIPTOR}.
   */
  public static Record descriptor(final String repoId) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.REPOSITORY_ID, repoId);
    return new Record(Type.DESCRIPTOR, entries);
  }

  /**
   * Creates a record of type {@link Type#ALL_GROUPS}.
   */
  public static Record allGroups(final Collection<String> allGroups) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.ALL_GROUPS, allGroups.toArray(new String[allGroups.size()]));
    return new Record(Type.ALL_GROUPS, entries);
  }

  /**
   * Creates a record of type {@link Type#ROOT_GROUPS}.
   */
  public static Record rootGroups(final Collection<String> rootGroups) {
    HashMap<EntryKey, Object> entries = new HashMap<EntryKey, Object>();
    entries.put(Record.ROOT_GROUPS, rootGroups.toArray(new String[rootGroups.size()]));
    return new Record(Type.ROOT_GROUPS, entries);
  }

  /**
   * Helper to translate the "NA" (not available) input into {@code null} value.
   */
  public static String renvl(final String v) {
    return NOT_AVAILABLE.equals(v) ? null : v;
  }

  /**
   * Helper to translate {@code null} into "NA" (not available) value.
   */
  public static String nvl(final String v) {
    return v == null ? NOT_AVAILABLE : v;
  }

  /**
   * Returns the "root group" of given groupId.
   */
  public static String rootGroup(final String groupId) {
    int n = groupId.indexOf('.');
    if (n > -1) {
      return groupId.substring(0, n);
    }
    return groupId;
  }
}
