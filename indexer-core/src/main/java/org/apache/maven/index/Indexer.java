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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SearchExpression;

/**
 * Indexer component. It is the main component of Maven Indexer, offering {@link IndexingContext} creation and close
 * methods, context maintenance (scan, add, remove) and search methods. Supersedes the {@link NexusIndexer} component,
 * making it less cludged, and focusing on main use cases. This component does not hold any reference to contexts
 * it creates or uses, and caller of every method (except the createIndexingContext naturally) is obliged to
 * explicitly supply {@link IndexingContext} to work with (perform searches or such).
 * 
 * @author cstamas
 * @since 5.1.0
 */
public interface Indexer
{
    /**
     * Creates an indexing context.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents. You might have several contexts
     *            indexing same repository ID, but on separate locations.
     * @param repository the location of the repository on FS.
     * @param indexDirectory the location of the Lucene indexes on FS.
     * @param repositoryUrl the location of the remote repository or {@code null} if this indexing context does not need
     *            remote updates (is not a proxy).
     * @param indexUpdateUrl the alternate location of the remote repository indexes (if they are not in default place)
     *            or {@code null} if defaults are applicable.
     * @param searchable if context should be searched in non-targeted mode.
     * @param reclaim if indexDirectory is known to contain (or should contain) valid Maven Indexer lucene index, and no
     *            checks needed to be performed, or, if we want to "stomp" over existing index (unsafe to do!).
     * @param indexers the set of indexers to apply to this context.
     * @return the context created.
     * @throws IOException in case of some serious IO problem.
     * @throws ExistingLuceneIndexMismatchException if a Lucene index already exists where location is specified, but
     *             it has no Nexus descriptor record or it has, but the embedded repoId differs from the repoId
     *             specified from the supplied one. Never thrown if {@code reclaim} is {@code true}, as in that case, if
     *             Lucene index exists but any of those criteria above are not met, the existing index is overwritten,
     *             and equipped with proper descriptor silently.
     * @throws IllegalArgumentException in case the supplied list of IndexCreators are having non-satisfiable
     *             dependencies.
     */
    IndexingContext createIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                           String repositoryUrl, String indexUpdateUrl, boolean searchable,
                                           boolean reclaim, List<? extends IndexCreator> indexers )
        throws IOException, ExistingLuceneIndexMismatchException, IllegalArgumentException;

    /**
     * Creates a merged indexing context.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents. You might have several contexts
     *            indexing same repository ID, but on separate locations.
     * @param repository the location of the repository on FS.
     * @param indexDirectory the location of the Lucene indexes on FS.
     * @param searchable if context should be searched in non-targeted mode.
     * @param membersProvider the {@link ContextMemberProvider}, never null.
     * @return the context created.
     * @throws IOException in case of some serious IO problem.
     */
    IndexingContext createMergedIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                                 boolean searchable, ContextMemberProvider membersProvider )
        throws IOException;

    /**
     * Closes the indexing context: closes it and deletes (if specified) the index files.
     * 
     * @param context the one needed to be closed, never {@code null}.
     * @param deleteFiles {@code true} if all indexer related files (including Lucene index!) needs to be deleted,
     *            {@code false} otherwise.
     * @throws IOException
     */
    void closeIndexingContext( IndexingContext context, boolean deleteFiles )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Modifying
    // ----------------------------------------------------------------------------

    /**
     * Adds the passed in artifact contexts to passed in indexing context.
     *
     * @param ac
     * @param context
     * @throws IOException
     */
    void addArtifactToIndex( ArtifactContext ac, IndexingContext context )
        throws IOException;

    /**
     * Adds the passed in artifact contexts to passed in indexing context.
     * 
     * @param acs
     * @param context
     * @throws IOException
     */
    void addArtifactsToIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException;

    /**
     * Removes the passed in artifacts contexts from passed in indexing context.
     * 
     * @param acs
     * @param context
     * @throws IOException
     */
    void deleteArtifactsFromIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Searching
    // ----------------------------------------------------------------------------

    /**
     * Searches according the request parameters.
     * 
     * @param request
     * @return search response
     * @throws IOException
     */
    FlatSearchResponse searchFlat( FlatSearchRequest request )
        throws IOException;

    /**
     * Searches according to request parameters.
     * 
     * @param request
     * @return search response
     * @throws IOException
     */
    IteratorSearchResponse searchIterator( IteratorSearchRequest request )
        throws IOException;

    /**
     * Searches according the request parameters.
     * 
     * @param request
     * @return search response
     * @throws IOException
     */
    GroupedSearchResponse searchGrouped( GroupedSearchRequest request )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Identify
    // ----------------------------------------------------------------------------

    /**
     * Performs an "identity" search. Passed in {@link File} will have SHA1 hash calculated, and an
     * {@link #identify(Query, Collection)} method will be invoked searching with calculated hash the {@link MAVEN#SHA1}
     * field. This is just a shorthand method, as these calls are simply calculating hex encoded SHA1 of the file, and
     * invoking the {@link #constructQuery(Field, SearchExpression)} and {@link #identify(Query, Collection)} methods.
     * 
     * @param artifact the file
     * @param contexts in which to perform the action
     * @return collection of identified matches.
     * @throws IOException
     */
    Collection<ArtifactInfo> identify( File artifact, Collection<IndexingContext> contexts )
        throws IOException;

    /**
     * Performs an "identity" search. Those are usually simple key-value queries, involving "unique" fields like
     * {@link MAVEN#SHA1} or such.
     * 
     * @param query
     * @param contexts
     * @return collection of identified matches.
     * @throws IOException
     */
    Collection<ArtifactInfo> identify( Query query, Collection<IndexingContext> contexts )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Query construction
    // ----------------------------------------------------------------------------

    /**
     * Helper method to construct Lucene query for given field without need for knowledge (on caller side) HOW is a
     * field indexed, and WHAT query is needed to achieve that search.
     * 
     * @param field
     * @param expression
     * @return the query to be used for search.
     * @see SearchExpression
     * @see org.apache.maven.index.expr.UserInputSearchExpression
     * @see org.apache.maven.index.expr.SourcedSearchExpression
     * @throws IllegalArgumentException
     */
    Query constructQuery( Field field, SearchExpression expression )
        throws IllegalArgumentException;

    /**
     * Helper method to construct Lucene query for given field without need for knowledge (on caller side) HOW is a
     * field indexed, and WHAT query is needed to achieve that search.
     *
     * @param field
     * @param expression
     * @param searchType
     * @return
     * @throws IllegalArgumentException
     */
    Query constructQuery( Field field, String expression, SearchType searchType )
        throws IllegalArgumentException;
}
