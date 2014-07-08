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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.maven.index.*;
import org.apache.maven.index.ArtifactAvailability;
import org.apache.maven.index.artifact.Gav;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.locator.JavadocLocator;
import org.apache.maven.index.locator.Locator;
import org.apache.maven.index.locator.Sha1Locator;
import org.apache.maven.index.locator.SignatureLocator;
import org.apache.maven.index.locator.SourcesLocator;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * A minimal index creator used to provide basic information about Maven artifact. This creator will create the index
 * fast, will not open any file to be fastest as possible but it has some drawbacks: The information gathered by this
 * creator are sometimes based on "best-effort" only, and does not reflect the reality (ie. maven archetype packaging @see
 * {@link MavenArchetypeArtifactInfoIndexCreator}).
 * 
 * @author cstamas
 */
@Singleton
@Named (MinimalArtifactInfoIndexCreator.ID)
public class MinimalArtifactInfoIndexCreator
    extends AbstractIndexCreator
    implements LegacyDocumentUpdater
{
    public static final String ID = "min";

    /**
     * Info: packaging, lastModified, size, sourcesExists, javadocExists, signatureExists. Stored, not indexed.
     */
    public static final IndexerField FLD_INFO = new IndexerField( NEXUS.INFO, IndexerFieldVersion.V1, "i",
        "Artifact INFO (not indexed, stored)", Store.YES, Index.NO );

    public static final IndexerField FLD_GROUP_ID_KW = new IndexerField( MAVEN.GROUP_ID, IndexerFieldVersion.V1, "g",
        "Artifact GroupID (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_GROUP_ID = new IndexerField( MAVEN.GROUP_ID, IndexerFieldVersion.V3,
        "groupId", "Artifact GroupID (tokenized)", Store.NO, Index.ANALYZED );

    public static final IndexerField FLD_ARTIFACT_ID_KW = new IndexerField( MAVEN.ARTIFACT_ID, IndexerFieldVersion.V1,
        "a", "Artifact ArtifactID (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_ARTIFACT_ID = new IndexerField( MAVEN.ARTIFACT_ID, IndexerFieldVersion.V3,
        "artifactId", "Artifact ArtifactID (tokenized)", Store.NO, Index.ANALYZED );

    public static final IndexerField FLD_VERSION_KW = new IndexerField( MAVEN.VERSION, IndexerFieldVersion.V1, "v",
        "Artifact Version (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_VERSION = new IndexerField( MAVEN.VERSION, IndexerFieldVersion.V3, "version",
        "Artifact Version (tokenized)", Store.NO, Index.ANALYZED );

    public static final IndexerField FLD_PACKAGING = new IndexerField( MAVEN.PACKAGING, IndexerFieldVersion.V1, "p",
        "Artifact Packaging (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_EXTENSION = new IndexerField( MAVEN.EXTENSION, IndexerFieldVersion.V1, "e",
        "Artifact extension (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_CLASSIFIER = new IndexerField( MAVEN.CLASSIFIER, IndexerFieldVersion.V1, "l",
        "Artifact classifier (as keyword)", Store.NO, Index.NOT_ANALYZED );

    public static final IndexerField FLD_NAME = new IndexerField( MAVEN.NAME, IndexerFieldVersion.V1, "n",
        "Artifact name (tokenized, stored)", Store.YES, Index.ANALYZED );

    public static final IndexerField FLD_DESCRIPTION = new IndexerField( MAVEN.DESCRIPTION, IndexerFieldVersion.V1,
        "d", "Artifact description (tokenized, stored)", Store.YES, Index.ANALYZED );

    public static final IndexerField FLD_LAST_MODIFIED = new IndexerField( MAVEN.LAST_MODIFIED, IndexerFieldVersion.V1,
        "m", "Artifact last modified (not indexed, stored)", Store.YES, Index.NO );

    public static final IndexerField FLD_SHA1 = new IndexerField( MAVEN.SHA1, IndexerFieldVersion.V1, "1",
        "Artifact SHA1 checksum (as keyword, stored)", Store.YES, Index.NOT_ANALYZED );

    private Locator jl = new JavadocLocator();

    private Locator sl = new SourcesLocator();

    private Locator sigl = new SignatureLocator();

    private Locator sha1l = new Sha1Locator();

    public MinimalArtifactInfoIndexCreator()
    {
        super( ID );
    }

    public void populateArtifactInfo( ArtifactContext ac )
    {
        File artifact = ac.getArtifact();

        File pom = ac.getPom();

        ArtifactInfo ai = ac.getArtifactInfo();

        if ( pom != null && pom.isFile() )
        {
            ai.setLastModified( pom.lastModified() );

            ai.setFileExtension( "pom" );
        }

        // TODO handle artifacts without poms
        if ( pom != null && pom.isFile() )
        {
            if ( ai.getClassifier() != null )
            {
                ai.setSourcesExists( ArtifactAvailability.NOT_AVAILABLE );

                ai.setJavadocExists( ArtifactAvailability.NOT_AVAILABLE );
            }
            else
            {
                File sources = sl.locate( pom );
                if ( !sources.exists() )
                {
                    ai.setSourcesExists( ArtifactAvailability.NOT_PRESENT );
                }
                else
                {
                    ai.setSourcesExists( ArtifactAvailability.PRESENT );
                }

                File javadoc = jl.locate( pom );
                if ( !javadoc.exists() )
                {
                    ai.setJavadocExists( ArtifactAvailability.NOT_PRESENT );
                }
                else
                {
                    ai.setJavadocExists( ArtifactAvailability.PRESENT );
                }
            }
        }

        Model model = ac.getPomModel();

        if ( model != null )
        {
            ai.setName( model.getName() );

            ai.setDescription( model.getDescription() );

            // for main artifacts (without classifier) only:
            if ( ai.getClassifier() == null )
            {
                // only when this is not a classified artifact
                if ( model.getPackaging() != null )
                {
                    // set the read value that is coming from POM
                    ai.setPackaging( model.getPackaging() );
                }
                else
                {
                    // default it, since POM is present, is read, but does not contain explicit packaging
                    // TODO: this change breaks junit tests, but not sure why is "null" expected value?
                    ai.setPackaging( "jar" );
                }
            }
        }

        if ( "pom".equals( ai.getPackaging() ) )
        {
            // special case, the POM _is_ the artifact
            artifact = pom;
        }

        if ( artifact != null )
        {
            File signature = sigl.locate( artifact );

            ai.setSignatureExists( signature.exists() ? ArtifactAvailability.PRESENT : ArtifactAvailability.NOT_PRESENT );

            File sha1 = sha1l.locate( artifact );

            if ( sha1.exists() )
            {
                try
                {
                    ai.setSha1( StringUtils.chomp( FileUtils.fileRead( sha1 ) ).trim().split( " " )[0] );
                }
                catch ( IOException e )
                {
                    ac.addError( e );
                }
            }

            ai.setLastModified( artifact.lastModified() );

            ai.setSize( artifact.length() );

            ai.setFileExtension( getExtension( artifact, ac.getGav() ) );
        }
    }

    private String getExtension( File artifact, Gav gav )
    {
        if ( gav != null && StringUtils.isNotBlank( gav.getExtension() ) )
        {
            return gav.getExtension();
        }

        // last resort, the extension of the file
        String artifactFileName = artifact.getName().toLowerCase();

        // tar.gz? and other "special" combinations
        if ( artifactFileName.endsWith( "tar.gz" ) )
        {
            return "tar.gz";
        }
        else if ( artifactFileName.equals( "tar.bz2" ) )
        {
            return "tar.bz2";
        }

        // get the part after the last dot
        return FileUtils.getExtension( artifactFileName );
    }

    public void updateDocument( ArtifactInfo ai, Document doc )
    {
        String info =
            new StringBuilder().append( ArtifactInfo.nvl( ai.getPackaging() )).append( ArtifactInfo.FS ).append(
                Long.toString( ai.getLastModified() ) ).append( ArtifactInfo.FS ).append( Long.toString( ai.getSize() ) ).append(
                ArtifactInfo.FS ).append( ai.getSourcesExists().toString() ).append( ArtifactInfo.FS ).append(
                ai.getJavadocExists().toString() ).append( ArtifactInfo.FS ).append( ai.getSignatureExists().toString() ).append(
                ArtifactInfo.FS ).append( ai.getFileExtension() ).toString();

        doc.add( FLD_INFO.toField( info ) );

        doc.add( FLD_GROUP_ID_KW.toField( ai.getGroupId() ) );
        doc.add( FLD_ARTIFACT_ID_KW.toField( ai.getArtifactId() ) );
        doc.add( FLD_VERSION_KW.toField( ai.getVersion() ) );

        // V3
        doc.add( FLD_GROUP_ID.toField( ai.getGroupId() ) );
        doc.add( FLD_ARTIFACT_ID.toField( ai.getArtifactId() ) );
        doc.add( FLD_VERSION.toField( ai.getVersion() ) );
        doc.add( FLD_EXTENSION.toField( ai.getFileExtension() ) );

        if ( ai.getName() != null )
        {
            doc.add( FLD_NAME.toField( ai.getName() ) );
        }

        if ( ai.getDescription() != null )
        {
            doc.add( FLD_DESCRIPTION.toField( ai.getDescription() ) );
        }

        if ( ai.getPackaging() != null )
        {
            doc.add( FLD_PACKAGING.toField( ai.getPackaging() ) );
        }

        if ( ai.getClassifier() != null )
        {
            doc.add( FLD_CLASSIFIER.toField( ai.getClassifier() ) );
        }

        if ( ai.getSha1() != null )
        {
            doc.add( FLD_SHA1.toField( ai.getSha1() ) );
        }
    }

    public void updateLegacyDocument( ArtifactInfo ai, Document doc )
    {
        updateDocument( ai, doc );

        // legacy!
        if ( ai.getPrefix() != null )
        {
            doc.add( new Field( ArtifactInfo.PLUGIN_PREFIX, ai.getPrefix(), Field.Store.YES, Field.Index.NOT_ANALYZED ) );
        }

        if ( ai.getGoals() != null )
        {
            doc.add( new Field( ArtifactInfo.PLUGIN_GOALS, ArtifactInfo.lst2str( ai.getGoals() ), Field.Store.YES,
                Field.Index.NO ) );
        }

        doc.removeField( ArtifactInfo.GROUP_ID );
        doc.add( new Field( ArtifactInfo.GROUP_ID, ai.getGroupId(), Field.Store.NO, Field.Index.NOT_ANALYZED ) );
    }

    public boolean updateArtifactInfo( Document doc, ArtifactInfo ai )
    {
        boolean res = false;

        String uinfo = doc.get( ArtifactInfo.UINFO );

        if ( uinfo != null )
        {
            String[] r = ArtifactInfo.FS_PATTERN.split( uinfo );

            ai.setGroupId( r[0] );

            ai.setArtifactId( r[1] );

            ai.setVersion( r[2] );

            ai.setClassifier( ArtifactInfo.renvl( r[3] ) );

            if ( r.length > 4 ) 
            {
              ai.setFileExtension( r[4] );
            }

            res = true;
        }

        String info = doc.get( ArtifactInfo.INFO );

        if ( info != null )
        {
            String[] r = ArtifactInfo.FS_PATTERN.split( info );

            ai.setPackaging( ArtifactInfo.renvl( r[0] ));

            ai.setLastModified( Long.parseLong( r[1] ) );

            ai.setSize( Long.parseLong( r[2] ) );

            ai.setSourcesExists( ArtifactAvailability.fromString( r[ 3 ] ) );

            ai.setJavadocExists( ArtifactAvailability.fromString( r[ 4 ] ) );

            ai.setSignatureExists( ArtifactAvailability.fromString( r[ 5 ] ) );

            if ( r.length > 6 )
            {
                ai.setFileExtension( r[6] );
            }
            else
            {
                if ( ai.getClassifier() != null //
                    || "pom".equals( ai.getPackaging() ) //
                    || "war".equals( ai.getPackaging() ) //
                    || "ear".equals( ai.getPackaging() ) )
                {
                    ai.setFileExtension( ai.getPackaging() );
                }
                else
                {
                    ai.setFileExtension( "jar" ); // best guess
                }
            }

            res = true;
        }

        String name = doc.get( ArtifactInfo.NAME );

        if ( name != null )
        {
            ai.setName( name );

            res = true;
        }

        String description = doc.get( ArtifactInfo.DESCRIPTION );

        if ( description != null )
        {
            ai.setDescription( description );

            res = true;
        }

        // sometimes there's a pom without packaging(default to jar), but no artifact, then the value will be a "null"
        // String
        if ( "null".equals( ai.getPackaging() ) )
        {
            ai.setPackaging( null );
        }

        String sha1 = doc.get( ArtifactInfo.SHA1 );

        if ( sha1 != null )
        {
            ai.setSha1( sha1 );
        }

        return res;

        // artifactInfo.fname = ???
    }

    // ==

    @Override
    public String toString()
    {
        return ID;
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Arrays.asList( FLD_INFO, FLD_GROUP_ID_KW, FLD_GROUP_ID, FLD_ARTIFACT_ID_KW, FLD_ARTIFACT_ID,
            FLD_VERSION_KW, FLD_VERSION, FLD_PACKAGING, FLD_CLASSIFIER, FLD_NAME, FLD_DESCRIPTION, FLD_LAST_MODIFIED,
            FLD_SHA1 );
    }
}
