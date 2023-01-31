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
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// FIXME - hardcoded assumptions in test that break with lucene 4, or bugs?
// @Ignore("Segment merge may work differently in Lucene 4")
public class Nexus1911IncrementalTest extends AbstractIndexCreatorHelper {
    NexusIndexer indexer;

    IndexingContext context;

    IndexingContext reindexedContext;

    IndexPacker packer;

    File indexDir;

    File indexPackDir;

    File reposTargetDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        indexer = lookup(NexusIndexer.class);
        packer = lookup(IndexPacker.class);

        indexDir = super.getDirectory("index/nexus-1911");
        indexPackDir = indexDir; // super.getDirectory( "index/nexus-1911-pack" );

        File reposSrcDir = new File(getBasedir(), "src/test/nexus-1911");
        this.reposTargetDir = super.getDirectory("repos/nexus-1911");

        FileUtils.copyDirectoryStructure(reposSrcDir, reposTargetDir);

        File repo = new File(reposTargetDir, "repo");
        repo.mkdirs();
        reindexedContext =
                context = indexer.addIndexingContext("test", "test", repo, indexDir, null, null, DEFAULT_CREATORS);
        indexer.scan(context);
    }

    @Override
    public void tearDown() throws Exception {
        indexer.removeIndexingContext(context, true);
        super.deleteDirectory(this.reposTargetDir);
        super.deleteDirectory(this.indexDir);
        super.deleteDirectory(this.indexPackDir);
        super.tearDown();
    }

    @Test
    public void testNoIncremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        Set<String> filenames = getFilenamesFromFiles(indexPackDir.listFiles());
        Properties props = getPropertiesFromFiles(indexPackDir.listFiles());

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
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER), "0");
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    public void test1Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);

        Set<String> filenames = getFilenamesFromFiles(indexPackDir.listFiles());
        Properties props = getPropertiesFromFiles(indexPackDir.listFiles());

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".1.gz"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".2.gz"));

        assertNotNull(props);

        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"), "1");
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER), "1");
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    public void test2Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-2"), IndexPackingRequest.MAX_CHUNKS);

        Set<String> filenames = getFilenamesFromFiles(indexPackDir.listFiles());
        Properties props = getPropertiesFromFiles(indexPackDir.listFiles());

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        // 1 is missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".2.gz"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".3.gz"));

        assertNotNull(props);

        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"), "2");
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"), "1");
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER), "2");
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    public void test3Incremental() throws Exception {
        final IndexSearcher indexSearcher = context.acquireIndexSearcher();
        try {
            IndexPackingRequest request =
                    new IndexPackingRequest(context, indexSearcher.getIndexReader(), indexPackDir);
            request.setCreateIncrementalChunks(true);
            packer.packIndex(request);
        } finally {
            context.releaseIndexSearcher(indexSearcher);
        }

        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-1"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-2"), IndexPackingRequest.MAX_CHUNKS);
        copyRepoContentsAndReindex(
                new File(getBasedir(), "src/test/nexus-1911/repo-inc-3"), IndexPackingRequest.MAX_CHUNKS);

        Set<String> filenames = getFilenamesFromFiles(indexPackDir.listFiles());
        Properties props = getPropertiesFromFiles(indexPackDir.listFiles());

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        // 1,2 are missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".1.gz" ) );
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".3.gz"));

        assertNotNull(props);

        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"), "3");
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"), "2");
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"), "1");
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER), "3");
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    @Test
    public void testMaxChunks() throws Exception {
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

        copyRepoContentsAndReindex(new File(getBasedir(), "src/test/nexus-1911/repo-inc-1"), 3);
        copyRepoContentsAndReindex(new File(getBasedir(), "src/test/nexus-1911/repo-inc-2"), 3);
        copyRepoContentsAndReindex(new File(getBasedir(), "src/test/nexus-1911/repo-inc-3"), 3);
        copyRepoContentsAndReindex(new File(getBasedir(), "src/test/nexus-1911/repo-inc-4"), 3);

        Set<String> filenames = getFilenamesFromFiles(indexPackDir.listFiles());
        Properties props = getPropertiesFromFiles(indexPackDir.listFiles());

        System.out.println(filenames);

        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".gz"));
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".properties"));
        assertFalse(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".1.gz"));

        // 2,3 are missing with updated Lucene 4 implementation
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".2.gz" ) );
        //        Assert.assertTrue( filenames.contains( IndexingContext.INDEX_FILE_PREFIX + ".3.gz" ) );
        assertTrue(filenames.contains(IndexingContext.INDEX_FILE_PREFIX + ".4.gz"));

        assertNotNull(props);

        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "0"), "4");
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "1"), "3");
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "2"), "2");
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "3"));
        assertNull(props.getProperty(IndexingContext.INDEX_CHUNK_PREFIX + "4"));
        assertEquals(props.getProperty(IndexingContext.INDEX_CHUNK_COUNTER), "4");
        assertNotNull(props.getProperty(IndexingContext.INDEX_CHAIN_ID));
    }

    private void copyRepoContentsAndReindex(File src, int maxIndexChunks) throws Exception {
        File reposTargetDir = new File(getBasedir(), "target/repos/nexus-1911/repo");

        FileUtils.copyDirectoryStructure(src, reposTargetDir);

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

    private Set<String> getFilenamesFromFiles(File[] files) {
        Set<String> filenames = new HashSet<>();

        for (File file : files) {
            filenames.add(file.getName());
        }

        return filenames;
    }

    private Properties getPropertiesFromFiles(File[] files) throws Exception {
        Properties props = new Properties();
        File propertyFile = null;

        for (File file : files) {
            if ((IndexingContext.INDEX_REMOTE_PROPERTIES_FILE).equalsIgnoreCase(file.getName())) {
                propertyFile = file;
                break;
            }
        }

        try (FileInputStream fis = new FileInputStream(propertyFile)) {
            props.load(fis);
        }

        return props;
    }
}
