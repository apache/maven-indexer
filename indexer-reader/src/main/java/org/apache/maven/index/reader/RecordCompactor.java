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

import org.apache.maven.index.reader.Transform.Function;
import org.apache.maven.index.reader.Record.Type;

import static org.apache.maven.index.reader.Utils.FIELD_SEPARATOR;
import static org.apache.maven.index.reader.Utils.INFO;
import static org.apache.maven.index.reader.Utils.UINFO;
import static org.apache.maven.index.reader.Utils.nvl;

/**
 * Maven 2 Index record transformer, that transforms {@link Record}s into "native" Maven Indexer records.
 *
 * @since 5.1.2
 */
public class RecordCompactor
    implements Function<Record, Map<String, String>>
{
  public Map<String, String> apply(final Record record) {
    if (Type.DESCRIPTOR == record.getType()) {
      return compactDescriptor(record);
    }
    else if (Type.ALL_GROUPS == record.getType()) {
      return compactAllGroups(record);
    }
    else if (Type.ROOT_GROUPS == record.getType()) {
      return compactRootGroups(record);
    }
    else if (Type.ARTIFACT_REMOVE == record.getType()) {
      return compactDeletedArtifact(record);
    }
    else if (Type.ARTIFACT_ADD == record.getType()) {
      return compactAddedArtifact(record);
    }
    else {
      throw new IllegalArgumentException("Unknown record: " + record);
    }
  }

  private static Map<String, String> compactDescriptor(final Record record) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put("DESCRIPTOR", "NexusIndex");
    result.put("IDXINFO", "1.0|" + record.get(Record.REPOSITORY_ID));
    return result;
  }

  private static Map<String, String> compactAllGroups(final Record record) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put("allGroups", "allGroups");
    putIfNotNullAsStringArray(record.get(Record.ALL_GROUPS), result, "allGroupsList");
    return result;
  }

  private static Map<String, String> compactRootGroups(final Record record) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put("rootGroups", "allGroups");
    putIfNotNullAsStringArray(record.get(Record.ROOT_GROUPS), result, "rootGroupsList");
    return result;
  }

  private static Map<String, String> compactDeletedArtifact(final Record record) {
    final Map<String, String> result = new HashMap<String, String>();
    putIfNotNullTS(record.get(Record.REC_MODIFIED), result, "m");
    result.put("del", compactUinfo(record));
    return result;
  }

  /**
   * Expands the "encoded" Maven Indexer record by splitting the synthetic fields and applying expanded field naming.
   */
  private static Map<String, String> compactAddedArtifact(final Record record) {
    final Map<String, String> result = new HashMap<String, String>();

    // Minimal
    result.put(UINFO, compactUinfo(record));

    StringBuilder info = new StringBuilder();
    info.append(nvl(record.get(Record.PACKAGING)));
    info.append(FIELD_SEPARATOR);
    info.append(record.get(Record.FILE_MODIFIED));
    info.append(FIELD_SEPARATOR);
    info.append(record.get(Record.FILE_SIZE));
    info.append(FIELD_SEPARATOR);
    info.append(record.get(Record.HAS_SOURCES) ? "1" : "0");
    info.append(FIELD_SEPARATOR);
    info.append(record.get(Record.HAS_JAVADOC) ? "1" : "0");
    info.append(FIELD_SEPARATOR);
    info.append(record.get(Record.HAS_SIGNATURE) ? "1" : "0");
    info.append(FIELD_SEPARATOR);
    info.append(nvl(record.get(Record.FILE_EXTENSION)));
    result.put(INFO, info.toString());

    putIfNotNullTS(record.get(Record.REC_MODIFIED), result, "m");
    putIfNotNull(record.get(Record.NAME), result, "n");
    putIfNotNull(record.get(Record.DESCRIPTION), result, "d");
    putIfNotNull(record.get(Record.SHA1), result, "1");

    // Jar file contents (optional)
    putIfNotNullAsStringArray(record.get(Record.CLASSNAMES), result, "classnames");

    // Maven Plugin (optional)
    putIfNotNull(record.get(Record.PLUGIN_PREFIX), result, "px");
    putIfNotNullAsStringArray(record.get(Record.PLUGIN_GOALS), result, "gx");

    // OSGi (optional)
    putIfNotNull(record.get(Record.OSGI_BUNDLE_SYMBOLIC_NAME), result, "Bundle-SymbolicName");
    putIfNotNull(record.get(Record.OSGI_BUNDLE_VERSION), result, "Bundle-Version");
    putIfNotNull(record.get(Record.OSGI_EXPORT_PACKAGE), result, "Export-Package");
    putIfNotNull(record.get(Record.OSGI_EXPORT_SERVICE), result, "Export-Service");
    putIfNotNull(record.get(Record.OSGI_BUNDLE_DESCRIPTION), result, "Bundle-Description");
    putIfNotNull(record.get(Record.OSGI_BUNDLE_NAME), result, "Bundle-Name");
    putIfNotNull(record.get(Record.OSGI_BUNDLE_LICENSE), result, "Bundle-License");
    putIfNotNull(record.get(Record.OSGI_EXPORT_DOCURL), result, "Bundle-DocURL");
    putIfNotNull(record.get(Record.OSGI_IMPORT_PACKAGE), result, "Import-Package");
    putIfNotNull(record.get(Record.OSGI_REQUIRE_BUNDLE), result, "Require-Bundle");

    return result;
  }

  /**
   * Creates UINFO synthetic field.
   */
  private static String compactUinfo(final Record record) {
    final String classifier = record.get(Record.CLASSIFIER);
    StringBuilder sb = new StringBuilder();
    sb.append(record.get(Record.GROUP_ID))
        .append(FIELD_SEPARATOR)
        .append(record.get(Record.ARTIFACT_ID))
        .append(FIELD_SEPARATOR)
        .append(record.get(Record.VERSION))
        .append(FIELD_SEPARATOR)
        .append(nvl(classifier));
    if (classifier != null) {
      sb.append(record.get(Record.FILE_EXTENSION));
    }
    return sb.toString();
  }

  /**
   * Helper to put a value from source map into target map, if not null.
   */
  private static void putIfNotNull(
      final String source,
      final Map<String, String> target,
      final String targetName)
  {
    if (source != null) {
      target.put(targetName, source);
    }
  }

  /**
   * Helper to put a {@link Long} value from source map into target map, if not null.
   */
  private static void putIfNotNullTS(
      final Long source,
      final Map<String, String> target,
      final String targetName)
  {
    if (source != null) {
      target.put(targetName, String.valueOf(source));
    }
  }

  /**
   * Helper to put a array value from source map into target map joined with {@link Utils#FIELD_SEPARATOR}, if not
   * null.
   */
  private static void putIfNotNullAsStringArray(
      final String[] source,
      final Map<String, String> target,
      final String targetName)
  {
    if (source != null && source.length > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append(source[0]);
      for (int i = 1; i < source.length; i++) {
        sb.append(FIELD_SEPARATOR).append(source[i]);
      }
      target.put(targetName, sb.toString());
    }
  }
}
