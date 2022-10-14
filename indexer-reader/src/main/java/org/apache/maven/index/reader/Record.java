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
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Maven Index record.
 *
 * @since 5.1.2
 */
public final class Record
{
    /**
     * Entry key is field key with some metadata.
     */
    public static final class EntryKey
    {
        private final String name;

        private final Class<?> proto;

        public EntryKey( final String name, final Class<?> proto )
        {
            requireNonNull( name, "name is null" );
            requireNonNull( proto, "proto is null" );
            this.name = name;
            this.proto = proto;
        }

        public String getName()
        {
            return name;
        }

        public Class<?> getProto()
        {
            return proto;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            EntryKey entryKey = (EntryKey) o;
            return Objects.equals( name, entryKey.name );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( name );
        }

        @Override
        public String toString()
        {
            return "Key{" + "name='" + name + '\'' + ", type=" + proto.getSimpleName() + '}';
        }
    }

    /**
     * Key of repository ID entry, that contains {@link String}.
     */
    public static final EntryKey REPOSITORY_ID = new EntryKey( "repositoryId", String.class );

    /**
     * Key of all groups list entry, that contains {@link java.util.List<String>}.
     */
    public static final EntryKey ALL_GROUPS = new EntryKey( "allGroups", String[].class );

    /**
     * Key of root groups list entry, that contains {@link java.util.List<String>}.
     */
    public static final EntryKey ROOT_GROUPS = new EntryKey( "rootGroups", String[].class );

    /**
     * Key of index record modification (added to index or removed from index) timestamp entry, that contains {@link
     * Long}.
     */
    public static final EntryKey REC_MODIFIED = new EntryKey( "recordModified", Long.class );

    /**
     * Key of artifact groupId entry, that contains {@link String}.
     */
    public static final EntryKey GROUP_ID = new EntryKey( "groupId", String.class );

    /**
     * Key of artifact artifactId entry, that contains {@link String}.
     */
    public static final EntryKey ARTIFACT_ID = new EntryKey( "artifactId", String.class );

    /**
     * Key of artifact version entry, that contains {@link String}.
     */
    public static final EntryKey VERSION = new EntryKey( "version", String.class );

    /**
     * Key of artifact classifier entry, that contains {@link String}.
     */
    public static final EntryKey CLASSIFIER = new EntryKey( "classifier", String.class );

    /**
     * Key of artifact packaging entry, that contains {@link String}.
     */
    public static final EntryKey PACKAGING = new EntryKey( "packaging", String.class );

    /**
     * Key of artifact file extension, that contains {@link String}.
     */
    public static final EntryKey FILE_EXTENSION = new EntryKey( "fileExtension", String.class );

    /**
     * Key of artifact file last modified timestamp, that contains {@link Long}.
     */
    public static final EntryKey FILE_MODIFIED = new EntryKey( "fileModified", Long.class );

    /**
     * Key of artifact file size in bytes, that contains {@link Long}.
     */
    public static final EntryKey FILE_SIZE = new EntryKey( "fileSize", Long.class );

    /**
     * Key of artifact Sources presence flag, that contains {@link Boolean}.
     */
    public static final EntryKey HAS_SOURCES = new EntryKey( "hasSources", Boolean.class );

    /**
     * Key of artifact Javadoc presence flag, that contains {@link Boolean}.
     */
    public static final EntryKey HAS_JAVADOC = new EntryKey( "hasJavadoc", Boolean.class );

    /**
     * Key of artifact signature presence flag, that contains {@link Boolean}.
     */
    public static final EntryKey HAS_SIGNATURE = new EntryKey( "hasSignature", Boolean.class );

    /**
     * Key of artifact name (as set in POM), that contains {@link String}.
     */
    public static final EntryKey NAME = new EntryKey( "name", String.class );

    /**
     * Key of artifact description (as set in POM), that contains {@link String}.
     */
    public static final EntryKey DESCRIPTION = new EntryKey( "description", String.class );

    /**
     * Key of artifact SHA1 digest, that contains {@link String}.
     */
    public static final EntryKey SHA1 = new EntryKey( "sha1", String.class );

    /**
     * Key of artifact contained class names, that contains {@link java.util.List<String>}. Extracted by {@code
     * JarFileContentsIndexCreator}.
     */
    public static final EntryKey CLASSNAMES = new EntryKey( "classNames", String[].class );

    /**
     * Key of plugin artifact prefix, that contains {@link String}. Extracted by {@code
     * MavenPluginArtifactInfoIndexCreator}.
     */
    public static final EntryKey PLUGIN_PREFIX = new EntryKey( "pluginPrefix", String.class );

    /**
     * Key of plugin artifact goals, that contains {@link java.util.List<String>}. Extracted by {@code
     * MavenPluginArtifactInfoIndexCreator}.
     */
    public static final EntryKey PLUGIN_GOALS = new EntryKey( "pluginGoals", String[].class );

    /**
     * Key of OSGi "Bundle-SymbolicName" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BUNDLE_SYMBOLIC_NAME = new EntryKey( "Bundle-SymbolicName",
            String.class );

    /**
     * Key of OSGi "Bundle-Version" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BUNDLE_VERSION = new EntryKey( "Bundle-Version", String.class );

    /**
     * Key of OSGi "Export-Package" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_EXPORT_PACKAGE = new EntryKey( "Export-Package", String.class );

    /**
     * Key of OSGi "Export-Service" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_EXPORT_SERVICE = new EntryKey( "Export-Service", String.class );

    /**
     * Key of OSGi "Bundle-Description" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BUNDLE_DESCRIPTION = new EntryKey( "Bundle-Description", String.class );

    /**
     * Key of OSGi "Bundle-Name" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BUNDLE_NAME = new EntryKey( "Bundle-Name", String.class );

    /**
     * Key of OSGi "Bundle-License" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BUNDLE_LICENSE = new EntryKey( "Bundle-License", String.class );

    /**
     * Key of OSGi "Bundle-DocURL" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_EXPORT_DOCURL = new EntryKey( "Bundle-DocURL", String.class );

    /**
     * Key of OSGi "Import-Package" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_IMPORT_PACKAGE = new EntryKey( "Import-Package", String.class );

    /**
     * Key of OSGi "Require-Bundle" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_REQUIRE_BUNDLE = new EntryKey( "Require-Bundle", String.class );

    /**
     * Key of OSGi "Provide-Capability" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_PROVIDE_CAPABILITY = new EntryKey( "Provide-Capability", String.class );

    /**
     * Key of OSGi "Require-Capability" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_REQUIRE_CAPABILITY = new EntryKey( "Require-Capability", String.class );

    /**
     * Key of OSGi "Fragment-Host" manifest entry, that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_FRAGMENT_HOST = new EntryKey( "Fragment-Host", String.class );

    /**
     * Key of deprecated OSGi "Bundle-RequiredExecutionEnvironment" manifest entry, that contains {@link String}.
     * Extracted by {@code OsgiArtifactIndexCreator}.
     */
    public static final EntryKey OSGI_BREE = new EntryKey( "Bundle-RequiredExecutionEnvironment",
            String.class );

    /**
     * Key for SHA-256 checksum  needed for OSGI content capability that contains {@link String}. Extracted by {@code
     * OsgiArtifactIndexCreator}.
     */
    public static final EntryKey SHA_256 = new EntryKey( "sha256", String.class );


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
         * Artifact REMOVE record. In case of incremental updates, signals that this artifact was removed. Records of
         * this type should be removed from your indexing system.
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
         * Special record, containing all the root groups of Maven "groupId"s that are enlisted on the index. Can be
         * safely ignored.
         * Contains following entries:
         * <ul>
         * <li>{@link #ROOT_GROUPS}</li>
         * </ul>
         */
        ROOT_GROUPS
    }

    private final Type type;

    private final Map<EntryKey, Object> expanded;

    public Record( final Type type, final Map<EntryKey, Object> expanded )
    {
        this.type = type;
        this.expanded = expanded;
    }

    /**
     * Returns the {@link Type} of this record. Usually users would be interested in {@link Type#ARTIFACT_ADD} and
     * {@link Type#ARTIFACT_REMOVE} types only to maintain their own index. Still, indexer offers extra records too,
     * see {@link Type} for all existing types.
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Returns the expanded (processed and expanded synthetic fields) record as {@link Map} ready for consumption.
     */
    public Map<EntryKey, Object> getExpanded()
    {
        return expanded;
    }

    /**
     * Returns {@code true} if this record contains given {@link EntryKey}.
     */
    boolean containsKey( final EntryKey entryKey )
    {
        return expanded.containsKey( entryKey );
    }

    /**
     * Type safe handy method to get value from expanded map.
     */
    public Object get( final EntryKey entryKey )
    {
        return expanded.get( entryKey );
    }

    /**
     * Type safe handy method to get string value from expanded map.
     *
     * @since TBD
     */
    public String getString( final EntryKey entryKey )
    {
        if ( !String.class.isAssignableFrom( entryKey.proto ) )
        {
            throw new IllegalArgumentException( "Key " + entryKey
                    + " does not hold type compatible to java.lang.String" );
        }
        return (String) expanded.get( entryKey );
    }

    /**
     * Type safe handy method to get String[] value from expanded map.
     *
     * @since TBD
     */
    public String[] getStringArray( final EntryKey entryKey )
    {
        if ( !String[].class.isAssignableFrom( entryKey.proto ) )
        {
            throw new IllegalArgumentException( "Key " + entryKey
                    + " does not hold type compatible to java.lang.String[]" );
        }
        return (String[]) expanded.get( entryKey );
    }

    /**
     * Type safe handy method to get Long value from expanded map.
     *
     * @since TBD
     */
    public Long getLong( final EntryKey entryKey )
    {
        if ( !Long.class.isAssignableFrom( entryKey.proto ) )
        {
            throw new IllegalArgumentException( "Key " + entryKey
                    + " does not hold type compatible to java.lang.Long" );
        }
        return (Long) expanded.get( entryKey );
    }

    /**
     * Type safe handy method to get Boolean value from expanded map.
     *
     * @since TBD
     */
    public Boolean getBoolean( final EntryKey entryKey )
    {
        if ( !Boolean.class.isAssignableFrom( entryKey.proto ) )
        {
            throw new IllegalArgumentException( "Key " + entryKey
                    + " does not hold type compatible to java.lang.Boolean" );
        }
        return (Boolean) expanded.get( entryKey );
    }

    /**
     * Type safe handy method to put value to expanded map. Accepts {@code null} values, that removes the mapping.
     */
    public Object put( final EntryKey entryKey, final Object value )
    {
        if ( value == null )
        {
            return expanded.remove( entryKey );
        }
        else
        {
            if ( !entryKey.proto.isAssignableFrom( value.getClass() ) )
            {
                throw new IllegalArgumentException( "Key " + entryKey + " does not accepts value " + value );
            }
            return expanded.put( entryKey, value );
        }
    }

    @Override
    public String toString()
    {
        return "Record{" + "type=" + type + ", expanded=" + expanded + '}';
    }
}
