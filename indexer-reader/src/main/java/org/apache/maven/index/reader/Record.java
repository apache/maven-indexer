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

import java.util.Map;

/**
 * Maven 2 Index record.
 *
 * @since 5.1.2
 */
public final class Record
{
  public static final class EntryKey<T>
  {
    private final String name;

    private final Class<T> proto;

    public EntryKey(final String name, final Class<T> proto) {
      if (name == null) {
        throw new NullPointerException("name is null");
      }
      if (proto == null) {
        throw new NullPointerException("proto is null");
      }
      this.name = name;
      this.proto = proto;
    }

    public T coerce(final Object object) {
      return (T) proto.cast(object);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof EntryKey)) {
        return false;
      }
      EntryKey entryKey = (EntryKey) o;
      return name.equals(entryKey.name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "Key{" +
          "name='" + name + '\'' +
          ", type=" + proto.getSimpleName() +
          '}';
    }
  }

  /**
   * Key of repository ID entry, that contains {@link String}.
   */
  public static final EntryKey<String> REPOSITORY_ID = new EntryKey<String>("repositoryId", String.class);

  /**
   * Key of all groups list entry, that contains {@link java.util.List<String>}.
   */
  public static final EntryKey<String[]> ALL_GROUPS = new EntryKey<String[]>("allGroups", String[].class);

  /**
   * Key of root groups list entry, that contains {@link java.util.List<String>}.
   */
  public static final EntryKey<String[]> ROOT_GROUPS = new EntryKey<String[]>("rootGroups", String[].class);

  /**
   * Key of index record modification (added to index or removed from index) timestamp entry, that contains {@link
   * Long}.
   */
  public static final EntryKey<Long> REC_MODIFIED = new EntryKey<Long>("recordModified", Long.class);

  /**
   * Key of artifact groupId entry, that contains {@link String}.
   */
  public static final EntryKey<String> GROUP_ID = new EntryKey<String>("groupId", String.class);

  /**
   * Key of artifact artifactId entry, that contains {@link String}.
   */
  public static final EntryKey<String> ARTIFACT_ID = new EntryKey<String>("artifactId", String.class);

  /**
   * Key of artifact version entry, that contains {@link String}.
   */
  public static final EntryKey<String> VERSION = new EntryKey<String>("version", String.class);

  /**
   * Key of artifact classifier entry, that contains {@link String}.
   */
  public static final EntryKey<String> CLASSIFIER = new EntryKey<String>("classifier", String.class);

  /**
   * Key of artifact packaging entry, that contains {@link String}.
   */
  public static final EntryKey<String> PACKAGING = new EntryKey<String>("packaging", String.class);

  /**
   * Key of artifact file extension, that contains {@link String}.
   */
  public static final EntryKey<String> FILE_EXTENSION = new EntryKey<String>("fileExtension", String.class);

  /**
   * Key of artifact file last modified timestamp, that contains {@link Long}.
   */
  public static final EntryKey<Long> FILE_MODIFIED = new EntryKey<Long>("fileModified", Long.class);

  /**
   * Key of artifact file size in bytes, that contains {@link Long}.
   */
  public static final EntryKey<Long> FILE_SIZE = new EntryKey<Long>("fileSize", Long.class);

  /**
   * Key of artifact Sources presence flag, that contains {@link Boolean}.
   */
  public static final EntryKey<Boolean> HAS_SOURCES = new EntryKey<Boolean>("hasSources", Boolean.class);

  /**
   * Key of artifact Javadoc presence flag, that contains {@link Boolean}.
   */
  public static final EntryKey<Boolean> HAS_JAVADOC = new EntryKey<Boolean>("hasJavadoc", Boolean.class);

  /**
   * Key of artifact signature presence flag, that contains {@link Boolean}.
   */
  public static final EntryKey<Boolean> HAS_SIGNATURE = new EntryKey<Boolean>("hasSignature", Boolean.class);

  /**
   * Key of artifact name (as set in POM), that contains {@link String}.
   */
  public static final EntryKey<String> NAME = new EntryKey<String>("name", String.class);

  /**
   * Key of artifact description (as set in POM), that contains {@link String}.
   */
  public static final EntryKey<String> DESCRIPTION = new EntryKey<String>("description", String.class);

  /**
   * Key of artifact SHA1 digest, that contains {@link String}.
   */
  public static final EntryKey<String> SHA1 = new EntryKey<String>("sha1", String.class);

  /**
   * Key of artifact contained class names, that contains {@link java.util.List<String>}. Extracted by {@code
   * JarFileContentsIndexCreator}.
   */
  public static final EntryKey<String[]> CLASSNAMES = new EntryKey<String[]>("classNames", String[].class);

  /**
   * Key of plugin artifact prefix, that contains {@link String}. Extracted by {@code
   * MavenPluginArtifactInfoIndexCreator}.
   */
  public static final EntryKey<String> PLUGIN_PREFIX = new EntryKey<String>("pluginPrefix", String.class);

  /**
   * Key of plugin artifact goals, that contains {@link java.util.List<String>}. Extracted by {@code
   * MavenPluginArtifactInfoIndexCreator}.
   */
  public static final EntryKey<String[]> PLUGIN_GOALS = new EntryKey<String[]>("pluginGoals", String[].class);

  /**
   * Key of OSGi "Bundle-SymbolicName" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_BUNDLE_SYMBOLIC_NAME = new EntryKey<String>("Bundle-SymbolicName",
      String.class);

  /**
   * Key of OSGi "Bundle-Version" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_BUNDLE_VERSION = new EntryKey<String>("Bundle-Version", String.class);

  /**
   * Key of OSGi "Export-Package" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_EXPORT_PACKAGE = new EntryKey<String>("Export-Package", String.class);

  /**
   * Key of OSGi "Export-Service" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_EXPORT_SERVICE = new EntryKey<String>("Export-Service", String.class);

  /**
   * Key of OSGi "Bundle-Description" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_BUNDLE_DESCRIPTION = new EntryKey<String>("Bundle-Description",
      String.class);

  /**
   * Key of OSGi "Bundle-Name" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_BUNDLE_NAME = new EntryKey<String>("Bundle-Name", String.class);

  /**
   * Key of OSGi "Bundle-License" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_BUNDLE_LICENSE = new EntryKey<String>("Bundle-License", String.class);

  /**
   * Key of OSGi "Bundle-DocURL" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_EXPORT_DOCURL = new EntryKey<String>("Bundle-DocURL", String.class);

  /**
   * Key of OSGi "Import-Package" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_IMPORT_PACKAGE = new EntryKey<String>("Import-Package", String.class);

  /**
   * Key of OSGi "Require-Bundle" manifest entry, that contains {@link String}. Extracted by {@code
   * OsgiArtifactIndexCreator}.
   */
  public static final EntryKey<String> OSGI_REQUIRE_BUNDLE = new EntryKey<String>("Require-Bundle", String.class);

  /**
   * Types of returned records returned from index.
   */
  public enum Type
  {
    /**
     * Descriptor record. Can be safely ignored.
     * Contains following entries:
     * <ul>
     * <li>{@link #REPOSITORY_ID}</li>
     * </ul>
     */
    DESCRIPTOR,

    /**
     * Artifact ADD record. Records of this type should be added to your indexing system.
     * Contains following entries:
     * <ul>
     * <li>{@link #REC_MODIFIED} (when record was added/modified on index)</li>
     * <li>{@link #GROUP_ID}</li>
     * <li>{@link #ARTIFACT_ID}</li>
     * <li>{@link #VERSION}</li>
     * <li>{@link #CLASSIFIER} (optional)</li>
     * <li>{@link #FILE_EXTENSION}</li>
     * <li>{@link #FILE_MODIFIED}</li>
     * <li>{@link #FILE_SIZE}</li>
     * <li>{@link #PACKAGING}</li>
     * <li>{@link #HAS_SOURCES}</li>
     * <li>{@link #HAS_JAVADOC}</li>
     * <li>{@link #HAS_SIGNATURE}</li>
     * <li>{@link #NAME}</li>
     * <li>{@link #DESCRIPTION}</li>
     * <li>{@link #SHA1}</li>
     * <li>{@link #CLASSNAMES} (optional)</li>
     * <li>{@link #PLUGIN_PREFIX} (optional, for maven-plugins only)</li>
     * <li>{@link #PLUGIN_GOALS} (optional, for maven-plugins only)</li>
     * </ul>
     */
    ARTIFACT_ADD,

    /**
     * Artifact REMOVE record. In case of incremental updates, signals that this artifact was removed. Records of this
     * type should be removed from your indexing system.
     * Contains following entries:
     * <ul>
     * <li>{@link #REC_MODIFIED} (when record was deleted from index)</li>
     * <li>{@link #GROUP_ID}</li>
     * <li>{@link #ARTIFACT_ID}</li>
     * <li>{@link #VERSION}</li>
     * <li>{@link #CLASSIFIER} (optional)</li>
     * <li>{@link #FILE_EXTENSION} (if {@link #CLASSIFIER} present)</li>
     * <li>{@link #PACKAGING} (optional)</li>
     * </ul>
     */
    ARTIFACT_REMOVE,

    /**
     * Special record, containing all the Maven "groupId"s that are enlisted on the index. Can be safely ignored.
     * Contains following entries:
     * <ul>
     * <li>{@link #ALL_GROUPS}</li>
     * </ul>
     */
    ALL_GROUPS,

    /**
     * Special record, containing all the root groups of Maven "groupId"s that are enlisted on the index. Can be safely
     * ignored.
     * Contains following entries:
     * <ul>
     * <li>{@link #ROOT_GROUPS}</li>
     * </ul>
     */
    ROOT_GROUPS
  }

  private final Type type;

  private final Map<EntryKey, Object> expanded;

  public Record(final Type type, final Map<EntryKey, Object> expanded) {
    this.type = type;
    this.expanded = expanded;
  }

  /**
   * Returns the {@link Type} of this record. Usually users would be interested in {@link Type#ARTIFACT_ADD} and {@link
   * Type#ARTIFACT_REMOVE} types only to maintain their own index. Still, indexer offers extra records too, see {@link
   * Type} for all existing types.
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the expanded (processed and expanded synthetic fields) record as {@link Map} ready for consumption.
   */
  public Map<EntryKey, Object> getExpanded() {
    return expanded;
  }

  /**
   * Returns {@code true} if this record contains given {@link EntryKey}.
   */
  boolean containsKey(final EntryKey<?> entryKey) { return expanded.containsKey(entryKey); }

  /**
   * Type safe handy method to get value from expanded map.
   */
  public <T> T get(final EntryKey<T> entryKey) {
    return entryKey.coerce(expanded.get(entryKey));
  }

  /**
   * Type safe handy method to put value to expanded map. Accepts {@code null} values, that removes the mapping.
   */
  public <T> T put(final EntryKey<T> entryKey, final T value) {
    if (value == null) {
      return entryKey.coerce(expanded.remove(entryKey));
    }
    else {
      if (!entryKey.proto.isAssignableFrom(value.getClass())) {
        throw new IllegalArgumentException("Key " + entryKey + " does not accepts value " + value);
      }
      return entryKey.coerce(expanded.put(entryKey, value));
    }
  }

  @Override
  public String toString() {
    return "Record{" +
        "type=" + type +
        ", expanded=" + expanded +
        '}';
  }
}
