package org.apache.maven.indexer.examples.indexing;

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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.index.ArtifactInfo;

/**
 * This is a convenience class for holding search results mapped by repository.
 *
 * @author mtodorov
 */
public class SearchResults
{

    /**
     * K: repositoryId
     * V: artifactInfos
     */
    private Map<String, Collection<ArtifactInfo>> results = new LinkedHashMap<>();


    public SearchResults()
    {
    }

    public Map<String, Collection<ArtifactInfo>> getResults()
    {
        return results;
    }

    public void setResults( Map<String, Collection<ArtifactInfo>> results )
    {
        this.results = results;
    }

}
