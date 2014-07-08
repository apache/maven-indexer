package org.apache.maven.index;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.creator.OsgiArtifactIndexCreator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import com.google.common.base.Strings;

/**
 * ArtifactInfo holds the values known about an repository artifact. This is a simple Value Object kind of stuff.
 * Phasing out.
 * 
 * @author Jason van Zyl
 * @author Eugene Kuleshov
 */
public class ArtifactInfo
    extends ArtifactInfoRecord
{
    private static final long serialVersionUID = 6028843453477511104L;

    // --

    public static final String ROOT_GROUPS = "rootGroups";

    public static final String ROOT_GROUPS_VALUE = "rootGroups";

    public static final String ROOT_GROUPS_LIST = "rootGroupsList";

    public static final String ALL_GROUPS = "allGroups";

    public static final String ALL_GROUPS_VALUE = "allGroups";

    public static final String ALL_GROUPS_LIST = "allGroupsList";

    // ----------

    /**
     * Unique groupId, artifactId, version, classifier, extension (or packaging). Stored, indexed untokenized
     */
    public static final String UINFO = FLD_UINFO.getKey();

    /**
     * Field that contains {@link #UINFO} value for deleted artifact
     */
    public static final String DELETED = FLD_DELETED.getKey();

    /**
     * GroupId. Not stored, indexed untokenized
     */
    public static final String GROUP_ID = MinimalArtifactInfoIndexCreator.FLD_GROUP_ID_KW.getKey();

    /**
     * ArtifactId. Not stored, indexed tokenized
     */
    public static final String ARTIFACT_ID = MinimalArtifactInfoIndexCreator.FLD_ARTIFACT_ID_KW.getKey();

    /**
     * Version. Not stored, indexed tokenized
     */
    public static final String VERSION = MinimalArtifactInfoIndexCreator.FLD_VERSION_KW.getKey();

    /**
     * Packaging. Not stored, indexed untokenized
     */
    public static final String PACKAGING = MinimalArtifactInfoIndexCreator.FLD_PACKAGING.getKey();

    /**
     * Classifier. Not stored, indexed untokenized
     */
    public static final String CLASSIFIER = MinimalArtifactInfoIndexCreator.FLD_CLASSIFIER.getKey();

    /**
     * Info: packaging, lastModified, size, sourcesExists, javadocExists, signatureExists. Stored, not indexed.
     */
    public static final String INFO = MinimalArtifactInfoIndexCreator.FLD_INFO.getKey();

    /**
     * Name. Stored, not indexed
     */
    public static final String NAME = MinimalArtifactInfoIndexCreator.FLD_NAME.getKey();

    /**
     * Description. Stored, not indexed
     */
    public static final String DESCRIPTION = MinimalArtifactInfoIndexCreator.FLD_DESCRIPTION.getKey();

    /**
     * Last modified. Stored, not indexed
     */
    public static final String LAST_MODIFIED = MinimalArtifactInfoIndexCreator.FLD_LAST_MODIFIED.getKey();

    /**
     * SHA1. Stored, indexed untokenized
     */
    public static final String SHA1 = MinimalArtifactInfoIndexCreator.FLD_SHA1.getKey();

    /**
     * Class names Stored compressed, indexed tokenized
     */
    public static final String NAMES = JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.getKey();

    /**
     * Plugin prefix. Stored, not indexed
     */
    public static final String PLUGIN_PREFIX = MavenPluginArtifactInfoIndexCreator.FLD_PLUGIN_PREFIX.getKey();

    /**
     * Plugin goals. Stored, not indexed
     */
    public static final String PLUGIN_GOALS = MavenPluginArtifactInfoIndexCreator.FLD_PLUGIN_GOALS.getKey();


    /**
     * @since 1.4.2
     */
    public static final String BUNDLE_SYMBOLIC_NAME = OsgiArtifactIndexCreator.FLD_BUNDLE_SYMBOLIC_NAME.getKey();

    /**
     * @since 1.4.2
     */
    public static final String BUNDLE_VERSION = OsgiArtifactIndexCreator.FLD_BUNDLE_VERSION.getKey();

    /**
     * @since 1.4.2
     */
    public static final String BUNDLE_EXPORT_PACKAGE = OsgiArtifactIndexCreator.FLD_BUNDLE_EXPORT_PACKAGE.getKey();

    public static final Comparator<ArtifactInfo> VERSION_COMPARATOR = new VersionComparator();

    public static final Comparator<ArtifactInfo> REPOSITORY_VERSION_COMPARATOR = new RepositoryVersionComparator();

    public static final Comparator<ArtifactInfo> CONTEXT_VERSION_COMPARATOR = new ContextVersionComparator();

    private String fileName;

    private String fileExtension;

    private String groupId;

    private String artifactId;

    private String version;

    private transient Version artifactVersion;

    private transient float luceneScore;

    private String classifier;

    /**
     * Artifact packaging for the main artifact and extension for secondary artifact (no classifier)
     */
    private String packaging;

    private String name;

    private String description;

    private long lastModified = -1;

    private long size = -1;

    private String md5;

    private String sha1;

    private ArtifactAvailability sourcesExists = ArtifactAvailability.NOT_PRESENT;

    private ArtifactAvailability javadocExists = ArtifactAvailability.NOT_PRESENT;

    private ArtifactAvailability signatureExists = ArtifactAvailability.NOT_PRESENT;

    private String classNames;

    private String repository;

    private String path;

    private String remoteUrl;

    private String context;

    /**
     * Plugin goal prefix (only if packaging is "maven-plugin")
     */
    private String prefix;

    /**
     * Plugin goals (only if packaging is "maven-plugin")
     */
    private List<String> goals;

    /**
     * contains osgi metadata Bundle-Version if available
     * @since 4.1.2
     */
    private String bundleVersion;

    /**
     * contains osgi metadata Bundle-SymbolicName if available
     * @since 4.1.2
     */
    private String bundleSymbolicName;

    /**
     * contains osgi metadata Export-Package if available
     * @since 4.1.2
     */
    private String bundleExportPackage;

    /**
     * contains osgi metadata Export-Service if available
     * @since 4.1.2
     */
    private String bundleExportService;

    /**
     * contains osgi metadata Bundle-Description if available
     * @since 4.1.2
     */
    private String bundleDescription;

    /**
     * contains osgi metadata Bundle-Name if available
     * @since 4.1.2
     */
    private String bundleName;

    /**
     * contains osgi metadata Bundle-License if available
     * @since 4.1.2
     */
    private String bundleLicense;

    /**
     * contains osgi metadata Bundle-DocURL if available
     * @since 4.1.2
     */
    private String bundleDocUrl;

    /**
     * contains osgi metadata Import-Package if available
     * @since 4.1.2
     */
    private String bundleImportPackage;

    /**
     * contains osgi metadata Require-Bundle if available
     * @since 4.1.2
     */
    private String bundleRequireBundle;

    private final Map<String, String> attributes = new HashMap<String, String>();

    private final List<MatchHighlight> matchHighlights = new ArrayList<MatchHighlight>();

    private final transient VersionScheme versionScheme;


    public ArtifactInfo()
    {
        versionScheme = new GenericVersionScheme();
    }

    public ArtifactInfo( String repository, String groupId, String artifactId, String version, String classifier, String extension )
    {
        this();
        this.repository = repository;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.fileExtension = extension;
    }

    public Version getArtifactVersion()
    {
        if ( artifactVersion == null )
        {
            try
            {
                artifactVersion = versionScheme.parseVersion( version );
            }
            catch ( InvalidVersionSpecificationException e )
            {
                // will not happen, only with version ranges but we should not have those
                // we handle POM versions here, not dependency versions
            }
        }

        return artifactVersion;
    }

    public float getLuceneScore()
    {
        return luceneScore;
    }

    public void setLuceneScore( float score )
    {
        this.luceneScore = score;
    }

    public String getUinfo()
    {
        return new StringBuilder() //
        .append( groupId ).append( FS ) //
        .append( artifactId ).append( FS ) //
        .append( version ).append( FS ) //
        .append( nvl( classifier ) ).append( FS ) //
        .append( fileExtension )
        // .append( StringUtils.isEmpty( classifier ) || StringUtils.isEmpty( packaging ) ? "" : FS + packaging ) //
        .toString(); // extension is stored in the packaging field when classifier is not used
    }

    public String getRootGroup()
    {
        int n = groupId.indexOf( '.' );
        if ( n > -1 )
        {
            return groupId.substring( 0, n );
        }
        return groupId;
    }

    public Gav calculateGav()
    {
        return new Gav( groupId, artifactId, version, classifier, fileExtension, null, // snapshotBuildNumber
            null, // snapshotTimeStamp
            fileName, // name
            false, // hash
            null, // hashType
            false, // signature
            null ); // signatureType
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    public List<MatchHighlight> getMatchHighlights()
    {
        return matchHighlights;
    }

    @Override
    public String toString()
    {
        final StringBuilder result = new StringBuilder( getUinfo() );
        if ( !Strings.isNullOrEmpty( getPackaging() ) )
        {
            result.append( "[" ).append( getPackaging() ).append( "]" );
        }
        return result.toString();
    }

    private static final List<Field> DEFAULT_FIELDS = new ArrayList<Field>();
    static
    {
        DEFAULT_FIELDS.add( MAVEN.GROUP_ID );
        DEFAULT_FIELDS.add( MAVEN.ARTIFACT_ID );
        DEFAULT_FIELDS.add( MAVEN.VERSION );
        DEFAULT_FIELDS.add( MAVEN.PACKAGING );
        DEFAULT_FIELDS.add( MAVEN.CLASSIFIER );
        DEFAULT_FIELDS.add( MAVEN.SHA1 );
        DEFAULT_FIELDS.add( MAVEN.NAME );
        DEFAULT_FIELDS.add( MAVEN.DESCRIPTION );
        DEFAULT_FIELDS.add( MAVEN.CLASSNAMES );
        DEFAULT_FIELDS.add( MAVEN.REPOSITORY_ID );
    }

    private List<Field> fields;

    public Collection<Field> getFields()
    {
        if ( fields == null )
        {
            fields = new ArrayList<Field>( DEFAULT_FIELDS.size() );

            fields.addAll( DEFAULT_FIELDS );
        }

        return fields;
    }

    /**
     * This method will disappear, once we drop ArtifactInfo.
     * 
     * @param field
     * @return
     */
    public String getFieldValue( Field field )
    {
        if ( MAVEN.GROUP_ID.equals( field ) )
        {
            return groupId;
        }
        else if ( MAVEN.ARTIFACT_ID.equals( field ) )
        {
            return artifactId;
        }
        else if ( MAVEN.VERSION.equals( field ) )
        {
            return version;
        }
        else if ( MAVEN.PACKAGING.equals( field ) )
        {
            return packaging;
        }
        else if ( MAVEN.CLASSIFIER.equals( field ) )
        {
            return classifier;
        }
        else if ( MAVEN.SHA1.equals( field ) )
        {
            return sha1;
        }
        else if ( MAVEN.NAME.equals( field ) )
        {
            return name;
        }
        else if ( MAVEN.DESCRIPTION.equals( field ) )
        {
            return description;
        }
        else if ( MAVEN.CLASSNAMES.equals( field ) )
        {
            return classNames;
        }
        else if ( MAVEN.REPOSITORY_ID.equals( field ) )
        {
            return repository;
        }

        // no match
        return null;
    }

    public ArtifactInfo setFieldValue( Field field, String value )
    {
        if ( MAVEN.GROUP_ID.equals( field ) )
        {
            groupId = value;
        }
        else if ( MAVEN.ARTIFACT_ID.equals( field ) )
        {
            artifactId = value;
        }
        else if ( MAVEN.VERSION.equals( field ) )
        {
            version = value;
        }
        else if ( MAVEN.PACKAGING.equals( field ) )
        {
            packaging = value;
        }
        else if ( MAVEN.CLASSIFIER.equals( field ) )
        {
            classifier = value;
        }
        else if ( MAVEN.SHA1.equals( field ) )
        {
            sha1 = value;
        }
        else if ( MAVEN.NAME.equals( field ) )
        {
            name = value;
        }
        else if ( MAVEN.DESCRIPTION.equals( field ) )
        {
            description = value;
        }
        else if ( MAVEN.CLASSNAMES.equals( field ) )
        {
            classNames = value;
        }
        else if ( MAVEN.REPOSITORY_ID.equals( field ) )
        {
            repository = value;
        }

        // no match
        return this;
    }

    // ----------------------------------------------------------------------------
    // Utils
    // ----------------------------------------------------------------------------

    public static String nvl( String v )
    {
        return v == null ? NA : v;
    }

    public static String renvl( String v )
    {
        return NA.equals( v ) ? null : v;
    }

    public static String lst2str( Collection<String> list )
    {
        StringBuilder sb = new StringBuilder();
        for ( String s : list )
        {
            sb.append( s ).append( ArtifactInfo.FS );
        }
        return sb.length() == 0 ? sb.toString() : sb.substring( 0, sb.length() - 1 );
    }

    public static List<String> str2lst( String str )
    {
        return Arrays.asList( ArtifactInfo.FS_PATTERN.split( str ) );
    }

    /**
     * A version comparator
     */
    static class VersionComparator
        implements Comparator<ArtifactInfo>
    {
        public int compare( final ArtifactInfo f1, final ArtifactInfo f2 )
        {
            int n = f1.groupId.compareTo( f2.groupId );
            if ( n != 0 )
            {
                return n;
            }

            n = f1.artifactId.compareTo( f2.artifactId );
            if ( n != 0 )
            {
                return n;
            }

            n = -f1.getArtifactVersion().compareTo( f2.getArtifactVersion() );
            if ( n != 0 )
            {
                return n;
            }

            {
                final String c1 = f1.classifier;
                final String c2 = f2.classifier;
                if ( c1 == null )
                {
                    if ( c2 != null )
                    {
                        return -1;
                    }
                }
                else
                {
                    if ( c2 == null )
                    {
                        return 1;
                    }

                    n = c1.compareTo( c2 );
                    if ( n != 0 )
                    {
                        return n;
                    }
                }
            }

            {
                final String p1 = f1.packaging;
                final String p2 = f2.packaging;
                if ( p1 == null )
                {
                    return p2 == null ? 0 : -1;
                }
                else
                {
                    return p2 == null ? 1 : p1.compareTo( p2 );
                }
            }
        }
    }

    /**
     * A repository and version comparator
     */
    static class RepositoryVersionComparator
        extends VersionComparator
    {
        @Override
        public int compare( final ArtifactInfo f1, final ArtifactInfo f2 )
        {
            final int n = super.compare( f1, f2 );
            if ( n != 0 )
            {
                return n;
            }

            final String r1 = f1.repository;
            final String r2 = f2.repository;
            if ( r1 == null )
            {
                return r2 == null ? 0 : -1;
            }
            else
            {
                return r2 == null ? 1 : r1.compareTo( r2 );
            }
        }
    }
    
    /**
     * A context and version comparator
     */
    static class ContextVersionComparator
        extends VersionComparator
    {
        @Override
        public int compare( final ArtifactInfo f1, final ArtifactInfo f2 )
        {
            final int n = super.compare( f1, f2 );
            if ( n != 0 )
            {
                return n;
            }

            final String r1 = f1.context;
            final String r2 = f2.context;
            if ( r1 == null )
            {
                return r2 == null ? 0 : -1;
            }
            else
            {
                return r2 == null ? 1 : r1.compareTo( r2 );
            }
        }
    }

    public String getFileName( )
    {
        return fileName;
    }

    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }

    public String getFileExtension( )
    {
        return fileExtension;
    }

    public void setFileExtension( String fileExtension )
    {
        this.fileExtension = fileExtension;
    }

    public String getGroupId( )
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId( )
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion( )
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setArtifactVersion( Version artifactVersion )
    {
        this.artifactVersion = artifactVersion;
    }

    public String getClassifier( )
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public String getPackaging( )
    {
        return packaging;
    }

    public void setPackaging( String packaging )
    {
        this.packaging = packaging;
    }

    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public long getLastModified( )
    {
        return lastModified;
    }

    public void setLastModified( long lastModified )
    {
        this.lastModified = lastModified;
    }

    public long getSize( )
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public String getMd5( )
    {
        return md5;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public String getSha1( )
    {
        return sha1;
    }

    public void setSha1( String sha1 )
    {
        this.sha1 = sha1;
    }

    public ArtifactAvailability getSourcesExists( )
    {
        return sourcesExists;
    }

    public void setSourcesExists( ArtifactAvailability sourcesExists )
    {
        this.sourcesExists = sourcesExists;
    }

    public ArtifactAvailability getJavadocExists( )
    {
        return javadocExists;
    }

    public void setJavadocExists( ArtifactAvailability javadocExists )
    {
        this.javadocExists = javadocExists;
    }

    public ArtifactAvailability getSignatureExists( )
    {
        return signatureExists;
    }

    public void setSignatureExists( ArtifactAvailability signatureExists )
    {
        this.signatureExists = signatureExists;
    }

    public String getClassNames( )
    {
        return classNames;
    }

    public void setClassNames( String classNames )
    {
        this.classNames = classNames;
    }

    public String getRepository( )
    {
        return repository;
    }

    public void setRepository( String repository )
    {
        this.repository = repository;
    }

    public String getPath( )
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getRemoteUrl( )
    {
        return remoteUrl;
    }

    public void setRemoteUrl( String remoteUrl )
    {
        this.remoteUrl = remoteUrl;
    }

    public String getContext( )
    {
        return context;
    }

    public void setContext( String context )
    {
        this.context = context;
    }

    public String getPrefix( )
    {
        return prefix;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    public List<String> getGoals( )
    {
        return goals;
    }

    public void setGoals( List<String> goals )
    {
        this.goals = goals;
    }

    public String getBundleVersion( )
    {
        return bundleVersion;
    }

    public void setBundleVersion( String bundleVersion )
    {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleSymbolicName( )
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName( String bundleSymbolicName )
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getBundleExportPackage( )
    {
        return bundleExportPackage;
    }

    public void setBundleExportPackage( String bundleExportPackage )
    {
        this.bundleExportPackage = bundleExportPackage;
    }

    public String getBundleExportService( )
    {
        return bundleExportService;
    }

    public void setBundleExportService( String bundleExportService )
    {
        this.bundleExportService = bundleExportService;
    }

    public String getBundleDescription( )
    {
        return bundleDescription;
    }

    public void setBundleDescription( String bundleDescription )
    {
        this.bundleDescription = bundleDescription;
    }

    public String getBundleName( )
    {
        return bundleName;
    }

    public void setBundleName( String bundleName )
    {
        this.bundleName = bundleName;
    }

    public String getBundleLicense( )
    {
        return bundleLicense;
    }

    public void setBundleLicense( String bundleLicense )
    {
        this.bundleLicense = bundleLicense;
    }

    public String getBundleDocUrl( )
    {
        return bundleDocUrl;
    }

    public void setBundleDocUrl( String bundleDocUrl )
    {
        this.bundleDocUrl = bundleDocUrl;
    }

    public String getBundleImportPackage( )
    {
        return bundleImportPackage;
    }

    public void setBundleImportPackage( String bundleImportPackage )
    {
        this.bundleImportPackage = bundleImportPackage;
    }

    public String getBundleRequireBundle( )
    {
        return bundleRequireBundle;
    }

    public void setBundleRequireBundle( String bundleRequireBundle )
    {
        this.bundleRequireBundle = bundleRequireBundle;
    }

    public VersionScheme getVersionScheme( )
    {
        return versionScheme;
    }

    public void setFields( List<Field> fields )
    {
        this.fields = fields;
    }

}
