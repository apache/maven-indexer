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

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.maven.index.expr.SearchExpression;

/**
 * A component the creates Lucene Queries from "human written" queries, but also helps client applications to assemble
 * proper queries for fields they want to search.
 * 
 * @author Tamas Cservenak
 */
public interface QueryCreator
{
    String ROLE = QueryCreator.class.getName();

    /**
     * Performs a selection of the appropriate IndexerField belonging to proper Field.
     * 
     * @param field
     * @param type
     * @return
     */
    IndexerField selectIndexerField( final Field field, final SearchType type );

    /**
     * Constructs query by parsing the query string, using field as default field. This method should be use to
     * construct queries (single term or phrase queries) against <b>single field</b>.
     * 
     * @param field
     * @param query
     * @param type
     * @return
     * @throws ParseException if query parsing is unsuccessful.
     */
    Query constructQuery( Field field, SearchExpression expression )
        throws ParseException;

    /**
     * Constructs query by parsing the query string, using field as default field. This method should be use to
     * construct queries (single term or phrase queries) against <b>single field</b>.
     * 
     * @param field
     * @param query
     * @param type
     * @return
     * @throws ParseException if query parsing is unsuccessful.
     * @deprecated Use {@link #constructQuery(Field, SearchExpression)} instead.
     */
    Query constructQuery( Field field, String query, SearchType type )
        throws ParseException;

    /**
     * Deprecated. Avoid it's use! Constructs query against <b>single</b> field, using it's "best effort" approach to
     * perform parsing, but letting caller to apply it's (usually wrong) knowledge about how field is indexed.
     * 
     * @param field
     * @param query
     * @return query if successfully parsed, or null.
     * @deprecated Use {@link #constructQuery(Field, SearchExpression)} instead.
     */
    Query constructQuery( String field, String query );

}
