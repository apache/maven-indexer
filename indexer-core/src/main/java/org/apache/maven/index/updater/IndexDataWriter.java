package org.apache.maven.index.updater;

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

import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexingContext;

/**
 * An index data writer used to write transfer index format.
 * 
 * @author Eugene Kuleshov
 */
public class IndexDataWriter
{
    static final int VERSION = 1;

    static final int F_INDEXED = 1;

    static final int F_TOKENIZED = 2;

    static final int F_STORED = 4;

    static final int F_COMPRESSED = 8;

    private final DataOutputStream dos;

    private final GZIPOutputStream gos;

    private final BufferedOutputStream bos;

    private final Set<String> allGroups;

    private final Set<String> rootGroups;

    private boolean descriptorWritten;

    public IndexDataWriter( OutputStream os )
        throws IOException
    {
        bos = new BufferedOutputStream( os, 1024 * 8 );
        gos = new GZIPOutputStream( bos, 1024 * 2 );
        dos = new DataOutputStream( gos );

        this.allGroups = new HashSet<String>();
        this.rootGroups = new HashSet<String>();
        this.descriptorWritten = false;
    }

    public int write( IndexingContext context, IndexReader indexReader, List<Integer> docIndexes )
        throws IOException
    {
        writeHeader( context );

        int n = writeDocuments( indexReader, docIndexes );

        writeGroupFields();

        close();

        return n;
    }

    public void close()
        throws IOException
    {
        dos.flush();

        gos.flush();
        gos.finish();

        bos.flush();
    }

    public void writeHeader( IndexingContext context )
        throws IOException
    {
        dos.writeByte( VERSION );

        Date timestamp = context.getTimestamp();
        dos.writeLong( timestamp == null ? -1 : timestamp.getTime() );
    }

    public void writeGroupFields()
        throws IOException
    {
        {
            List<IndexableField> allGroupsFields = new ArrayList<>( 2 );
            allGroupsFields.add( new StringField( ArtifactInfo.ALL_GROUPS, ArtifactInfo.ALL_GROUPS_VALUE, Store.YES));
            allGroupsFields.add( new StringField( ArtifactInfo.ALL_GROUPS_LIST, ArtifactInfo.lst2str( allGroups ), Store.YES) );
            writeDocumentFields( allGroupsFields );
        }

        {
            List<IndexableField> rootGroupsFields = new ArrayList<>( 2 );
            rootGroupsFields.add( new StringField( ArtifactInfo.ROOT_GROUPS, ArtifactInfo.ROOT_GROUPS_VALUE, Store.YES) );
            rootGroupsFields.add( new StringField( ArtifactInfo.ROOT_GROUPS_LIST, ArtifactInfo.lst2str( rootGroups ), Store.YES ));
            writeDocumentFields( rootGroupsFields );
        }
    }

    public int writeDocuments( IndexReader r, List<Integer> docIndexes )
        throws IOException
    {
        int n = 0;
        Bits liveDocs = MultiFields.getLiveDocs(r);

        if ( docIndexes == null )
        {
            for ( int i = 0; i < r.maxDoc(); i++ )
            {
                if (liveDocs == null || liveDocs.get(i) )
                {
                    if ( writeDocument( r.document( i ) ) )
                    {
                        n++;
                    }
                }
            }
        }
        else
        {
            for ( int i : docIndexes )
            {
                if ( liveDocs == null || liveDocs.get(i) )
                {
                    if ( writeDocument( r.document( i ) ) )
                    {
                        n++;
                    }
                }
            }
        }

        return n;
    }

    public boolean writeDocument( final Document document )
        throws IOException
    {
        List<IndexableField> fields = document.getFields();

        List<IndexableField> storedFields = new ArrayList<>( fields.size() );

        for (IndexableField field : fields )
        {
            if ( DefaultIndexingContext.FLD_DESCRIPTOR.equals( field.name() ) )
            {
                if ( descriptorWritten )
                {
                    return false;
                }
                else
                {
                    descriptorWritten = true;
                }
            }

            if ( ArtifactInfo.ALL_GROUPS.equals( field.name() ) )
            {
                final String groupList = document.get( ArtifactInfo.ALL_GROUPS_LIST );

                if ( groupList != null && groupList.trim().length() > 0 )
                {
                    allGroups.addAll( ArtifactInfo.str2lst( groupList ) );
                }

                return false;
            }

            if ( ArtifactInfo.ROOT_GROUPS.equals( field.name() ) )
            {
                final String groupList = document.get( ArtifactInfo.ROOT_GROUPS_LIST );

                if ( groupList != null && groupList.trim().length() > 0 )
                {
                    rootGroups.addAll( ArtifactInfo.str2lst( groupList ) );
                }

                return false;
            }

            if ( field.fieldType().stored())
            {
                storedFields.add( field );
            }
        }

        writeDocumentFields( storedFields );

        return true;
    }

    public void writeDocumentFields( List<IndexableField> fields )
        throws IOException
    {
        dos.writeInt( fields.size() );

        for ( IndexableField field : fields )
        {
            writeField( field );
        }
    }

    public void writeField( IndexableField field )
        throws IOException
    {
        int flags = ( field.fieldType().indexed() ? F_INDEXED : 0 ) //
            + ( field.fieldType().tokenized() ? F_TOKENIZED : 0 ) //
            + ( field.fieldType().stored() ? F_STORED : 0 ); //
        // + ( false ? F_COMPRESSED : 0 ); // Compressed not supported anymore

        String name = field.name();
        String value = field.stringValue();

        dos.write( flags );
        dos.writeUTF( name );
        writeUTF( value, dos );
    }

    private static void writeUTF( String str, DataOutput out )
        throws IOException
    {
        int strlen = str.length();
        int utflen = 0;
        int c;

        // use charAt instead of copying String to char array
        for ( int i = 0; i < strlen; i++ )
        {
            c = str.charAt( i );
            if ( ( c >= 0x0001 ) && ( c <= 0x007F ) )
            {
                utflen++;
            }
            else if ( c > 0x07FF )
            {
                utflen += 3;
            }
            else
            {
                utflen += 2;
            }
        }

        // TODO optimize storing int value
        out.writeInt( utflen );

        byte[] bytearr = new byte[utflen];

        int count = 0;

        int i = 0;
        for ( ; i < strlen; i++ )
        {
            c = str.charAt( i );
            if ( !( ( c >= 0x0001 ) && ( c <= 0x007F ) ) )
            {
                break;
            }
            bytearr[count++] = (byte) c;
        }

        for ( ; i < strlen; i++ )
        {
            c = str.charAt( i );
            if ( ( c >= 0x0001 ) && ( c <= 0x007F ) )
            {
                bytearr[count++] = (byte) c;

            }
            else if ( c > 0x07FF )
            {
                bytearr[count++] = (byte) ( 0xE0 | ( ( c >> 12 ) & 0x0F ) );
                bytearr[count++] = (byte) ( 0x80 | ( ( c >> 6 ) & 0x3F ) );
                bytearr[count++] = (byte) ( 0x80 | ( ( c >> 0 ) & 0x3F ) );
            }
            else
            {
                bytearr[count++] = (byte) ( 0xC0 | ( ( c >> 6 ) & 0x1F ) );
                bytearr[count++] = (byte) ( 0x80 | ( ( c >> 0 ) & 0x3F ) );
            }
        }

        out.write( bytearr, 0, utflen );
    }

}
