/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index.updater;

import java.io.File;

import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.fs.Locker;

/**
 * @author Eugene Kuleshov
 */
public class IndexUpdateRequest
{
    private final IndexingContext context;

    private ResourceFetcher resourceFetcher;

    private DocumentFilter documentFilter;

    private boolean forceFullUpdate;

    private File localIndexCacheDir;

    private Locker locker;

    private boolean offline;

    private boolean cacheOnly;

    public IndexUpdateRequest( IndexingContext context )
    {
        this.context = context;
        this.forceFullUpdate = false;
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

    /**
     * If null, the default wagon manager will be used, incorporating the ProxyInfo and TransferListener if supplied
     * 
     * @param resourceFetcher
     */
    public void setResourceFetcher( ResourceFetcher resourceFetcher )
    {
        this.resourceFetcher = resourceFetcher;
    }

    public void setForceFullUpdate( boolean forceFullUpdate )
    {
        this.forceFullUpdate = forceFullUpdate;
    }

    public boolean isForceFullUpdate()
    {
        return forceFullUpdate;
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
}
