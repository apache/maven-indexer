package org.apache.maven.index;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * Pageable search request. Adds "start" point, to skip wanted number of records, to implement paging. Use "count" of
 * AbstractSearchRequest to set page size.
 * 
 * @author cstamas
 */
public class AbstractSearchPageableRequest
    extends AbstractSearchRequest
{
    /**
     * The number of hit we want to skip from result set. Defaults to 0.
     */
    private int start;

    public AbstractSearchPageableRequest( Query query )
    {
        super( query, null );

        this.start = 0;
    }

    public AbstractSearchPageableRequest( Query query, List<IndexingContext> contexts )
    {
        super( query, contexts );

        this.start = 0;
    }

    /**
     * Returns the "start" of wanted results calculated from result set window. Simply, the count of documents to skip.
     * 
     * @return
     */
    public int getStart()
    {
        return start;
    }

    /**
     * Sets the "start" of wanted results calculated from result set window. Simply, the count of documents to skip.
     * 
     * @param start
     */
    public void setStart( int start )
    {
        if ( start < 0 )
        {
            throw new IllegalArgumentException( "Start cannot be less than 0!" );
        }

        this.start = start;
    }
}
