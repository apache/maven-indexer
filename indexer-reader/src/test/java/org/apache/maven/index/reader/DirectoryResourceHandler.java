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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * A trivial {@link Path} directory handler that does not perform any locking or extra bits, and just serves up files
 * by name from specified existing directory.
 */
public class DirectoryResourceHandler implements WritableResourceHandler {
    private final Path rootDirectory;

    public DirectoryResourceHandler(final Path rootDirectory) {
        if (rootDirectory == null) {
            throw new NullPointerException("null rootDirectory");
        }
        if (!Files.isDirectory(rootDirectory)) {
            throw new IllegalArgumentException("rootDirectory exists and is not a directory");
        }
        this.rootDirectory = rootDirectory;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public WritableResource locate(final String name) {
        return new PathResource(rootDirectory.resolve(name));
    }

    private static class PathResource implements WritableResource {
        private final Path file;

        private PathResource(final Path file) {
            this.file = file;
        }

        @Override
        public InputStream read() throws IOException {
            try {
                return new BufferedInputStream(Files.newInputStream(file));
            } catch (NoSuchFileException e) {
                return null;
            }
        }

        @Override
        public OutputStream write() throws IOException {
            return new BufferedOutputStream(Files.newOutputStream(file));
        }
    }
}
