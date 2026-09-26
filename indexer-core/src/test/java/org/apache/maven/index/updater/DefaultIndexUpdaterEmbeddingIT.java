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
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import org.apache.maven.index.Java11HttpClient;
import org.apache.maven.index.context.DefaultIndexingContext;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.updater.fixtures.ServerTestFixture;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultIndexUpdaterEmbeddingIT extends InjectedTest {
    private String baseUrl;

    private ServerTestFixture server;

    @Inject
    private IndexUpdater updater;

    @BeforeEach
    @Override
    public void setUp() throws Exception {

        int port;
        try (final ServerSocket ss = new ServerSocket(0)) {
            ss.setReuseAddress(true);
            port = ss.getLocalPort();
        }

        baseUrl = "http://127.0.0.1:" + port + "/";
        server = new ServerTestFixture(port);

        super.setUp();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void basicIndexRetrieval() throws Exception {
        Path basedir = Files.createTempDirectory("nexus-indexer.");
        Files.deleteIfExists(basedir);
        Files.createDirectories(basedir);

        try {
            IndexingContext ctx = newTestContext(basedir, baseUrl);

            IndexUpdateRequest updateRequest = new IndexUpdateRequest(ctx, new Java11HttpClient());

            updater.fetchAndUpdateIndex(updateRequest);

            ctx.close(false);
        } finally {
            try {
                FileUtils.forceDelete(basedir.toFile());
            } catch (IOException e) {
            }
        }
    }

    @Test
    void indexTempDirB() throws Exception {
        Path basedir = Files.createTempDirectory("nexus-indexer.");
        Files.deleteIfExists(basedir);
        Files.createDirectories(basedir);

        Path indexTempDir = Files.createTempDirectory("index-temp");
        Files.deleteIfExists(indexTempDir);
        // temp dir should not exists
        assertFalse(Files.exists(indexTempDir));

        try {
            IndexingContext ctx = newTestContext(basedir, baseUrl);

            IndexUpdateRequest updateRequest = new IndexUpdateRequest(ctx, new Java11HttpClient());
            updateRequest.setIndexTempDir(indexTempDir.toFile());

            updater.fetchAndUpdateIndex(updateRequest);

            // dir should still exists after retrival
            assertTrue(Files.exists(indexTempDir));
            Files.deleteIfExists(indexTempDir);
            ctx.close(false);
        } finally {
            try {
                FileUtils.forceDelete(basedir.toFile());
            } catch (IOException e) {
            }
        }
    }

    @Test
    void basicHighLatencyIndexRetrieval() throws Exception {
        Path basedir = Files.createTempDirectory("nexus-indexer.");

        try {
            IndexingContext ctx = newTestContext(basedir, baseUrl + "slow/");

            IndexUpdateRequest updateRequest = new IndexUpdateRequest(ctx, new Java11HttpClient());

            updater.fetchAndUpdateIndex(updateRequest);

            ctx.close(false);
        } finally {
            try {
                FileUtils.forceDelete(basedir.toFile());
            } catch (IOException e) {
            }
        }
    }

    @Test
    void indexRetrievalInfiniteRedirection() throws Exception {
        Path basedir = Files.createTempDirectory("nexus-indexer.");

        try {
            IndexingContext ctx = newTestContext(basedir, baseUrl + "redirect-trap/");

            IndexUpdateRequest updateRequest = new IndexUpdateRequest(ctx, new Java11HttpClient());

            try {
                updater.fetchAndUpdateIndex(updateRequest);
                fail("Should throw IOException from too many redirects.");
            } catch (IOException e) {
                System.out.println("Operation timed out due to too many redirects, as expected.");
            }

            ctx.close(false);
        } finally {
            try {
                FileUtils.forceDelete(basedir.toFile());
            } catch (IOException e) {
            }
        }
    }

    @Test
    void indexRetrievalBadHostname() throws Exception {
        Path basedir = Files.createTempDirectory("nexus-indexer.");

        try {
            IndexingContext ctx = newTestContext(basedir, "http://dummy/");

            IndexUpdateRequest updateRequest = new IndexUpdateRequest(ctx, new Java11HttpClient());

            try {
                updater.fetchAndUpdateIndex(updateRequest);
                fail("Should timeout and throw IOException.");
            } catch (Exception e) {
                System.out.println("Connection failed as expected.");
            }

            ctx.close(false);
        } finally {
            try {
                FileUtils.forceDelete(basedir.toFile());
            } catch (IOException e) {
            }
        }
    }

    private IndexingContext newTestContext(final Path basedir, final String baseUrl)
            throws IOException, UnsupportedExistingLuceneIndexException {
        IndexCreator min = lookup(IndexCreator.class, "min");
        IndexCreator jar = lookup(IndexCreator.class, "jarContent");

        List<IndexCreator> creators = new ArrayList<>();
        creators.add(min);
        creators.add(jar);

        String repositoryId = "test";

        return new DefaultIndexingContext(
                repositoryId, repositoryId, basedir.toFile(), basedir.toFile(), baseUrl, baseUrl, creators, true);
    }
}
