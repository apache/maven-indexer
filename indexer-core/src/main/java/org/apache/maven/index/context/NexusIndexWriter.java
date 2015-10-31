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

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

/**
 * An extension of <a
 * href="http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/index/IndexWriter.html">Lucene IndexWriter</a>
 * to allow to track if writer is closed
 */
public class NexusIndexWriter
    extends IndexWriter
{
    public interface IndexWriterConfigFactory {
        IndexWriterConfig create(Analyzer analyzer);
    }

    public static IndexWriterConfigFactory CONFIG_FACTORY = new IndexWriterConfigFactory() {
        public IndexWriterConfig create(final Analyzer analyzer) {
            IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_36, analyzer );
            config.setRAMBufferSizeMB( 2.0 ); // old default
            config.setMergeScheduler( new SerialMergeScheduler() ); // merging serially
            return config;
        }
    };

    public NexusIndexWriter( final Directory directory, final Analyzer analyzer, boolean create )
        throws CorruptIndexException, LockObtainFailedException, IOException
    {
        super(directory, CONFIG_FACTORY.create(analyzer).setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND));
    }

    public NexusIndexWriter( final Directory directory, final IndexWriterConfig config )
        throws CorruptIndexException, LockObtainFailedException, IOException
    {
        super( directory, config );
    }

    // ==

    public static IndexWriterConfig defaultConfig()
    {
        return CONFIG_FACTORY.create(new NexusAnalyzer());
    }
}
