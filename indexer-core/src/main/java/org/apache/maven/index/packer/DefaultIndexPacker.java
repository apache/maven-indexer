package org.apache.maven.index.packer;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.incremental.IncrementalHandler;
import org.apache.maven.index.updater.IndexDataWriter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link IndexPacker} implementation. Creates the properties, legacy index zip and new gz files.
 *
 * @author Tamas Cservenak
 * @author Eugene Kuleshov
 */
@Singleton
@Named
public class DefaultIndexPacker
    implements IndexPacker
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private final IncrementalHandler incrementalHandler;


    @Inject
    public DefaultIndexPacker( IncrementalHandler incrementalHandler )
    {
        this.incrementalHandler = incrementalHandler;
    }

    public void packIndex( IndexPackingRequest request )
        throws IOException, IllegalArgumentException
    {
        if ( request.getTargetDir() == null )
        {
            throw new IllegalArgumentException( "The target dir is null" );
        }

        if ( request.getTargetDir().exists() )
        {
            if ( !request.getTargetDir().isDirectory() )
            {
                throw new IllegalArgumentException( //
                                                    String.format( "Specified target path %s is not a directory",
                                                                   request.getTargetDir().getAbsolutePath() ) );
            }
            if ( !request.getTargetDir().canWrite() )
            {
                throw new IllegalArgumentException( String.format( "Specified target path %s is not writtable",
                                                                   request.getTargetDir().getAbsolutePath() ) );
            }
        }
        else
        {
            if ( !request.getTargetDir().mkdirs() )
            {
                throw new IllegalArgumentException( "Can't create " + request.getTargetDir().getAbsolutePath() );
            }
        }

        // These are all of the files we'll be dealing with (except for the incremental chunks of course)
        File v1File = new File( request.getTargetDir(), IndexingContext.INDEX_FILE_PREFIX + ".gz" );

        Properties info = null;

        try
        {
            // Note that for incremental indexes to work properly, a valid index.properties file
            // must be present
            info = readIndexProperties( request );

            if ( request.isCreateIncrementalChunks() )
            {
                List<Integer> chunk = incrementalHandler.getIncrementalUpdates( request, info );

                if ( chunk == null )
                {
                    getLogger().debug( "Problem with Chunks, forcing regeneration of whole index" );
                    incrementalHandler.initializeProperties( info );
                }
                else if ( chunk.isEmpty() )
                {
                    getLogger().debug( "No incremental changes, not writing new incremental chunk" );
                }
                else
                {
                    File file = new File( request.getTargetDir(), //
                                          IndexingContext.INDEX_FILE_PREFIX + "." + info.getProperty(
                                              IndexingContext.INDEX_CHUNK_COUNTER ) + ".gz" );

                    writeIndexData( request, chunk, file );

                    if ( request.isCreateChecksumFiles() )
                    {
                        FileUtils.fileWrite(
                            new File( file.getParentFile(), file.getName() + ".sha1" ).getAbsolutePath(),
                            DigesterUtils.getSha1Digest( file ) );

                        FileUtils.fileWrite(
                            new File( file.getParentFile(), file.getName() + ".md5" ).getAbsolutePath(),
                            DigesterUtils.getMd5Digest( file ) );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            getLogger().info( "Unable to read properties file, will force index regeneration" );
            info = new Properties();
            incrementalHandler.initializeProperties( info );
        }

        Date timestamp = request.getContext().getTimestamp();

        if ( timestamp == null )
        {
            timestamp = new Date( 0 ); // never updated
        }

        if ( request.getFormats().contains( IndexPackingRequest.IndexFormat.FORMAT_V1 ) )
        {
            info.setProperty( IndexingContext.INDEX_TIMESTAMP, format( timestamp ) );

            writeIndexData( request, null, v1File );

            if ( request.isCreateChecksumFiles() )
            {
                FileUtils.fileWrite( new File( v1File.getParentFile(), v1File.getName() + ".sha1" ).getAbsolutePath(),
                                     DigesterUtils.getSha1Digest( v1File ) );

                FileUtils.fileWrite( new File( v1File.getParentFile(), v1File.getName() + ".md5" ).getAbsolutePath(),
                                     DigesterUtils.getMd5Digest( v1File ) );
            }
        }

        writeIndexProperties( request, info );
    }

    private Properties readIndexProperties( IndexPackingRequest request )
        throws IOException
    {
        File file = null;

        if ( request.isUseTargetProperties() || request.getContext().getIndexDirectoryFile() == null )
        {
            file = new File( request.getTargetDir(), IndexingContext.INDEX_REMOTE_PROPERTIES_FILE );
        }
        else
        {
            file =
                new File( request.getContext().getIndexDirectoryFile(), IndexingContext.INDEX_PACKER_PROPERTIES_FILE );
        }

        Properties properties = new Properties();

        FileInputStream fos = null;

        try
        {
            fos = new FileInputStream( file );
            properties.load( fos );
        }
        finally
        {
            if ( fos != null )
            {
                fos.close();
            }
        }

        return properties;
    }

    void writeIndexData( IndexPackingRequest request, List<Integer> docIndexes, File targetArchive )
        throws IOException
    {
        if ( targetArchive.exists() )
        {
            targetArchive.delete();
        }

        OutputStream os = null;

        try
        {
            os = new FileOutputStream( targetArchive );

            IndexDataWriter dw = new IndexDataWriter( os );
            dw.write( request.getContext(), request.getIndexReader(), docIndexes );

            os.flush();
        }
        finally
        {
            IOUtil.close( os );
        }
    }

    void writeIndexProperties( IndexPackingRequest request, Properties info )
        throws IOException
    {
        File propertyFile =
            new File( request.getContext().getIndexDirectoryFile(), IndexingContext.INDEX_PACKER_PROPERTIES_FILE );
        File targetPropertyFile = new File( request.getTargetDir(), IndexingContext.INDEX_REMOTE_PROPERTIES_FILE );

        info.setProperty( IndexingContext.INDEX_ID, request.getContext().getId() );

        OutputStream os = null;

        try
        {
            os = new FileOutputStream( propertyFile );

            info.store( os, null );
        }
        finally
        {
            IOUtil.close( os );
        }

        try
        {
            os = new FileOutputStream( targetPropertyFile );

            info.store( os, null );
        }
        finally
        {
            IOUtil.close( os );
        }

        if ( request.isCreateChecksumFiles() )
        {
            FileUtils.fileWrite( new File( targetPropertyFile.getParentFile(),
                                           targetPropertyFile.getName() + ".sha1" ).getAbsolutePath(),
                                 DigesterUtils.getSha1Digest( targetPropertyFile ) );

            FileUtils.fileWrite(
                new File( targetPropertyFile.getParentFile(), targetPropertyFile.getName() + ".md5" ).getAbsolutePath(),
                DigesterUtils.getMd5Digest( targetPropertyFile ) );
        }
    }

    private String format( Date d )
    {
        SimpleDateFormat df = new SimpleDateFormat( IndexingContext.INDEX_TIME_FORMAT );
        df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        return df.format( d );
    }
}
