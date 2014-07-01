package org.apache.maven.index.archetype;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;

/**
 * Nexus Archetype Data Source.
 * 
 * @author Eugene Kuleshov
 * @deprecated Extend {@link AbstractArchetypeDataSource} instead, and make it suit your case.
 */
@Deprecated
@Named("nexus")
@Singleton
public class NexusArchetypeDataSource
    extends AbstractArchetypeDataSource
{

    private final NexusIndexer nexusIndexer;


    @Inject
    public NexusArchetypeDataSource( Indexer indexer,
                                     NexusIndexer nexusIndexer )
    {
        super( indexer );
        this.nexusIndexer = nexusIndexer;
    }

    @Override
    protected List<IndexingContext> getIndexingContexts()
    {
        return new ArrayList<IndexingContext>( nexusIndexer.getIndexingContexts().values() );
    }
}
