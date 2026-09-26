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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenArtifactRecognizerTest {

    @Test
    void isPom() {
        assertTrue(M2ArtifactRecognizer.isPom("aaa.pom"));
        assertTrue(M2ArtifactRecognizer.isPom("zxc-1-2-3.pom"));
        assertFalse(M2ArtifactRecognizer.isPom("aaa.jar"));
        assertFalse(M2ArtifactRecognizer.isPom("aaa.pom-a"));
    }

    @Test
    void isSnapshot1() {
        // NEXUS-3148
        assertTrue(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0SNAPSHOT/aid-1.0SNAPSHOT.jar"));

        assertTrue(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.jar"));
        assertTrue(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.0-SNAPSHOT.pom"));
        assertTrue(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0-SNAPSHOT/aid-1.2.3-.pom"));
        assertFalse(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/xsd-SNAPsHOT.jar"));
        assertFalse(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/xsd-SNAPHOT.pom"));
        assertFalse(M2ArtifactRecognizer.isSnapshot("/org/somewhere/aid/1.0/a/b/c/xsd-1.2.3NAPSHOT.pom"));
        assertFalse(M2ArtifactRecognizer.isSnapshot("/javax/mail/mail/1.4/mail-1.4.jar"));
    }

    @Test
    void isSnapshot2() {
        assertTrue(
                M2ArtifactRecognizer.isSnapshot(
                        "/org/somewhere/appassembler-maven-plugin/1.0-SNAPSHOT/appassembler-maven-plugin-1.0-20060714.142547-1.pom"));
        assertFalse(M2ArtifactRecognizer.isSnapshot(
                "/org/somewhere/appassembler-maven-plugin/1.0/appassembler-maven-plugin-1.0-20060714.142547-1.pom"));
    }

    @Test
    void isMetadata() {
        assertTrue(M2ArtifactRecognizer.isMetadata("maven-metadata.xml"));
        assertFalse(M2ArtifactRecognizer.isMetadata("aven-metadata.xml"));
        assertFalse(M2ArtifactRecognizer.isMetadata("/javax/mail/mail/1.4/mail-1.4.jar"));
    }

    @Test
    public void testChecksums() {
        for (String checksum : new String[] {".sha1", ".md5", ".sha256", ".sha512"}) {
            assertEquals(true, M2ArtifactRecognizer.isChecksum("aaa.jar" + checksum));
            assertEquals(true, M2ArtifactRecognizer.isPom("aaa.pom" + checksum));
            assertEquals(true, M2ArtifactRecognizer.isMetadata("maven-metadata.xml" + checksum));
        }
        assertEquals(false, M2ArtifactRecognizer.isChecksum("aaa.jar"));
        assertEquals(false, M2ArtifactRecognizer.isChecksum("aaa.jar.asc"));
        assertEquals(false, M2ArtifactRecognizer.isPom("aaa.jar.sha256"));
    }
}
