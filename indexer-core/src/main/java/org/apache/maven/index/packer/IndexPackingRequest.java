package org.apache.maven.index.packer;

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
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.index.context.IndexingContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An index packing request.
 */
public class IndexPackingRequest
{
    public static final int MAX_CHUNKS = 30;

    private final IndexingContext context;

    private final IndexReader indexReader;

    private final File targetDir;

    private boolean createIncrementalChunks;

    private boolean createChecksumFiles;

    private int maxIndexChunks;

    private boolean useTargetProperties;

    private Collection<IndexFormat> formats;

    public IndexPackingRequest( final IndexingContext context, final IndexReader indexReader, final File targetDir )
    {
        this.context = checkNotNull(context);

        this.indexReader = checkNotNull( indexReader );

        this.targetDir = checkNotNull( targetDir );

        this.createIncrementalChunks = true;

        this.createChecksumFiles = false;

        this.maxIndexChunks = MAX_CHUNKS;

        this.useTargetProperties = false;

        this.formats = Arrays.asList( IndexFormat.FORMAT_V1 );
    }

    public IndexingContext getContext()
    {
        return context;
    }

    public IndexReader getIndexReader() { return indexReader; }

    /**
     * Sets index formats to be created
     */
    public void setFormats( Collection<IndexFormat> formats )
    {
        this.formats = formats;
    }

    /**
     * Returns index formats to be created.
     */
    public Collection<IndexFormat> getFormats()
    {
        return formats;
    }

    public File getTargetDir()
    {
        return targetDir;
    }

    public boolean isCreateIncrementalChunks()
    {
        return createIncrementalChunks;
    }

    public void setCreateIncrementalChunks( boolean createIncrementalChunks )
    {
        this.createIncrementalChunks = createIncrementalChunks;
    }

    public boolean isCreateChecksumFiles()
    {
        return createChecksumFiles;
    }

    public void setCreateChecksumFiles( boolean createChecksumFiles )
    {
        this.createChecksumFiles = createChecksumFiles;
    }

    public int getMaxIndexChunks()
    {
        return maxIndexChunks;
    }

    public void setMaxIndexChunks( int maxIndexChunks )
    {
        this.maxIndexChunks = maxIndexChunks;
    }

    public boolean isUseTargetProperties()
    {
        return useTargetProperties;
    }

    public void setUseTargetProperties( boolean useTargetProperties )
    {
        this.useTargetProperties = useTargetProperties;
    }

    /**
     * Index format enumeration.
     */
    public static enum IndexFormat
    {
        FORMAT_V1;
    }
}
