package org.apache.maven.index.creator;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.OSGI;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This indexCreator will index some OSGI metadatas.
 * <br/>
 * All jars are indexed and not only the ones with packaging bundle.
 * <br/>
 * <p>
 * OSGI metadatas indexed :
 * <ul>
 * <li>Bundle-SymbolicName</li>
 * <li>Bundle-Version</li>
 * <li>Export-Package</li>
 * <li>Export-Service</li>
 * </ul>
 * </p>
 *
 * @author Olivier Lamy
 * @since 4.1.2
 */
@Singleton
@Named( OsgiArtifactIndexCreator.ID )
public class OsgiArtifactIndexCreator
    extends AbstractIndexCreator
{
    public static final String ID = "osgi-metadatas";

    public static final IndexerField FLD_SHA256 =
        new IndexerField( OSGI.SHA256, IndexerFieldVersion.V4, "sha256", "SHA-256 (not analyzed, stored)",
                          IndexerField.KEYWORD_STORED );

    private static final String BSN = "Bundle-SymbolicName";

    public static final IndexerField FLD_BUNDLE_SYMBOLIC_NAME =
        new IndexerField( OSGI.SYMBOLIC_NAME, IndexerFieldVersion.V4, BSN, "Bundle-SymbolicName (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BV = "Bundle-Version";

    public static final IndexerField FLD_BUNDLE_VERSION =
        new IndexerField( OSGI.VERSION, IndexerFieldVersion.V4, BV, "Bundle-Version (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BEP = "Export-Package";

    public static final IndexerField FLD_BUNDLE_EXPORT_PACKAGE =
        new IndexerField( OSGI.EXPORT_PACKAGE, IndexerFieldVersion.V4, BEP, "Export-Package (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    @Deprecated
    private static final String BES = "Export-Service";
    @Deprecated
    public static final IndexerField FLD_BUNDLE_EXPORT_SERVIVE =
        new IndexerField( OSGI.EXPORT_SERVICE, IndexerFieldVersion.V4, BES, "Export-Service (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BD = "Bundle-Description";

    public static final IndexerField FLD_BUNDLE_DESCRIPTION =
        new IndexerField( OSGI.DESCRIPTION, IndexerFieldVersion.V4, BD, "Bundle-Description (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BN = "Bundle-Name";

    public static final IndexerField FLD_BUNDLE_NAME =
        new IndexerField( OSGI.NAME, IndexerFieldVersion.V4, BN, "Bundle-Name (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BL = "Bundle-License";

    public static final IndexerField FLD_BUNDLE_LICENSE =
        new IndexerField( OSGI.LICENSE, IndexerFieldVersion.V4, BL, "Bundle-License (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BDU = "Bundle-DocURL";

    public static final IndexerField FLD_BUNDLE_DOCURL =
        new IndexerField( OSGI.DOCURL, IndexerFieldVersion.V4, BDU, "Bundle-DocURL (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BIP = "Import-Package";

    public static final IndexerField FLD_BUNDLE_IMPORT_PACKAGE =
        new IndexerField( OSGI.IMPORT_PACKAGE, IndexerFieldVersion.V4, BIP, "Import-Package (indexed, stored)",
                          IndexerField.ANALYZED_STORED );


    private static final String BRB = "Require-Bundle";

    public static final IndexerField FLD_BUNDLE_REQUIRE_BUNDLE =
        new IndexerField( OSGI.REQUIRE_BUNDLE, IndexerFieldVersion.V4, BRB, "Require-Bundle (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String PROVIDE_CAPABILITY = "Provide-Capability";

    public static final IndexerField FLD_BUNDLE_PROVIDE_CAPABILITY =
        new IndexerField( OSGI.PROVIDE_CAPABILITY, IndexerFieldVersion.V4, PROVIDE_CAPABILITY,
                          "Provide-Capability (indexed, stored)", IndexerField.ANALYZED_STORED );

    private static final String REQUIRE_CAPABILITY = "Require-Capability";

    public static final IndexerField FLD_BUNDLE_REQUIRE_CAPABILITY =
        new IndexerField( OSGI.REQUIRE_CAPABILITY, IndexerFieldVersion.V4, REQUIRE_CAPABILITY,
                          "Require-Capability (indexed, stored)", IndexerField.ANALYZED_STORED );

    private static final String FRAGMENT_HOST = "Fragment-Host";

    public static final IndexerField FLD_BUNDLE_FRAGMENT_HOST =
        new IndexerField( OSGI.FRAGMENT_HOST, IndexerFieldVersion.V4, FRAGMENT_HOST, "Fragment-Host (indexed, stored)",
                          IndexerField.ANALYZED_STORED );

    private static final String BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT = "Bundle-RequiredExecutionEnvironment";

    public static final IndexerField FLD_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT =
        new IndexerField( OSGI.BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT, IndexerFieldVersion.V4,
                          BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT,
                          "Bundle-RequiredExecutionEnvironment (indexed, stored)",
                          IndexerField.ANALYZED_STORED );


    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_BUNDLE_SYMBOLIC_NAME, FLD_BUNDLE_VERSION, FLD_BUNDLE_EXPORT_PACKAGE,
                              FLD_BUNDLE_EXPORT_SERVIVE, FLD_BUNDLE_DESCRIPTION, FLD_BUNDLE_NAME, FLD_BUNDLE_LICENSE,
                              FLD_BUNDLE_DOCURL, FLD_BUNDLE_IMPORT_PACKAGE, FLD_BUNDLE_REQUIRE_BUNDLE,
                              FLD_BUNDLE_PROVIDE_CAPABILITY, FLD_BUNDLE_REQUIRE_CAPABILITY, FLD_BUNDLE_FRAGMENT_HOST,
                              FLD_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT, FLD_SHA256 );
    }

    public OsgiArtifactIndexCreator()
    {
        super( ID );
    }

    public void populateArtifactInfo( ArtifactContext artifactContext )
        throws IOException
    {
        ArtifactInfo ai = artifactContext.getArtifactInfo();

        File artifactFile = artifactContext.getArtifact();

        // TODO : olamy : supports only jars ?

        if ( artifactFile != null && artifactFile.isFile() && artifactFile.getName().endsWith( ".jar" ) )
        {
            updateArtifactInfo( ai, artifactFile );
        }
    }

    public void updateDocument( ArtifactInfo artifactInfo, Document document )
    {

        if ( artifactInfo.getBundleSymbolicName() != null )
        {
            document.add( FLD_BUNDLE_SYMBOLIC_NAME.toField( artifactInfo.getBundleSymbolicName() ) );
        }

        if ( artifactInfo.getBundleVersion() != null )
        {
            document.add( FLD_BUNDLE_VERSION.toField( artifactInfo.getBundleVersion() ) );
        }

        if ( artifactInfo.getBundleExportPackage() != null )
        {
            document.add( FLD_BUNDLE_EXPORT_PACKAGE.toField( artifactInfo.getBundleExportPackage() ) );
        }

        if ( artifactInfo.getBundleExportService() != null )
        {
            document.add( FLD_BUNDLE_EXPORT_SERVIVE.toField( artifactInfo.getBundleExportService() ) );
        }

        if ( artifactInfo.getBundleDescription() != null )
        {
            document.add( FLD_BUNDLE_DESCRIPTION.toField( artifactInfo.getBundleDescription() ) );
        }

        if ( artifactInfo.getBundleName() != null )
        {
            document.add( FLD_BUNDLE_NAME.toField( artifactInfo.getBundleName() ) );
        }

        if ( artifactInfo.getBundleLicense() != null )
        {
            document.add( FLD_BUNDLE_LICENSE.toField( artifactInfo.getBundleLicense() ) );
        }

        if ( artifactInfo.getBundleDocUrl() != null )
        {
            document.add( FLD_BUNDLE_DOCURL.toField( artifactInfo.getBundleDocUrl() ) );
        }

        if ( artifactInfo.getBundleImportPackage() != null )
        {
            document.add( FLD_BUNDLE_IMPORT_PACKAGE.toField( artifactInfo.getBundleImportPackage() ) );
        }

        if ( artifactInfo.getBundleRequireBundle() != null )
        {
            document.add( FLD_BUNDLE_REQUIRE_BUNDLE.toField( artifactInfo.getBundleRequireBundle() ) );
        }

        if ( artifactInfo.getBundleProvideCapability() != null )
        {
            document.add( FLD_BUNDLE_PROVIDE_CAPABILITY.toField( artifactInfo.getBundleProvideCapability() ) );
        }

        if ( artifactInfo.getBundleRequireCapability() != null )
        {
            document.add( FLD_BUNDLE_REQUIRE_CAPABILITY.toField( artifactInfo.getBundleRequireCapability() ) );
        }

        if ( artifactInfo.getBundleFragmentHost() != null )
        {
            document.add( FLD_BUNDLE_FRAGMENT_HOST.toField( artifactInfo.getBundleFragmentHost() ) );
        }

        String bree = artifactInfo.getBundleRequiredExecutionEnvironment();
        if ( bree != null )
        {
            document.add( FLD_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT.toField( bree ) );
        }

        if ( artifactInfo.getSha256() != null )
        {
            document.add( FLD_SHA256.toField( artifactInfo.getSha256() ) );
        }

    }

    public boolean updateArtifactInfo( Document document, ArtifactInfo artifactInfo )
    {
        boolean updated = false;

        String bundleSymbolicName = document.get( FLD_BUNDLE_SYMBOLIC_NAME.getKey() );

        if ( bundleSymbolicName != null )
        {
            artifactInfo.setBundleSymbolicName( bundleSymbolicName );

            updated = true;
        }

        String bundleVersion = document.get( FLD_BUNDLE_VERSION.getKey() );

        if ( bundleVersion != null )
        {
            artifactInfo.setBundleVersion( bundleVersion );

            updated = true;
        }

        String bundleExportPackage = document.get( FLD_BUNDLE_EXPORT_PACKAGE.getKey() );

        if ( bundleExportPackage != null )
        {
            artifactInfo.setBundleExportPackage( bundleExportPackage );

            updated = true;
        }

        String bundleExportService = document.get( FLD_BUNDLE_EXPORT_SERVIVE.getKey() );

        if ( bundleExportService != null )
        {
            artifactInfo.setBundleExportService( bundleExportService );

            updated = true;
        }

        String bundleDescription = document.get( FLD_BUNDLE_DESCRIPTION.getKey() );

        if ( bundleDescription != null )
        {
            artifactInfo.setBundleDescription( bundleDescription );

            updated = true;
        }


        String bundleName = document.get( FLD_BUNDLE_NAME.getKey() );

        if ( bundleName != null )
        {
            artifactInfo.setBundleName( bundleName );

            updated = true;
        }


        String bundleLicense = document.get( FLD_BUNDLE_LICENSE.getKey() );

        if ( bundleLicense != null )
        {
            artifactInfo.setBundleLicense( bundleLicense );

            updated = true;
        }

        String bundleDocUrl = document.get( FLD_BUNDLE_DOCURL.getKey() );

        if ( bundleDocUrl != null )
        {
            artifactInfo.setBundleDocUrl( bundleDocUrl );

            updated = true;
        }

        String bundleImportPackage = document.get( FLD_BUNDLE_IMPORT_PACKAGE.getKey() );

        if ( bundleImportPackage != null )
        {
            artifactInfo.setBundleImportPackage( bundleImportPackage );

            updated = true;
        }

        String bundleRequireBundle = document.get( FLD_BUNDLE_REQUIRE_BUNDLE.getKey() );

        if ( bundleRequireBundle != null )
        {
            artifactInfo.setBundleRequireBundle( bundleRequireBundle );

            updated = true;
        }

        String bundleProvideCapability = document.get( FLD_BUNDLE_PROVIDE_CAPABILITY.getKey() );

        if ( bundleProvideCapability != null )
        {
            artifactInfo.setBundleProvideCapability( bundleProvideCapability );

            updated = true;
        }

        String bundleRequireCapability = document.get( FLD_BUNDLE_REQUIRE_CAPABILITY.getKey() );

        if ( bundleRequireCapability != null )
        {
            artifactInfo.setBundleRequireCapability( bundleRequireCapability );

            updated = true;
        }

        String bundleFragmentHost = document.get( FLD_BUNDLE_FRAGMENT_HOST.getKey() );

        if ( bundleFragmentHost != null )
        {
            artifactInfo.setBundleFragmentHost( bundleFragmentHost );

            updated = true;
        }

        String bundleRequiredExecutionEnvironment = document.get( FLD_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT.getKey() );

        if ( bundleRequiredExecutionEnvironment != null )
        {
            artifactInfo.setBundleRequiredExecutionEnvironment( bundleRequiredExecutionEnvironment );

            updated = true;
        }

        String sha256 = document.get( FLD_SHA256.getKey() );

        if ( sha256 != null )
        {
            artifactInfo.setSha256( sha256 );

            updated = true;
        }

        return updated;
    }

    private boolean updateArtifactInfo( ArtifactInfo ai, File f )
        throws IOException
    {
        boolean updated = false;

        try ( ZipFile zipFile = new ZipFile( f ) )
        {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while ( entries.hasMoreElements() )
            {
                ZipEntry zipEntry = entries.nextElement();

                if ( zipEntry.getName().equals( "META-INF/MANIFEST.MF" ) )
                {
                    Manifest manifest = new Manifest( zipFile.getInputStream( zipEntry ) );

                    Attributes mainAttributes = manifest.getMainAttributes();

                    if ( mainAttributes != null )
                    {
                        String attValue = mainAttributes.getValue( BSN );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleSymbolicName( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleSymbolicName( null );
                        }

                        attValue = mainAttributes.getValue( BV );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleVersion( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleVersion( null );
                        }

                        attValue = mainAttributes.getValue( BEP );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleExportPackage( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleExportPackage( null );
                        }

                        attValue = mainAttributes.getValue( BES );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleExportService( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleExportService( null );
                        }

                        attValue = mainAttributes.getValue( BD );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleDescription( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleDescription( null );
                        }

                        attValue = mainAttributes.getValue( BN );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleName( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleName( null );
                        }

                        attValue = mainAttributes.getValue( BL );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleLicense( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleLicense( null );
                        }

                        attValue = mainAttributes.getValue( BDU );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleDocUrl( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleDocUrl( null );
                        }

                        attValue = mainAttributes.getValue( BIP );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleImportPackage( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleImportPackage( null );
                        }

                        attValue = mainAttributes.getValue( BRB );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleRequireBundle( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleRequireBundle( null );
                        }

                        attValue = mainAttributes.getValue( PROVIDE_CAPABILITY );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleProvideCapability( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleProvideCapability( null );
                        }

                        attValue = mainAttributes.getValue( REQUIRE_CAPABILITY );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleRequireCapability( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleRequireCapability( null );
                        }

                        attValue = mainAttributes.getValue( FRAGMENT_HOST );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleFragmentHost( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleFragmentHost( null );
                        }

                        attValue = mainAttributes.getValue( BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.setBundleRequiredExecutionEnvironment( attValue );
                            updated = true;
                        }
                        else
                        {
                            ai.setBundleRequiredExecutionEnvironment( null );
                        }
                    }
                }
            }
        }

        // only calculate sha256 digest for if we are indexing a bundle.
        if ( ai.getBundleSymbolicName() != null )
        {
            String sha256 = computeSha256( f );
            if ( sha256 != null )
            {
                ai.setSha256( sha256 );
                updated = true;
            }
            else
            {
                ai.setSha256( null );
            }
        }

        return updated;
    }

    private String computeSha256( File f )
        throws IOException
    {
        String sha256 = null;
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
            DigestInputStream in = new DigestInputStream( new FileInputStream( f ), digest );

            try
            {
                byte buf[] = new byte[8192];
                while ( in.read( buf ) >= 0 )
                {
                    // nop
                }
                byte digestBytes[] = digest.digest();
                StringBuilder builder = new StringBuilder( 64 );
                for ( int b : digestBytes )
                {
                    b &= 0xff;
                    builder.append( String.format( "%02x", b ) );
                    sha256 = builder.toString();
                }
            }
            finally
            {
                in.close();
            }

        }
        catch ( NoSuchAlgorithmException e )
        {
        }
        return sha256;
    }

    @Override
    public String toString()
    {
        return ID;
    }
}
