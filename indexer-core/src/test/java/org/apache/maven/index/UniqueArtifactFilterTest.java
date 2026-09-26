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
package org.apache.maven.index;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UniqueArtifactFilterTest extends AbstractIndexCreatorHelper {
    private IndexingContext context;

    @Test
    void searchIterator() throws Exception {
        NexusIndexer indexer = prepare();

        Query q = indexer.constructQuery(MAVEN.GROUP_ID, "qdox", SearchType.SCORED);

        IteratorSearchRequest request = new IteratorSearchRequest(q);

        IteratorSearchResponse response = indexer.searchIterator(request);

        assertEquals(2, response.getTotalHits());

        for (ArtifactInfo ai : response.getResults()) {
            assertEquals("qdox", ai.getGroupId(), "GroupId must match \"qdox\"!");
        }
    }

    @Test
    void searchIteratorWithFilter() throws Exception {
        NexusIndexer indexer = prepare();

        Query q = indexer.constructQuery(MAVEN.GROUP_ID, "commons", SearchType.SCORED);

        UniqueArtifactFilterPostprocessor filter = new UniqueArtifactFilterPostprocessor();
        filter.addField(MAVEN.GROUP_ID);
        filter.addField(MAVEN.ARTIFACT_ID);

        IteratorSearchRequest request = new IteratorSearchRequest(q, filter);

        try (IteratorSearchResponse response = indexer.searchIterator(request)) {
            assertEquals(15, response.getTotalHits(), "15 total hits (before filtering!)");

            ArtifactInfo ai = response.getResults().next();
            assertNotNull(ai, "Iterator has to have next (2 should be returned)");

            ai = response.getResults().next();
            assertNotNull(ai, "Iterator has to have next (2 should be returned)");

            assertEquals(
                    UniqueArtifactFilterPostprocessor.COLLAPSED,
                    ai.getVersion(),
                    "Property that is not unique has to have \"COLLAPSED\" value!");
            assertEquals(
                    UniqueArtifactFilterPostprocessor.COLLAPSED,
                    ai.getPackaging(),
                    "Property that is not unique has to have \"COLLAPSED\" value!");
            assertEquals(
                    UniqueArtifactFilterPostprocessor.COLLAPSED,
                    ai.getClassifier(),
                    "Property that is not unique has to have \"COLLAPSED\" value!");
        }
    }

    // ==

    private NexusIndexer prepare() throws Exception, IOException, UnsupportedExistingLuceneIndexException {
        NexusIndexer indexer = lookup(NexusIndexer.class);

        // Directory indexDir = new RAMDirectory();
        Path indexDir = getTestPath("target/index/test-" + System.currentTimeMillis());
        deleteDirectory(indexDir);

        Path repo = getTestPath("src/test/repo");

        context = indexer.addIndexingContext(
                "test", "test", repo.toFile(), indexDir.toFile(), null, null, DEFAULT_CREATORS);

        indexer.scan(context);

        return indexer;
    }
}
