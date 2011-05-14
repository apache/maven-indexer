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
 * SourcedSearchExpression is a search expression usually "sourced" or backed from some programmatic source (drop down
 * with pre-filled values, values from previous searches, etc), and we already know it is complete, exact value that we
 * want to search for. Indexer will do it's best to match exactly the provided string value, no more no less.
 * 
 * @author cstamas
 */
public class SourcedSearchExpression
    extends SearchTypedStringSearchExpression
{
    public SourcedSearchExpression( final String expression )
        throws IllegalArgumentException
    {
        super( expression, SearchType.EXACT );
    }
}
