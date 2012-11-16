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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.OSGI;
import org.apache.maven.index.util.zip.ZipFacade;
import org.apache.maven.index.util.zip.ZipHandle;
import org.codehaus.plexus.util.StringUtils;

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

    private static final String BSN = "Bundle-SymbolicName";

    public static final IndexerField FLD_BUNDLE_SYMBOLIC_NAME =
        new IndexerField( OSGI.SYMBOLIC_NAME, IndexerFieldVersion.V4, BSN, "Bundle-SymbolicName (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );


    private static final String BV = "Bundle-Version";

    public static final IndexerField FLD_BUNDLE_VERSION =
        new IndexerField( OSGI.VERSION, IndexerFieldVersion.V4, BV, "Bundle-Version (indexed, stored)", Field.Store.YES,
                          Field.Index.ANALYZED );


    private static final String BEP = "Export-Package";

    public static final IndexerField FLD_BUNDLE_EXPORT_PACKAGE =
        new IndexerField( OSGI.EXPORT_PACKAGE, IndexerFieldVersion.V4, BEP, "Export-Package (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );

    private static final String BES = "Export-Service";

    public static final IndexerField FLD_BUNDLE_EXPORT_SERVIVE =
        new IndexerField( OSGI.EXPORT_SERVICE, IndexerFieldVersion.V4, BES, "Export-Service (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );


    private static final String BD = "Bundle-Description";

    public static final IndexerField FLD_BUNDLE_DESCRIPTION =
        new IndexerField( OSGI.DESCRIPTION, IndexerFieldVersion.V4, BD, "Bundle-Description (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );

    private static final String BN = "Bundle-Name";

    public static final IndexerField FLD_BUNDLE_NAME =
        new IndexerField( OSGI.NAME, IndexerFieldVersion.V4, BN, "Bundle-Name (indexed, stored)", Field.Store.YES,
                          Field.Index.ANALYZED );

    private static final String BL = "Bundle-License";

    public static final IndexerField FLD_BUNDLE_LICENSE =
        new IndexerField( OSGI.LICENSE, IndexerFieldVersion.V4, BL, "Bundle-License (indexed, stored)", Field.Store.YES,
                          Field.Index.ANALYZED );

    private static final String BDU = "Bundle-DocURL";

    public static final IndexerField FLD_BUNDLE_DOCURL =
        new IndexerField( OSGI.DOCURL, IndexerFieldVersion.V4, BDU, "Bundle-DocURL (indexed, stored)", Field.Store.YES,
                          Field.Index.ANALYZED );

    private static final String BIP = "Import-Package";

    public static final IndexerField FLD_BUNDLE_IMPORT_PACKAGE =
        new IndexerField( OSGI.IMPORT_PACKAGE, IndexerFieldVersion.V4, BIP, "Import-Package (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );


    private static final String BRB = "Require-Bundle";

    public static final IndexerField FLD_BUNDLE_REQUIRE_BUNDLE =
        new IndexerField( OSGI.REQUIRE_BUNDLE, IndexerFieldVersion.V4, BRB, "Require-Bundle (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );




    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_BUNDLE_SYMBOLIC_NAME, FLD_BUNDLE_VERSION, FLD_BUNDLE_EXPORT_PACKAGE,
                              FLD_BUNDLE_EXPORT_SERVIVE, FLD_BUNDLE_DESCRIPTION, FLD_BUNDLE_NAME, FLD_BUNDLE_LICENSE,
                              FLD_BUNDLE_DOCURL, FLD_BUNDLE_IMPORT_PACKAGE, FLD_BUNDLE_REQUIRE_BUNDLE );
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

        if ( artifactInfo.bundleSymbolicName != null )
        {
            document.add( FLD_BUNDLE_SYMBOLIC_NAME.toField( artifactInfo.bundleSymbolicName ) );
        }

        if ( artifactInfo.bundleVersion != null )
        {
            document.add( FLD_BUNDLE_VERSION.toField( artifactInfo.bundleVersion ) );
        }

        if ( artifactInfo.bundleExportPackage != null )
        {
            document.add( FLD_BUNDLE_EXPORT_PACKAGE.toField( artifactInfo.bundleExportPackage ) );
        }

        if ( artifactInfo.bundleExportService != null )
        {
            document.add( FLD_BUNDLE_EXPORT_SERVIVE.toField( artifactInfo.bundleExportService ) );
        }

        if ( artifactInfo.bundleDescription != null )
        {
            document.add( FLD_BUNDLE_DESCRIPTION.toField( artifactInfo.bundleDescription ) );
        }

        if ( artifactInfo.bundleName != null )
        {
            document.add( FLD_BUNDLE_NAME.toField( artifactInfo.bundleName ) );
        }

        if ( artifactInfo.bundleLicense != null )
        {
            document.add( FLD_BUNDLE_LICENSE.toField( artifactInfo.bundleLicense ) );
        }

        if ( artifactInfo.bundleDocUrl != null )
        {
            document.add( FLD_BUNDLE_DOCURL.toField( artifactInfo.bundleDocUrl ) );
        }

        if ( artifactInfo.bundleImportPackage != null )
        {
            document.add( FLD_BUNDLE_IMPORT_PACKAGE.toField( artifactInfo.bundleImportPackage ) );
        }

        if ( artifactInfo.bundleRequireBundle != null )
        {
            document.add( FLD_BUNDLE_REQUIRE_BUNDLE.toField( artifactInfo.bundleRequireBundle ) );
        }
    }

    public boolean updateArtifactInfo( Document document, ArtifactInfo artifactInfo )
    {
        boolean updated = false;

        String bundleSymbolicName = document.get( FLD_BUNDLE_SYMBOLIC_NAME.getKey() );

        if ( bundleSymbolicName != null )
        {
            artifactInfo.bundleSymbolicName = bundleSymbolicName;

            updated = true;
        }

        String bundleVersion = document.get( FLD_BUNDLE_VERSION.getKey() );

        if ( bundleVersion != null )
        {
            artifactInfo.bundleVersion = bundleVersion;

            updated = true;
        }

        String bundleExportPackage = document.get( FLD_BUNDLE_EXPORT_PACKAGE.getKey() );

        if ( bundleExportPackage != null )
        {
            artifactInfo.bundleExportPackage = bundleExportPackage;

            updated = true;

        }

        String bundleExportService = document.get( FLD_BUNDLE_EXPORT_SERVIVE.getKey() );

        if ( bundleExportService != null )
        {
            artifactInfo.bundleExportService = bundleExportService;

            updated = true;

        }

        String bundleDescription = document.get( FLD_BUNDLE_DESCRIPTION.getKey() );

        if ( bundleDescription != null )
        {
            artifactInfo.bundleDescription = bundleDescription;

            updated = true;

        }


        String bundleName = document.get( FLD_BUNDLE_NAME.getKey() );

        if ( bundleName != null )
        {
            artifactInfo.bundleName = bundleName;

            updated = true;

        }


        String bundleLicense = document.get( FLD_BUNDLE_LICENSE.getKey() );

        if ( bundleLicense != null )
        {
            artifactInfo.bundleLicense = bundleLicense;

            updated = true;

        }

        String bundleDocUrl = document.get( FLD_BUNDLE_DOCURL.getKey() );

        if ( bundleDocUrl != null )
        {
            artifactInfo.bundleDocUrl = bundleDocUrl;

            updated = true;

        }

        String bundleImportPackage = document.get( FLD_BUNDLE_IMPORT_PACKAGE.getKey() );

        if ( bundleImportPackage != null )
        {
            artifactInfo.bundleImportPackage = bundleImportPackage;

            updated = true;

        }

        String bundleRequireBundle = document.get( FLD_BUNDLE_REQUIRE_BUNDLE.getKey() );

        if ( bundleRequireBundle != null )
        {
            artifactInfo.bundleRequireBundle = bundleRequireBundle;

            updated = true;

        }


        return updated;
    }

    private boolean updateArtifactInfo( ArtifactInfo ai, File f )
        throws IOException
    {
        ZipHandle handle = null;

        boolean updated = false;

        try
        {
            handle = ZipFacade.getZipHandle( f );

            final List<String> entries = handle.getEntries();

            for ( String name : entries )
            {
                if ( name.equals( "META-INF/MANIFEST.MF" ) )
                {
                    Manifest manifest = new Manifest( handle.getEntryContent( name ) );

                    Attributes mainAttributes = manifest.getMainAttributes();

                    if ( mainAttributes != null )
                    {
                        String attValue = mainAttributes.getValue( BSN );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleSymbolicName = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleSymbolicName = null;
                        }

                        attValue = mainAttributes.getValue( BV );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleVersion = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleVersion = null;
                        }

                        attValue = mainAttributes.getValue( BEP );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleExportPackage = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleExportPackage = null;
                        }

                        attValue = mainAttributes.getValue( BES );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleExportService = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleExportService = null;
                        }

                        attValue = mainAttributes.getValue( BD );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleDescription = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleDescription = null;
                        }

                        attValue = mainAttributes.getValue( BN );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleName = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleName = null;
                        }

                        attValue = mainAttributes.getValue( BL );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleLicense = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleLicense = null;
                        }

                        attValue = mainAttributes.getValue( BDU );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleDocUrl = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleDocUrl = null;
                        }

                        attValue = mainAttributes.getValue( BIP );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleImportPackage = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleImportPackage = null;
                        }

                        attValue = mainAttributes.getValue( BRB );
                        if ( StringUtils.isNotBlank( attValue ) )
                        {
                            ai.bundleRequireBundle = attValue;
                            updated = true;
                        }
                        else
                        {
                            ai.bundleRequireBundle = null;
                        }

                    }
                }
            }

        }
        finally
        {
            try
            {
                ZipFacade.close( handle );
            }
            catch ( Exception e )
            {
                getLogger().error( "Could not close jar file properly.", e );
            }
        }
        return updated;
    }

    @Override
    public String toString()
    {
        return ID;
    }
}
