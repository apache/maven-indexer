package org.apache.maven.index;

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

public class AbstractSearchPageableRequest
    extends AbstractSearchRequest
{
    /**
     * Constant for denoting undefined value for result count.
     */
    protected static final int UNDEFINED = -1;

    /**
     * The number of hit we want to skip from result set. Defaults to 0.
     */
    private int start;

    /**
     * The page size, actually count of items in one page. Different than limit, because this will _cut_ the response to
     * the requested count.
     */
    private int count;

    public AbstractSearchPageableRequest( Query query )
    {
        super( query, null );

        this.start = 0;

        this.count = UNDEFINED;
    }

    public AbstractSearchPageableRequest( Query query, List<IndexingContext> contexts )
    {
        super( query, contexts );

        this.start = 0;

        this.count = UNDEFINED;
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

    /**
     * Returns the "count" of wanted results. See {@link #UNDEFINED}.
     * 
     * @return
     */
    public int getCount()
    {
        return count;
    }

    /**
     * Sets the "count" of wanted results. See {@link #UNDEFINED}.
     * 
     * @param count
     */
    public void setCount( int count )
    {
        if ( UNDEFINED != count && count < 1 )
        {
            throw new IllegalArgumentException( "Count cannot be less than 1!" );
        }

        this.count = count;
    }
}
