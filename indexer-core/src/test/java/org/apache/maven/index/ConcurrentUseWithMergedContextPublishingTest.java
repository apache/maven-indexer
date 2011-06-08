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

import java.io.File;
import java.io.IOException;

import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;

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

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        packer = lookup( IndexPacker.class );
    }

    @Override
    protected int readIndex( final NexusIndexer nexusIndexer, final IndexingContext indexingContext )
        throws IOException
    {
        final IndexPackingRequest request = new IndexPackingRequest( context, repoPublish );

        request.setCreateIncrementalChunks( false );

        packer.packIndex( request );

        return 1;
    }
}
