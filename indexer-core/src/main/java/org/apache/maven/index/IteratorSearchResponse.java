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

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.search.Query;

/**
 * A Search Response for the "iterator-like" search request. The totalHitsCount reports <em>total</em> hits found on
 * index, even if the set of ArtifactInfos are usually limited! On the flipside, the hitsCount is actually unknown,
 * since this instance performs filtering on the fly, hence it does not know how many hits it will return ahead of time.
 * 
 * @author cstamas
 */
public class IteratorSearchResponse
    extends AbstractSearchResponse
    implements Iterable<ArtifactInfo>, Closeable
{
    private final IteratorResultSet results;

    public IteratorSearchResponse( Query query, int totalHits, IteratorResultSet results )
    {
        super( query, totalHits, -1 );

        this.results = results;
    }

    public IteratorResultSet getResults()
    {
        return results;
    }

    public IteratorResultSet iterator()
    {
        return getResults();
    }

    @Override
    public void close()
        throws IOException
    {
        getResults().close();
    }

    /**
     * A helper method delegating the call to the IteratorResultSet.
     * 
     * @return
     */
    public int getTotalProcessedArtifactInfoCount()
    {
        return getResults().getTotalProcessedArtifactInfoCount();
    }

    // ==

    public static final IteratorResultSet EMPTY_ITERATOR_RESULT_SET = new IteratorResultSet()
    {
        public boolean hasNext()
        {
            return false;
        }

        public ArtifactInfo next()
        {
            return null;
        }

        public void remove()
        {
            throw new UnsupportedOperationException( "Method not supported on " + getClass().getName() );
        }

        public Iterator<ArtifactInfo> iterator()
        {
            return this;
        }

        public int getTotalProcessedArtifactInfoCount()
        {
            return 0;
        }

        public void close()
            throws IOException
        {
        }
    };

    public static final IteratorSearchResponse empty( final Query q )
    {
        return new IteratorSearchResponse( q, 0, EMPTY_ITERATOR_RESULT_SET );
    }

    /**
     * Empty search response.
     * 
     * @deprecated Use {@link #empty(Query)} instead.
     */
    public static final IteratorSearchResponse EMPTY_ITERATOR_SEARCH_RESPONSE = empty( null );

    /**
     * Too many search response.
     * 
     * @deprecated Left here for backward compatibility, but since version 4.1.0 (see MINDEXER-14) there is NO notion of
     *             "hit limit" anymore.
     */
    public static final IteratorSearchResponse TOO_MANY_HITS_ITERATOR_SEARCH_RESPONSE =
        new IteratorSearchResponse( null, -1, EMPTY_ITERATOR_RESULT_SET );
}
