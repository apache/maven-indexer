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
package org.apache.maven.search.backend.indexer.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactAvailability;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.backend.indexer.IndexerCoreSearchBackend;
import org.apache.maven.search.backend.indexer.IndexerCoreSearchResponse;
import org.apache.maven.search.api.request.Field;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.request.Paging;
import org.apache.maven.search.api.support.SearchBackendSupport;

import static java.util.Objects.requireNonNull;

/**
 * An engine to perform search trough single repository index (endpoint).
 */
public class IndexerCoreSearchBackendImpl extends SearchBackendSupport implements IndexerCoreSearchBackend {
    private static final Map<Field, org.apache.maven.index.Field> FIELD_TRANSLATION;

    static {
        HashMap<Field, org.apache.maven.index.Field> map = new HashMap<>();
        map.put(MAVEN.GROUP_ID, org.apache.maven.index.MAVEN.GROUP_ID);
        map.put(MAVEN.ARTIFACT_ID, org.apache.maven.index.MAVEN.ARTIFACT_ID);
        map.put(MAVEN.VERSION, org.apache.maven.index.MAVEN.VERSION);
        map.put(MAVEN.CLASSIFIER, org.apache.maven.index.MAVEN.CLASSIFIER);
        map.put(MAVEN.PACKAGING, org.apache.maven.index.MAVEN.PACKAGING);
        map.put(MAVEN.CLASS_NAME, org.apache.maven.index.MAVEN.CLASSNAMES);
        map.put(MAVEN.FQ_CLASS_NAME, org.apache.maven.index.MAVEN.CLASSNAMES);
        map.put(MAVEN.SHA1, org.apache.maven.index.MAVEN.SHA1);
        FIELD_TRANSLATION = Collections.unmodifiableMap(map);
    }

    private final Indexer indexer;

    private final IndexingContext indexingContext;

    /**
     * Creates backend instance using provided indexer and context.
     */
    public IndexerCoreSearchBackendImpl(Indexer indexer, IndexingContext indexingContext) {
        super(indexingContext.getId(), indexingContext.getRepositoryId());
        this.indexer = requireNonNull(indexer);
        this.indexingContext = indexingContext;
    }

    @Override
    public IndexingContext getIndexingContext() {
        return indexingContext;
    }

    @Override
    public IndexerCoreSearchResponse search(SearchRequest searchRequest) throws IOException {
        Paging paging = searchRequest.getPaging();
        int totalHitsCount;
        List<ArtifactInfo> artifactInfos = new ArrayList<>(paging.getPageSize());
        List<Record> page = new ArrayList<>(paging.getPageSize());

        // if GA present in query: doing flat, otherwise grouped search to mimic SMO
        HashSet<Field> searchedFields = new HashSet<>();
        Query query = toQuery(searchedFields, searchRequest.getQuery());
        if (searchedFields.contains(MAVEN.SHA1)
                || (searchedFields.contains(MAVEN.GROUP_ID) && searchedFields.contains(MAVEN.ARTIFACT_ID))) {
            if (!searchedFields.contains(MAVEN.CLASSIFIER)) {
                query = new BooleanQuery.Builder()
                        .add(new BooleanClause(query, BooleanClause.Occur.MUST))
                        .add(
                                indexer.constructQuery(
                                        org.apache.maven.index.MAVEN.CLASSIFIER,
                                        new SourcedSearchExpression(org.apache.maven.index.Field.NOT_PRESENT)),
                                BooleanClause.Occur.MUST_NOT)
                        .build();
            }
            IteratorSearchRequest iteratorSearchRequest =
                    new IteratorSearchRequest(query, Collections.singletonList(indexingContext));
            iteratorSearchRequest.setCount(paging.getPageSize());
            iteratorSearchRequest.setStart(paging.getPageSize() * paging.getPageOffset());

            try (IteratorSearchResponse iteratorSearchResponse = indexer.searchIterator(iteratorSearchRequest)) {
                totalHitsCount = iteratorSearchResponse.getTotalHitsCount();
                StreamSupport.stream(iteratorSearchResponse.iterator().spliterator(), false)
                        .sorted(ArtifactInfo.VERSION_COMPARATOR)
                        .forEach(ai -> {
                            artifactInfos.add(ai);
                            page.add(convert(ai, null));
                        });
            }
            return new IndexerCoreSearchResponseImpl(searchRequest, totalHitsCount, page, query, artifactInfos);
        } else {
            GroupedSearchRequest groupedSearchRequest =
                    new GroupedSearchRequest(query, new GAGrouping(), indexingContext);

            try (GroupedSearchResponse groupedSearchResponse = indexer.searchGrouped(groupedSearchRequest)) {
                totalHitsCount = groupedSearchResponse.getResults().size();
                groupedSearchResponse.getResults().values().stream()
                        .skip((long) paging.getPageSize() * paging.getPageOffset())
                        .limit(paging.getPageSize())
                        .forEach(aig -> {
                            ArtifactInfo ai = aig.getArtifactInfos().iterator().next();
                            artifactInfos.add(ai);
                            page.add(convert(ai, aig.getArtifactInfos().size()));
                        });
            }
            return new IndexerCoreSearchResponseImpl(searchRequest, totalHitsCount, page, query, artifactInfos);
        }
    }

    private Query toQuery(HashSet<Field> searchedFields, org.apache.maven.search.api.request.Query query) {
        if (query instanceof org.apache.maven.search.api.request.BooleanQuery.And) {
            org.apache.maven.search.api.request.BooleanQuery bq = (org.apache.maven.search.api.request.BooleanQuery) query;
            return new BooleanQuery.Builder()
                    .add(new BooleanClause(toQuery(searchedFields, bq.getLeft()), BooleanClause.Occur.MUST))
                    .add(new BooleanClause(toQuery(searchedFields, bq.getRight()), BooleanClause.Occur.MUST))
                    .build();
        } else if (query instanceof FieldQuery) {
            FieldQuery fq = (FieldQuery) query;
            org.apache.maven.index.Field icFieldName = FIELD_TRANSLATION.get(fq.getField());
            if (icFieldName != null) {
                searchedFields.add(fq.getField());
                if (fq.getValue().endsWith("*")) {
                    return indexer.constructQuery(icFieldName, fq.getValue(), SearchType.SCORED);
                } else {
                    return indexer.constructQuery(icFieldName, fq.getValue(), SearchType.EXACT);
                }
            } else {
                throw new IllegalArgumentException("Unsupported Indexer field: " + fq.getField());
            }
        }
        return new BooleanQuery.Builder()
                .add(new BooleanClause(
                        indexer.constructQuery(
                                org.apache.maven.index.MAVEN.GROUP_ID, query.getValue(), SearchType.SCORED),
                        BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(
                        indexer.constructQuery(
                                org.apache.maven.index.MAVEN.ARTIFACT_ID, query.getValue(), SearchType.SCORED),
                        BooleanClause.Occur.SHOULD))
                .add(new BooleanClause(
                        indexer.constructQuery(org.apache.maven.index.MAVEN.NAME, query.getValue(), SearchType.SCORED),
                        BooleanClause.Occur.SHOULD))
                .build();
    }

    private Record convert(ArtifactInfo ai, /* nullable */ Integer versionCount) {
        HashMap<Field, Object> result = new HashMap<>();

        mayPut(result, MAVEN.GROUP_ID, ai.getGroupId());
        mayPut(result, MAVEN.ARTIFACT_ID, ai.getArtifactId());
        mayPut(result, MAVEN.VERSION, ai.getVersion());
        mayPut(result, MAVEN.PACKAGING, ai.getPackaging());
        mayPut(result, MAVEN.CLASSIFIER, ai.getClassifier());
        mayPut(result, MAVEN.FILE_EXTENSION, ai.getFileExtension());

        mayPut(result, MAVEN.VERSION_COUNT, versionCount);

        mayPut(result, MAVEN.HAS_SOURCE, ai.getSourcesExists() == ArtifactAvailability.PRESENT);
        mayPut(result, MAVEN.HAS_JAVADOC, ai.getJavadocExists() == ArtifactAvailability.PRESENT);
        mayPut(result, MAVEN.HAS_GPG_SIGNATURE, ai.getSignatureExists() == ArtifactAvailability.PRESENT);

        return new Record(getBackendId(), getRepositoryId(), ai.getUinfo(), ai.getLastModified(), result);
    }

    private static void mayPut(Map<Field, Object> result, Field fieldName, /* nullable */ Object value) {
        if (value != null) {
            result.put(fieldName, value);
        }
    }
}
