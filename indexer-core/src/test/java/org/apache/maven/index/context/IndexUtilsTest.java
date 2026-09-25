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
package org.apache.maven.index.context;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexUtilsTest {

    @Test
    void copyDirectoryWithPropertiesFiles() throws IOException {
        try (Directory source = new ByteBuffersDirectory();
                Directory target = new ByteBuffersDirectory()) {
            write(source, IndexingContext.INDEX_PACKER_PROPERTIES_FILE, 1);
            write(source, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, 2);
            write(source, "other", 3);
            IndexUtils.updateTimestamp(source, new Date(42L));

            IndexUtils.copyDirectory(source, target);

            assertEquals(1, read(target, IndexingContext.INDEX_PACKER_PROPERTIES_FILE));
            assertEquals(2, read(target, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE));
            assertEquals(3, read(target, "other"));
            assertEquals(new Date(42L), IndexUtils.getTimestamp(target));
        }
    }

    @Test
    void copyDirectoryReplacesExistingPropertiesFiles() throws IOException {
        try (Directory source = new ByteBuffersDirectory();
                Directory target = new ByteBuffersDirectory()) {
            write(source, IndexingContext.INDEX_PACKER_PROPERTIES_FILE, 1);
            write(source, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, 2);
            write(target, IndexingContext.INDEX_PACKER_PROPERTIES_FILE, 10);
            write(target, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE, 20);

            IndexUtils.copyDirectory(source, target);

            assertEquals(1, read(target, IndexingContext.INDEX_PACKER_PROPERTIES_FILE));
            assertEquals(2, read(target, IndexingContext.INDEX_UPDATER_PROPERTIES_FILE));
        }
    }

    private static void write(Directory directory, String name, int value) throws IOException {
        try (IndexOutput out = directory.createOutput(name, IOContext.DEFAULT)) {
            out.writeInt(value);
        }
    }

    private static int read(Directory directory, String name) throws IOException {
        try (IndexInput in = directory.openInput(name, IOContext.DEFAULT)) {
            return in.readInt();
        }
    }
}
