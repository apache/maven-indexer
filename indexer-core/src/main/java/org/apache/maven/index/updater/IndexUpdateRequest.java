package org.apache.maven.index.updater;

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

import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.fs.Locker;

/**
 * Request to update indexes.
 * 
 * @author Eugene Kuleshov
 * @author cstamas
 */
public class IndexUpdateRequest
{
    private final IndexingContext context;

    private final ResourceFetcher resourceFetcher;

    private DocumentFilter documentFilter;

    private boolean forceFullUpdate;
    
    private boolean incrementalOnly;

    private File localIndexCacheDir;

    private Locker locker;

    private boolean offline;

    private boolean cacheOnly;

    private FSDirectoryFactory directoryFactory;

    public IndexUpdateRequest( final IndexingContext context, final ResourceFetcher resourceFetcher )
    {
        assert context != null : "Context to be updated cannot be null!";
        assert resourceFetcher != null : "ResourceFetcher has to be provided!";

        this.context = context;
        this.resourceFetcher = resourceFetcher;
        this.forceFullUpdate = false;
        this.incrementalOnly = false;
    }

    public IndexingContext getIndexingContext()
    {
        return context;
    }

    public ResourceFetcher getResourceFetcher()
    {
        return resourceFetcher;
    }

    public DocumentFilter getDocumentFilter()
    {
        return documentFilter;
    }

    public void setDocumentFilter( DocumentFilter documentFilter )
    {
        this.documentFilter = documentFilter;
    }

    public void setForceFullUpdate( boolean forceFullUpdate )
    {
        this.forceFullUpdate = forceFullUpdate;
    }

    public boolean isForceFullUpdate()
    {
        return forceFullUpdate;
    }
    
    public boolean isIncrementalOnly()
    {
        return incrementalOnly;
    }

    public void setIncrementalOnly(boolean incrementalOnly)
    {
        this.incrementalOnly = incrementalOnly;
    }

    public File getLocalIndexCacheDir()
    {
        return localIndexCacheDir;
    }

    public void setLocalIndexCacheDir( File dir )
    {
        this.localIndexCacheDir = dir;
    }

    public Locker getLocker()
    {
        return locker;
    }

    public void setLocker( Locker locker )
    {
        this.locker = locker;
    }

    public void setOffline( boolean offline )
    {
        this.offline = offline;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public void setCacheOnly( boolean cacheOnly )
    {
        this.cacheOnly = cacheOnly;
    }

    public boolean isCacheOnly()
    {
        return cacheOnly;
    }

    public void setFSDirectoryFactory( FSDirectoryFactory factory )
    {
        this.directoryFactory = factory;
    }

    public FSDirectoryFactory getFSDirectoryFactory()
    {
        return directoryFactory != null ? directoryFactory : FSDirectoryFactory.DEFAULT;
    }
}