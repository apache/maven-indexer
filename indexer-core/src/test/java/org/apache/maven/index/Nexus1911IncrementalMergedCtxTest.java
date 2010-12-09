package org.apache.maven.index;

import java.util.Collections;

import org.apache.maven.index.context.IndexingContext;

public class Nexus1911IncrementalMergedCtxTest
    extends Nexus1911IncrementalTest
{
    IndexingContext member;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        member = context;

        context =
            indexer.addMergedIndexingContext( "merged", "merged", member.getRepository(), false,
                Collections.singletonList( member ) );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        indexer.removeIndexingContext( context, true );
        
        context = member;
        
        super.tearDown();
    }
}
