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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MavenArchetypeArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MavenPluginArtifactInfoIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class AbstractIndexCreatorHelper extends AbstractTestSupport {
    public List<IndexCreator> DEFAULT_CREATORS;

    public List<IndexCreator> FULL_CREATORS;

    public List<IndexCreator> MIN_CREATORS;

    Random rand = new Random();

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        DEFAULT_CREATORS = new ArrayList<>();
        FULL_CREATORS = new ArrayList<>();
        MIN_CREATORS = new ArrayList<>();

        IndexCreator min = lookup(IndexCreator.class, MinimalArtifactInfoIndexCreator.ID);
        IndexCreator mavenPlugin = lookup(IndexCreator.class, MavenPluginArtifactInfoIndexCreator.ID);
        IndexCreator mavenArchetype = lookup(IndexCreator.class, MavenArchetypeArtifactInfoIndexCreator.ID);
        IndexCreator jar = lookup(IndexCreator.class, JarFileContentsIndexCreator.ID);

        MIN_CREATORS.add(min);

        DEFAULT_CREATORS.add(min);
        DEFAULT_CREATORS.add(mavenPlugin);
        DEFAULT_CREATORS.add(mavenArchetype);

        FULL_CREATORS.add(min);
        FULL_CREATORS.add(mavenPlugin);
        FULL_CREATORS.add(mavenArchetype);
        FULL_CREATORS.add(jar);
    }

    protected void deleteDirectory(Path dir) throws IOException {
        FileUtils.deleteDirectory(dir.toFile());
    }

    protected Path getDirectory(String name) {
        // pick random output location
        Path outputFolder = getTestPath("target/tests/" + name + "-" + rand.nextLong());
        try {
            Files.deleteIfExists(outputFolder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertFalse(Files.exists(outputFolder));
        return outputFolder;
    }

    @Test
    protected void directory() throws Exception {
        Path dir = this.getDirectory("foo");
        assert (dir.toAbsolutePath().toString().contains("foo"));
        this.deleteDirectory(dir);
        assertFalse(Files.exists(dir));

        Path dir2 = this.getDirectory("foo");
        assertNotEquals(
                dir.toAbsolutePath().normalize(), dir2.toAbsolutePath().normalize(), "Directories aren't unique");
    }
}
