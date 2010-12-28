package org.apache.maven.index.context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
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
            this.directoryFile = ( (FSDirectory) indexDirectory ).getFile();
        }
    }

    protected Collection<IndexingContext> getMembers()
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

    public IndexReader getIndexReader()
        throws IOException
    {
        Collection<IndexingContext> members = getMembers();

        ArrayList<IndexReader> contextsToSearch = new ArrayList<IndexReader>( members.size() );

        for ( IndexingContext ctx : members )
        {
            contextsToSearch.add( ctx.getIndexReader() );
        }

        MultiReader multiReader =
            new MultiReader( contextsToSearch.toArray( new IndexReader[contextsToSearch.size()] ) );

        return multiReader;
    }

    public IndexSearcher getIndexSearcher()
        throws IOException
    {
        return new NexusIndexSearcher( getIndexReader() );
    }

    public IndexWriter getIndexWriter()
        throws IOException
    {
        // noop?
        return null;
        // throw new UnsupportedOperationException( "Merged indexing context is read-only!" );
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

    public void lock()
    {
        for ( IndexingContext ctx : getMembers() )
        {
            ctx.lock();
        }
    }

    public void unlock()
    {
        for ( IndexingContext ctx : getMembers() )
        {
            ctx.unlock();
        }
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
