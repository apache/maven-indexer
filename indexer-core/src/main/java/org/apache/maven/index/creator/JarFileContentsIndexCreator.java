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
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.util.zip.ZipFacade;
import org.apache.maven.index.util.zip.ZipHandle;
import org.codehaus.plexus.util.StringUtils;

/**
 * An index creator used to index Java class names from a Maven artifact (JAR or WAR for now). Will open up the file and
 * collect all the class names from it.
 */
@Singleton
@Named (JarFileContentsIndexCreator.ID)
public class JarFileContentsIndexCreator
    extends AbstractIndexCreator
    implements LegacyDocumentUpdater
{
    public static final String ID = "jarContent";

    public static final IndexerField FLD_CLASSNAMES = new IndexerField( MAVEN.CLASSNAMES, IndexerFieldVersion.V3,
        "classnames", "Artifact Classes (tokenized)", Store.NO, Index.ANALYZED );

    /**
     * NexusAnalyzer makes exception with this field only, to keep backward compatibility with old consumers of
     * nexus-indexer. This field is here for "backward" compat only! The order is important too! FLD_CLASSNAMES must be
     * registered BEFORE FLD_CLASSNAMES_KW!
     */
    public static final IndexerField FLD_CLASSNAMES_KW = new IndexerField( MAVEN.CLASSNAMES, IndexerFieldVersion.V1,
        "c", "Artifact Classes (tokenized on newlines only)", Store.YES, Index.ANALYZED );

    public JarFileContentsIndexCreator()
    {
        super( ID );
    }

    public void populateArtifactInfo( final ArtifactContext artifactContext )
        throws IOException
    {
        ArtifactInfo ai = artifactContext.getArtifactInfo();

        File artifactFile = artifactContext.getArtifact();

        if ( artifactFile != null && artifactFile.isFile()
            && ( artifactFile.getName().endsWith( ".jar" ) || artifactFile.getName().endsWith( ".war" ) ) )
        {
            updateArtifactInfo( ai, artifactFile );
        }
    }

    public void updateDocument( final ArtifactInfo ai, final Document doc )
    {
        if ( ai.getClassNames() != null )
        {
            doc.add( FLD_CLASSNAMES_KW.toField( ai.getClassNames() ) );
            doc.add( FLD_CLASSNAMES.toField( ai.getClassNames() ) );
        }
    }

    public void updateLegacyDocument( final ArtifactInfo ai, final Document doc )
    {
        if ( ai.getClassNames() != null )
        {
            String classNames = ai.getClassNames();

            // downgrade the classNames if needed
            if ( classNames.length() > 0 && classNames.charAt( 0 ) == '/' )
            {
                // conversion from the new format
                String[] lines = classNames.split( "\\n" );
                StringBuilder sb = new StringBuilder();
                for ( String line : lines )
                {
                    sb.append( line.substring( 1 ) ).append( '\n' );
                }

                classNames = sb.toString();
            }

            doc.add( FLD_CLASSNAMES_KW.toField( classNames ) );
        }
    }

    public boolean updateArtifactInfo( final Document doc, final ArtifactInfo artifactInfo )
    {
        String names = doc.get( FLD_CLASSNAMES_KW.getKey() );

        if ( names != null )
        {
            if ( names.length() == 0 || names.charAt( 0 ) == '/' )
            {
                artifactInfo.setClassNames( names );
            }
            else
            {
                // conversion from the old format
                String[] lines = names.split( "\\n" );
                StringBuilder sb = new StringBuilder();
                for ( String line : lines )
                {
                    sb.append( '/' ).append( line ).append( '\n' );
                }
                artifactInfo.setClassNames( sb.toString() );
            }

            return true;
        }

        return false;
    }

    private void updateArtifactInfo( final ArtifactInfo ai, final File f )
        throws IOException
    {
        if ( f.getName().endsWith( ".jar" ) )
        {
            updateArtifactInfo( ai, f, null );
        }
        else if ( f.getName().endsWith( ".war" ) )
        {
            updateArtifactInfo( ai, f, "WEB-INF/classes/" );
        }
    }

    private void updateArtifactInfo( final ArtifactInfo ai, final File f, final String strippedPrefix )
        throws IOException
    {
        ZipHandle handle = null;

        try
        {
            handle = ZipFacade.getZipHandle( f );

            final List<String> entries = handle.getEntries();

            final StringBuilder sb = new StringBuilder();

            for ( String name : entries )
            {
                if ( name.endsWith( ".class" ) )
                {
                    // TODO verify if class is public or protected
                    // TODO skip all inner classes for now

                    int i = name.indexOf( "$" );

                    if ( i == -1 )
                    {
                        if ( name.charAt( 0 ) != '/' )
                        {
                            sb.append( '/' );
                        }

                        if ( StringUtils.isBlank( strippedPrefix ) )
                        {
                            // class name without ".class"
                            sb.append( name.substring( 0, name.length() - 6 ) ).append( '\n' );
                        }
                        else if ( name.startsWith( strippedPrefix ) && (name.length() > ( strippedPrefix.length() + 6 )) )
                        {
                            // class name without ".class" and stripped prefix
                            sb.append( name.substring( strippedPrefix.length(), name.length() - 6 ) ).append( '\n' );
                        }
                    }
                }
            }

            final String fieldValue = sb.toString().trim();

            if ( fieldValue.length() != 0 )
            {
                ai.setClassNames( fieldValue );
            }
            else
            {
                ai.setClassNames( null );
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
    }

    @Override
    public String toString()
    {
        return ID;
    }

    @Override
    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_CLASSNAMES, FLD_CLASSNAMES_KW );
    }
}
