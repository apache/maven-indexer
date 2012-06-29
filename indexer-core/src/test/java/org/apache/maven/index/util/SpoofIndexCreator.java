package org.apache.maven.index.util;

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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.creator.AbstractIndexCreator;

public class SpoofIndexCreator
    extends AbstractIndexCreator
{
    protected SpoofIndexCreator( final String id, final List<String> creatorDependencies )
    {
        super( id, creatorDependencies );
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Collections.emptyList();
    }

    public void populateArtifactInfo( ArtifactContext artifactContext )
        throws IOException
    {
        // TODO Auto-generated method stub
    }

    public void updateDocument( ArtifactInfo artifactInfo, Document document )
    {
        // TODO Auto-generated method stub
    }

    public boolean updateArtifactInfo( Document document, ArtifactInfo artifactInfo )
    {
        // TODO Auto-generated method stub
        return false;
    }
}
