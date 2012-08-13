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
    }

    public MergedIndexingContext( String id, String repositoryId, File repository, File indexDirectoryFile,
                                  boolean searchable, ContextMemberProvider membersProvider )
        throws IOException
    {
        this( membersProvider, id, repositoryId, repository, FSDirectory.open( indexDirectoryFile ), searchable );

        this.directoryFile = indexDirectoryFile;
    }

    public MergedIndexingContext( String id, String repositoryId, File repository, Directory indexDirectory,
                                  boolean searchable, ContextMemberProvider membersProvider )
        throws IOException
    {
        this( membersProvider, id, repositoryId, repository, indexDirectory, searchable );

        if ( indexDirectory instanceof FSDirectory )
        {
            this.directoryFile = ( (FSDirectory) indexDirectory ).getDirectory();
        }
    }

    public Collection<IndexingContext> getMembers()
    {
        return membersProvider.getMembers();
    }

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getRepositoryId()
    {
        return repositoryId;
    }

    @Override
    public File getRepository()
    {
        return repository;
    }

    @Override
    public String getRepositoryUrl()
    {
        return null;
    }

    @Override
    public String getIndexUpdateUrl()
    {
        return null;
    }

    @Override
    public boolean isSearchable()
    {
        return searchable;
    }

    @Override
    public void setSearchable( boolean searchable )
    {
        this.searchable = searchable;
    }

    @Override
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

    @Override
    public void updateTimestamp()
        throws IOException
    {
        // noop
    }

    @Override
    public void updateTimestamp( boolean save )
        throws IOException
    {
        // noop
    }

    @Override
    public void updateTimestamp( boolean save, Date date )
        throws IOException
    {
        // noop
    }

    @Override
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

    @Override
    public IndexSearcher acquireIndexSearcher()
        throws IOException
    {
        final NexusIndexMultiReader mr = new NexusIndexMultiReader( getMembers() );
        return new NexusIndexMultiSearcher( mr );
    }

    @Override
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

    @Override
    public IndexWriter getIndexWriter()
        throws IOException
    {
        throw new UnsupportedOperationException( getClass().getName() + " indexing context is read-only!" );
    }

    @Override
    public List<IndexCreator> getIndexCreators()
    {
        HashSet<IndexCreator> creators = new HashSet<IndexCreator>();

        for ( IndexingContext ctx : getMembers() )
        {
            creators.addAll( ctx.getIndexCreators() );
        }

        return new ArrayList<IndexCreator>( creators );
    }

    @Override
    public Analyzer getAnalyzer()
    {
        return new NexusAnalyzer();
    }

    @Override
    public void commit()
        throws IOException
    {
        // noop
    }

    @Override
    public void rollback()
        throws IOException
    {
        // noop
    }

    @Override
    public void optimize()
        throws IOException
    {
        // noop
    }

    @Override
    public void close( boolean deleteFiles )
        throws IOException
    {
        // noop
    }

    @Override
    public void purge()
        throws IOException
    {
        // noop
    }

    @Override
    public void merge( Directory directory )
        throws IOException
    {
        // noop
    }

    @Override
    public void merge( Directory directory, DocumentFilter filter )
        throws IOException
    {
        // noop
    }

    @Override
    public void replace( Directory directory )
        throws IOException
    {
        // noop
    }

    @Override
    public Directory getIndexDirectory()
    {
        return directory;
    }

    @Override
    public File getIndexDirectoryFile()
    {
        return directoryFile;
    }

    @Override
    public GavCalculator getGavCalculator()
    {
        return gavCalculator;
    }

    @Override
    public void setAllGroups( Collection<String> groups )
        throws IOException
    {
        // noop
    }

    @Override
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

    @Override
    public void setRootGroups( Collection<String> groups )
        throws IOException
    {
        // noop
    }

    @Override
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

    @Override
    public void rebuildGroups()
        throws IOException
    {
        // noop
    }
}
