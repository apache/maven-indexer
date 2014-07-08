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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
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

    public static final String FLD_DESCRIPTOR_CONTENTS = "NexusIndex";

    public static final String FLD_IDXINFO = "IDXINFO";

    public static final String VERSION = "1.0";

    private static final Term DESCRIPTOR_TERM = new Term( FLD_DESCRIPTOR, FLD_DESCRIPTOR_CONTENTS );

    private Directory indexDirectory;

    private File indexDirectoryFile;

    private String id;

    private boolean searchable;

    private String repositoryId;

    private File repository;

    private String repositoryUrl;

    private String indexUpdateUrl;

    private NexusIndexWriter indexWriter;

    private SearcherManager searcherManager;

    private Date timestamp;

    private List<? extends IndexCreator> indexCreators;

    /**
     * Currently nexus-indexer knows only M2 reposes
     * <p>
     * XXX move this into a concrete Scanner implementation
     */
    private GavCalculator gavCalculator;

    private DefaultIndexingContext( String id,
                                    String repositoryId,
                                    File repository, //
                                    String repositoryUrl, String indexUpdateUrl,
                                    List<? extends IndexCreator> indexCreators, Directory indexDirectory,
                                    boolean reclaimIndex )
        throws ExistingLuceneIndexMismatchException, IOException
    {
        this.id = id;

        this.searchable = true;

        this.repositoryId = repositoryId;

        this.repository = repository;

        this.repositoryUrl = repositoryUrl;

        this.indexUpdateUrl = indexUpdateUrl;

        this.indexWriter = null;

        this.searcherManager = null;

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

        setIndexDirectoryFile( null );
    }

    public DefaultIndexingContext( String id, String repositoryId, File repository, File indexDirectoryFile,
                                   String repositoryUrl, String indexUpdateUrl,
                                   List<? extends IndexCreator> indexCreators, boolean reclaimIndex )
        throws IOException, ExistingLuceneIndexMismatchException
    {
        this( id, repositoryId, repository, repositoryUrl, indexUpdateUrl, indexCreators,
            FSDirectory.open( indexDirectoryFile ), reclaimIndex );

        setIndexDirectoryFile( indexDirectoryFile );
    }

    @Deprecated
    public DefaultIndexingContext( String id, String repositoryId, File repository, Directory indexDirectory,
                                   String repositoryUrl, String indexUpdateUrl,
                                   List<? extends IndexCreator> indexCreators, boolean reclaimIndex )
        throws IOException, ExistingLuceneIndexMismatchException
    {
        this( id, repositoryId, repository, repositoryUrl, indexUpdateUrl, indexCreators, indexDirectory, reclaimIndex );

        if ( indexDirectory instanceof FSDirectory )
        {
            setIndexDirectoryFile(( (FSDirectory) indexDirectory ).getDirectory() );
        }
    }

    public Directory getIndexDirectory()
    {
        return indexDirectory;
    }
    
    /**
     * Sets index location. As usually index is persistent (is on disk), this will point to that value, but in
     * some circumstances (ie, using RAMDisk for index), this will point to an existing tmp directory.
     */
    protected void setIndexDirectoryFile(File dir) throws IOException
    {
        if ( dir == null )
        {
            // best effort, to have a directory thru the life of a ctx
            File tmpFile = File.createTempFile( "mindexer-ctx" + id, "tmp" );
            tmpFile.delete();
            tmpFile.mkdirs();
            this.indexDirectoryFile = tmpFile;
        }
        else
        {
            this.indexDirectoryFile = dir;
        }
    }

    public File getIndexDirectoryFile()
    {
        return indexDirectoryFile;
    }

    private void prepareIndex( boolean reclaimIndex )
        throws IOException, ExistingLuceneIndexMismatchException
    {
        if ( DirectoryReader.indexExists( indexDirectory ) )
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
        throws IOException, ExistingLuceneIndexMismatchException
    {
        if ( reclaimIndex )
        {
            // forcefully "reclaiming" the ownership of the index as ours
            storeDescriptor();
            return;
        }

        // check for descriptor if this is not a "virgin" index
        if ( getSize() > 0 )
        {
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 1, false );
            final IndexSearcher indexSearcher = acquireIndexSearcher();
            try
            {
                indexSearcher.search( new TermQuery( DESCRIPTOR_TERM ), collector );

                if ( collector.getTotalHits() == 0 )
                {
                    throw new ExistingLuceneIndexMismatchException(
                        "The existing index has no NexusIndexer descriptor" );
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
                    Document descriptor = indexSearcher.doc( collector.topDocs().scoreDocs[0].doc );
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
                        throw new ExistingLuceneIndexMismatchException( "The existing index is for repository " //
                            + "[" + repoId + "] and not for repository [" + getRepositoryId() + "]" );
                    }
                }
            }
            finally
            {
                releaseIndexSearcher( indexSearcher );
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
        if ( indexDirectory != null )
        {
            String[] names = indexDirectory.listAll();

            if ( names != null )
            {

                for (String name : names)
                {
                    if (! (name.equals(INDEX_PACKER_PROPERTIES_FILE) || name.equals(INDEX_UPDATER_PROPERTIES_FILE) ))
                    {
                        indexDirectory.deleteFile(name);
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
    }

    // ==

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
        final IndexSearcher is = acquireIndexSearcher();
        try
        {
            return is.getIndexReader().numDocs();
        }
        finally
        {
            releaseIndexSearcher( is );
        }
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
        if ( searcherManager != null )
        {
            searcherManager.close();

            searcherManager = null;
        }

        this.indexWriter = new NexusIndexWriter( getIndexDirectory(), getWriterConfig() );
        this.indexWriter.commit(); // LUCENE-2386
        this.searcherManager = new SearcherManager( indexWriter, false, new NexusIndexSearcherFactory( this ) );
    }

    /**
     * Returns new IndexWriterConfig instance
     * 
     * @since 5.1
     */
    protected IndexWriterConfig getWriterConfig()
    {
        return NexusIndexWriter.defaultConfig();
    }

    public IndexWriter getIndexWriter()
        throws IOException
    {
        return indexWriter;
    }

    public IndexSearcher acquireIndexSearcher()
        throws IOException
    {
        // TODO: move this to separate thread to not penalty next incoming searcher
        searcherManager.maybeRefresh();
        return searcherManager.acquire();
    }

    public void releaseIndexSearcher( final IndexSearcher is )
        throws IOException
    {
        if ( is == null )
        {
            return;
        }
        searcherManager.release( is );
    }

    public void commit()
        throws IOException
    {
        getIndexWriter().commit();
    }

    public void rollback()
        throws IOException
    {
        getIndexWriter().rollback();
    }

    public synchronized void optimize()
        throws CorruptIndexException, IOException
    {
        getIndexWriter().forceMerge(1);
        commit();
    }

    public synchronized void close( boolean deleteFiles )
        throws IOException
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
        indexDirectory = null;
    }

    public synchronized void purge()
        throws IOException
    {
        closeReaders();
        deleteIndexFiles( true );
        openAndWarmup();
        try
        {
            prepareIndex( true );
        }
        catch ( ExistingLuceneIndexMismatchException e )
        {
            // just deleted it
        }
        rebuildGroups();
        updateTimestamp( true, null );
    }

    public synchronized void replace( Directory directory )
        throws IOException
    {
        final Date ts = IndexUtils.getTimestamp( directory );
        closeReaders();
        deleteIndexFiles( false );
        IndexUtils.copyDirectory( directory, indexDirectory );
        openAndWarmup();
        // reclaim the index as mine
        storeDescriptor();
        rebuildGroups();
        updateTimestamp( true, ts );
        optimize();
    }

    public synchronized void merge( Directory directory )
        throws IOException
    {
        merge( directory, null );
    }

    public synchronized void merge( Directory directory, DocumentFilter filter )
        throws IOException
    {
        final IndexSearcher s = acquireIndexSearcher();
        try
        {
            final IndexWriter w = getIndexWriter();
            final IndexReader directoryReader = IndexReader.open( directory);
            TopScoreDocCollector collector = null;
            try
            {
                int numDocs = directoryReader.maxDoc();
                
                Bits liveDocs = MultiFields.getLiveDocs(directoryReader);
                for ( int i = 0; i < numDocs; i++ )
                {
                    if (liveDocs != null && ! liveDocs.get(i) )
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
                commit();
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
            releaseIndexSearcher( s );
        }
    }

    private void closeReaders()
        throws CorruptIndexException, IOException
    {
        if ( searcherManager != null )
        {
            searcherManager.close();
            searcherManager = null;
        }
        if ( indexWriter != null )
        {
            indexWriter.close();
            indexWriter = null;
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

    public synchronized void rebuildGroups()
        throws IOException
    {
        final IndexSearcher is = acquireIndexSearcher();
        try
        {
            final IndexReader r = is.getIndexReader();

            Set<String> rootGroups = new LinkedHashSet<String>();
            Set<String> allGroups = new LinkedHashSet<String>();

            int numDocs = r.maxDoc();
            Bits liveDocs = MultiFields.getLiveDocs(r);

            for ( int i = 0; i < numDocs; i++ )
            {
                if (liveDocs != null && !liveDocs.get(i) )
                {
                    continue;
                }

                Document d = r.document( i );

                String uinfo = d.get( ArtifactInfo.UINFO );

                if ( uinfo != null )
                {
                    ArtifactInfo info = IndexUtils.constructArtifactInfo( d, this );
                    rootGroups.add( info.getRootGroup() );
                    allGroups.add( info.getGroupId() );
                }
            }

            setRootGroups( rootGroups );
            setAllGroups( allGroups );

            optimize();
        }
        finally
        {
            releaseIndexSearcher( is );
        }
    }

    public Set<String> getAllGroups()
        throws IOException
    {
        return getGroups( ArtifactInfo.ALL_GROUPS, ArtifactInfo.ALL_GROUPS_VALUE, ArtifactInfo.ALL_GROUPS_LIST );
    }

    public synchronized void setAllGroups( Collection<String> groups )
        throws IOException
    {
        setGroups( groups, ArtifactInfo.ALL_GROUPS, ArtifactInfo.ALL_GROUPS_VALUE, ArtifactInfo.ALL_GROUPS_LIST );
        commit();
    }

    public Set<String> getRootGroups()
        throws IOException
    {
        return getGroups( ArtifactInfo.ROOT_GROUPS, ArtifactInfo.ROOT_GROUPS_VALUE, ArtifactInfo.ROOT_GROUPS_LIST );
    }

    public synchronized void setRootGroups( Collection<String> groups )
        throws IOException
    {
        setGroups( groups, ArtifactInfo.ROOT_GROUPS, ArtifactInfo.ROOT_GROUPS_VALUE, ArtifactInfo.ROOT_GROUPS_LIST );
        commit();
    }

    protected Set<String> getGroups( String field, String filedValue, String listField )
        throws IOException, CorruptIndexException
    {
        final TopScoreDocCollector collector = TopScoreDocCollector.create( 1, false );
        final IndexSearcher indexSearcher = acquireIndexSearcher();
        try
        {
            indexSearcher.search( new TermQuery( new Term( field, filedValue ) ), collector );
            TopDocs topDocs = collector.topDocs();
            Set<String> groups = new LinkedHashSet<String>( Math.max( 10, topDocs.totalHits ) );
            if ( topDocs.totalHits > 0 )
            {
                Document doc = indexSearcher.doc( topDocs.scoreDocs[0].doc );
                String groupList = doc.get( listField );
                if ( groupList != null )
                {
                    groups.addAll( Arrays.asList( groupList.split( "\\|" ) ) );
                }
            }
            return groups;
        }
        finally
        {
            releaseIndexSearcher( indexSearcher );
        }
    }

    protected void setGroups( Collection<String> groups, String groupField, String groupFieldValue,
                              String groupListField )
        throws IOException, CorruptIndexException
    {
        final IndexWriter w = getIndexWriter();
        w.updateDocument( new Term( groupField, groupFieldValue ),
            createGroupsDocument( groups, groupField, groupFieldValue, groupListField ) );
    }

    protected Document createGroupsDocument( Collection<String> groups, String field, String fieldValue,
                                             String listField )
    {
        final Document groupDoc = new Document();
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
}
