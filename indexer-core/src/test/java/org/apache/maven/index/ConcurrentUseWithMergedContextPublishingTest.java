package org.apache.maven.index;

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

import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The point in this test is: we use Merged context, and we modify some of the "members" in the merged context, while we
 * try to publish those simultaneously.
 *
 * @author cstamas
 */
public class ConcurrentUseWithMergedContextPublishingTest
    extends ConcurrentUseWithMergedContextTest
{
    protected IndexPacker packer;

    protected File repoPublish = new File( getBasedir(), "target/repo-publish" );

    protected final AtomicInteger counter = new AtomicInteger();

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        packer = lookup( IndexPacker.class );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        File props = new File( IndexingContext.INDEX_PACKER_PROPERTIES_FILE );
        if ( props.exists() )
        {
            props.delete();
        }
        super.tearDown();
    }

    @Override
    protected int readIndex( final NexusIndexer nexusIndexer, final IndexingContext indexingContext )
        throws IOException
    {
        // note: concurrent Index publishing into SAME directory is not supported and should be avoided.
        // This test had multiple threads doing it, and since it was not checking actual results of publish (that was
        // not the goal of the test, but simultaneous publishing of merged context that has member changes happening),
        // it was probably publish rubbish anyway.
        final File publish = new File( repoPublish, "publish-" + counter.getAndIncrement() );

        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try
        {
            final IndexPackingRequest request = new IndexPackingRequest( context, indexSearcher.getIndexReader(), publish );
            request.setCreateIncrementalChunks( false );
            packer.packIndex( request );
        } finally {
            context.releaseIndexSearcher( indexSearcher );
        }

        return 1;
    }
}
