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
package org.apache.maven.index.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.reader.Record.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test support.
 */
public class TestSupport {
    @Rule
    public TestName testName = new TestName();

    private Path tempDir;

    private List<DirectoryResourceHandler> directoryResourceHandlers;

    /**
     * Creates the temp directory and list for resource handlers.
     */
    @Before
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory(getClass().getSimpleName() + ".temp");
        this.directoryResourceHandlers = new ArrayList<>();
    }

    /**
     * Closes all the registered resources handlers and deletes the temp directory.
     */
    @After
    public void cleanup() throws IOException {
        for (DirectoryResourceHandler directoryResourceHandler : directoryResourceHandlers) {
            directoryResourceHandler.close();
        }
        // delete(tempDir);
    }

    /**
     * Creates a temp file within {@link #tempDir} with given name.
     */
    protected Path createTempFile(final String name) throws IOException {
        Path file = tempDir.resolve(name);
        file.toFile().deleteOnExit();
        return file;
    }

    /**
     * Creates a temp directory within {@link #tempDir}.
     */
    protected Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(tempDir, testName.getMethodName() + "-dir");
    }

    /**
     * Creates an empty {@link DirectoryResourceHandler}.
     */
    protected WritableResourceHandler createWritableResourceHandler() throws IOException {
        DirectoryResourceHandler result = new DirectoryResourceHandler(createTempDirectory());
        directoryResourceHandlers.add(result);
        return result;
    }

    /**
     * Creates a "test" {@link ResourceHandler} that contains predefined files, is mapped to test resources under given
     * name.
     */
    protected DirectoryResourceHandler testResourceHandler(final String name) throws IOException {
        DirectoryResourceHandler result = new DirectoryResourceHandler(Path.of("src/test/resources/" + name));
        directoryResourceHandlers.add(result);
        return result;
    }

    /**
     * Consumes {@link ChunkReader} and creates a map "by type" with records.
     */
    protected Map<Type, List<Record>> loadRecordsByType(final ChunkReader chunkReader) throws IOException {
        HashMap<Type, List<Record>> stat = new HashMap<>();
        try (chunkReader) {
            assertThat(chunkReader.getVersion(), equalTo(1));
            final RecordExpander recordExpander = new RecordExpander();
            for (Map<String, String> rec : chunkReader) {
                final Record record = recordExpander.apply(rec);
                if (!stat.containsKey(record.getType())) {
                    stat.put(record.getType(), new ArrayList<>());
                }
                stat.get(record.getType()).add(record);
            }
        }
        return stat;
    }

    /**
     * Consumes {@link ChunkReader} and creates a map "by type" with record type counts.
     */
    protected Map<Type, Integer> countRecordsByType(final ChunkReader chunkReader) throws IOException {
        HashMap<Type, Integer> stat = new HashMap<>();
        try (chunkReader) {
            assertThat(chunkReader.getVersion(), equalTo(1));
            final RecordExpander recordExpander = new RecordExpander();
            for (Map<String, String> rec : chunkReader) {
                final Record record = recordExpander.apply(rec);
                if (!stat.containsKey(record.getType())) {
                    stat.put(record.getType(), 0);
                }
                stat.put(record.getType(), stat.get(record.getType()) + 1);
            }
        }
        return stat;
    }
}
