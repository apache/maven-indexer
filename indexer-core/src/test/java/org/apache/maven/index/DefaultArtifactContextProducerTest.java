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

import org.apache.maven.index.artifact.DefaultArtifactPackagingMapper;
import org.apache.maven.index.artifact.M2GavCalculator;
import org.apache.maven.index.context.IndexingContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultArtifactContextProducerTest {
    private static final Path REPO = Path.of("src/test/repo").toAbsolutePath();

    /**
     * Checksums and signatures are never indexed as artifacts, even if {@code isIndexable} lets them through.
     */
    @Test
    void sidecarsAreNotArtifacts() {
        DefaultArtifactContextProducer producer =
                new DefaultArtifactContextProducer(new DefaultArtifactPackagingMapper()) {
                    @Override
                    protected boolean isIndexable(File file) {
                        return file != null;
                    }
                };
        IndexingContext context = mock(IndexingContext.class);
        when(context.getRepository()).thenReturn(REPO.toFile());
        when(context.getRepositoryId()).thenReturn("test");
        when(context.getGavCalculator()).thenReturn(new M2GavCalculator());

        Path dir = REPO.resolve("qdox/qdox/1.5");
        for (String sidecar : new String[] {".sha256", ".sha512", ".sha1", ".md5"}) {
            assertNull(
                    producer.getArtifactContext(
                            context, dir.resolve("qdox-1.5.jar" + sidecar).toFile()),
                    sidecar);
        }
        ArtifactContext jar =
                producer.getArtifactContext(context, dir.resolve("qdox-1.5.jar").toFile());
        assertNotNull(jar);
        assertEquals("qdox-1.5.jar", jar.getArtifactInfo().getFileName());
    }
}
