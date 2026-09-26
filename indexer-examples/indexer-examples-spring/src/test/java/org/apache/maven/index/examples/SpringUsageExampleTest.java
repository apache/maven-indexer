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
package org.apache.maven.index.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.examples.indexing.SearchRequest;
import org.apache.maven.index.examples.indexing.SearchResults;
import org.apache.maven.index.examples.services.ArtifactIndexingService;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mtodorov
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/META-INF/spring/*-context.xml", "classpath*:/META-INF/spring/*-context.xml"})
public class SpringUsageExampleTest {

    public static final Path REPOSITORIES_BASEDIR = Path.of("target/repositories");

    @Autowired
    private ArtifactIndexingService artifactIndexingService;

    private SimpleArtifactGenerator generator = new SimpleArtifactGenerator();

    @BeforeEach
    void setUp() throws Exception {
        if (!Files.exists(
                REPOSITORIES_BASEDIR.resolve("releases/org/apache/maven/indexer/examples/indexer-examples-spring"))) {
            Files.createDirectories(REPOSITORIES_BASEDIR);

            // Generate some valid test artifacts:
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.0",
                    "jar",
                    null);
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.1",
                    "jar",
                    null);
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.2",
                    "jar",
                    null);
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.3",
                    "jar",
                    null);
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.3.1",
                    "jar",
                    null);
            generateArtifactAndAddToIndex(
                    REPOSITORIES_BASEDIR.resolve("releases"),
                    "releases",
                    "org.apache.maven.indexer.examples",
                    "indexer-examples-spring",
                    "1.4",
                    "jar",
                    null);
        }
    }

    @Test
    void addAndDelete() throws Exception {
        // Create a search request matching GAV "org.apache.maven.indexer.examples:indexer-examples-spring:1.4"
        SearchRequest request =
                new SearchRequest("releases", "+g:org.apache.maven.indexer.examples +a:indexer-examples-spring +v:1.4");

        assertTrue(artifactIndexingService.contains(request), "Couldn't find existing artifact!");

        // Delete the artifact from the index:
        artifactIndexingService.deleteFromIndex(
                "releases", "org.apache.maven.indexer.examples", "indexer-examples-spring", "1.4", "jar", null);

        assertFalse(artifactIndexingService.contains(request), "Failed to remove artifact from index!");
    }

    @Test
    void search() throws Exception {
        // Create a search request matching GAV "org.apache.maven.indexer.examples:indexer-examples-spring:1.3"
        SearchRequest request = new SearchRequest(
                "releases", "+g:org.apache.maven.indexer.examples +a:indexer-examples-spring +v:1.3*");

        assertTrue(artifactIndexingService.contains(request), "Couldn't find existing artifact!");

        final SearchResults searchResults = artifactIndexingService.search(request);

        assertFalse(searchResults.getResults().isEmpty());

        for (String repositoryId : searchResults.getResults().keySet()) {
            System.out.println("Matches in repository " + repositoryId + ":");

            final Collection<ArtifactInfo> artifactInfos =
                    searchResults.getResults().get(repositoryId);
            for (ArtifactInfo artifactInfo : artifactInfos) {
                System.out.println("   " + artifactInfo.getGroupId()
                        + ":" + artifactInfo.getArtifactId()
                        + ":" + artifactInfo.getVersion()
                        + ":" + artifactInfo.getPackaging()
                        + ":" + artifactInfo.getClassifier());
            }
        }
    }

    /**
     * This method creates some valid dummy artifacts in order to be able to add them to index.
     *
     * @param repositoryBasedir
     * @param repositoryId
     * @param groupId
     * @param artifactId
     * @param version
     * @param extension
     * @param classifier
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws XmlPullParserException
     */
    private Path generateArtifactAndAddToIndex(
            Path repositoryBasedir,
            String repositoryId,
            String groupId,
            String artifactId,
            String version,
            String extension,
            String classifier)
            throws IOException, NoSuchAlgorithmException, XmlPullParserException {
        final Path artifactFile =
                generator.generateArtifact(repositoryBasedir, groupId, artifactId, version, classifier, extension);

        // Add the artifact to the index:
        artifactIndexingService.addToIndex(
                repositoryId, artifactFile.toFile(), groupId, artifactId, version, extension, classifier);

        return artifactFile;
    }
}
