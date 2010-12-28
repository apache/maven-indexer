package org.apache.maven.index;

import java.io.File;
import java.util.Collections;

import org.apache.maven.index.context.IndexingContext;

public class Nexus1911IncrementalMergedCtxTest
    extends Nexus1911IncrementalTest
{
    IndexingContext member;

    File indexMergedDir;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        member = context;

        indexMergedDir = super.getDirectory( "index/nexus-1911-merged" );

        context =
            indexer.addMergedIndexingContext( "merged", "merged", member.getRepository(), indexMergedDir, false,
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
