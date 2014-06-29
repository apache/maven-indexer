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
        indexMergedDir.mkdirs();

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
