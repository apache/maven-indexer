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

import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * Pageable search request. Adds "start" point, to skip wanted number of records, to implement paging. Use "count" of
 * AbstractSearchRequest to set page size.
 * 
 * @author cstamas
 */
public class AbstractSearchPageableRequest
    extends AbstractSearchRequest
{
    /**
     * The number of hit we want to skip from result set. Defaults to 0.
     */
    private int start;

    public AbstractSearchPageableRequest( Query query )
    {
        super( query, null );

        this.start = 0;
    }

    public AbstractSearchPageableRequest( Query query, List<IndexingContext> contexts )
    {
        super( query, contexts );

        this.start = 0;
    }

    /**
     * Returns the "start" of wanted results calculated from result set window. Simply, the count of documents to skip.
     * 
     * @return
     */
    public int getStart()
    {
        return start;
    }

    /**
     * Sets the "start" of wanted results calculated from result set window. Simply, the count of documents to skip.
     * 
     * @param start
     */
    public void setStart( int start )
    {
        if ( start < 0 )
        {
            throw new IllegalArgumentException( "Start cannot be less than 0!" );
        }

        this.start = start;
    }
}
