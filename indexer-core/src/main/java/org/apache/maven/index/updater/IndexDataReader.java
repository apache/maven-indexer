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

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;

/**
 * An index data reader used to parse transfer index format.
 * 
 * @author Eugene Kuleshov
 */
public class IndexDataReader
{
    private final DataInputStream dis;

    public IndexDataReader( final InputStream is )
        throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(is, 1024 * 8);
        // MINDEXER-13
        // LightweightHttpWagon may have performed automatic decompression
        // Handle it transparently
        bis.mark( 2 );
        InputStream data;
        if ( bis.read() == 0x1f && bis.read() == 0x8b ) // GZIPInputStream.GZIP_MAGIC
        {
            bis.reset();
            data = new GZIPInputStream( bis, 1024 * 8 );
        }
        else
        {
            bis.reset();
            data = bis;
        }

        this.dis = new DataInputStream( data );
    }

    public IndexDataReadResult readIndex( IndexWriter w, IndexingContext context )
        throws IOException
    {
        long timestamp = readHeader();

        Date date = null;

        if ( timestamp != -1 )
        {
            date = new Date( timestamp );

            IndexUtils.updateTimestamp( w.getDirectory(), date );
        }

        int n = 0;

        Document doc;
        while ( ( doc = readDocument() ) != null )
        {
            w.addDocument( IndexUtils.updateDocument( doc, context, false ) );

            n++;
        }

        w.commit();

        IndexDataReadResult result = new IndexDataReadResult();
        result.setDocumentCount( n );
        result.setTimestamp( date );
        return result;
    }

    public long readHeader()
        throws IOException
    {
        final byte HDRBYTE = (byte) ( ( IndexDataWriter.VERSION << 24 ) >> 24 );

        if ( HDRBYTE != dis.readByte() )
        {
            // data format version mismatch
            throw new IOException( "Provided input contains unexpected data (0x01 expected as 1st byte)!" );
        }

        return dis.readLong();
    }

    public Document readDocument()
        throws IOException
    {
        int fieldCount;
        try
        {
            fieldCount = dis.readInt();
        }
        catch ( EOFException ex )
        {
            return null; // no more documents
        }

        Document doc = new Document();

        for ( int i = 0; i < fieldCount; i++ )
        {
            doc.add( readField() );
        }

        return doc;
    }

    private Field readField()
        throws IOException
    {
        int flags = dis.read();

        Index index = Index.NO;
        if ( ( flags & IndexDataWriter.F_INDEXED ) > 0 )
        {
            boolean isTokenized = ( flags & IndexDataWriter.F_TOKENIZED ) > 0;
            index = isTokenized ? Index.ANALYZED : Index.NOT_ANALYZED;
        }

        Store store = Store.NO;
        if ( ( flags & IndexDataWriter.F_STORED ) > 0 )
        {
            store = Store.YES;
        }

        String name = dis.readUTF();
        String value = readUTF( dis );

        return new Field( name, value, store, index );
    }

    private static String readUTF( DataInput in )
        throws IOException
    {
        int utflen = in.readInt();

        byte[] bytearr;
        char[] chararr;

        try
        {
            bytearr = new byte[utflen];
            chararr = new char[utflen];
        }
        catch ( OutOfMemoryError e )
        {
            final IOException ex =
                new IOException(
                    "Index data content is inappropriate (is junk?), leads to OutOfMemoryError! See MINDEXER-28 for more information!" );
            ex.initCause( e );
            throw ex;
        }

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        in.readFully( bytearr, 0, utflen );

        while ( count < utflen )
        {
            c = bytearr[count] & 0xff;
            if ( c > 127 )
            {
                break;
            }
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while ( count < utflen )
        {
            c = bytearr[count] & 0xff;
            switch ( c >> 4 )
            {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx */
                    count++;
                    chararr[chararr_count++] = (char) c;
                    break;

                case 12:
                case 13:
                    /* 110x xxxx 10xx xxxx */
                    count += 2;
                    if ( count > utflen )
                    {
                        throw new UTFDataFormatException( "malformed input: partial character at end" );
                    }
                    char2 = bytearr[count - 1];
                    if ( ( char2 & 0xC0 ) != 0x80 )
                    {
                        throw new UTFDataFormatException( "malformed input around byte " + count );
                    }
                    chararr[chararr_count++] = (char) ( ( ( c & 0x1F ) << 6 ) | ( char2 & 0x3F ) );
                    break;

                case 14:
                    /* 1110 xxxx 10xx xxxx 10xx xxxx */
                    count += 3;
                    if ( count > utflen )
                    {
                        throw new UTFDataFormatException( "malformed input: partial character at end" );
                    }
                    char2 = bytearr[count - 2];
                    char3 = bytearr[count - 1];
                    if ( ( ( char2 & 0xC0 ) != 0x80 ) || ( ( char3 & 0xC0 ) != 0x80 ) )
                    {
                        throw new UTFDataFormatException( "malformed input around byte " + ( count - 1 ) );
                    }
                    chararr[chararr_count++] =
                        (char) ( ( ( c & 0x0F ) << 12 ) | ( ( char2 & 0x3F ) << 6 ) | ( ( char3 & 0x3F ) << 0 ) );
                    break;

                default:
                    /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException( "malformed input around byte " + count );
            }
        }

        // The number of chars produced may be less than utflen
        return new String( chararr, 0, chararr_count );
    }

    /**
     * An index data read result holder
     */
    public static class IndexDataReadResult
    {
        private Date timestamp;

        private int documentCount;

        public void setDocumentCount( int documentCount )
        {
            this.documentCount = documentCount;
        }

        public int getDocumentCount()
        {
            return documentCount;
        }

        public void setTimestamp( Date timestamp )
        {
            this.timestamp = timestamp;
        }

        public Date getTimestamp()
        {
            return timestamp;
        }

    }

    /**
     * Reads index content by using a visitor. <br>
     * The visitor is called for each read documents after it has been populated with Lucene fields.
     * 
     * @param visitor an index data visitor
     * @param context indexing context
     * @return statistics about read data
     * @throws IOException in case of an IO exception during index file access
     */
    public IndexDataReadResult readIndex( final IndexDataReadVisitor visitor, final IndexingContext context )
        throws IOException
    {
        dis.readByte(); // data format version

        long timestamp = dis.readLong();

        Date date = null;

        if ( timestamp != -1 )
        {
            date = new Date( timestamp );
        }

        int n = 0;

        Document doc;
        while ( ( doc = readDocument() ) != null )
        {
            visitor.visitDocument( IndexUtils.updateDocument( doc, context, false ) );

            n++;
        }

        IndexDataReadResult result = new IndexDataReadResult();
        result.setDocumentCount( n );
        result.setTimestamp( date );
        return result;
    }

    /**
     * Visitor of indexed Lucene documents.
     */
    public static interface IndexDataReadVisitor
    {

        /**
         * Called on each read document. The document is already populated with fields.
         * 
         * @param document read document
         */
        void visitDocument( Document document );

    }

}
