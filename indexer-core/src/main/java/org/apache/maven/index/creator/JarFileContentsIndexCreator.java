/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index.creator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.codehaus.plexus.component.annotations.Component;

/**
 * An index creator used to index Java class names from a Maven artifact. Will open up the JAR and collect all the class
 * names from it.
 */
@Component( role = IndexCreator.class, hint = JarFileContentsIndexCreator.ID )
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

    public void populateArtifactInfo( ArtifactContext artifactContext )
        throws IOException
    {
        ArtifactInfo ai = artifactContext.getArtifactInfo();

        File artifactFile = artifactContext.getArtifact();

        if ( artifactFile != null && artifactFile.isFile() && artifactFile.getName().endsWith( ".jar" ) )
        {
            updateArtifactInfo( ai, artifactFile );
        }
    }

    public void updateDocument( ArtifactInfo ai, Document doc )
    {
        if ( ai.classNames != null )
        {
            doc.add( FLD_CLASSNAMES_KW.toField( ai.classNames ) );
            doc.add( FLD_CLASSNAMES.toField( ai.classNames ) );
        }
    }

    public void updateLegacyDocument( ArtifactInfo ai, Document doc )
    {
        if ( ai.classNames != null )
        {
            String classNames = ai.classNames;

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

    public boolean updateArtifactInfo( Document doc, ArtifactInfo artifactInfo )
    {
        String names = doc.get( FLD_CLASSNAMES_KW.getKey() );

        if ( names != null )
        {
            if ( names.length() == 0 || names.charAt( 0 ) == '/' )
            {
                artifactInfo.classNames = names;
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
                artifactInfo.classNames = sb.toString();
            }

            return true;
        }

        return false;
    }

    private void updateArtifactInfo( ArtifactInfo ai, File f )
        throws IOException
    {
        ZipFile jar = null;

        try
        {
            jar = new ZipFile( f );

            StringBuilder sb = new StringBuilder();

            Enumeration<?> en = jar.entries();
            while ( en.hasMoreElements() )
            {
                ZipEntry e = (ZipEntry) en.nextElement();

                String name = e.getName();

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

                        // class name without ".class"
                        sb.append( name.substring( 0, name.length() - 6 ) ).append( '\n' );
                    }
                }
            }

            if ( sb.toString().trim().length() != 0 )
            {
                ai.classNames = sb.toString();
            }
            else
            {
                ai.classNames = null;
            }
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
                    jar.close();
                }
                catch ( Exception e )
                {
                    getLogger().error( "Could not close jar file properly.", e );
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return ID;
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_CLASSNAMES, FLD_CLASSNAMES_KW );
    }
}
