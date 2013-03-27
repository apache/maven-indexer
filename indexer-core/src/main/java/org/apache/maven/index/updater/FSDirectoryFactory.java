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
import java.io.IOException;

import org.apache.lucene.store.FSDirectory;

/**
 * FSDirectoryFactory allows host application choose specific FSDirectory implementation used during index update. This
 * is useful in some environments where default Lucene heuristics results in suboptimal choice of FSDirectory. For
 * example, MMapDirectory used by default on 64 bit Linux JDK results in heavy operating system swap with certain Linux
 * configuration and host application can choose NIOFSDirectory to avoid the problem.
 */
public interface FSDirectoryFactory
{
    /**
     * Default implementation that lets Lucene choose FSDirectory implementation.
     */
    public static final FSDirectoryFactory DEFAULT = new FSDirectoryFactory()
    {
        public FSDirectory open( File indexDir )
            throws IOException
        {
            return FSDirectory.open( indexDir );
        }
    };

    public FSDirectory open( File indexDir )
        throws IOException;
}
