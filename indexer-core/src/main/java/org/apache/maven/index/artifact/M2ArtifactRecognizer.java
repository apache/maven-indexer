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
package org.apache.maven.index.artifact;

/**
 * Utility methods for basic "detection" of artifact kind in M2 repository.
 */
public class M2ArtifactRecognizer {
    private static final String[] CHECKSUM_SUFFIXES = {".sha1", ".md5", ".sha256", ".sha512"};

    private static boolean endsWithChecksumOf(String path, String suffix) {
        for (String checksum : CHECKSUM_SUFFIXES) {
            if (path.endsWith(suffix + checksum)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is this item M2 Checksum?
     */
    public static boolean isChecksum(String path) {
        return endsWithChecksumOf(path, "");
    }

    /**
     * Is this item M2 POM?
     */
    public static boolean isPom(String path) {
        return path.endsWith(".pom") || endsWithChecksumOf(path, ".pom");
    }

    /**
     * Is this item M2 Snapshot?
     *
     * @param path
     * @return
     */
    public static boolean isSnapshot(String path) {
        return path.contains("SNAPSHOT");
    }

    /**
     * Is this item M2 metadata?
     */
    public static boolean isMetadata(String path) {
        return path.endsWith("maven-metadata.xml") || endsWithChecksumOf(path, "maven-metadata.xml");
    }

    public static boolean isSignature(String path) {
        return path.endsWith(".asc");
    }
}
