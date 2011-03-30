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
package org.apache.maven.index.context;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexFileNameFilter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.index.artifact.M2GavCalculator;
import org.codehaus.plexus.util.StringUtils;

/**
 * The default {@link IndexingContext} implementation.
 * 
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
public class DefaultIndexingContext
    extends AbstractIndexingContext
{
    /**
     * A standard location for indices served up by a webserver.
     */
    private static final String INDEX_DIRECTORY = ".index";

    public static final String FLD_DESCRIPTOR = "DESCRIPTOR";

    private static final String FLD_DESCRIPTOR_CONTENTS = "NexusIndex";

    private static final String FLD_IDXINFO = "IDXINFO";

    private static final String VERSION = "1.0";

    private static final Term DESCRIPTOR_TERM = new Term( FLD_DESCRIPTOR, FLD_DESCRIPTOR_CONTENTS );

    private Directory indexDirectory;

    private File indexDirectoryFile;

    private String id;

    private boolean searchable;

    private String repositoryId;

    private File repository;

    private String repositoryUrl;

    private String indexUpdateUrl;

    private IndexReader indexReader;

    private NexusIndexSearcher indexSearcher;

    // disabled for now, see getReadOnlyIndexSearcher() method for explanation
    // private NexusIndexSearcher readOnlyIndexSearcher;

    private NexusIndexWriter indexWriter;

    private Date timestamp;

    private List<? extends IndexCreator> indexCreators;

    /**
     * Currently nexus-indexer knows only M2 reposes
     * <p>
     * XXX move this into a concrete Scanner implementation
     */
    private GavCalculator gavCalculator;

    private ReadWriteLock indexMaintenanceLock = new ReentrantReadWriteLock();

    private Thread bottleWarmerThread;

    private DefaultIndexingContext( String id,
                                    String repositoryId,
                                    File repository, //
                                    String repositoryUrl, String indexUpdateUrl,
                                    List<? extends IndexCreator> indexCreators, Directory indexDirectory,
                                    boolean reclaimIndex )
        throws UnsupportedExistingLuceneIndexException, IOException
    {
        this.id = id;

        this.searchable = true;

        this.repositoryId = repositoryId;

        this.repository = repository;

        this.repositoryUrl = repositoryUrl;

        this.indexUpdateUrl = indexUpdateUrl;

        this.indexReader = null;

        this.indexWriter = null;

        this.indexCreators = indexCreators;

        this.indexDirectory = indexDirectory;

        // eh?
        // Guice does NOT initialize these, and we have to do manually?
        // While in Plexus, all is well, but when in guice-shim,
        // these objects are still LazyHintedBeans or what not and IndexerFields are NOT registered!
        for ( IndexCreator indexCreator : indexCreators )
        {
            indexCreator.getIndexerFields();
        }

        this.gavCalculator = new M2GavCalculator();

        prepareIndex( reclaimIndex );

        installBottleWarmer();
    }

    public DefaultIndexingContext( String id, String repositoryId, File repository, File indexDirectoryFile,
                                   String repositoryUrl, String indexUpdateUrl,
                                   List<? extends IndexCreator> indexCreators, boolean reclaimIndex )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        this( id, repositoryId, repository, repositoryUrl, indexUpdateUrl, indexCreators,
            FSDirectory.open( indexDirectoryFile ), reclaimIndex );

        this.indexDirectoryFile = indexDirectoryFile;
    }

    public DefaultIndexingContext( String id, String repositoryId, File repository, Directory indexDirectory,
                                   String repositoryUrl, String indexUpdateUrl,
                                   List<? extends IndexCreator> indexCreators, boolean reclaimIndex )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        this( id, repositoryId, repository, repositoryUrl, indexUpdateUrl, indexCreators, indexDirectory, reclaimIndex );

        if ( indexDirectory instanceof FSDirectory )
        {
            this.indexDirectoryFile = ( (FSDirectory) indexDirectory ).getFile();
        }
    }

    public void lock()
    {
        indexMaintenanceLock.readLock().lock();
    }

    public void unlock()
    {
        indexMaintenanceLock.readLock().unlock();
    }

    public void lockExclusively()
    {
        indexMaintenanceLock.writeLock().lock();
    }

    public void unlockExclusively()
    {
        indexMaintenanceLock.writeLock().unlock();
    }

    public Directory getIndexDirectory()
    {
        return indexDirectory;
    }

    public File getIndexDirectoryFile()
    {
        return indexDirectoryFile;
    }

    private void prepareIndex( boolean reclaimIndex )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        if ( IndexReader.indexExists( indexDirectory ) )
        {
            try
            {
                // unlock the dir forcibly
                if ( IndexWriter.isLocked( indexDirectory ) )
                {
                    IndexWriter.unlock( indexDirectory );
                }

                openAndWarmup();

                checkAndUpdateIndexDescriptor( reclaimIndex );
            }
            catch ( IOException e )
            {
                if ( reclaimIndex )
                {
                    prepareCleanIndex( true );
                }
                else
                {
                    throw e;
                }
            }
        }
        else
        {
            prepareCleanIndex( false );
        }

        timestamp = IndexUtils.getTimestamp( indexDirectory );
    }

    private void prepareCleanIndex( boolean deleteExisting )
        throws IOException
    {
        if ( deleteExisting )
        {
            closeReaders();

            // unlock the dir forcibly
            if ( IndexWriter.isLocked( indexDirectory ) )
            {
                IndexWriter.unlock( indexDirectory );
            }

            deleteIndexFiles( true );
        }

        openAndWarmup();

        if ( StringUtils.isEmpty( getRepositoryId() ) )
        {
            throw new IllegalArgumentException( "The repositoryId cannot be null when creating new repository!" );
        }

        storeDescriptor();
    }

    private void checkAndUpdateIndexDescriptor( boolean reclaimIndex )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        if ( reclaimIndex )
        {
            // forcefully "reclaiming" the ownership of the index as ours
            storeDescriptor();
            return;
        }

        // check for descriptor if this is not a "virgin" index
        if ( getIndexReader().numDocs() > 0 )
        {
            TopScoreDocCollector collector = TopScoreDocCollector.create( 1, false );

            getIndexSearcher().search( new TermQuery( DESCRIPTOR_TERM ), collector );

            if ( collector.getTotalHits() == 0 )
            {
                throw new UnsupportedExistingLuceneIndexException( "The existing index has no NexusIndexer descriptor" );
            }

            if ( collector.getTotalHits() > 1 )
            {
                // eh? this is buggy index it seems, just iron it out then
                storeDescriptor();
                return;
            }
            else
            {
                // good, we have one descriptor as should
                Document descriptor = getIndexSearcher().doc( collector.topDocs().scoreDocs[0].doc );
                String[] h = StringUtils.split( descriptor.get( FLD_IDXINFO ), ArtifactInfo.FS );
                // String version = h[0];
                String repoId = h[1];

                // // compare version
                // if ( !VERSION.equals( version ) )
                // {
                // throw new UnsupportedExistingLuceneIndexException(
                // "The existing index has version [" + version + "] and not [" + VERSION + "] version!" );
                // }

                if ( getRepositoryId() == null )
                {
                    repositoryId = repoId;
                }
                else if ( !getRepositoryId().equals( repoId ) )
                {
                    throw new UnsupportedExistingLuceneIndexException( "The existing index is for repository " //
                        + "[" + repoId + "] and not for repository [" + getRepositoryId() + "]" );
                }
            }
        }
    }

    private void storeDescriptor()
        throws IOException
    {
        Document hdr = new Document();

        hdr.add( new Field( FLD_DESCRIPTOR, FLD_DESCRIPTOR_CONTENTS, Field.Store.YES, Field.Index.NOT_ANALYZED ) );

        hdr.add( new Field( FLD_IDXINFO, VERSION + ArtifactInfo.FS + getRepositoryId(), Field.Store.YES, Field.Index.NO ) );

        IndexWriter w = getIndexWriter();

        w.updateDocument( DESCRIPTOR_TERM, hdr );

        w.commit();
    }

    private void deleteIndexFiles( boolean full )
        throws IOException
    {
        String[] names = indexDirectory.listAll();

        if ( names != null )
        {
            IndexFileNameFilter filter = IndexFileNameFilter.getFilter();

            for ( int i = 0; i < names.length; i++ )
            {
                if ( filter.accept( null, names[i] ) )
                {
                    indexDirectory.deleteFile( names[i] );
                }
            }
        }

        if ( full )
        {
            if ( indexDirectory.fileExists( INDEX_PACKER_PROPERTIES_FILE ) )
            {
                indexDirectory.deleteFile( INDEX_PACKER_PROPERTIES_FILE );
            }

            if ( indexDirectory.fileExists( INDEX_UPDATER_PROPERTIES_FILE ) )
            {
                indexDirectory.deleteFile( INDEX_UPDATER_PROPERTIES_FILE );
            }
        }

        IndexUtils.deleteTimestamp( indexDirectory );
    }

    public boolean isSearchable()
    {
        return searchable;
    }

    public void setSearchable( boolean searchable )
    {
        this.searchable = searchable;
    }

    public String getId()
    {
        return id;
    }

    public void updateTimestamp()
        throws IOException
    {
        updateTimestamp( false );
    }

    public void updateTimestamp( boolean save )
        throws IOException
    {
        updateTimestamp( save, new Date() );
    }

    public void updateTimestamp( boolean save, Date timestamp )
        throws IOException
    {
        this.timestamp = timestamp;

        if ( save )
        {
            IndexUtils.updateTimestamp( indexDirectory, getTimestamp() );
        }
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public int getSize()
        throws IOException
    {
        return getIndexReader().numDocs();
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public File getRepository()
    {
        return repository;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public String getIndexUpdateUrl()
    {
        if ( repositoryUrl != null )
        {
            if ( indexUpdateUrl == null || indexUpdateUrl.trim().length() == 0 )
            {
                return repositoryUrl + ( repositoryUrl.endsWith( "/" ) ? "" : "/" ) + INDEX_DIRECTORY;
            }
        }
        return indexUpdateUrl;
    }

    public Analyzer getAnalyzer()
    {
        return new NexusAnalyzer();
    }

    protected void openAndWarmup()
        throws IOException
    {
        // IndexWriter (close)
        if ( indexWriter != null )
        {
            indexWriter.close();

            indexWriter = null;
        }
        // IndexSearcher (close only, since we did supply this.indexReader explicitly)
        if ( indexSearcher != null )
        {
            indexSearcher.close();

            indexSearcher = null;
        }
        // IndexReader
        if ( indexReader != null )
        {
            indexReader.close();

            indexReader = null;
        }

        // IndexWriter open
        final boolean create = !IndexReader.indexExists( indexDirectory );

        indexWriter = new NexusIndexWriter( getIndexDirectory(), new NexusAnalyzer(), create );

        indexWriter.setRAMBufferSizeMB( 2 );

        indexWriter.setMergeScheduler( new SerialMergeScheduler() );

        openAndWarmupReaders();
    }

    protected void openAndWarmupReaders()
        throws IOException
    {
        if ( indexReader != null && indexReader.isCurrent() )
        {
            return;
        }

        // IndexReader open
        IndexReader newIndexReader = IndexReader.open( indexDirectory, true );

        // IndexSearcher open, but with new reader
        NexusIndexSearcher newIndexSearcher = new NexusIndexSearcher( this, newIndexReader );

        // warm up
        warmUp( newIndexSearcher );

        lockExclusively();

        try
        {
            // IndexSearcher (close only, since we did supply this.indexReader explicitly)
            if ( indexSearcher != null )
            {
                indexSearcher.close();
            }
            // IndexReader
            if ( indexReader != null )
            {
                indexReader.close();
            }

            indexReader = newIndexReader;

            indexSearcher = newIndexSearcher;
        }
        finally
        {
            unlockExclusively();
        }
    }

    protected void warmUp( NexusIndexSearcher searcher )
        throws IOException
    {
        try
        {
            // TODO: figure this out better and non blocking
            searcher.search( new TermQuery( new Term( "g", "org.apache" ) ), 1000 );
        }
        catch ( IOException e )
        {
            close( false );

            throw e;
        }
    }

    public IndexWriter getIndexWriter()
        throws IOException
    {
        lock();

        try
        {
            return indexWriter;
        }
        finally
        {
            unlock();
        }
    }

    public IndexReader getIndexReader()
        throws IOException
    {
        lock();

        try
        {
            return indexReader;
        }
        finally
        {
            unlock();
        }
    }

    public IndexSearcher getIndexSearcher()
        throws IOException
    {
        lock();

        try
        {
            return indexSearcher;
        }
        finally
        {
            unlock();
        }
    }

    public void commit()
        throws IOException
    {
        // TODO: detect is writer "dirty"?
        if ( true )
        {
            if ( BLOCKING_COMMIT )
            {
                lockExclusively();
            }
            else
            {
                lock();
            }

            try
            {
                doCommit( BLOCKING_COMMIT );
            }
            finally
            {
                if ( BLOCKING_COMMIT )
                {
                    unlockExclusively();
                }
                else
                {
                    unlock();
                }
            }
        }
    }

    protected void doCommit( boolean blocking )
        throws IOException
    {
        try
        {
            // TODO: is this needed? Why not put the commit() call into synchronized
            // since all callers of doCommit() aside of commit() already possess exclusive lock
            synchronized ( this )
            {
                getIndexWriter().commit();
            }

            // TODO: define some treshold or requirement
            // for reopening readers (is expensive)
            // For example: by inserting 1 record among 1M, do we really want to reopen?
            if ( true )
            {
                if ( blocking )
                {
                    openAndWarmupReaders();
                }
                else
                {
                    flagNeedsReopen();
                }
            }
        }
        catch ( CorruptIndexException e )
        {
            close( false );

            throw e;
        }
        catch ( IOException e )
        {
            close( false );

            throw e;
        }
    }

    public void rollback()
        throws IOException
    {
        // detect is writer "dirty"?
        if ( true )
        {
            lock();

            try
            {
                IndexWriter w = getIndexWriter();

                try
                {
                    synchronized ( this )
                    {
                        w.rollback();
                    }
                }
                catch ( CorruptIndexException e )
                {
                    close( false );

                    throw e;
                }
                catch ( IOException e )
                {
                    close( false );

                    throw e;
                }
            }
            finally
            {
                unlock();
            }
        }
    }

    public void optimize()
        throws CorruptIndexException, IOException
    {
        lockExclusively();

        try
        {
            IndexWriter w = getIndexWriter();

            try
            {
                w.optimize();

                doCommit( true );
            }
            catch ( CorruptIndexException e )
            {
                close( false );

                throw e;
            }
            catch ( IOException e )
            {
                close( false );

                throw e;
            }
        }
        finally
        {
            unlockExclusively();
        }
    }

    public void close( boolean deleteFiles )
        throws IOException
    {
        lockExclusively();

        try
        {
            if ( indexDirectory != null )
            {
                IndexUtils.updateTimestamp( indexDirectory, getTimestamp() );

                closeReaders();

                if ( deleteFiles )
                {
                    deleteIndexFiles( true );
                }

                indexDirectory.close();
            }

            // TODO: this will prevent from reopening them, but needs better solution
            // Needed to make bottleWarmerThread die off
            indexDirectory = null;
        }
        finally
        {
            unlockExclusively();
        }
    }

    public void purge()
        throws IOException
    {
        lockExclusively();

        try
        {
            closeReaders();

            deleteIndexFiles( true );

            openAndWarmup();

            try
            {
                prepareIndex( true );
            }
            catch ( UnsupportedExistingLuceneIndexException e )
            {
                // just deleted it
            }

            rebuildGroups();

            updateTimestamp( true, null );
        }
        finally
        {
            unlockExclusively();
        }
    }

    public void replace( Directory directory )
        throws IOException
    {
        lockExclusively();

        try
        {
            Date ts = IndexUtils.getTimestamp( directory );

            closeReaders();

            deleteIndexFiles( false );

            IndexUtils.copyDirectory( directory, indexDirectory );

            openAndWarmup();

            // reclaim the index as mine
            storeDescriptor();

            updateTimestamp( true, ts );

            optimize();
        }
        finally
        {
            unlockExclusively();
        }
    }

    public void merge( Directory directory )
        throws IOException
    {
        merge( directory, null );
    }

    public void merge( Directory directory, DocumentFilter filter )
        throws IOException
    {
        lockExclusively();

        try
        {
            IndexWriter w = getIndexWriter();

            IndexSearcher s = getIndexSearcher();

            IndexReader directoryReader = IndexReader.open( directory, true );

            TopScoreDocCollector collector = null;

            try
            {
                int numDocs = directoryReader.maxDoc();

                for ( int i = 0; i < numDocs; i++ )
                {
                    if ( directoryReader.isDeleted( i ) )
                    {
                        continue;
                    }

                    Document d = directoryReader.document( i );

                    if ( filter != null && !filter.accept( d ) )
                    {
                        continue;
                    }

                    String uinfo = d.get( ArtifactInfo.UINFO );

                    if ( uinfo != null )
                    {
                        collector = TopScoreDocCollector.create( 1, false );

                        s.search( new TermQuery( new Term( ArtifactInfo.UINFO, uinfo ) ), collector );

                        if ( collector.getTotalHits() == 0 )
                        {
                            w.addDocument( IndexUtils.updateDocument( d, this, false ) );
                        }
                    }
                    else
                    {
                        String deleted = d.get( ArtifactInfo.DELETED );

                        if ( deleted != null )
                        {
                            // Deleting the document loses history that it was delete,
                            // so incrementals wont work. Therefore, put the delete
                            // document in as well
                            w.deleteDocuments( new Term( ArtifactInfo.UINFO, deleted ) );
                            w.addDocument( d );
                        }
                    }
                }

            }
            finally
            {
                directoryReader.close();

                doCommit( true );
            }

            rebuildGroups();

            Date mergedTimestamp = IndexUtils.getTimestamp( directory );

            if ( getTimestamp() != null && mergedTimestamp != null && mergedTimestamp.after( getTimestamp() ) )
            {
                // we have both, keep the newest
                updateTimestamp( true, mergedTimestamp );
            }
            else
            {
                updateTimestamp( true );
            }

            optimize();
        }
        finally
        {
            unlockExclusively();
        }
    }

    private void closeReaders()
        throws CorruptIndexException, IOException
    {
        if ( indexWriter != null )
        {
            indexWriter.close();

            indexWriter = null;
        }
        if ( indexSearcher != null )
        {
            indexSearcher.close();

            indexSearcher = null;
        }
        if ( indexReader != null )
        {
            indexReader.close();

            indexReader = null;
        }
    }

    public GavCalculator getGavCalculator()
    {
        return gavCalculator;
    }

    public List<IndexCreator> getIndexCreators()
    {
        return Collections.unmodifiableList( indexCreators );
    }

    // groups

    public void rebuildGroups()
        throws IOException
    {
        lockExclusively();

        try
        {
            IndexReader r = getIndexReader();

            Set<String> rootGroups = new LinkedHashSet<String>();
            Set<String> allGroups = new LinkedHashSet<String>();

            int numDocs = r.maxDoc();

            for ( int i = 0; i < numDocs; i++ )
            {
                if ( r.isDeleted( i ) )
                {
                    continue;
                }

                Document d = r.document( i );

                String uinfo = d.get( ArtifactInfo.UINFO );

                if ( uinfo != null )
                {
                    ArtifactInfo info = IndexUtils.constructArtifactInfo( d, this );
                    rootGroups.add( info.getRootGroup() );
                    allGroups.add( info.groupId );
                }
            }

            setRootGroups( rootGroups );
            setAllGroups( allGroups );

            optimize();
        }
        finally
        {
            unlockExclusively();
        }
    }

    public Set<String> getAllGroups()
        throws IOException
    {
        lock();

        try
        {
            return getGroups( ArtifactInfo.ALL_GROUPS, ArtifactInfo.ALL_GROUPS_VALUE, ArtifactInfo.ALL_GROUPS_LIST );
        }
        finally
        {
            unlock();
        }
    }

    public void setAllGroups( Collection<String> groups )
        throws IOException
    {
        lockExclusively();

        try
        {
            setGroups( groups, ArtifactInfo.ALL_GROUPS, ArtifactInfo.ALL_GROUPS_VALUE, ArtifactInfo.ALL_GROUPS_LIST );

            doCommit( true );
        }
        finally
        {
            unlockExclusively();
        }
    }

    public Set<String> getRootGroups()
        throws IOException
    {
        lock();

        try
        {
            return getGroups( ArtifactInfo.ROOT_GROUPS, ArtifactInfo.ROOT_GROUPS_VALUE, ArtifactInfo.ROOT_GROUPS_LIST );
        }
        finally
        {
            unlock();
        }
    }

    public void setRootGroups( Collection<String> groups )
        throws IOException
    {
        lockExclusively();

        try
        {
            setGroups( groups, ArtifactInfo.ROOT_GROUPS, ArtifactInfo.ROOT_GROUPS_VALUE, ArtifactInfo.ROOT_GROUPS_LIST );

            doCommit( true );
        }
        finally
        {
            unlockExclusively();
        }
    }

    protected Set<String> getGroups( String field, String filedValue, String listField )
        throws IOException, CorruptIndexException
    {
        TopScoreDocCollector collector = TopScoreDocCollector.create( 1, false );

        getIndexSearcher().search( new TermQuery( new Term( field, filedValue ) ), collector );

        TopDocs topDocs = collector.topDocs();

        Set<String> groups = new LinkedHashSet<String>( Math.max( 10, topDocs.totalHits ) );

        if ( topDocs.totalHits > 0 )
        {
            Document doc = getIndexSearcher().doc( topDocs.scoreDocs[0].doc );

            String groupList = doc.get( listField );

            if ( groupList != null )
            {
                groups.addAll( Arrays.asList( groupList.split( "\\|" ) ) );
            }
        }

        return groups;
    }

    protected void setGroups( Collection<String> groups, String groupField, String groupFieldValue,
                              String groupListField )
        throws IOException, CorruptIndexException
    {
        IndexWriter w = getIndexWriter();

        w.updateDocument( new Term( groupField, groupFieldValue ),
            createGroupsDocument( groups, groupField, groupFieldValue, groupListField ) );
    }

    protected Document createGroupsDocument( Collection<String> groups, String field, String fieldValue,
                                             String listField )
    {
        Document groupDoc = new Document();

        groupDoc.add( new Field( field, //
            fieldValue, Field.Store.YES, Field.Index.NOT_ANALYZED ) );

        groupDoc.add( new Field( listField, //
            ArtifactInfo.lst2str( groups ), Field.Store.YES, Field.Index.NO ) );

        return groupDoc;
    }

    @Override
    public String toString()
    {
        return id + " : " + timestamp;
    }

    // ==

    private volatile boolean needsReaderReopen = false;

    protected void flagNeedsReopen()
    {
        needsReaderReopen = true;
    }

    protected void unflagNeedsReopen()
    {
        needsReaderReopen = false;
    }

    protected boolean isReopenNeeded()
    {
        return needsReaderReopen;
    }

    protected void installBottleWarmer()
    {
        if ( BLOCKING_COMMIT )
        {
            return;
        }

        Runnable bottleWarmer = new Runnable()
        {
            public void run()
            {
                // die off when context is closed
                while ( indexDirectory != null )
                {
                    try
                    {
                        if ( isReopenNeeded() )
                        {
                            openAndWarmupReaders();

                            unflagNeedsReopen();
                        }

                        Thread.sleep( 1000 );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        };

        bottleWarmerThread = new Thread( bottleWarmer, "Index-BottleWarmer" );
        bottleWarmerThread.setDaemon( true );
        bottleWarmerThread.start();
    }

    /**
     * A flag useful for tests, to make this IndexingContext implementation blocking. If this flag is true, context will
     * block the commit() calls and will return from it when Lucene commit done AND all the readers are reopened and are
     * current. TODO: this is currently inherently unsafe (is not final), and is meant to be used in Unit tests only!
     * Think something and tie this knot properly.
     */
    public static boolean BLOCKING_COMMIT = false;
}
