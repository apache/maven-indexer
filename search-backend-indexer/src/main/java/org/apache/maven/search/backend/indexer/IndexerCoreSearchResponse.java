package org.apache.maven.search.backend.indexer;

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

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.search.SearchResponse;

/**
 * The Indexer Core search response.
 */
public interface IndexerCoreSearchResponse extends SearchResponse
{
    /**
     * Returns the Lucene query used to create this response, never {@code null}.
     */
    Query getQuery();

    /**
     * Returns the "raw" list of {@link ArtifactInfo}s used to create this response, never {@code null}.
     */
    List<ArtifactInfo> getArtifactInfos();
}
