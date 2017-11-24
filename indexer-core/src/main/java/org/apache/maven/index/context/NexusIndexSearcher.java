package org.apache.maven.index.context;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;

/**
 * An extended Searcher, that holds reference to the IndexingContext that is a searcher for. Needed to provide "extra"
 * data for search hits, that are not on index, and support ArtifactInfoPostprocessor's.
 *
 * @author cstamas
 */
public class NexusIndexSearcher
        extends IndexSearcher
{
    private final IndexingContext indexingContext;

    public NexusIndexSearcher( final IndexReader reader ) throws IOException
    {
        this( null, reader );
    }

    public NexusIndexSearcher( final IndexingContext indexingContext, final IndexReader reader ) throws IOException
    {
        super( reader );

        this.indexingContext = indexingContext;

        // setSimilarity( new NexusSimilarity() );
    }

    public IndexingContext getIndexingContext()
    {
        return indexingContext;
    }
}
