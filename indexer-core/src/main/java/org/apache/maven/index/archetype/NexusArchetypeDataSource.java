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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.archetype.source.ArchetypeDataSource;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Nexus Archetype Data Source.
 * 
 * @author Eugene Kuleshov
 * @deprecated Extend {@link AbstractArchetypeDataSource} instead, and make it suit your case.
 */
@Deprecated
@Component( role = ArchetypeDataSource.class, hint = "nexus" )
public class NexusArchetypeDataSource
    extends AbstractArchetypeDataSource
{
    @Requirement
    private NexusIndexer nexusIndexer;

    @Override
    protected List<IndexingContext> getIndexingContexts()
    {
        return new ArrayList<IndexingContext>( nexusIndexer.getIndexingContexts().values() );
    }
}
