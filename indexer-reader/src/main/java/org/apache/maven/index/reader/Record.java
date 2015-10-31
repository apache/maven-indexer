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
public class Record
{
  /**
   * Key of repository ID entry, that contains {@link String}.
   */
  public static final String REPOSITORY_ID = "repositoryId";

  /**
   * Key of all groups list entry, that contains {@link java.util.List<String>}.
   */
  public static final String ALL_GROUPS_LIST = "allGroupsList";

  /**
   * Key of root groups list entry, that contains {@link java.util.List<String>}.
   */
  public static final String ROOT_GROUPS_LIST = "rootGroupsList";

  /**
   * Key of index record modification (added to index or removed from index) timestamp entry, that contains {@link
   * Long}.
   */
  public static final String REC_MODIFIED = "recordModified";

  /**
   * Key of artifact groupId entry, that contains {@link String}.
   */
  public static final String GROUP_ID = "groupId";

  /**
   * Key of artifact artifactId entry, that contains {@link String}.
   */
  public static final String ARTIFACT_ID = "artifactId";

  /**
   * Key of artifact version entry, that contains {@link String}.
   */
  public static final String VERSION = "version";

  /**
   * Key of artifact classifier entry, that contains {@link String}.
   */
  public static final String CLASSIFIER = "classifier";

  /**
   * Key of artifact packaging entry, that contains {@link String}.
   */
  public static final String PACKAGING = "packaging";

  /**
   * Key of artifact file extension, that contains {@link String}.
   */
  public static final String FILE_EXTENSION = "fileExtension";

  /**
   * Key of artifact file last modified timestamp, that contains {@link Long}.
   */
  public static final String FILE_MODIFIED = "fileModified";

  /**
   * Key of artifact file size in bytes, that contains {@link Long}.
   */
  public static final String FILE_SIZE = "fileSize";

  /**
   * Key of artifact Sources presence flag, that contains {@link Boolean}.
   */
  public static final String HAS_SOURCES = "hasSources";

  /**
   * Key of artifact Javadoc presence flag, that contains {@link Boolean}.
   */
  public static final String HAS_JAVADOC = "hasJavadoc";

  /**
   * Key of artifact signature presence flag, that contains {@link Boolean}.
   */
  public static final String HAS_SIGNATURE = "hasSignature";

  /**
   * Key of artifact name (as set in POM), that contains {@link String}.
   */
  public static final String NAME = "name";

  /**
   * Key of artifact description (as set in POM), that contains {@link String}.
   */
  public static final String DESCRIPTION = "description";

  /**
   * Key of artifact SHA1 digest, that contains {@link String}.
   */
  public static final String SHA1 = "sha1";

  /**
   * Key of artifact contained class names, that contains {@link java.util.List<String>}.
   */
  public static final String CLASSNAMES = "classNames";

  /**
   * Key of plugin artifact prefix, that contains {@link String}.
   */
  public static final String PLUGIN_PREFIX = "pluginPrefix";

  /**
   * Key of plugin artifact goals, that contains {@link java.util.List<String>}.
   */
  public static final String PLUGIN_GOALS = "pluginGoals";

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
     * Artifact REMOTE record. In case of incremental updates, notes that this artifact was removed. Records of this
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
     * <li>{@link #ALL_GROUPS_LIST}</li>
     * </ul>
     */
    ALL_GROUPS,

    /**
     * Special record, containing all the root groups of Maven "groupId"s that are enlisted on the index. Can be safely
     * ignored.
     * Contains following entries:
     * <ul>
     * <li>{@link #ROOT_GROUPS_LIST}</li>
     * </ul>
     */
    ROOT_GROUPS
  }

  private final Type type;

  private final Map<String, String> raw;

  private final Map<String, Object> expanded;

  public Record(final Type type, final Map<String, String> raw, final Map<String, Object> expanded) {
    this.type = type;
    this.raw = raw;
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
   * Returns the "raw", Maven Indexer specific record as a {@link Map}.
   */
  public Map<String, String> getRaw() {
    return raw;
  }

  /**
   * Returns the expanded (processed and expanded synthetic fields) record as {@link Map} ready for consumption.
   */
  public Map<String, Object> getExpanded() {
    return expanded;
  }
}
