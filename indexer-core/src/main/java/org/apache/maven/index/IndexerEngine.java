package org.apache.maven.index;

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

import java.io.IOException;

import org.apache.maven.index.context.IndexingContext;

/**
 * An indexer engine used to index, update and remote artifacts to the indexing context.
 */
public interface IndexerEngine
{
    /**
     * Add new artifact to the index
     */
    void index( IndexingContext context, ArtifactContext ac )
        throws IOException;

    /**
     * Replace data for a previously indexed artifact
     */
    void update( IndexingContext context, ArtifactContext ac )
        throws IOException;

    /**
     * Remove artifact to the index
     */
    void remove( IndexingContext context, ArtifactContext ac )
        throws IOException;

}
