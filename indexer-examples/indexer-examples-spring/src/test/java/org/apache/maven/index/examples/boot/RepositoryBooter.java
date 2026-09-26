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
package org.apache.maven.index.examples.boot;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.annotation.PostConstruct;
import org.apache.maven.index.examples.indexing.RepositoryIndexManager;
import org.apache.maven.index.examples.indexing.RepositoryIndexer;
import org.apache.maven.index.examples.indexing.RepositoryIndexerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a dummy artifact repository creator.
 *
 * @author mtodorov
 */
@Component
public class RepositoryBooter {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryBooter.class);

    @Autowired
    private RepositoryIndexManager repositoryIndexManager;

    @Autowired
    private RepositoryIndexerFactory repositoryIndexerFactory;

    public RepositoryBooter() {
        // no op
    }

    @PostConstruct
    public void initialize() throws IOException {
        Path repositoriesBaseDir = Path.of("target/repositories");

        if (!lockExists(repositoriesBaseDir)) {
            createLockFile(repositoriesBaseDir);
            initializeRepositories(repositoriesBaseDir);
        } else {
            logger.error("Failed to initialize the repositories. Another JVM may have already done this.");
        }

        logger.debug("Initialized repositories.");
    }

    private void createLockFile(Path repositoriesRootDir) throws IOException {
        final Path lockFile = repositoriesRootDir.resolve("repositories.lock");
        Files.createDirectories(lockFile.getParent());
        try {
            Files.createFile(lockFile);
        } catch (FileAlreadyExistsException e) {
            // already created concurrently, nothing to do
        }
    }

    private boolean lockExists(Path repositoriesRootDir) throws IOException {
        Path lockFile = repositoriesRootDir.resolve("repositories.lock");

        return Files.exists(lockFile);
    }

    private void initializeRepositories(Path repositoriesBaseDir) throws IOException {
        initializeRepository(repositoriesBaseDir, "releases");
        initializeRepository(repositoriesBaseDir, "snapshots");
    }

    private void initializeRepository(Path repositoriesBaseDir, String repositoryName) throws IOException {
        createRepositoryStructure(repositoriesBaseDir.toAbsolutePath(), repositoryName);

        initializeRepositoryIndex(repositoriesBaseDir.toAbsolutePath().resolve(repositoryName), repositoryName);
    }

    public void createRepositoryStructure(Path repositoriesBaseDir, String repositoryName) throws IOException {
        Files.createDirectories(repositoriesBaseDir.resolve(repositoryName));
        Files.createDirectories(repositoriesBaseDir.resolve(repositoryName).resolve(".index"));

        logger.debug("Created directory structure for repository '"
                + repositoriesBaseDir.toAbsolutePath().resolve(repositoryName) + "'.");
    }

    private void initializeRepositoryIndex(Path repositoryBasedir, String repositoryId) throws IOException {
        final Path indexDir = repositoryBasedir.resolve(".index");

        RepositoryIndexer repositoryIndexer = repositoryIndexerFactory.createRepositoryIndexer(
                repositoryId, repositoryBasedir.toFile(), indexDir.toFile());

        repositoryIndexManager.addRepositoryIndex(repositoryId, repositoryIndexer);
    }

    public RepositoryIndexManager getRepositoryIndexManager() {
        return repositoryIndexManager;
    }

    public void setRepositoryIndexManager(RepositoryIndexManager repositoryIndexManager) {
        this.repositoryIndexManager = repositoryIndexManager;
    }
}
