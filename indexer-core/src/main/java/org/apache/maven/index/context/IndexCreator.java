package org.apache.maven.index.context;

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

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * An index creator is responsible for storing and reading data to and from Lucene index.
 * 
 * @author Jason van Zyl
 * @see org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator
 * @see org.apache.maven.index.creator.JarFileContentsIndexCreator
 */
public interface IndexCreator
{
    /**
     * Returns IndexCreator ID, that has to be unique across all existing creators.
     * 
     * @return
     */
    String getId();

    /**
     * Returns list of IndexCreator IDs that this creator depends on. Needed to perform a topological sort on
     * IndexCreators to guarantee proper ordering of them, as some IndexCreators might rely on informations already
     * extracted by some other IndexCreator.
     * 
     * @return
     */
    List<String> getCreatorDependencies();

    /**
     * Returns the indexer fields that this IndexCreator introduces to index.
     * 
     * @return
     */
    Collection<IndexerField> getIndexerFields();

    /**
     * Populate an <code>ArtifactContext</code> with information about corresponding artifact.
     */
    void populateArtifactInfo( ArtifactContext artifactContext )
        throws IOException;

    /**
     * Update Lucene <code>Document</code> from a given <code>ArtifactInfo</code>.
     */
    void updateDocument( ArtifactInfo artifactInfo, Document document );

    /**
     * Update an <code>ArtifactInfo</code> from given Lucene <code>Document</code>.
     * 
     * @return true is artifact info has been updated
     */
    boolean updateArtifactInfo( Document document, ArtifactInfo artifactInfo );

}
