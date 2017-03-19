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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.index.reader.Record.EntryKey;
import org.apache.maven.index.reader.Record.Type;

import static org.apache.maven.index.reader.Utils.FIELD_SEPARATOR;
import static org.apache.maven.index.reader.Utils.FS_PATTERN;
import static org.apache.maven.index.reader.Utils.INFO;
import static org.apache.maven.index.reader.Utils.NOT_AVAILABLE;
import static org.apache.maven.index.reader.Utils.UINFO;
import static org.apache.maven.index.reader.Utils.renvl;

/**
 * Maven 2 Index record transformer, that transforms "native" Maven Indexer records into {@link Record}s.
 *
 * @since 5.1.2
 */
public class RecordExpander
{
  /**
   * Expands MI low level record into {@link Record}.
   */
  public Record apply(final Map<String, String> recordMap) {
    if (recordMap.containsKey("DESCRIPTOR")) {
      return expandDescriptor(recordMap);
    }
    else if (recordMap.containsKey("allGroups")) {
      return expandAllGroups(recordMap);
    }
    else if (recordMap.containsKey("rootGroups")) {
      return expandRootGroups(recordMap);
    }
    else if (recordMap.containsKey("del")) {
      return expandDeletedArtifact(recordMap);
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
      return expandAddedArtifact(recordMap);
    }
  }

  private static Record expandDescriptor(final Map<String, String> raw) {
    final Record result = new Record(Type.DESCRIPTOR, new HashMap<EntryKey, Object>());
    String[] r = FS_PATTERN.split(raw.get("IDXINFO"));
    result.put(Record.REPOSITORY_ID, r[1]);
    return result;
  }

  private static Record expandAllGroups(final Map<String, String> raw) {
    final Record result = new Record(Type.ALL_GROUPS, new HashMap<EntryKey, Object>());
    putIfNotNullAsStringArray(raw, "allGroupsList", result, Record.ALL_GROUPS);
    return result;
  }

  private static Record expandRootGroups(final Map<String, String> raw) {
    final Record result = new Record(Type.ROOT_GROUPS, new HashMap<EntryKey, Object>());
    putIfNotNullAsStringArray(raw, "rootGroupsList", result, Record.ROOT_GROUPS);
    return result;
  }

  private static Record expandDeletedArtifact(final Map<String, String> raw) {
    final Record result = new Record(Type.ARTIFACT_REMOVE, new HashMap<EntryKey, Object>());
    putIfNotNullTS(raw, "m", result, Record.REC_MODIFIED);
    if (raw.containsKey("del")) {
      expandUinfo(raw.get("del"), result);
    }
    return result;
  }

  /**
   * Expands the "encoded" Maven Indexer record by splitting the synthetic fields and applying expanded field naming.
   */
  private static Record expandAddedArtifact(final Map<String, String> raw) {
    final Record result = new Record(Type.ARTIFACT_ADD, new HashMap<EntryKey, Object>());

    // Minimal
    expandUinfo(raw.get(UINFO), result);
    final String info = raw.get(INFO);
    if (info != null) {
      String[] r = FS_PATTERN.split(info);
      result.put(Record.PACKAGING, renvl(r[0]));
      result.put(Record.FILE_MODIFIED, Long.valueOf(r[1]));
      result.put(Record.FILE_SIZE, Long.valueOf(r[2]));
      result.put(Record.HAS_SOURCES, "1".equals(r[3]) ? Boolean.TRUE : Boolean.FALSE);
      result.put(Record.HAS_JAVADOC, "1".equals(r[4]) ? Boolean.TRUE : Boolean.FALSE);
      result.put(Record.HAS_SIGNATURE, "1".equals(r[5]) ? Boolean.TRUE : Boolean.FALSE);
      if (r.length > 6) {
        result.put(Record.FILE_EXTENSION, r[6]);
      }
      else {
        final String packaging = Record.PACKAGING.coerce(result.get(Record.PACKAGING));
        if (result.containsKey(Record.CLASSIFIER)
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
    putIfNotNullAsStringArray(raw, "classnames", result, Record.CLASSNAMES);

    // Maven Plugin (optional)
    putIfNotNull(raw, "px", result, Record.PLUGIN_PREFIX);
    putIfNotNullAsStringArray(raw, "gx", result, Record.PLUGIN_GOALS);

    // OSGi (optional)
    putIfNotNull(raw, "Bundle-SymbolicName", result, Record.OSGI_BUNDLE_SYMBOLIC_NAME);
    putIfNotNull(raw, "Bundle-Version", result, Record.OSGI_BUNDLE_VERSION);
    putIfNotNull(raw, "Export-Package", result, Record.OSGI_EXPORT_PACKAGE);
    putIfNotNull(raw, "Export-Service", result, Record.OSGI_EXPORT_SERVICE);
    putIfNotNull(raw, "Bundle-Description", result, Record.OSGI_BUNDLE_DESCRIPTION);
    putIfNotNull(raw, "Bundle-Name", result, Record.OSGI_BUNDLE_NAME);
    putIfNotNull(raw, "Bundle-License", result, Record.OSGI_BUNDLE_LICENSE);
    putIfNotNull(raw, "Bundle-DocURL", result, Record.OSGI_EXPORT_DOCURL);
    putIfNotNull(raw, "Import-Package", result, Record.OSGI_IMPORT_PACKAGE);
    putIfNotNull(raw, "Require-Bundle", result, Record.OSGI_REQUIRE_BUNDLE);

    return result;
  }

  /**
   * Expands UINFO synthetic field. Handles {@code null} String inputs.
   */
  private static void expandUinfo(final String uinfo, final Record result) {
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

  /**
   * Helper to put a value from source map into target map, if not null.
   */
  private static void putIfNotNull(
      final Map<String, String> source,
      final String sourceName,
      final Record target,
      final EntryKey targetName)
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
      final Record target,
      final EntryKey targetName)
  {
    String value = source.get(sourceName);
    if (value != null && value.trim().length() != 0) {
      target.put(targetName, Long.valueOf(value));
    }
  }

  /**
   * Helper to put a collection value from source map into target map as {@link java.util.List}, if not null.
   */
  private static void putIfNotNullAsStringArray(
      final Map<String, String> source,
      final String sourceName,
      final Record target,
      final EntryKey targetName)
  {
    String value = source.get(sourceName);
    if (value != null && value.trim().length() != 0) {
      target.put(targetName, FS_PATTERN.split(value));
    }
  }
}
