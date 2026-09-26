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
package org.apache.maven.index.creator;

import java.nio.file.Path;

import org.apache.maven.index.AbstractTestSupport;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alin Dreghiciu
 */
class JarFileContentsIndexCreatorTest extends AbstractTestSupport {
    protected IndexCreator indexCreator;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        indexCreator = this.lookup(IndexCreator.class, "jarContent");
    }

    @Test
    void nexus_2318_index_jar_with_classes() throws Exception {
        Path artifact = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.jar");

        Path pom = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.pom");

        ArtifactInfo artifactInfo = new ArtifactInfo("test", "aopalliance", "aopalliance", "1.0", null, "jar");

        ArtifactContext artifactContext =
                new ArtifactContext(pom.toFile(), artifact.toFile(), null, artifactInfo, null);

        indexCreator.populateArtifactInfo(artifactContext);

        assertNotNull(artifactContext.getArtifactInfo().getClassNames(), "Classes should not be null");
    }

    @Test
    void nexus_2318_index_zip_with_classes() throws Exception {
        Path artifact = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.zip");

        Path pom = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.pom");

        ArtifactInfo artifactInfo = new ArtifactInfo("test", "aopalliance", "aopalliance", "1.0", null, "zip");

        ArtifactContext artifactContext =
                new ArtifactContext(pom.toFile(), artifact.toFile(), null, artifactInfo, null);

        indexCreator.populateArtifactInfo(artifactContext);

        assertNotNull(artifactContext.getArtifactInfo().getClassNames(), "Classes should not be null");

        String[] classNames = artifactContext.getArtifactInfo().getClassNames().split("\n");

        assertEquals(9, classNames.length, "Unexpected classes length");

        assertEquals("/org/aopalliance/aop/Advice", classNames[0], "Advice class was expected");

        assertEquals("/org/aopalliance/aop/AspectException", classNames[1], "AspectException class was expected");

        assertEquals(
                "/org/aopalliance/intercept/ConstructorInterceptor",
                classNames[2],
                "ConstructorInterceptor class was expected");

        assertEquals(
                "/org/aopalliance/intercept/ConstructorInvocation",
                classNames[3],
                "ConstructorInvocation class was expected");

        assertEquals("/org/aopalliance/intercept/Interceptor", classNames[4], "Interceptor class was expected");

        assertEquals("/org/aopalliance/intercept/Invocation", classNames[5], "Invocation class was expected");

        assertEquals("/org/aopalliance/intercept/Joinpoint", classNames[6], "Joinpoint class was expected");

        assertEquals(
                "/org/aopalliance/intercept/MethodInterceptor", classNames[7], "MethodInterceptor class was expected");

        assertEquals(
                "/org/aopalliance/intercept/MethodInvocation", classNames[8], "MethodInvocation class was expected");
    }

    @Test
    void nexus_2318_index_jar_with_sources() throws Exception {
        Path artifact = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0-sources.jar");

        Path pom = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.pom");

        ArtifactInfo artifactInfo = new ArtifactInfo("test", "aopalliance", "aopalliance", "1.0", null, "jar");

        ArtifactContext artifactContext =
                new ArtifactContext(pom.toFile(), artifact.toFile(), null, artifactInfo, null);

        indexCreator.populateArtifactInfo(artifactContext);

        assertNull(artifactContext.getArtifactInfo().getClassNames(), "Classes should be null");
    }

    @Test
    void nexus_2318_index_zip_with_sources() throws Exception {
        Path artifact = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0-sources.zip");

        Path pom = getTestPath("src/test/nexus-2318/aopalliance/aopalliance/1.0/aopalliance-1.0.pom");

        ArtifactInfo artifactInfo = new ArtifactInfo("test", "aopalliance", "aopalliance", "1.0", null, "zip");

        ArtifactContext artifactContext =
                new ArtifactContext(pom.toFile(), artifact.toFile(), null, artifactInfo, null);

        indexCreator.populateArtifactInfo(artifactContext);

        assertNull(artifactContext.getArtifactInfo().getClassNames(), "Classes should be null");
    }

    @Test
    void mindexer35ScanWar() throws Exception {
        Path artifact = getTestPath(
                "src/test/mindexer-35/org/apache/maven/indexer/test/sample-war/1.0-SNAPSHOT/sample-war-1.0-SNAPSHOT.war");

        Path pom = getTestPath(
                "src/test/mindexer-35/org/apache/maven/indexer/test/sample-war/1.0-SNAPSHOT/sample-war-1.0-SNAPSHOT.pom");

        ArtifactInfo artifactInfo =
                new ArtifactInfo("test", "org.apache.maven.indexer.test", "sample-war", "1.0-SNAPSHOT", null, "war");

        ArtifactContext artifactContext =
                new ArtifactContext(pom.toFile(), artifact.toFile(), null, artifactInfo, null);

        indexCreator.populateArtifactInfo(artifactContext);

        assertTrue(
                artifactContext.getArtifactInfo().getClassNames().contains("WebappClass"),
                "Classes should contain WebappClass");
        assertEquals(
                "/org/apache/maven/indexer/samples/webapp/WebappClass",
                artifactContext.getArtifactInfo().getClassNames(),
                "WebappClass should have proper package");
    }
}
