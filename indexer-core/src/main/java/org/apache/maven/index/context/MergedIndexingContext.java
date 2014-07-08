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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.artifact.GavCalculator;
import org.apache.maven.index.artifact.M2GavCalculator;

/**
 * A merged indexing context that offers read only "view" on multiple other indexing contexts merged and presented as
 * one. Usable for searching and publishing, but all write operations are basically noop.
 * 
 * @author cstamas
 */
public class MergedIndexingContext
    extends AbstractIndexingContext
{
    private final String id;

    private final String repositoryId;

    private final File repository;

    private final ContextMemberProvider membersProvider;

    private final GavCalculator gavCalculator;

    private final Directory directory;

    private File directoryFile;

    private boolean searchable;

    private MergedIndexingContext( ContextMemberProvider membersProvider, String id, String repositoryId,
                                   File repository, Directory indexDirectory, boolean searchable )
        throws IOException
    {
        this.id = id;
        this.repositoryId = repositoryId;
        this.repository = repository;
        this.membersProvider = membersProvider;
        this.gavCalculator = new M2GavCalculator();
        this.directory = indexDirectory;
        this.searchable = searchable;
        setIndexDirectoryFile( null );
    }

    public MergedIndexingContext( String id, String repositoryId, File repository, File indexDirectoryFile,
                                  boolean searchable, ContextMemberProvider membersProvider )
        throws IOException
    {
        this( membersProvider, id, repositoryId, repository, FSDirectory.open( indexDirectoryFile ), searchable );

        setIndexDirectoryFile( indexDirectoryFile );
    }

    @Deprecated
    public MergedIndexingContext( String id, String repositoryId, File repository, Directory indexDirectory,
                                  boolean searchable, ContextMemberProvider membersProvider )
        throws IOException
    {
        this( membersProvider, id, repositoryId, repository, indexDirectory, searchable );

        if ( indexDirectory instanceof FSDirectory )
        {
            setIndexDirectoryFile( ( (FSDirectory) indexDirectory ).getDirectory() );
        }
    }

    public Collection<IndexingContext> getMembers()
    {
        return membersProvider.getMembers();
    }

    public String getId()
    {
        return id;
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
        return null;
    }

    public String getIndexUpdateUrl()
    {
        return null;
    }

    public boolean isSearchable()
    {
        return searchable;
    }

    public void setSearchable( boolean searchable )
    {
        this.searchable = searchable;
    }

    public Date getTimestamp()
    {
        Date ts = null;

        for ( IndexingContext ctx : getMembers() )
        {
            Date cts = ctx.getTimestamp();

            if ( cts != null )
            {
                if ( ts == null || cts.after( ts ) )
                {
                    ts = cts;
                }
            }
        }

        return ts;
    }

    public void updateTimestamp()
        throws IOException
    {
        // noop
    }

    public void updateTimestamp( boolean save )
        throws IOException
    {
        // noop
    }

    public void updateTimestamp( boolean save, Date date )
        throws IOException
    {
        // noop
    }

    public int getSize()
        throws IOException
    {
        int size = 0;

        for ( IndexingContext ctx : getMembers() )
        {
            size += ctx.getSize();
        }

        return size;
    }

    public IndexSearcher acquireIndexSearcher()
        throws IOException
    {
        final NexusIndexMultiReader mr = new NexusIndexMultiReader( getMembers() );
        return new NexusIndexMultiSearcher( mr );
    }

    public void releaseIndexSearcher( IndexSearcher indexSearcher )
        throws IOException
    {
        if ( indexSearcher instanceof NexusIndexMultiSearcher )
        {
            ( (NexusIndexMultiSearcher) indexSearcher ).release();
        }
        else
        {
            throw new IllegalArgumentException( String.format(
                "Illegal argument to merged idexing context: it emits class %s but and cannot release class %s!",
                NexusIndexMultiSearcher.class.getName(), indexSearcher.getClass().getName() ) );
        }

    }

    public IndexWriter getIndexWriter()
        throws IOException
    {
        throw new UnsupportedOperationException( getClass().getName() + " indexing context is read-only!" );
    }

    public List<IndexCreator> getIndexCreators()
    {
        HashSet<IndexCreator> creators = new HashSet<IndexCreator>();

        for ( IndexingContext ctx : getMembers() )
        {
            creators.addAll( ctx.getIndexCreators() );
        }

        return new ArrayList<IndexCreator>( creators );
    }

    public Analyzer getAnalyzer()
    {
        return new NexusAnalyzer();
    }

    public void commit()
        throws IOException
    {
        // noop
    }

    public void rollback()
        throws IOException
    {
        // noop
    }

    public void optimize()
        throws IOException
    {
        // noop
    }

    public void close( boolean deleteFiles )
        throws IOException
    {
        // noop
    }

    public void purge()
        throws IOException
    {
        // noop
    }

    public void merge( Directory directory )
        throws IOException
    {
        // noop
    }

    public void merge( Directory directory, DocumentFilter filter )
        throws IOException
    {
        // noop
    }

    public void replace( Directory directory )
        throws IOException
    {
        // noop
    }

    public Directory getIndexDirectory()
    {
        return directory;
    }

    public File getIndexDirectoryFile()
    {
        return directoryFile;
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
            this.directoryFile = tmpFile;
        }
        else
        {
            this.directoryFile = dir;
        }
    }

    public GavCalculator getGavCalculator()
    {
        return gavCalculator;
    }

    public void setAllGroups( Collection<String> groups )
        throws IOException
    {
        // noop
    }

    public Set<String> getAllGroups()
        throws IOException
    {
        HashSet<String> result = new HashSet<String>();

        for ( IndexingContext ctx : getMembers() )
        {
            result.addAll( ctx.getAllGroups() );
        }

        return result;
    }

    public void setRootGroups( Collection<String> groups )
        throws IOException
    {
        // noop
    }

    public Set<String> getRootGroups()
        throws IOException
    {
        HashSet<String> result = new HashSet<String>();

        for ( IndexingContext ctx : getMembers() )
        {
            result.addAll( ctx.getRootGroups() );
        }

        return result;
    }

    public void rebuildGroups()
        throws IOException
    {
        // noop
    }
}
