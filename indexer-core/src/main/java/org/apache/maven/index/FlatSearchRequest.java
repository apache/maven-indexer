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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * A flat search request.
 * 
 * @see Indexer#searchFlat(FlatSearchRequest)
 */
public class FlatSearchRequest
    extends AbstractSearchRequest
{
    private Comparator<ArtifactInfo> artifactInfoComparator;

    public FlatSearchRequest( Query query )
    {
        this( query, ArtifactInfo.VERSION_COMPARATOR );
    }

    public FlatSearchRequest( Query query, Comparator<ArtifactInfo> artifactInfoComparator )
    {
        this( query, artifactInfoComparator, null );
    }

    public FlatSearchRequest( Query query, IndexingContext context )
    {
        this( query, ArtifactInfo.VERSION_COMPARATOR, context );
    }

    public FlatSearchRequest( Query query, Comparator<ArtifactInfo> artifactInfoComparator, IndexingContext context )
    {
        super( query, context != null ? Arrays.asList( new IndexingContext[] { context } ) : null );

        this.artifactInfoComparator = artifactInfoComparator;
    }

    public Comparator<ArtifactInfo> getArtifactInfoComparator()
    {
        return artifactInfoComparator;
    }

    public void setArtifactInfoComparator( Comparator<ArtifactInfo> artifactInfoComparator )
    {
        this.artifactInfoComparator = artifactInfoComparator;
    }
}
