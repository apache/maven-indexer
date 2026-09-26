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
package org.apache.maven.index.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.fs.Locker;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LocalIndexCacheTest extends AbstractIndexUpdaterTest {
    private Path remoteRepo;

    private Path localCacheDir;

    private Path indexDir;

    private IndexingContext tempContext;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        remoteRepo = Path.of("target/localcache/remoterepo")
                .toFile()
                .getCanonicalFile()
                .toPath();
        FileUtils.deleteDirectory(remoteRepo.toFile());
        Files.createDirectories(remoteRepo);

        localCacheDir =
                Path.of("target/localcache/cache").toFile().getCanonicalFile().toPath();
        FileUtils.deleteDirectory(localCacheDir.toFile());
        Files.createDirectories(localCacheDir);

        indexDir =
                Path.of("target/localcache/index").toFile().getCanonicalFile().toPath();
        FileUtils.deleteDirectory(indexDir.toFile());
        Files.createDirectories(indexDir);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        removeTempContext();

        super.tearDown();
    }

    private IndexingContext getNewTempContext() throws IOException, UnsupportedExistingLuceneIndexException {
        removeTempContext();

        tempContext = indexer.addIndexingContext(
                repositoryId + "temp",
                repositoryId,
                repoDir.toFile(),
                indexDir.toFile(),
                repositoryUrl,
                null,
                MIN_CREATORS);

        return tempContext;
    }

    private void removeTempContext() throws IOException {
        if (tempContext != null) {
            indexer.removeIndexingContext(tempContext, true);
            tempContext = null;
            FileUtils.cleanDirectory(indexDir.toFile());
        }
    }

    @Test
    void basic() throws Exception {
        // create initial remote repo index
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;
        IndexingContext testContext;

        // initial index download (expected: full index download)
        testContext = getNewTempContext();
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(2, fetcher.getRetrievedResources().size());
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.gz")));
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.properties")));
        assertGroupCount(1, "commons-lang", testContext);

        // update the same index (expected: no index download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(1, fetcher.getRetrievedResources().size());
        assertEquals(
                "nexus-maven-repository-index.properties",
                fetcher.getRetrievedResources().get(0));
        assertGroupCount(1, "commons-lang", testContext);

        // nuke index but keep the cache (expected: no index download)
        testContext = getNewTempContext();
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(1, fetcher.getRetrievedResources().size());
        assertEquals(
                "nexus-maven-repository-index.properties",
                fetcher.getRetrievedResources().get(0));
        assertGroupCount(1, "commons-lang", testContext);

        // incremental remote update
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.3", null), context);
        packIndex(remoteRepo, context);

        // update via cache (expected: incremental chunk download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(2, fetcher.getRetrievedResources().size());
        assertEquals(
                "nexus-maven-repository-index.properties",
                fetcher.getRetrievedResources().get(0));
        assertEquals(
                "nexus-maven-repository-index.1.gz",
                fetcher.getRetrievedResources().get(1));
        assertGroupCount(2, "commons-lang", testContext);

        // nuke index but keep the cache (expected: no index download, index contains both initial and delta chunks)
        testContext = getNewTempContext();
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(1, fetcher.getRetrievedResources().size());
        assertEquals(
                "nexus-maven-repository-index.properties",
                fetcher.getRetrievedResources().get(0));
        assertGroupCount(2, "commons-lang", testContext);

        // kill the cache, but keep the index (expected: full index download)
        // TODO how to assert if merge==false internally?
        FileUtils.deleteDirectory(localCacheDir.toFile());
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(2, fetcher.getRetrievedResources().size());
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.gz")));
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.properties")));
        assertGroupCount(2, "commons-lang", testContext);
    }

    private void assertGroupCount(int expectedCount, String groupId, IndexingContext context) throws IOException {
        TermQuery query = new TermQuery(new Term(ArtifactInfo.GROUP_ID, groupId));
        FlatSearchResponse response = indexer.searchFlat(new FlatSearchRequest(query, context));
        assertEquals(expectedCount, response.getTotalHits());
    }

    @Test
    void forceIndexDownload() throws Exception {
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;

        // initial index download (expected: no index download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);

        // corrupt local cache
        try (OutputStream fos = Files.newOutputStream(localCacheDir.resolve("nexus-maven-repository-index.gz"))) {
            IOUtil.copy("corrupted", fos);
        }

        // try download again (it would have failed if force did not update local cache)
        removeTempContext();
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updateRequest.setForceFullUpdate(true);
        updater.fetchAndUpdateIndex(updateRequest);
    }

    @Test
    void initialForcedFullDownload() throws Exception {
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;

        // initial forced full index download (expected: successfull download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updateRequest.setForceFullUpdate(true);
        updater.fetchAndUpdateIndex(updateRequest);
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.gz")));
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.properties")));
    }

    @Test
    void failedIndexDownload() throws Exception {
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;

        // failed download
        fetcher = new TrackingFetcher(remoteRepo) {
            public InputStream retrieve(String name) throws IOException, java.io.FileNotFoundException {
                if (name.equals(IndexingContext.INDEX_FILE_PREFIX + ".gz")
                        || name.equals(IndexingContext.INDEX_FILE_PREFIX + ".zip")) {
                    throw new IOException();
                }
                return super.retrieve(name);
            }
        };
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        try {
            updater.fetchAndUpdateIndex(updateRequest);
            fail();
        } catch (IOException e) {
            // expected
        }

        // try successful download
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.gz")));
        assertTrue(Files.exists(localCacheDir.resolve("nexus-maven-repository-index.properties")));
    }

    @Test
    void cleanCacheDirectory() throws Exception {
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;

        // initial index download (expected: successfull download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);

        // new remote index delta
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.3", null), context);
        packIndex(remoteRepo, context);

        // delta index download (expected: successfull download)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);

        // sanity check
        assertTrue(Files.isReadable(localCacheDir.resolve("nexus-maven-repository-index.1.gz")));

        // .lock files are expected to be preserved
        Path lockFile = localCacheDir.resolve(Locker.LOCK_FILE);
        try (OutputStream lockFileOutput = Files.newOutputStream(lockFile)) {
            IOUtil.copy("", lockFileOutput);
        }
        assertTrue(Files.isReadable(lockFile));

        // all unknown files and directories are expected to be removed
        Path unknownFile = localCacheDir.resolve("unknownFile");
        try (OutputStream fileOutputStream = Files.newOutputStream(unknownFile)) {
            IOUtil.copy("", fileOutputStream);
        }

        Path unknownDirectory = localCacheDir.resolve("unknownDirectory");
        Files.createDirectories(unknownDirectory);
        assertTrue(Files.isReadable(unknownFile));
        assertTrue(Files.isDirectory(unknownDirectory));

        // forced full update
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(getNewTempContext(), fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updateRequest.setForceFullUpdate(true);
        updater.fetchAndUpdateIndex(updateRequest);

        assertTrue(Files.isReadable(lockFile));
        assertFalse(Files.isReadable(localCacheDir.resolve("nexus-maven-repository-index.1.gz")));
        assertFalse(Files.isReadable(unknownFile));
        assertFalse(Files.isDirectory(unknownDirectory));
    }

    @Test
    void offline() throws Exception {
        indexer.addArtifactToIndex(
                createArtifactContext(repositoryId, "commons-lang", "commons-lang", "2.2", null), context);
        packIndex(remoteRepo, context);

        //
        TrackingFetcher fetcher;
        IndexUpdateRequest updateRequest;

        // initial index download (expected: successfull download)
        fetcher = new TrackingFetcher(remoteRepo);
        IndexingContext testContext = getNewTempContext();
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updater.fetchAndUpdateIndex(updateRequest);

        // recreate local index from the cache without remote access (and NULL fetcher)
        // fetcher is null, so we no way to assert that
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updateRequest.setOffline(true);
        updater.fetchAndUpdateIndex(updateRequest);
        assertGroupCount(1, "commons-lang", testContext);

        // recreate local index from the cache without remote access (and NOT NULL fetcher)
        fetcher = new TrackingFetcher(remoteRepo);
        updateRequest = new IndexUpdateRequest(testContext, fetcher);
        updateRequest.setLocalIndexCachePath(localCacheDir);
        updateRequest.setOffline(true);
        updater.fetchAndUpdateIndex(updateRequest);
        assertEquals(0, fetcher.getRetrievedResources().size());
        assertGroupCount(1, "commons-lang", testContext);
    }
}
