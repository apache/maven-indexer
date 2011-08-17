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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.OSGI;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.util.zip.ZipFacade;
import org.apache.maven.index.util.zip.ZipHandle;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This indexCreator will index some OSGI metadatas.
 * <br/>
 * All jars are indexed and not only the ones with packaging bundle.
 * <br/>
 * <p>
 * OSGI metadatas indexed :
 * <ul>
 *   <li>Bundle-SymbolicName</li>
 *   <li>Bundle-Version</li>
 *   <li>Export-Package</li>
 *   <li>Export-Service</li>
 * </ul>
 * </p>
 * @author Olivier Lamy
 * @since 4.1.2
 */
@Component( role = IndexCreator.class, hint = OSGIArtifactIndexCreator.ID )
public class OSGIArtifactIndexCreator
    extends AbstractIndexCreator
{
    public static final String ID = "osgi-metadatas";

    private static final String BSN = "Bundle-SymbolicName";

    public static final IndexerField FLD_BUNDLE_SYMBOLIC_NAME =
        new IndexerField( OSGI.SYMBOLIC_NAME, IndexerFieldVersion.V4, "bsn", "Bundle-SymbolicName (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );


    private static final String BV = "Bundle-Version";

    public static final IndexerField FLD_BUNDLE_VERSION =
        new IndexerField( OSGI.VERSION, IndexerFieldVersion.V4, "bv", "Bundle-Version (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );


    private static final String BEP = "Export-Package";

    public static final IndexerField FLD_BUNDLE_EXPORT_PACKAGE =
        new IndexerField( OSGI.EXPORT_PACKAGE, IndexerFieldVersion.V4, "bep", "Export-Package (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );

    private static final String BES = "Export-Service";

    public static final IndexerField FLD_BUNDLE_EXPORT_SERVIVE =
        new IndexerField( OSGI.EXPORT_SERVICE, IndexerFieldVersion.V4, "bes", "Export-Service (indexed, stored)",
                          Field.Store.YES, Field.Index.ANALYZED );

    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_BUNDLE_SYMBOLIC_NAME, FLD_BUNDLE_VERSION, FLD_BUNDLE_EXPORT_PACKAGE,
                              FLD_BUNDLE_EXPORT_SERVIVE );
    }
    
    public OSGIArtifactIndexCreator()
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
