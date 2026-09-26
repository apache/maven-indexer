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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.maven.index.Java11HttpClient;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.fixtures.HttpServerFixture;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadRemoteIndexerManagerTest extends AbstractIndexUpdaterTest {
    private HttpServerFixture server;

    private Path fakeCentral;

    private IndexingContext centralContext;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        fakeCentral = getTestPath("target/repos/fake-central");
        Files.createDirectories(fakeCentral);

        // create proxy server
        ServerSocket s = new ServerSocket(0);
        int port = s.getLocalPort();
        s.close();

        server = new HttpServerFixture(port, fakeCentral);
        server.start();

        // make context "fake central"
        centralContext = indexer.addIndexingContext(
                "central",
                "central",
                fakeCentral.toFile(),
                getDirectory("central").toFile(),
                "http://localhost:" + port,
                null,
                MIN_CREATORS);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        server.stop();

        FileUtils.forceDelete(fakeCentral.toFile());

        super.tearDown();
    }

    @Test
    void repoReindex() throws Exception {
        IndexUpdateRequest iur;

        Path index1 = getTestPath("src/test/resources/repo-index/index");
        Path index2 = getTestPath("src/test/resources/repo-index/index2");
        Path centralIndex = fakeCentral.resolve(".index");

        // copy index 02
        overwriteIndex(index2, centralIndex);

        iur = new IndexUpdateRequest(centralContext, new Java11HttpClient());
        iur.setForceFullUpdate(true);

        updater.fetchAndUpdateIndex(iur);

        searchFor("org.sonatype.nexus", 8, centralContext);

        // copy index 01
        overwriteIndex(index1, centralIndex);

        iur = new IndexUpdateRequest(centralContext, new Java11HttpClient());
        iur.setForceFullUpdate(true);
        // just a dummy filter to invoke filtering! -- this is what I broke unnoticing it
        iur.setDocumentFilter(doc -> true);

        updater.fetchAndUpdateIndex(iur);

        searchFor("org.sonatype.nexus", 1, centralContext);

        // copy index 02
        overwriteIndex(index2, centralIndex);

        iur = new IndexUpdateRequest(centralContext, new Java11HttpClient());
        iur.setForceFullUpdate(true);

        updater.fetchAndUpdateIndex(iur);

        searchFor("org.sonatype.nexus", 8, centralContext);
    }

    private void overwriteIndex(Path source, Path destination) throws Exception {
        Path indexFile = destination.resolve("nexus-maven-repository-index.gz");
        Path indexProperties = destination.resolve("nexus-maven-repository-index.properties");

        long lastMod = -1;
        if (Files.exists(destination)) {
            FileUtils.forceDelete(destination.toFile());
            // indexFile no longer exists at this point; File#lastModified() tolerates that (returns 0),
            // Files.getLastModifiedTime would throw NoSuchFileException, so File is used deliberately here.
            lastMod = indexFile.toFile().lastModified();
        }
        FileUtils.copyDirectory(source.toFile(), destination.toFile());
        long lastMod2 = Files.getLastModifiedTime(indexFile).toMillis();
        assertTrue(lastMod < lastMod2);

        Properties p = new Properties();
        try (InputStream input = Files.newInputStream(indexProperties)) {
            p.load(input);
        }

        p.setProperty("nexus.index.time", format(new Date()));
        p.setProperty("nexus.index.timestamp", format(new Date()));

        try (OutputStream output = Files.newOutputStream(indexProperties)) {
            p.store(output, null);
        }
    }

    private String format(Date d) {
        SimpleDateFormat df = new SimpleDateFormat(IndexingContext.INDEX_TIME_FORMAT);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(d);
    }
}
