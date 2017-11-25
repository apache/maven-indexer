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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;

public class NexusIndexMultiReader
{
    private final List<IndexingContext> contexts;

    private List<IndexSearcher> searchers;

    public NexusIndexMultiReader( final Collection<IndexingContext> contexts )
    {
        this.contexts = Collections.unmodifiableList( new ArrayList<IndexingContext>( contexts ) );
    }

    public synchronized IndexReader acquire()
        throws IOException
    {
        if ( searchers != null )
        {
            release();
            throw new IllegalStateException( "acquire() called 2nd time without release() in between!" );
        }
        this.searchers = new ArrayList<IndexSearcher>();
        final ArrayList<IndexReader> contextReaders = new ArrayList<IndexReader>( contexts.size() );
        for ( IndexingContext ctx : contexts )
        {
            final IndexSearcher indexSearcher = ctx.acquireIndexSearcher();
            searchers.add( indexSearcher );
            contextReaders.add( indexSearcher.getIndexReader() );
        }
        return new MultiReader( contextReaders.toArray( new IndexReader[contextReaders.size()] ) );
    }

    public synchronized void release()
        throws IOException
    {
        if ( searchers != null )
        {
            final Iterator<IndexingContext> ic = contexts.iterator();
            final Iterator<IndexSearcher> is = searchers.iterator();

            while ( ic.hasNext() && is.hasNext() )
            {
                ic.next().releaseIndexSearcher( is.next() );
            }

            if ( ic.hasNext() || is.hasNext() )
            {
                throw new IllegalStateException( "Context and IndexSearcher mismatch: " + contexts + " vs "
                    + searchers );
            }
        }

        searchers = null;
    }

    /**
     * Watch out with this method, as it's use depends on (if you control it at all) was {@link #acquire()} method
     * invoked at all or not. Returns {@code null} if not, otherwise the list of acquired searchers. Not thread safe.
     * 
     * @return
     */
    public synchronized List<IndexSearcher> getAcquiredSearchers()
    {
        return searchers;
    }
}
