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

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The {@link Path} variants of the file system API delegate to the deprecated {@link File} ones.
 */
@SuppressWarnings("deprecation")
class PathApiTest {
    @TempDir
    Path dir;

    @Test
    void indexingContextPaths() {
        IndexingContext context = mock(IndexingContext.class, CALLS_REAL_METHODS);
        when(context.getRepository()).thenReturn(dir.resolve("repo").toFile());
        when(context.getIndexDirectoryFile()).thenReturn(null);

        assertEquals(dir.resolve("repo"), context.getRepositoryPath());
        assertNull(context.getIndexDirectoryPath());
    }

    @Test
    void indexerCreateIndexingContextDelegates() throws Exception {
        Indexer indexer = mock(Indexer.class, CALLS_REAL_METHODS);
        when(indexer.createIndexingContext(
                        anyString(),
                        anyString(),
                        any(File.class),
                        any(File.class),
                        any(),
                        any(),
                        anyBoolean(),
                        anyBoolean(),
                        anyList()))
                .thenReturn(null);

        indexer.createIndexingContext("id", "repo", null, dir, null, null, true, false, List.of());

        verify(indexer)
                .createIndexingContext(
                        eq("id"),
                        eq("repo"),
                        isNull(),
                        eq(dir.toFile()),
                        isNull(),
                        isNull(),
                        eq(true),
                        eq(false),
                        eq(List.of()));
    }

    @Test
    void artifactContextPaths() {
        ArtifactContext ac = new ArtifactContext(
                dir.resolve("a.pom").toFile(),
                null,
                null,
                new ArtifactInfo("repo", "g", "a", "1.0", null, "jar"),
                null);

        assertEquals(dir.resolve("a.pom"), ac.getPomPath());
        assertNull(ac.getArtifactPath());
        assertNull(ac.getMetadataPath());
    }

    @Test
    void indexPackingRequestPath() {
        IndexPackingRequest request =
                new IndexPackingRequest(mock(IndexingContext.class), mock(IndexReader.class), dir);

        assertEquals(dir, request.getTargetPath());
        assertEquals(dir.toFile(), request.getTargetDir());
    }

    @Test
    void indexUpdateRequestPaths() {
        IndexUpdateRequest request =
                new IndexUpdateRequest(mock(IndexingContext.class), new DefaultIndexUpdater.FileFetcher(dir));

        request.setIndexTempPath(dir.resolve("tmp"));
        request.setLocalIndexCachePath(null);

        assertEquals(dir.resolve("tmp"), request.getIndexTempPath());
        assertEquals(dir.resolve("tmp").toFile(), request.getIndexTempDir());
        assertNull(request.getLocalIndexCachePath());
    }
}
