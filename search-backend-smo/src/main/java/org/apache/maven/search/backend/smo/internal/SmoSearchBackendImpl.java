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
package org.apache.maven.search.backend.smo.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.Field;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.request.Paging;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.api.support.SearchBackendSupport;
import org.apache.maven.search.api.transport.Transport;
import org.apache.maven.search.backend.smo.SmoSearchBackend;
import org.apache.maven.search.backend.smo.SmoSearchResponse;

import static java.util.Objects.requireNonNull;

public class SmoSearchBackendImpl extends SearchBackendSupport implements SmoSearchBackend {
    private static final Map<Field, String> FIELD_TRANSLATION;

    static {
        FIELD_TRANSLATION = Map.of(
                MAVEN.GROUP_ID,
                "g",
                MAVEN.ARTIFACT_ID,
                "a",
                MAVEN.VERSION,
                "v",
                MAVEN.CLASSIFIER,
                "l",
                MAVEN.PACKAGING,
                "p",
                MAVEN.CLASS_NAME,
                "c",
                MAVEN.FQ_CLASS_NAME,
                "fc",
                MAVEN.SHA1,
                "1");
    }

    private final String smoUri;

    private final Transport transport;

    private final Map<String, String> commonHeaders;

    /**
     * Creates a customized instance of SMO backend, like an in-house instances of SMO or different IDs.
     */
    public SmoSearchBackendImpl(String backendId, String repositoryId, String smoUri, Transport transport) {
        super(backendId, repositoryId);
        this.smoUri = requireNonNull(smoUri);
        this.transport = requireNonNull(transport);

        this.commonHeaders = new HashMap<>();
        this.commonHeaders.put(
                "User-Agent",
                "Apache-Maven-Search-SMO/" + discoverVersion() + " "
                        + transport.getClass().getSimpleName());
        this.commonHeaders.put("Accept", "application/json");
    }

    private String discoverVersion() {
        Properties properties = new Properties();
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream("org/apache/maven/search/backend/smo/internal/smo-version.properties");
        if (inputStream != null) {
            try (InputStream is = inputStream) {
                properties.load(is);
            } catch (IOException e) {
                // fall through
            }
        }
        return properties.getProperty("version", "unknown");
    }

    @Override
    public String getSmoUri() {
        return smoUri;
    }

    @Override
    public SmoSearchResponse search(SearchRequest searchRequest) throws IOException {
        String searchUri = toURI(searchRequest);
        String payload = fetch(searchUri, commonHeaders);
        JsonObject raw = JsonParser.parseString(payload).getAsJsonObject();
        List<Record> page = new ArrayList<>(searchRequest.getPaging().getPageSize());
        int totalHits = populateFromRaw(raw, page);
        return new SmoSearchResponseImpl(searchRequest, totalHits, page, searchUri, payload);
    }

    private String toURI(SearchRequest searchRequest) {
        Paging paging = searchRequest.getPaging();
        HashSet<Field> searchedFields = new HashSet<>();
        String smoQuery = toSMOQuery(searchedFields, searchRequest.getQuery());
        smoQuery += "&start=" + paging.getPageSize() * paging.getPageOffset();
        smoQuery += "&rows=" + paging.getPageSize();
        smoQuery += "&wt=json";
        if (searchedFields.contains(MAVEN.GROUP_ID) && searchedFields.contains(MAVEN.ARTIFACT_ID)) {
            smoQuery += "&core=gav";
        }
        return smoUri + "?q=" + smoQuery;
    }

    private String fetch(String serviceUri, Map<String, String> headers) throws IOException {
        try (Transport.Response response = transport.get(serviceUri, headers)) {
            if (response.getCode() == HttpURLConnection.HTTP_OK) {
                return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IOException("Unexpected response: " + response);
            }
        }
    }

    private String toSMOQuery(HashSet<Field> searchedFields, Query query) {
        if (query instanceof BooleanQuery.And) {
            BooleanQuery bq = (BooleanQuery) query;
            return toSMOQuery(searchedFields, bq.getLeft()) + "%20AND%20" + toSMOQuery(searchedFields, bq.getRight());
        } else if (query instanceof FieldQuery) {
            FieldQuery fq = (FieldQuery) query;
            String smoFieldName = FIELD_TRANSLATION.get(fq.getField());
            if (smoFieldName != null) {
                searchedFields.add(fq.getField());
                return smoFieldName + ":" + encodeQueryParameterValue(fq.getValue());
            } else {
                throw new IllegalArgumentException("Unsupported SMO field: " + fq.getField());
            }
        }
        return encodeQueryParameterValue(query.getValue());
    }

    private String encodeQueryParameterValue(String parameterValue) {
        return URLEncoder.encode(parameterValue, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private int populateFromRaw(JsonObject raw, List<Record> page) {
        JsonObject response = raw.getAsJsonObject("response");
        Number numFound = response.get("numFound").getAsNumber();

        JsonArray docs = response.getAsJsonArray("docs");
        for (JsonElement doc : docs) {
            page.add(convert((JsonObject) doc));
        }
        return numFound.intValue();
    }

    private Record convert(JsonObject doc) {
        HashMap<Field, Object> result = new HashMap<>();

        mayPut(result, MAVEN.GROUP_ID, mayGet("g", doc));
        mayPut(result, MAVEN.ARTIFACT_ID, mayGet("a", doc));
        String version = mayGet("v", doc);
        if (version == null) {
            version = mayGet("latestVersion", doc);
        }
        mayPut(result, MAVEN.VERSION, version);
        mayPut(result, MAVEN.PACKAGING, mayGet("p", doc));
        mayPut(result, MAVEN.CLASSIFIER, mayGet("l", doc));

        // version count
        Number versionCount = doc.has("versionCount") ? doc.get("versionCount").getAsNumber() : null;
        if (versionCount != null) {
            mayPut(result, MAVEN.VERSION_COUNT, versionCount.intValue());
        }
        // ec
        JsonArray ec = doc.getAsJsonArray("ec");
        if (ec != null) {
            result.put(MAVEN.HAS_SOURCE, ec.contains(EC_SOURCE_JAR));
            result.put(MAVEN.HAS_JAVADOC, ec.contains(EC_JAVADOC_JAR));
            // result.put( MAVEN.HAS_GPG_SIGNATURE, ec.contains( ".jar.asc" ) );
        }

        return new Record(
                getBackendId(),
                getRepositoryId(),
                doc.has("id") ? doc.get("id").getAsString() : null,
                doc.has("timestamp") ? doc.get("timestamp").getAsLong() : null,
                result);
    }

    private static final JsonPrimitive EC_SOURCE_JAR = new JsonPrimitive("-sources.jar");

    private static final JsonPrimitive EC_JAVADOC_JAR = new JsonPrimitive("-javadoc.jar");

    private static String mayGet(String field, JsonObject object) {
        return object.has(field) ? object.get(field).getAsString() : null;
    }

    private static void mayPut(Map<Field, Object> result, Field fieldName, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return;
        }
        result.put(fieldName, value);
    }
}
