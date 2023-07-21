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
package org.apache.maven.search.backend.remoterepository.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.search.MAVEN;
import org.apache.maven.search.Record;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackend;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchResponse;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchTransport;
import org.apache.maven.search.request.Field;
import org.apache.maven.search.support.SearchBackendSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import static java.util.Objects.requireNonNull;

public class RemoteRepositorySearchBackendImpl extends SearchBackendSupport implements RemoteRepositorySearchBackend {
    private static final Map<Field, String> FIELD_TRANSLATION;

    static {
        HashMap<Field, String> map = new HashMap<>();
        map.put(MAVEN.GROUP_ID, "g");
        map.put(MAVEN.ARTIFACT_ID, "a");
        map.put(MAVEN.VERSION, "v");
        map.put(MAVEN.CLASSIFIER, "l");
        map.put(MAVEN.PACKAGING, "p");
        map.put(MAVEN.CLASS_NAME, "c");
        map.put(MAVEN.FQ_CLASS_NAME, "fc");
        map.put(MAVEN.SHA1, "1");
        FIELD_TRANSLATION = Collections.unmodifiableMap(map);
    }

    private final String baseUri;

    private final RemoteRepositorySearchTransport transport;

    private final Map<String, String> commonHeaders;

    private enum State {
        G,
        GA,
        GAV,
        GAVCE,
        GAVCE1
    }

    /**
     * Creates a customized instance of SMO backend, like an in-house instances of SMO or different IDs.
     */
    public RemoteRepositorySearchBackendImpl(
            String backendId, String repositoryId, String baseUri, RemoteRepositorySearchTransport transport) {
        super(backendId, repositoryId);
        this.baseUri = requireNonNull(baseUri);
        this.transport = requireNonNull(transport);

        this.commonHeaders = new HashMap<>();
        this.commonHeaders.put(
                "User-Agent",
                "Apache-Maven-Search-RR/" + discoverVersion() + " "
                        + transport.getClass().getSimpleName());
        this.commonHeaders.put("Accept", "application/json");
    }

    private String discoverVersion() {
        Properties properties = new Properties();
        InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "org/apache/maven/search/backend/smo/internal/remoterepository-version.properties");
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
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public RemoteRepositorySearchResponse search(SearchRequest searchRequest) throws IOException {
        Context context = new Context(searchRequest);
        String uri = baseUri;
        State state = null;
        if (context.getGroupId() != null) {
            uri += context.getGroupId().replace('.', '/') + "/";
            state = State.G;
            if (context.getArtifactId() != null) {
                uri += context.getArtifactId() + "/";
                state = State.GA;
                if (context.getVersion() == null) {
                    uri += "maven-metadata.xml";
                } else {
                    uri += context.getVersion() + "/";
                    state = State.GAV;
                    if (context.getFileExtension() != null) {
                        // we go for actually specified artifact
                        uri += context.getArtifactId() + "-" + context.getVersion();
                        if (context.getClassifier() != null) {
                            uri += "-" + context.getClassifier();
                        }
                        uri += "." + context.getFileExtension();
                        state = State.GAVCE;
                        if (context.getSha1() != null) {
                            state = State.GAVCE1;
                        }
                    }
                }
            }
        }
        if (state == null) {
            throw new IllegalArgumentException("Unsupported Query: " + searchRequest.getQuery());
        }

        Parser parser = state == State.GA ? Parser.xmlParser() : Parser.htmlParser();
        int totalHits = 0;
        List<Record> page = new ArrayList<>(searchRequest.getPaging().getPageSize());
        Document document = null;
        if (state.ordinal() < State.GAVCE.ordinal()) {
            try (RemoteRepositorySearchTransport.Response response = transport.get(uri, commonHeaders)) {
                if (response.getCode() == 200) {
                    document = Jsoup.parse(response.getBody(), StandardCharsets.UTF_8.name(), uri, parser);
                }
            }

            if (document == null) {
                return new RemoteRepositorySearchResponseImpl(searchRequest, 0, Collections.emptyList(), uri, null);
            }

            switch (state) {
                case G:
                    totalHits = populateG(context, document, page);
                    break;
                case GA:
                    totalHits = populateGA(context, document, page);
                    break;
                case GAV:
                    totalHits = populateGAV(context, document, page);
                    break;
                default:
                    throw new IllegalStateException("State" + state); // checkstyle
            }
        } else {
            try (RemoteRepositorySearchTransport.Response response = transport.head(uri, commonHeaders)) {
                if (response.getCode() == 200) {
                    totalHits = populateGAVCE(context, response, context.getSha1(), page);
                }
            }
        }
        return new RemoteRepositorySearchResponseImpl(searchRequest, totalHits, page, uri, document);
    }

    private boolean accept(String href) {
        return !href.contains("..") && !href.startsWith("maven-metadata.xml");
    }

    private int populateG(Context context, Document document, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/
        int result = 0;
        Element contents = document.getElementById("contents");
        if (contents != null) {
            for (Element element : contents.getElementsByTag("a")) {
                String href = element.attr("href");
                if (accept(href)) {
                    String artifactId = href.substring(0, href.length() - 1);
                    page.add(create(context.getGroupId(), artifactId, null, null, null));
                }
            }
        }
        return result;
    }

    private int populateGA(Context context, Document document, List<Record> page) {
        // Maven Metadata XML like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml
        int result = 0;
        Element metadata = document.getElementsByTag("metadata").first();
        if (metadata != null) {
            Element versioning = metadata.getElementsByTag("versioning").first();
            if (versioning != null) {
                Element versions = versioning.getElementsByTag("versions").first();
                if (versions != null) {
                    for (Element version : versions.getElementsByTag("version")) {
                        page.add(create(context.getGroupId(), context.getArtifactId(), version.text(), null, null));
                    }
                }
            }
        }
        return result;
    }

    private int populateGAV(Context context, Document document, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/
        return 0;
    }

    private int populateGAVCE(
            Context context, RemoteRepositorySearchTransport.Response response, String sha1, List<Record> page) {
        // Concrete file like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/search-api-7.0.3.pom
        page.add(create(
                context.getGroupId(),
                context.getArtifactId(),
                context.getVersion(),
                context.getClassifier(),
                context.getFileExtension()));
        return 1;
    }

    private Record create(String groupId, String artifactId, String version, String classifier, String fileExtension) {
        HashMap<Field, Object> result = new HashMap<>();

        mayPut(result, MAVEN.GROUP_ID, groupId);
        mayPut(result, MAVEN.ARTIFACT_ID, artifactId);
        mayPut(result, MAVEN.VERSION, version);
        mayPut(result, MAVEN.CLASSIFIER, classifier);
        mayPut(result, MAVEN.FILE_EXTENSION, fileExtension);
        return new Record(getBackendId(), getRepositoryId(), null, null, result);
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
