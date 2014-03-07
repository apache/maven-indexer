package org.apache.maven.index.context;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.maven.index.ArtifactInfo;
import org.codehaus.plexus.util.FileUtils;

public class IndexUtils
{
    public static final String TIMESTAMP_FILE = "timestamp";

    private static final int BUFFER_SIZE = 16384;

    // Directory

    public static void copyDirectory( Directory source, Directory target )
        throws IOException
    {
        //FIXME: check if this copies too much, Lucene 4 has no filter for lucene files
        //Directory.copy( source, target, false );
        
        for (String file : source.listAll())
        {
            source.copy(target, file, file, IOContext.DEFAULT); 
        }

        copyFile( source, target, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE );
        copyFile( source, target, IndexingContext.INDEX_PACKER_PROPERTIES_FILE );

        Date ts = getTimestamp( source );
        updateTimestamp( target, ts );
    }

    public static boolean copyFile( Directory source, Directory target, String name )
        throws IOException
    {
        return copyFile( source, target, name, name );
    }

    public static boolean copyFile( Directory source, Directory target, String srcName, String targetName )
        throws IOException
    {
        if ( !source.fileExists( srcName ) )
        {
            return false;
        }

        byte[] buf = new byte[BUFFER_SIZE];

        IndexInput is = null;
        IndexOutput os = null;

        try
        {
            is = source.openInput( srcName, IOContext.DEFAULT);

            os = target.createOutput( targetName, IOContext.DEFAULT);

            // and copy to dest directory
            long len = is.length();
            long readCount = 0;
            while ( readCount < len )
            {
                int toRead = readCount + BUFFER_SIZE > len ? (int) ( len - readCount ) : BUFFER_SIZE;
                is.readBytes( buf, 0, toRead );
                os.writeBytes( buf, toRead );
                readCount += toRead;
            }

            return true;
        }
        finally
        {
            close( os );

            close( is );
        }
    }

    // timestamp

    public static ArtifactInfo constructArtifactInfo( Document doc, IndexingContext context )
    {
        // if no UINFO can't create, must be a different type of record
        if ( doc.get( ArtifactInfo.UINFO ) == null )
        {
            return null;
        }

        boolean res = false;

        ArtifactInfo artifactInfo = new ArtifactInfo();

        for ( IndexCreator ic : context.getIndexCreators() )
        {
            res |= ic.updateArtifactInfo( doc, artifactInfo );
        }

        return res ? artifactInfo : null;
    }

    public static Document updateDocument( Document doc, IndexingContext context )
    {
        return updateDocument( doc, context, true );
    }

    public static Document updateDocument( Document doc, IndexingContext context, boolean updateLastModified )
    {
        ArtifactInfo ai = constructArtifactInfo( doc, context );
        if ( ai == null )
        {
            return doc;
        }

        Document document = new Document();

        // unique key
        document.add( new Field( ArtifactInfo.UINFO, ai.getUinfo(), Field.Store.YES, Field.Index.NOT_ANALYZED ) );

        if ( updateLastModified || doc.getField( ArtifactInfo.LAST_MODIFIED ) == null )
        {
            document.add( new Field( ArtifactInfo.LAST_MODIFIED, //
                Long.toString( System.currentTimeMillis() ), Field.Store.YES, Field.Index.NO ) );
        }
        else
        {
            document.add( doc.getField( ArtifactInfo.LAST_MODIFIED ) );
        }

        for ( IndexCreator ic : context.getIndexCreators() )
        {
            ic.updateDocument( ai, document );
        }

        return document;
    }

    public static void deleteTimestamp( Directory directory )
        throws IOException
    {
        if ( directory.fileExists( TIMESTAMP_FILE ) )
        {
            directory.deleteFile( TIMESTAMP_FILE );
        }
    }

    public static void updateTimestamp( Directory directory, Date timestamp )
        throws IOException
    {
        synchronized ( directory )
        {
            Date currentTimestamp = getTimestamp( directory );

            if ( timestamp != null && ( currentTimestamp == null || !currentTimestamp.equals( timestamp ) ) )
            {
                deleteTimestamp( directory );

                IndexOutput io = directory.createOutput( TIMESTAMP_FILE, IOContext.DEFAULT);

                try
                {
                    io.writeLong( timestamp.getTime() );

                    io.flush();
                }
                finally
                {
                    close( io );
                }
            }
        }
    }

    public static Date getTimestamp( Directory directory )
    {
        synchronized ( directory )
        {
            Date result = null;
            try
            {
                if ( directory.fileExists( TIMESTAMP_FILE ) )
                {
                    IndexInput ii = null;

                    try
                    {
                        ii = directory.openInput( TIMESTAMP_FILE, IOContext.DEFAULT);

                        result = new Date( ii.readLong() );
                    }
                    finally
                    {
                        close( ii );
                    }
                }
            }
            catch ( IOException ex )
            {
            }

            return result;
        }
    }

    // close helpers

    public static void close( OutputStream os )
    {
        if ( os != null )
        {
            try
            {
                os.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( InputStream is )
    {
        if ( is != null )
        {
            try
            {
                is.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( IndexOutput io )
    {
        if ( io != null )
        {
            try
            {
                io.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( IndexInput in )
    {
        if ( in != null )
        {
            try
            {
                in.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( IndexReader r )
    {
        if ( r != null )
        {
            try
            {
                r.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( IndexWriter w )
    {
        if ( w != null )
        {
            try
            {
                w.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void close( Directory d )
    {
        if ( d != null )
        {
            try
            {
                d.close();
            }
            catch ( IOException e )
            {
                // ignore
            }
        }
    }

    public static void delete( File indexDir )
    {
        try
        {
            FileUtils.deleteDirectory( indexDir );
        }
        catch ( IOException ex )
        {
            // ignore
        }
    }

}
