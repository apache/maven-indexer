package org.apache.maven.index.expr;

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

import org.apache.maven.index.SearchType;

/**
 * A search typed implementation of string backed search expression.
 * 
 * @author cstamas
 */
public class SearchTypedStringSearchExpression
    extends StringSearchExpression
    implements SearchTyped
{
    private final SearchType searchType;

    public SearchTypedStringSearchExpression( final String expression, final SearchType searchType )
        throws IllegalArgumentException
    {
        super( expression );

        if ( searchType == null )
        {
            throw new IllegalArgumentException( "SearchType cannot be null!" );
        }

        this.searchType = searchType;
    }

    public SearchType getSearchType()
    {
        return searchType;
    }
}
