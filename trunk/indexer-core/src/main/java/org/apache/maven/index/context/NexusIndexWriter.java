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
package org.apache.maven.index.context;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * An extension of <a
 * href="http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/index/IndexWriter.html">Lucene IndexWriter</a>
 * to allow to track if writer is closed
 */
public class NexusIndexWriter
    extends IndexWriter
{
    private boolean closed;

    public NexusIndexWriter( final Directory directory, final Analyzer analyzer, boolean create )
        throws CorruptIndexException, LockObtainFailedException, IOException
    {
        this( directory, analyzer, create, false /* autoCommit */);
    }

    public NexusIndexWriter( final Directory directory, final Analyzer analyzer, boolean create, boolean autoCommit )
        throws CorruptIndexException, LockObtainFailedException, IOException
    {
        super( directory, autoCommit, analyzer, create );

        this.closed = false;
    }

    @Override
    public void close()
        throws CorruptIndexException, IOException
    {
        super.close();

        this.closed = true;
    }

    public boolean isClosed()
    {
        return closed;
    }

    public boolean hasUncommittedChanges()
    {
        try
        {
            Field pendingCommit = IndexWriter.class.getDeclaredField( "pendingCommit" );

            pendingCommit.setAccessible( true );

            return pendingCommit.get( this ) != null;
        }
        catch ( Exception x )
        {
            if ( x instanceof RuntimeException )
            {
                throw (RuntimeException) x;
            }
            else
            {
                throw new RuntimeException( "Could not access the \"IndexWriter.pendingCommit\" field!", x );
            }
        }
    }
}
