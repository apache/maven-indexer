package org.apache.maven.indexer.examples;

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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.indexer.examples.indexing.SearchRequest;
import org.apache.maven.indexer.examples.indexing.SearchResults;
import org.apache.maven.indexer.examples.services.ArtifactIndexingService;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/*-context.xml",
                                    "classpath*:/META-INF/spring/*-context.xml" })
public class SpringUsageExampleTest
{

    public static final File REPOSITORIES_BASEDIR = new File( "target/repositories" );

    @Autowired
    private ArtifactIndexingService artifactIndexingService;

    private SimpleArtifactGenerator generator = new SimpleArtifactGenerator();


    @Before
    public void setUp()
            throws Exception
    {
        if ( !new File( REPOSITORIES_BASEDIR,
                        "releases/org/apache/maven/indexer/examples/indexer-examples-spring" ).exists() )
        {
            //noinspection ResultOfMethodCallIgnored
            REPOSITORIES_BASEDIR.mkdirs();

            // Generate some valid test artifacts:
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.0",
                                           "jar",
                                           null );
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.1",
                                           "jar",
                                           null );
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.2",
                                           "jar",
                                           null );
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.3",
                                           "jar",
                                           null );
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.3.1",
                                           "jar",
                                           null );
            generateArtifactAndAddToIndex( new File( REPOSITORIES_BASEDIR, "releases" ).getAbsolutePath(),
                                           "releases",
                                           "org.apache.maven.indexer.examples",
                                           "indexer-examples-spring",
                                           "1.4",
                                           "jar",
                                           null );
        }
    }

    @Test
    public void testAddAndDelete()
            throws IOException, ParseException, NoSuchAlgorithmException, XmlPullParserException
    {
        // Create a search request matching GAV "org.apache.maven.indexer.examples:indexer-examples-spring:1.4"
        SearchRequest request = new SearchRequest( "releases",
                                                   "+g:org.apache.maven.indexer.examples +a:indexer-examples-spring +v:1.4" );

        assertTrue( "Couldn't find existing artifact!", artifactIndexingService.contains( request ) );

        // Delete the artifact from the index:
        artifactIndexingService.deleteFromIndex( "releases",
                                                 "org.apache.maven.indexer.examples",
                                                 "indexer-examples-spring",
                                                 "1.4",
                                                 "jar",
                                                 null );

        assertFalse( "Failed to remove artifact from index!", artifactIndexingService.contains( request ) );
    }

    @Test
    public void testSearch()
            throws IOException, ParseException
    {
        // Create a search request matching GAV "org.apache.maven.indexer.examples:indexer-examples-spring:1.3"
        SearchRequest request = new SearchRequest( "releases",
                                                   "+g:org.apache.maven.indexer.examples +a:indexer-examples-spring +v:1.3*" );

        assertTrue( "Couldn't find existing artifact!", artifactIndexingService.contains( request ) );

        final SearchResults searchResults = artifactIndexingService.search( request );

        assertFalse( searchResults.getResults().isEmpty() );

        for ( String repositoryId : searchResults.getResults().keySet() )
        {
            System.out.println( "Matches in repository " + repositoryId + ":" );

            final Collection<ArtifactInfo> artifactInfos = searchResults.getResults().get( repositoryId );
            for ( ArtifactInfo artifactInfo : artifactInfos )
            {
                System.out.println( "   " +
                                    artifactInfo.getGroupId() + ":" +
                                    artifactInfo.getArtifactId() + ":" +
                                    artifactInfo.getVersion() + ":" +
                                    artifactInfo.getPackaging() + ":" +
                                    artifactInfo.getClassifier() );
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
    private File generateArtifactAndAddToIndex( String repositoryBasedir,
                                                String repositoryId,
                                                String groupId,
                                                String artifactId,
                                                String version,
                                                String extension,
                                                String classifier )
            throws IOException, NoSuchAlgorithmException, XmlPullParserException
    {
        final File artifactFile = generator.generateArtifact( repositoryBasedir,
                                                              groupId,
                                                              artifactId,
                                                              version,
                                                              classifier,
                                                              extension );

        // Add the artifact to the index:
        artifactIndexingService.addToIndex( repositoryId,
                                            artifactFile,
                                            groupId,
                                            artifactId,
                                            version,
                                            extension,
                                            classifier );

        return artifactFile;
    }

}
