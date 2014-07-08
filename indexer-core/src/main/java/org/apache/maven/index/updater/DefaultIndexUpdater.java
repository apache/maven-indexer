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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusAnalyzer;
import org.apache.maven.index.context.NexusIndexWriter;
import org.apache.maven.index.fs.Lock;
import org.apache.maven.index.fs.Locker;
import org.apache.maven.index.incremental.IncrementalHandler;
import org.apache.maven.index.updater.IndexDataReader.IndexDataReadResult;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default index updater implementation
 * 
 * @author Jason van Zyl
 * @author Eugene Kuleshov
 */
@Singleton
@Named
public class DefaultIndexUpdater
    implements IndexUpdater
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private final IncrementalHandler incrementalHandler;

    private final List<IndexUpdateSideEffect> sideEffects;


    @Inject
    public DefaultIndexUpdater( final IncrementalHandler incrementalHandler, final List<IndexUpdateSideEffect> sideEffects )
    {
        this.incrementalHandler = incrementalHandler;
        this.sideEffects = sideEffects;
    }

    public IndexUpdateResult fetchAndUpdateIndex( final IndexUpdateRequest updateRequest )
        throws IOException
    {
        IndexUpdateResult result = new IndexUpdateResult();

        IndexingContext context = updateRequest.getIndexingContext();

        ResourceFetcher fetcher = null;

        if ( !updateRequest.isOffline() )
        {
            fetcher = updateRequest.getResourceFetcher();

            // If no resource fetcher passed in, use the wagon fetcher by default
            // and put back in request for future use
            if ( fetcher == null )
            {
                throw new IOException( "Update of the index without provided ResourceFetcher is impossible." );
            }

            fetcher.connect( context.getId(), context.getIndexUpdateUrl() );
        }

        File cacheDir = updateRequest.getLocalIndexCacheDir();
        Locker locker = updateRequest.getLocker();
        Lock lock = locker != null && cacheDir != null ? locker.lock( cacheDir ) : null;
        try
        {
            if ( cacheDir != null )
            {
                LocalCacheIndexAdaptor cache = new LocalCacheIndexAdaptor( cacheDir, result );

                if ( !updateRequest.isOffline() )
                {
                    cacheDir.mkdirs();

                    try
                    {
                        if( fetchAndUpdateIndex( updateRequest, fetcher, cache ).isSuccessful() )
                        {
                            cache.commit();
                        }
                    }
                    finally
                    {
                        fetcher.disconnect();
                    }
                }

                fetcher = cache.getFetcher();
            }
            else if ( updateRequest.isOffline() )
            {
                throw new IllegalArgumentException( "LocalIndexCacheDir can not be null in offline mode" );
            }

            try
            {
                if ( !updateRequest.isCacheOnly() )
                {
                    LuceneIndexAdaptor target = new LuceneIndexAdaptor( updateRequest );
                    result = fetchAndUpdateIndex( updateRequest, fetcher, target );
                    
                    if(result.isSuccessful())
                    {
                        target.commit();
                    }
                }
            }
            finally
            {
                fetcher.disconnect();
            }
        }
        finally
        {
            if ( lock != null )
            {
                lock.release();
            }
        }

        return result;
    }

    private Date loadIndexDirectory( final IndexUpdateRequest updateRequest, final ResourceFetcher fetcher,
                                     final boolean merge, final String remoteIndexFile )
        throws IOException
    {
        File indexDir = File.createTempFile( remoteIndexFile, ".dir" );
        indexDir.delete();
        indexDir.mkdirs();

        final Directory directory = updateRequest.getFSDirectoryFactory().open( indexDir );

        BufferedInputStream is = null;

        try
        {
            is = new BufferedInputStream( fetcher.retrieve( remoteIndexFile ) );

            Date timestamp = null;

            if ( remoteIndexFile.endsWith( ".gz" ) )
            {
                timestamp = unpackIndexData( is, directory, //
                    updateRequest.getIndexingContext() );
            }
            else
            {
                // legacy transfer format
                throw new IllegalArgumentException("The legacy format is no longer supported by this version of maven-indexer.");
            }

            if ( updateRequest.getDocumentFilter() != null )
            {
                filterDirectory( directory, updateRequest.getDocumentFilter() );
            }

            if ( merge )
            {
                updateRequest.getIndexingContext().merge( directory );
            }
            else
            {
                updateRequest.getIndexingContext().replace( directory );
            }
            if ( sideEffects != null && sideEffects.size() > 0 )
            {
                getLogger().info( IndexUpdateSideEffect.class.getName() + " extensions found: " + sideEffects.size() );
                for ( IndexUpdateSideEffect sideeffect : sideEffects )
                {
                    sideeffect.updateIndex( directory, updateRequest.getIndexingContext(), merge );
                }
            }

            return timestamp;
        }
        finally
        {
            IOUtil.close( is );

            if ( directory != null )
            {
                directory.close();
            }

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

    private static void filterDirectory( final Directory directory, final DocumentFilter filter )
        throws IOException
    {
        IndexReader r = null;
        IndexWriter w = null;
        try
        {
            r = IndexReader.open( directory );
            w = new NexusIndexWriter( directory, new NexusAnalyzer(), false );
            
            Bits liveDocs = MultiFields.getLiveDocs(r);

            int numDocs = r.maxDoc();

            for ( int i = 0; i < numDocs; i++ )
            {
                if (liveDocs != null && ! liveDocs.get(i) )
                {
                    continue;
                }

                Document d = r.document( i );

                if ( !filter.accept( d ) )
                {
                    boolean success = w.tryDeleteDocument(r, i);
                    //FIXME handle deletion failure
                }
            }
            w.commit();
        }
        finally
        {
            IndexUtils.close( r );
            IndexUtils.close( w );
        }

        w = null;
        try
        {
            // analyzer is unimportant, since we are not adding/searching to/on index, only reading/deleting
            w = new NexusIndexWriter( directory, new NexusAnalyzer(), false );

            w.forceMerge(4);
            w.commit();
        }
        finally
        {
            IndexUtils.close( w );
        }
    }

    private Properties loadIndexProperties( final File indexDirectoryFile, final String remoteIndexPropertiesName )
    {
        File indexProperties = new File( indexDirectoryFile, remoteIndexPropertiesName );

        FileInputStream fis = null;

        try
        {
            Properties properties = new Properties();

            fis = new FileInputStream( indexProperties );

            properties.load( fis );

            return properties;
        }
        catch ( IOException e )
        {
            getLogger().debug( "Unable to read remote properties stored locally", e );
        }
        finally
        {
            IOUtil.close( fis );
        }

        return null;
    }

    private void storeIndexProperties( final File dir, final String indexPropertiesName, final Properties properties )
        throws IOException
    {
        File file = new File( dir, indexPropertiesName );

        if ( properties != null )
        {
            OutputStream os = new BufferedOutputStream( new FileOutputStream( file ) );
            try
            {
                properties.store( os, null );
            }
            finally
            {
                IOUtil.close( os );
            }
        }
        else
        {
            file.delete();
        }
    }

    private Properties downloadIndexProperties( final ResourceFetcher fetcher )
        throws IOException
    {
        InputStream fis = fetcher.retrieve( IndexingContext.INDEX_REMOTE_PROPERTIES_FILE );

        try
        {
            Properties properties = new Properties();

            properties.load( fis );

            return properties;
        }
        finally
        {
            IOUtil.close( fis );
        }
    }

    public Date getTimestamp( final Properties properties, final String key )
    {
        String indexTimestamp = properties.getProperty( key );

        if ( indexTimestamp != null )
        {
            try
            {
                SimpleDateFormat df = new SimpleDateFormat( IndexingContext.INDEX_TIME_FORMAT );
                df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
                return df.parse( indexTimestamp );
            }
            catch ( ParseException ex )
            {
            }
        }
        return null;
    }

    /**
     * Unpack index data using specified Lucene Index writer
     * 
     * @param is an input stream to unpack index data from
     * @param w a writer to save index data
     * @param ics a collection of index creators for updating unpacked documents.
     */
    public static Date unpackIndexData( final InputStream is, final Directory d, final IndexingContext context )
        throws IOException
    {
        NexusIndexWriter w = new NexusIndexWriter( d, new NexusAnalyzer(), true );
        try
        {
            IndexDataReader dr = new IndexDataReader( is );

            IndexDataReadResult result = dr.readIndex( w, context );

            return result.getTimestamp();
        }
        finally
        {
            IndexUtils.close( w );
        }
    }

    /**
     * Filesystem-based ResourceFetcher implementation
     */
    public static class FileFetcher
        implements ResourceFetcher
    {
        private final File basedir;

        public FileFetcher( File basedir )
        {
            this.basedir = basedir;
        }

        public void connect( String id, String url )
            throws IOException
        {
            // don't need to do anything
        }

        public void disconnect()
            throws IOException
        {
            // don't need to do anything
        }

        public void retrieve( String name, File targetFile )
            throws IOException, FileNotFoundException
        {
            FileUtils.copyFile( getFile( name ), targetFile );

        }

        public InputStream retrieve( String name )
            throws IOException, FileNotFoundException
        {
            return new FileInputStream( getFile( name ) );
        }

        private File getFile( String name )
        {
            return new File( basedir, name );
        }

    }

    private abstract class IndexAdaptor
    {
        protected final File dir;

        protected Properties properties;

        protected IndexAdaptor( File dir )
        {
            this.dir = dir;
        }

        public abstract Properties getProperties();

        public abstract void storeProperties()
            throws IOException;

        public abstract void addIndexChunk( ResourceFetcher source, String filename )
            throws IOException;

        public abstract Date setIndexFile( ResourceFetcher source, String string )
            throws IOException;

        public Properties setProperties( ResourceFetcher source )
            throws IOException
        {
            this.properties = downloadIndexProperties( source );
            return properties;
        }

        public abstract Date getTimestamp();

        public void commit()
            throws IOException
        {
            storeProperties();
        }
    }

    private class LuceneIndexAdaptor
        extends IndexAdaptor
    {
        private final IndexUpdateRequest updateRequest;

        public LuceneIndexAdaptor( IndexUpdateRequest updateRequest )
        {
            super( updateRequest.getIndexingContext().getIndexDirectoryFile() );
            this.updateRequest = updateRequest;
        }

        public Properties getProperties()
        {
            if ( properties == null )
            {
                properties = loadIndexProperties( dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE );
            }
            return properties;
        }

        public void storeProperties()
            throws IOException
        {
            storeIndexProperties( dir, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, properties );
        }

        public Date getTimestamp()
        {
            return updateRequest.getIndexingContext().getTimestamp();
        }

        public void addIndexChunk( ResourceFetcher source, String filename )
            throws IOException
        {
            loadIndexDirectory( updateRequest, source, true, filename );
        }

        public Date setIndexFile( ResourceFetcher source, String filename )
            throws IOException
        {
            return loadIndexDirectory( updateRequest, source, false, filename );
        }

        public void commit()
            throws IOException
        {
            super.commit();

            updateRequest.getIndexingContext().commit();
        }

    }

    private class LocalCacheIndexAdaptor
        extends IndexAdaptor
    {
        private static final String CHUNKS_FILENAME = "chunks.lst";

        private static final String CHUNKS_FILE_ENCODING = "UTF-8";

        private final IndexUpdateResult result;

        private final ArrayList<String> newChunks = new ArrayList<String>();

        public LocalCacheIndexAdaptor( File dir, IndexUpdateResult result )
        {
            super( dir );
            this.result = result;
        }

        public Properties getProperties()
        {
            if ( properties == null )
            {
                properties = loadIndexProperties( dir, IndexingContext.INDEX_REMOTE_PROPERTIES_FILE );
            }
            return properties;
        }

        public void storeProperties()
            throws IOException
        {
            storeIndexProperties( dir, IndexingContext.INDEX_REMOTE_PROPERTIES_FILE, properties );
        }

        public Date getTimestamp()
        {
            Properties properties = getProperties();
            if ( properties == null )
            {
                return null;
            }

            Date timestamp = DefaultIndexUpdater.this.getTimestamp( properties, IndexingContext.INDEX_TIMESTAMP );

            if ( timestamp == null )
            {
                timestamp = DefaultIndexUpdater.this.getTimestamp( properties, IndexingContext.INDEX_LEGACY_TIMESTAMP );
            }

            return timestamp;
        }

        public void addIndexChunk( ResourceFetcher source, String filename )
            throws IOException
        {
            File chunk = new File( dir, filename );
            FileUtils.copyStreamToFile( new RawInputStreamFacade( source.retrieve( filename ) ), chunk );
            newChunks.add( filename );
        }

        public Date setIndexFile( ResourceFetcher source, String filename )
            throws IOException
        {
            cleanCacheDirectory( dir );

            result.setFullUpdate( true );

            File target = new File( dir, filename );
            FileUtils.copyStreamToFile( new RawInputStreamFacade( source.retrieve( filename ) ), target );

            return null;
        }

        @Override
        public void commit()
            throws IOException
        {
            File chunksFile = new File( dir, CHUNKS_FILENAME );
            BufferedOutputStream os = new BufferedOutputStream( new FileOutputStream( chunksFile, true ) );
            Writer w = new OutputStreamWriter( os, CHUNKS_FILE_ENCODING );
            try
            {
                for ( String filename : newChunks )
                {
                    w.write( filename + "\n" );
                }
                w.flush();
            }
            finally
            {
                IOUtil.close( w );
                IOUtil.close( os );
            }
            super.commit();
        }

        public List<String> getChunks()
            throws IOException
        {
            ArrayList<String> chunks = new ArrayList<String>();

            File chunksFile = new File( dir, CHUNKS_FILENAME );
            BufferedReader r =
                new BufferedReader( new InputStreamReader( new FileInputStream( chunksFile ), CHUNKS_FILE_ENCODING ) );
            try
            {
                String str;
                while ( ( str = r.readLine() ) != null )
                {
                    chunks.add( str );
                }
            }
            finally
            {
                IOUtil.close( r );
            }
            return chunks;
        }

        public ResourceFetcher getFetcher()
        {
            return new LocalIndexCacheFetcher( dir )
            {
                @Override
                public List<String> getChunks()
                    throws IOException
                {
                    return LocalCacheIndexAdaptor.this.getChunks();
                }
            };
        }
    }

    abstract static class LocalIndexCacheFetcher
        extends FileFetcher
    {
        public LocalIndexCacheFetcher( File basedir )
        {
            super( basedir );
        }

        public abstract List<String> getChunks()
            throws IOException;
    }

    private IndexUpdateResult fetchAndUpdateIndex( final IndexUpdateRequest updateRequest, ResourceFetcher source,
                                      IndexAdaptor target )
        throws IOException
    {
        IndexUpdateResult result = new IndexUpdateResult();
        
        if ( !updateRequest.isForceFullUpdate() )
        {
            Properties localProperties = target.getProperties();
            Date localTimestamp = null;

            if ( localProperties != null )
            {
                localTimestamp = getTimestamp( localProperties, IndexingContext.INDEX_TIMESTAMP );
            }

            // this will download and store properties in the target, so next run
            // target.getProperties() will retrieve it
            Properties remoteProperties = target.setProperties( source );

            Date updateTimestamp = getTimestamp( remoteProperties, IndexingContext.INDEX_TIMESTAMP );

            // If new timestamp is missing, dont bother checking incremental, we have an old file
            if ( updateTimestamp != null )
            {
                List<String> filenames =
                    incrementalHandler.loadRemoteIncrementalUpdates( updateRequest, localProperties, remoteProperties );

                // if we have some incremental files, merge them in
                if ( filenames != null )
                {
                    for ( String filename : filenames )
                    {
                        target.addIndexChunk( source, filename );
                    }

                    result.setTimestamp(updateTimestamp);
                    result.setSuccessful(true);
                    return result;
                }
            }
            else
            {
                updateTimestamp = getTimestamp( remoteProperties, IndexingContext.INDEX_LEGACY_TIMESTAMP );
            }

            // fallback to timestamp comparison, but try with one coming from local properties, and if not possible (is
            // null)
            // fallback to context timestamp
            if ( localTimestamp != null )
            {
                // if we have localTimestamp
                // if incremental can't be done for whatever reason, simply use old logic of
                // checking the timestamp, if the same, nothing to do
                if ( updateTimestamp != null && localTimestamp != null && !updateTimestamp.after( localTimestamp ) )
                {
                    //Index is up to date
                    result.setSuccessful(true);
                    return result;
                }
            }
        }
        else
        {
            // create index properties during forced full index download
            target.setProperties( source );
        }

        if( !updateRequest.isIncrementalOnly() )
        {
            Date timestamp = null;
            try
            {
                timestamp = target.setIndexFile( source, IndexingContext.INDEX_FILE_PREFIX + ".gz" );
                if ( source instanceof LocalIndexCacheFetcher )
                {
                    // local cache has inverse organization compared to remote indexes,
                    // i.e. initial index file and delta chunks to apply on top of it
                    for ( String filename : ( (LocalIndexCacheFetcher) source ).getChunks() )
                    {
                        target.addIndexChunk( source, filename );
                    }
                }
            }
            catch ( IOException ex )
            {
                // try to look for legacy index transfer format
                try
                {
                    timestamp = target.setIndexFile( source, IndexingContext.INDEX_FILE_PREFIX + ".zip" );
                }
                catch ( IOException ex2 )
                {
                    getLogger().error( "Fallback to *.zip also failed: " + ex2 ); // do not bother with stack trace
                    
                    throw ex; // original exception more likely to be interesting
                }
            }
            
            result.setTimestamp(timestamp);
            result.setSuccessful(true);
            result.setFullUpdate(true);
        }
        
        return result;
    }

    /**
     * Cleans specified cache directory. If present, Locker.LOCK_FILE will not be deleted.
     */
    protected void cleanCacheDirectory( File dir )
        throws IOException
    {
        File[] members = dir.listFiles();
        if ( members == null )
        {
            return;
        }

        for ( File member : members )
        {
            if ( !Locker.LOCK_FILE.equals( member.getName() ) )
            {
                FileUtils.forceDelete( member );
            }
        }
    }

}
