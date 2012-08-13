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

import java.util.Arrays;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexingContext;

/**
 * The point in this test is: we use Merged context, and we modify some of the "members" in the merged context, while we
 * try to search over merged one simultaneously.
 * 
 * @author cstamas
 */
public class ConcurrentUseWithMergedContextTest
    extends ConcurrentUseTest
{
    protected Directory indexDir1 = new RAMDirectory();

    protected IndexingContext context1;

    protected Directory indexDir2 = new RAMDirectory();

    protected IndexingContext context2;

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context1 =
            nexusIndexer.addIndexingContext( "test-default-member1", "test1", repo, indexDir1, null, null,
                DEFAULT_CREATORS );

        nexusIndexer.scan( context1 );

        context2 =
            nexusIndexer.addIndexingContext( "test-default-member2", "test2", repo, indexDir2, null, null,
                DEFAULT_CREATORS );

        nexusIndexer.scan( context2 );

        context =
            nexusIndexer.addMergedIndexingContext( "test-default", "test", repo, indexDir, true,
                Arrays.asList( context1, context2 ) );

        // Group contexts are known, they inherit member timestamp and they are scanned already
        // assertNull( context.getTimestamp() ); // unknown upon creation

        // nexusIndexer.scan( context );

        assertNotNull( context.getTimestamp() );
    }

    @Override
    protected IndexUserThread createThread( final ArtifactInfo ai )
    {
        // we search the merged one and modify one member context concurrently
        return new IndexUserThread( this, nexusIndexer, context, context1, ai );
    }
}
