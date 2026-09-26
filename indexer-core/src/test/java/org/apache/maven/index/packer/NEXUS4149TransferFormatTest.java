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
package org.apache.maven.index.packer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.AbstractNexusIndexerTest;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.MergedIndexingContext;
import org.apache.maven.index.packer.IndexPackingRequest.IndexFormat;
import org.apache.maven.index.updater.IndexDataReader;
import org.codehaus.plexus.util.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NEXUS4149TransferFormatTest extends AbstractNexusIndexerTest {
    protected Path reposBase = getTestPath("src/test/nexus-4149");

    protected Path idxsBase = getTestPath("target/index/nexus-4149");

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void prepareNexusIndexer(NexusIndexer nexusIndexer) throws Exception {
        IndexingContext ctx1 = nexusIndexer.addIndexingContext(
                "repo1",
                "repo1",
                reposBase.resolve("repo1").toFile(),
                idxsBase.resolve("repo1").toFile(),
                null,
                null,
                MIN_CREATORS);
        nexusIndexer.scan(ctx1);

        IndexingContext ctx2 = nexusIndexer.addIndexingContext(
                "repo2",
                "repo2",
                reposBase.resolve("repo2").toFile(),
                idxsBase.resolve("repo2").toFile(),
                null,
                null,
                MIN_CREATORS);
        nexusIndexer.scan(ctx2);

        IndexingContext ctx3 = nexusIndexer.addIndexingContext(
                "repo3",
                "repo3",
                reposBase.resolve("repo3").toFile(),
                idxsBase.resolve("repo3").toFile(),
                null,
                null,
                MIN_CREATORS);
        nexusIndexer.scan(ctx3);

        IndexingContext ctx4 = nexusIndexer.addIndexingContext(
                "repo4",
                "repo4",
                reposBase.resolve("repo4").toFile(),
                idxsBase.resolve("repo4").toFile(),
                null,
                null,
                MIN_CREATORS);
        nexusIndexer.scan(ctx4);

        context = nexusIndexer.addMergedIndexingContext(
                "ctx",
                "ctx",
                reposBase.resolve("merged").toFile(),
                idxsBase.resolve("merged").toFile(),
                false,
                Arrays.asList(ctx1, ctx2, ctx3, ctx4));

        Files.createDirectories(context.getIndexDirectoryPath());
    }

    @Override
    protected void unprepareNexusIndexer(NexusIndexer nexusIndexer) throws Exception {
        // remove the merged
        nexusIndexer.removeIndexingContext(context, true);

        // remove members
        MergedIndexingContext mctx = (MergedIndexingContext) context;

        for (IndexingContext member : mctx.getMembers()) {
            nexusIndexer.removeIndexingContext(member, true);
        }
    }

    @Override
    @Test
    protected void directory() throws Exception {
        // we use no directory
    }

    @Test
    void membersAndMergedRootGroups() throws Exception {
        MergedIndexingContext mctx = (MergedIndexingContext) context;

        for (IndexingContext member : mctx.getMembers()) {
            if (!"repo4".equals(member.getId())) // repo4 is empty
            {
                assertEquals(1, member.getRootGroups().size(), "Members should have one root group!");
            }
        }

        assertEquals(
                3, mctx.getRootGroups().size(), "Merged should have one root multiply members count (sans repo4)!");
    }

    @Test
    void transportFile() throws Exception {
        Path packTargetDir = getTestPath("target/nexus-4149/packed");

        IndexPacker packer = lookup(IndexPacker.class);

        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), packTargetDir);
            request.setCreateIncrementalChunks(false);
            request.setFormats(Arrays.asList(IndexFormat.FORMAT_V1));

            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        // read it up and verify, but stay "low level", directly consume the GZ file and count
        InputStream fis =
                new BufferedInputStream(Files.newInputStream(packTargetDir.resolve("nexus-maven-repository-index.gz")));
        IndexDataReader reader = new IndexDataReader(fis);
        try {
            // read header and neglect it
            reader.readHeader();

            // read docs
            int totalDocs = 0;
            int specialDocs = 0;
            int artifactDocs = 0;
            String allGroups = null;
            String rootGroups = null;
            Document doc;
            while ((doc = reader.readDocument()) != null) {
                totalDocs++;
                if (doc.getField("DESCRIPTOR") != null
                        || doc.getField(ArtifactInfo.ALL_GROUPS) != null
                        || doc.getField(ArtifactInfo.ROOT_GROUPS) != null) {
                    specialDocs++;

                    if (doc.get(ArtifactInfo.ALL_GROUPS) != null) {
                        allGroups = doc.get(ArtifactInfo.ALL_GROUPS_LIST);
                    }
                    if (doc.get(ArtifactInfo.ROOT_GROUPS) != null) {
                        rootGroups = doc.get(ArtifactInfo.ROOT_GROUPS_LIST);
                    }
                } else {
                    artifactDocs++;
                }
            }

            assertNotNull(allGroups, "Group transport file should contain allGroups!");
            assertNotNull(rootGroups, "Group transport file should contain rootGroups!");
            checkListOfStringDoesNotContainEmptyString(ArtifactInfo.str2lst(allGroups));
            checkListOfStringDoesNotContainEmptyString(ArtifactInfo.str2lst(rootGroups));

            assertEquals(15, totalDocs);
            // 1 descriptor + 1 allGroups + 1 rootGroups
            assertEquals(3, specialDocs);
            // repo1 has 1 artifact, repo2 has 1 artifact and repo3 has 10 artifact
            assertEquals(12, artifactDocs);

        } finally {
            fis.close();
        }
    }

    protected void checkListOfStringDoesNotContainEmptyString(List<String> lst) {
        if (lst != null) {
            for (String str : lst) {
                if (StringUtils.isBlank(str)) {
                    throw new IllegalArgumentException("List " + lst + " contains empty string!");
                }
            }
        }
    }
}
