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

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * A search engine used to perform searches trough repository indexes.
 * 
 * @author Eugene Kuleshov
 * @author Jason van Zyl
 * @author Tamas Cservenak
 */
public interface SearchEngine
{
    @Deprecated
    Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator, IndexingContext indexingContext,
                                  Query query )
        throws IOException;

    @Deprecated
    Set<ArtifactInfo> searchFlat( Comparator<ArtifactInfo> artifactInfoComparator,
                                  Collection<IndexingContext> indexingContexts, Query query )
        throws IOException;

    /**
     * Do the search only on searchable contexts
     */
    FlatSearchResponse searchFlatPaged( FlatSearchRequest request, Collection<IndexingContext> indexingContexts )
        throws IOException;

    /**
     * Do the search only on searchable contexts
     */
    IteratorSearchResponse searchIteratorPaged( IteratorSearchRequest request,
                                                Collection<IndexingContext> indexingContexts )
        throws IOException;

    /**
     * Do the search only on searchable contexts
     */
    GroupedSearchResponse searchGrouped( GroupedSearchRequest request, Collection<IndexingContext> indexingContexts )
        throws IOException;

    /**
     * Do the search in all contexts, no matter if the context is searchable or not
     */
    FlatSearchResponse forceSearchFlatPaged( FlatSearchRequest request, Collection<IndexingContext> indexingContexts )
        throws IOException;

    /**
     * Do the search in all contexts, no matter if the context is searchable or not
     */
    IteratorSearchResponse forceSearchIteratorPaged( IteratorSearchRequest request,
                                                     Collection<IndexingContext> indexingContexts )
        throws IOException;

    /**
     * Do the search in all contexts, no matter if the context is searchable or not
     */
    GroupedSearchResponse forceSearchGrouped( GroupedSearchRequest request,
                                              Collection<IndexingContext> indexingContexts )
        throws IOException;
}
