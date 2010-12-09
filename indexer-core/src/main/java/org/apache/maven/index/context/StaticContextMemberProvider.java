package org.apache.maven.index.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A simple "static" context member provider, when the members are known in advance.
 * 
 * @author cstamas
 */
public class StaticContextMemberProvider
    implements ContextMemberProvider
{
    private final List<IndexingContext> members;

    public StaticContextMemberProvider( Collection<IndexingContext> members )
    {
        ArrayList<IndexingContext> m = new ArrayList<IndexingContext>();

        if ( members != null )
        {
            m.addAll( members );
        }

        this.members = Collections.unmodifiableList( m );
    }

    public List<IndexingContext> getMembers()
    {
        return members;
    }
}
