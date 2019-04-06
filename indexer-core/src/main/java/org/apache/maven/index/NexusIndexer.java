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
import java.util.Map;

import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.expr.SearchExpression;

/**
 * The Nexus indexer is a statefull facade that maintains state of indexing contexts.
 * <p>
 * The following code snippet shows how to register indexing context, which should be done once on the application
 * startup and Nexus indexer instance should be reused after that.
 * 
 * <pre>
 * NexusIndexer indexer;
 * 
 * IndexingContext context = indexer.addIndexingContext( indexId, // index id (usually the same as repository id)
 *     repositoryId, // repository id
 *     directory, // Lucene directory where index is stored
 *     repositoryDir, // local repository dir or null for remote repo
 *     repositoryUrl, // repository url, used by index updater
 *     indexUpdateUrl, // index update url or null if derived from repositoryUrl
 *     false, false );
 * </pre>
 * 
 * An indexing context could be populated using one of {@link #scan(IndexingContext)},
 * {@link #addArtifactToIndex(ArtifactContext, IndexingContext)} or
 * {@link #deleteArtifactFromIndex(ArtifactContext, IndexingContext)} methods.
 * <p>
 * An {@link org.apache.maven.index.updater.IndexUpdater} could be used to fetch indexes from remote repositories.
 * These indexers could be created using the Indexer CLI command line tool or
 * {@link org.apache.maven.index.packer.IndexPacker} API.
 * <p>
 * Once index is populated you can perform search queries using field names declared in the {@link ArtifactInfo}:
 * 
 * <pre>
 *   // run search query
 *   BooleanQuery q = new BooleanQuery.Builder()
 *    .add(indexer.constructQuery(ArtifactInfo.GROUP_ID, term), Occur.SHOULD)
 *    .add(indexer.constructQuery(ArtifactInfo.ARTIFACT_ID, term), Occur.SHOULD)
 *    .add(new PrefixQuery(new Term(ArtifactInfo.SHA1, term)), Occur.SHOULD)
 *    .build();
 *   
 *   FlatSearchRequest request = new FlatSearchRequest(q);
 *   FlatSearchResponse response = indexer.searchFlat(request);
 *   ...
 * </pre>
 * 
 * Query could be also constructed using a convenience {@link NexusIndexer#constructQuery(Field, SearchExpression)}
 * method that handles creation of the wildcard queries. Also see {@link DefaultQueryCreator} for more details on
 * supported queries.
 * 
 * @see IndexingContext
 * @see org.apache.maven.index.updater.IndexUpdater
 * @see DefaultQueryCreator
 * @author Jason van Zyl
 * @author Tamas Cservenak
 * @author Eugene Kuleshov
 * @deprecated Use {@link Indexer} instead.
 */
@Deprecated
public interface NexusIndexer
{
    String ROLE = NexusIndexer.class.getName();

    /**
     * Adds an indexing context to Nexus indexer.
     * 
     * @since 5.1.0
     */
    void addIndexingContext( IndexingContext context );

    /**
     * Adds an indexing context to Nexus indexer.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents.
     * @param repository the location of the repository.
     * @param indexDirectory the location of the Lucene indexes.
     * @param repositoryUrl the location of the remote repository.
     * @param indexUpdateUrl the alternate location of the remote repository indexes (if they are not in default place).
     * @param indexers the set of indexers to apply to this context.
     * @return
     * @throws IOException in case of some serious IO problem.
     * @throws UnsupportedExistingLuceneIndexException if a Lucene index already exists where location is specified, but
     *             it has no Nexus descriptor record or it has, but the embedded repoId differs from the repoId
     *             specified from the supplied one.
     * @throws IllegalArgumentException in case the supplied list of IndexCreators are not satisfiable
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    IndexingContext addIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                        String repositoryUrl, String indexUpdateUrl,
                                        List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException;

    /**
     * Adds an indexing context to Nexus indexer. It "forces" this operation, thus no
     * UnsupportedExistingLuceneIndexException is thrown. If it founds an existing lucene index, it will simply
     * stomp-over and rewrite (or add) the Nexus index descriptor.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents.
     * @param repository the location of the repository.
     * @param indexDirectory the location of the Lucene indexes.
     * @param repositoryUrl the location of the remote repository.
     * @param indexUpdateUrl the alternate location of the remote repository indexes (if they are not in default place).
     * @param indexers the set of indexers to apply to this context.
     * @return
     * @throws IOException in case of some serious IO problem.
     * @throws IllegalArgumentException in case the supplied list of IndexCreators are not satisfiable
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    IndexingContext addIndexingContextForced( String id, String repositoryId, File repository, File indexDirectory,
                                              String repositoryUrl, String indexUpdateUrl,
                                              List<? extends IndexCreator> indexers )
        throws IOException;

    /**
     * Adds an indexing context to Nexus indexer.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents.
     * @param repository the location of the repository.
     * @param directory the location of the Lucene indexes.
     * @param repositoryUrl the location of the remote repository.
     * @param indexUpdateUrl the alternate location of the remote repository indexes (if they are not in default place).
     * @param indexers the set of indexers to apply to this context.
     * @return
     * @throws IOException in case of some serious IO problem.
     * @throws UnsupportedExistingLuceneIndexException if a Lucene index already exists where location is specified, but
     *             it has no Nexus descriptor record or it has, but the embedded repoId differs from the repoId
     *             specified from the supplied one.
     * @throws IllegalArgumentException in case the supplied list of IndexCreators are not satisfiable
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    IndexingContext addIndexingContext( String id, String repositoryId, File repository, Directory directory,
                                        String repositoryUrl, String indexUpdateUrl,
                                        List<? extends IndexCreator> indexers )
        throws IOException, UnsupportedExistingLuceneIndexException;

    /**
     * Adds an indexing context to Nexus indexer. It "forces" this operation, thus no
     * UnsupportedExistingLuceneIndexException is thrown. If it founds an existing lucene index, it will simply
     * stomp-over and rewrite (or add) the Nexus index descriptor.
     * 
     * @param id the ID of the context.
     * @param repositoryId the ID of the repository that this context represents.
     * @param repository the location of the repository.
     * @param directory the location of the Lucene indexes.
     * @param repositoryUrl the location of the remote repository.
     * @param indexUpdateUrl the alternate location of the remote repository indexes (if they are not in default place).
     * @param indexers the set of indexers to apply to this context.
     * @return
     * @throws IOException in case of some serious IO problem.
     * @throws IllegalArgumentException in case the supplied list of IndexCreators are not satisfiable
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    IndexingContext addIndexingContextForced( String id, String repositoryId, File repository, Directory directory,
                                              String repositoryUrl, String indexUpdateUrl,
                                              List<? extends IndexCreator> indexers )
        throws IOException;

    @Deprecated
    IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                              boolean searchable, Collection<IndexingContext> contexts )
        throws IOException;

    @Deprecated
    IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository, File indexDirectory,
                                              boolean searchable, ContextMemberProvider membersProvider )
        throws IOException;

    @Deprecated
    IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
                                              Directory indexDirectory, boolean searchable,
                                              Collection<IndexingContext> contexts )
        throws IOException;

    @Deprecated
    IndexingContext addMergedIndexingContext( String id, String repositoryId, File repository,
                                              Directory indexDirectory, boolean searchable,
                                              ContextMemberProvider membersProvider )
        throws IOException;

    /**
     * Removes the indexing context from Nexus indexer, closes it and deletes (if specified) the index files.
     * 
     * @param context
     * @param deleteFiles
     * @throws IOException
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void removeIndexingContext( IndexingContext context, boolean deleteFiles )
        throws IOException;

    /**
     * Returns the map of indexing contexts keyed by their ID.
     * 
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    Map<String, IndexingContext> getIndexingContexts();

    // ----------------------------------------------------------------------------
    // Scanning
    // ----------------------------------------------------------------------------
    /**
     * Performs full scan (reindex) for the local repository belonging to supplied context.
     * 
     * @param context
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void scan( IndexingContext context )
        throws IOException;

    /**
     * Performs full scan (reindex) for the local repository belonging to supplied context. ArtifactListener is used
     * during that process.
     * 
     * @param context
     * @param listener
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void scan( IndexingContext context, ArtifactScanningListener listener )
        throws IOException;

    /**
     * Performs optionally incremental scan (reindex/full reindex) for the local repository belonging to the supplied
     * context.
     * 
     * @param context
     * @param update if incremental reindex wanted, set true, otherwise false and full reindex will happen
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void scan( IndexingContext context, boolean update )
        throws IOException;

    /**
     * Performs optionally incremental scan (reindex) for the local repository, with listener.
     * 
     * @param context
     * @param listener
     * @param update if incremental reindex wanted, set true, otherwise false and full reindex will happen
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void scan( IndexingContext context, ArtifactScanningListener listener, boolean update )
        throws IOException;

    /**
     * Performs optionally incremental scan (reindex) for the local repository.
     * 
     * @param context
     * @param fromPath a path segment if you want "sub-path" reindexing (ie. reindex just a given subfolder of a
     *            repository, ot whole repository from root.
     * @param listener
     * @param update if incremental reindex wanted, set true, otherwise false and full reindex will happen
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    void scan( IndexingContext context, String fromPath, ArtifactScanningListener listener, boolean update )
        throws IOException;

    @Deprecated
    void artifactDiscovered( ArtifactContext ac, IndexingContext context )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Modifying
    // ----------------------------------------------------------------------------

    @Deprecated
    void addArtifactToIndex( ArtifactContext ac, IndexingContext context )
        throws IOException;

    @Deprecated
    void addArtifactsToIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException;

    @Deprecated
    void deleteArtifactFromIndex( ArtifactContext ac, IndexingContext context )
        throws IOException;

    @Deprecated
    void deleteArtifactsFromIndex( Collection<ArtifactContext> acs, IndexingContext context )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Searching
    // ----------------------------------------------------------------------------

    /**
     * Searches according the request parameters.
     * 
     * @param request
     * @return
     * @throws IOException
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    FlatSearchResponse searchFlat( FlatSearchRequest request )
        throws IOException;

    /**
     * Searches according to request parameters.
     * 
     * @param request
     * @return
     * @throws IOException
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    IteratorSearchResponse searchIterator( IteratorSearchRequest request )
        throws IOException;

    /**
     * Searches according the request parameters.
     * 
     * @param request
     * @return
     * @throws IOException
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    GroupedSearchResponse searchGrouped( GroupedSearchRequest request )
        throws IOException;

    // ----------------------------------------------------------------------------
    // Query construction
    // ----------------------------------------------------------------------------

    /**
     * Helper method to construct Lucene query for given field without need for knowledge (on caller side) HOW is a
     * field indexed, and WHAT query is needed to achieve that.
     * 
     * @param field
     * @param query
     * @param type
     * @return
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    Query constructQuery( Field field, String query, SearchType type )
        throws IllegalArgumentException;

    /**
     * Helper method to construct Lucene query for given field without need for knowledge (on caller side) HOW is a
     * field indexed, and WHAT query is needed to achieve that.
     * 
     * @param field
     * @param expression
     * @return
     * @deprecated Use {@link Indexer} instead.
     */
    @Deprecated
    Query constructQuery( Field field, SearchExpression expression )
        throws IllegalArgumentException;

    // ----------------------------------------------------------------------------
    // Identification
    // Since 4.0: Indexer does not make any assumptions, it is caller call to decide what to do with multiple results
    // ----------------------------------------------------------------------------

    @Deprecated
    Collection<ArtifactInfo> identify( Field field, String query )
        throws IllegalArgumentException, IOException;

    @Deprecated
    Collection<ArtifactInfo> identify( File artifact )
        throws IOException;

    @Deprecated
    Collection<ArtifactInfo> identify( File artifact, Collection<IndexingContext> contexts )
        throws IOException;

    @Deprecated
    Collection<ArtifactInfo> identify( Query query )
        throws IOException;

    @Deprecated
    Collection<ArtifactInfo> identify( Query query, Collection<IndexingContext> contexts )
        throws IOException;

}
