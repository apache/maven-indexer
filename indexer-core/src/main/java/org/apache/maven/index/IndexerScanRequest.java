package org.apache.maven.index;

import java.io.File;

import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.util.StringUtils;

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

/**
 * Scanning request for initiating a {@link Indexer#scan(IndexerScanRequest)} operation of an {@link IndexingContext}.
 * 
 * @author cstamas
 * @since 5.1.0
 */
public class IndexerScanRequest
{
    private final IndexingContext context;

    private final ArtifactScanningListener artifactScanningListener;

    private final String startingPath;

    private final boolean update;

    /**
     * Constructor.
     * 
     * @param context
     * @param artifactScanningListener
     * @param startingPath
     * @param update
     */
    public IndexerScanRequest( final IndexingContext context, final ArtifactScanningListener artifactScanningListener,
                               final String startingPath, final boolean update )
    {
        this.context = context;
        this.artifactScanningListener = artifactScanningListener;
        this.startingPath = startingPath;
        this.update = update;
    }

    public IndexingContext getIndexingContext()
    {
        return context;
    }

    public ArtifactScanningListener getArtifactScanningListener()
    {
        return artifactScanningListener;
    }

    public String getStartingPath()
    {
        return startingPath;
    }

    public boolean isUpdate()
    {
        return update;
    }

    public File getStartingDirectory()
    {
        if ( StringUtils.isBlank( startingPath ) )
        {
            return getIndexingContext().getRepository();
        }
        else
        {
            return new File( getIndexingContext().getRepository(), startingPath );
        }
    }
}
