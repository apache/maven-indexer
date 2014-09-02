package org.apache.maven.indexer.examples.services;

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

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.maven.indexer.examples.indexing.SearchRequest;
import org.apache.maven.indexer.examples.indexing.SearchResults;

/**
 * A simple indexing and search service.
 *
 * @author mtodorov
 */
public interface ArtifactIndexingService
{

    void addToIndex( String repositoryId,
                     File artifactFile,
                     String groupId,
                     String artifactId,
                     String version,
                     String extension,
                     String classifier )
            throws IOException;

    void deleteFromIndex( String repositoryId,
                          String groupId,
                          String artifactId,
                          String version,
                          String extension,
                          String classifier )
            throws IOException;

    SearchResults search( SearchRequest searchRequest )
            throws IOException, ParseException;

    boolean contains( SearchRequest searchRequest )
            throws IOException, ParseException;

}
