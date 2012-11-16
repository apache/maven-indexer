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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * A grouped search request. This kinds of request is not pageable, since order of incoming hits are not defined, hence
 * paging between Document hits makes no sense, would produce unpredictable (and probably not meaningful) results.
 * 
 * @see Indexer#searchGrouped(GroupedSearchRequest)
 */
public class GroupedSearchRequest
    extends AbstractSearchRequest
{
    private Grouping grouping;

    private Comparator<String> groupKeyComparator;

    public GroupedSearchRequest( Query query, Grouping grouping )
    {
        this( query, grouping, String.CASE_INSENSITIVE_ORDER );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, Comparator<String> groupKeyComparator )
    {
        this( query, grouping, groupKeyComparator, null );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, IndexingContext context )
    {
        this( query, grouping, String.CASE_INSENSITIVE_ORDER, context );
    }

    public GroupedSearchRequest( Query query, Grouping grouping, Comparator<String> groupKeyComparator,
                                 IndexingContext context )
    {
        super( query, context != null ? Arrays.asList( new IndexingContext[] { context } ) : null );

        this.grouping = grouping;

        this.groupKeyComparator = groupKeyComparator;
    }

    public Grouping getGrouping()
    {
        return grouping;
    }

    public void setGrouping( Grouping grouping )
    {
        this.grouping = grouping;
    }

    public Comparator<String> getGroupKeyComparator()
    {
        return groupKeyComparator;
    }

    public void setGroupKeyComparator( Comparator<String> groupKeyComparator )
    {
        this.groupKeyComparator = groupKeyComparator;
    }
}
