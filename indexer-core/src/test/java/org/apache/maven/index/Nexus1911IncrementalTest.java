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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FIXME - hardcoded assumptions in test that break with lucene 4, or bugs?
// @Ignore("Segment merge may work differently in Lucene 4")
public class Nexus1911IncrementalTest extends AbstractIndexCreatorHelper {
    NexusIndexer indexer;

    IndexingContext context;

    IndexingContext reindexedContext;

    IndexPacker packer;

    Path indexDir;

    Path indexPackDir;

    Path reposTargetDir;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        indexer = lookup(NexusIndexer.class);
        packer = lookup(IndexPacker.class);

        indexDir = super.getDirectory("index/nexus-1911");
        indexPackDir = indexDir; // super.getDirectory( "index/nexus-1911-pack" );

        Path reposSrcDir = getTestPath("src/test/nexus-1911");
        this.reposTargetDir = super.getDirectory("repos/nexus-1911");

        FileUtils.copyDirectoryStructure(reposSrcDir.toFile(), reposTargetDir.toFile());

        Path repo = reposTargetDir.resolve("repo");
        Files.createDirectories(repo);
        reindexedContext = context = indexer.addIndexingContext(
                "test", "test", repo.toFile(), indexDir.toFile(), null, null, DEFAULT_CREATORS);
        indexer.scan(context);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        indexer.removeIndexingContext(context, true);
        super.deleteDirectory(this.reposTargetDir);
        super.deleteDirectory(this.indexDir);
        super.deleteDirectory(this.indexPackDir);
        super.tearDown();
    }

    @Test
    void noIncremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        List<Path> packedFiles = listDir(indexPackDir);
        Set<String> filenames = getFilenamesFromFiles(packedFiles);
        Properties props = getPropertiesFromFiles(packedFiles);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".1.gz"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".2.gz"));

        assertNotNull(props);

        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals("0", props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER));
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    void test1Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);

        List<Path> packedFiles = listDir(indexPackDir);
        Set<String> filenames = getFilenamesFromFiles(packedFiles);
        Properties props = getPropertiesFromFiles(packedFiles);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".1.gz"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".2.gz"));

        assertNotNull(props);

        assertEquals("1", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals("1", props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER));
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    void test2Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-2"), IndexPackingRequest.MAX_CHUNKS);

        List<Path> packedFiles = listDir(indexPackDir);
        Set<String> filenames = getFilenamesFromFiles(packedFiles);
        Properties props = getPropertiesFromFiles(packedFiles);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        // 1 is missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".2.gz"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".3.gz"));

        assertNotNull(props);

        assertEquals("2", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"));
        assertEquals("1", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals("2", props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER));
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    void test3Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-2"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-3"), IndexPackingRequest.MAX_CHUNKS);

        List<Path> packedFiles = listDir(indexPackDir);
        Set<String> filenames = getFilenamesFromFiles(packedFiles);
        Properties props = getPropertiesFromFiles(packedFiles);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        // 1,2 are missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".3.gz"));

        assertNotNull(props);

        assertEquals("3", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"));
        assertEquals("2", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertEquals("1", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals("3", props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER));
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    void maxChunks() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            request.setMaxIndexChunks(3);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-1"), 3);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-2"), 3);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-3"), 3);
        copyRepoContentsAndReindex(getTestPath("src/test/nexus-1911/repo-inc-4"), 3);

        List<Path> packedFiles = listDir(indexPackDir);
        Set<String> filenames = getFilenamesFromFiles(packedFiles);
        Properties props = getPropertiesFromFiles(packedFiles);

        System.out.println(filenames);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".1.gz"));

        // 2,3 are missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".3.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".4.gz"));

        assertNotNull(props);

        assertEquals("4", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"));
        assertEquals("3", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertEquals("2", props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals("4", props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER));
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    private void copyRepoContentsAndReindex(Path src, int maxIndexChunks) throws Exception {
        Path reposTargetDir = getTestPath("target/repos/nexus-1911/repo");

        FileUtils.copyDirectoryStructure(src.toFile(), reposTargetDir.toFile());

        // this was ALWAYS broken, if incremental reindex wanted, this has to be TRUE!!!
        // TODO: fix this!
        indexer.scan(reindexedContext, false);

        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            request.setMaxIndexChunks(maxIndexChunks);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }
    }

    private List<Path> listDir(Path dir) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.collect(Collectors.toList());
        }
    }

    private Set<String> getFilenamesFromFiles(List<Path> files) {
        Set<String> filenames = new HashSet<>();

        for (Path file : files) {
            filenames.add(file.getFileName().toString());
        }

        return filenames;
    }

    private Properties getPropertiesFromFiles(List<Path> files) throws Exception {
        Properties props = new Properties();
        Path propertyFile = null;

        for (Path file : files) {
            if ((IndexingContext.INDEX_REMOTE_PROPERTIES_FILE)
                    .equalsIgnoreCase(file.getFileName().toString())) {
                propertyFile = file;
                break;
            }
        }

        try (InputStream fis = Files.newInputStream(propertyFile)) {
            props.load(fis);
        }

        return props;
    }
}
