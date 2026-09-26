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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Class names are stored by indexer-core JarFileContentsIndexCreator in field "c", one class name per line.
 */
class ClassNamesTest {
    private static final String[] CLASS_NAMES = {
        "/com/thoughtworks/qdox/ant/AbstractQdoxTask", "/com/thoughtworks/qdox/directorywalker/DirectoryScanner"
    };

    private static Map<String, String> artifact() {
        Map<String, String> raw = new HashMap<>();
        raw.put("u", "qdox|qdox|1.5|NA|jar");
        raw.put("i", "jar|1180422566000|154327|0|0|0|jar");
        return raw;
    }

    @Test
    void expandIndexerCoreClassNames() {
        Map<String, String> raw = artifact();
        // as written by indexer-core, including the trailing newline
        raw.put("c", String.join("\n", CLASS_NAMES) + "\n");

        Record record = new RecordExpander().apply(raw);

        assertArrayEquals(CLASS_NAMES, record.getStringArray(Record.CLASSNAMES));
    }

    @Test
    void expandLegacyClassNames() {
        Map<String, String> raw = artifact();
        raw.put("classnames", String.join("|", CLASS_NAMES));

        Record record = new RecordExpander().apply(raw);

        assertArrayEquals(CLASS_NAMES, record.getStringArray(Record.CLASSNAMES));
    }

    @Test
    void compactWritesIndexerCoreFormat() {
        Map<String, String> raw = artifact();
        raw.put("c", String.join("\n", CLASS_NAMES));

        Map<String, String> compacted = new RecordCompactor().apply(new RecordExpander().apply(raw));

        assertEquals(String.join("\n", CLASS_NAMES), compacted.get("c"));
        assertFalse(compacted.containsKey("classnames"));
    }
}
