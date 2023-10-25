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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.search.Record;
import org.apache.maven.search.SearchRequest;
import org.apache.maven.search.backend.remoterepository.Context;
import org.apache.maven.search.backend.remoterepository.RecordFactory;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackend;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchResponse;
import org.apache.maven.search.backend.remoterepository.ResponseExtractor;
import org.apache.maven.search.support.SearchBackendSupport;
import org.apache.maven.search.transport.Transport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link RemoteRepositorySearchBackend} that is tested against Maven Central.
 * All the methods are "loosely encapsulated" (are protected) to enable easy override of any
 * required aspect of this implementation, to suit it against different remote repositories
 * (HTML parsing) if needed.
 */
public class RemoteRepositorySearchBackendImpl extends SearchBackendSupport implements RemoteRepositorySearchBackend {
    private final String baseUri;

    private final Transport transport;

    private final ResponseExtractor responseExtractor;

    private final Map<String, String> commonHeaders;

    protected enum State {
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
            String backendId,
            String repositoryId,
            String baseUri,
            Transport transport,
            ResponseExtractor responseExtractor) {
        super(backendId, repositoryId);
        this.baseUri = requireNonNull(baseUri);
        this.transport = requireNonNull(transport);
        this.responseExtractor = requireNonNull(responseExtractor);

        this.commonHeaders = Map.of(
                "User-Agent",
                "Apache-Maven-Search-RR/" + discoverVersion() + " "
                        + transport.getClass().getSimpleName());
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

        int totalHits = 0;
        List<Record> page = new ArrayList<>(searchRequest.getPaging().getPageSize());
        RecordFactory recordFactory = new RecordFactory(this);
        Document document = null;
        if (state.ordinal() < State.GAVCE.ordinal()) {
            Parser parser = state == State.GA ? Parser.xmlParser() : Parser.htmlParser();
            try (Transport.Response response = transport.get(uri, commonHeaders)) {
                if (response.getCode() == 200) {
                    document = Jsoup.parse(response.getBody(), StandardCharsets.UTF_8.name(), uri, parser);
                }
            }

            if (document == null) {
                throw new IOException("Unexpected response from: " + uri);
            }

            switch (state) {
                case G:
                    totalHits = responseExtractor.populateG(context, document, recordFactory, page);
                    break;
                case GA:
                    totalHits = responseExtractor.populateGA(context, document, recordFactory, page);
                    break;
                case GAV:
                    totalHits = responseExtractor.populateGAV(context, document, recordFactory, page);
                    break;
                default:
                    throw new IllegalStateException("State" + state); // checkstyle
            }
        } else {
            try (Transport.Response response = transport.head(uri, commonHeaders)) {
                if (response.getCode() == 200) {
                    boolean matches = context.getSha1() == null;
                    if (context.getSha1() != null) {
                        try (Transport.Response sha1Response = transport.get(uri + ".sha1", commonHeaders)) {
                            if (response.getCode() == 200) {
                                try (InputStream body = sha1Response.getBody()) {
                                    String remoteSha1 = readChecksum(body);
                                    matches = Objects.equals(context.getSha1(), remoteSha1);
                                }
                            }
                        }
                    }
                    if (matches) {
                        page.add(recordFactory.create(
                                context.getGroupId(),
                                context.getArtifactId(),
                                context.getVersion(),
                                context.getClassifier(),
                                context.getFileExtension()));
                        totalHits = 1;
                    }
                }
            }
        }
        return new RemoteRepositorySearchResponseImpl(searchRequest, totalHits, page, uri, document);
    }

    private static String readChecksum(InputStream inputStream) throws IOException {
        String checksum = "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8), 512)) {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    checksum = line;
                    break;
                }
            }
        }

        if (checksum.matches(".+= [0-9A-Fa-f]+")) {
            int lastSpacePos = checksum.lastIndexOf(' ');
            checksum = checksum.substring(lastSpacePos + 1);
        } else {
            int spacePos = checksum.indexOf(' ');

            if (spacePos != -1) {
                checksum = checksum.substring(0, spacePos);
            }
        }

        return checksum;
    }
}
