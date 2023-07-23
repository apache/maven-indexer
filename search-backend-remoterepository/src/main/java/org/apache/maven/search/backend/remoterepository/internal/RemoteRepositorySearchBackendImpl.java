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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

/**
 * Implementation of {@link RemoteRepositorySearchBackend} that is tested against Maven Central.
 * All the methods are "loosely encapsulated" (are protected) to enable easy override of any
 * required aspect of this implementation, to suit it against different remote repositories
 * (HTML parsing) if needed.
 */
public class RemoteRepositorySearchBackendImpl extends SearchBackendSupport implements RemoteRepositorySearchBackend {
    private final String baseUri;

    private final RemoteRepositorySearchTransport transport;

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
            String backendId, String repositoryId, String baseUri, RemoteRepositorySearchTransport transport) {
        super(backendId, repositoryId);
        this.baseUri = requireNonNull(baseUri);
        this.transport = requireNonNull(transport);

        this.commonHeaders = Map.of(
                "User-Agent",
                "Apache-Maven-Search-RR/" + discoverVersion() + " "
                        + transport.getClass().getSimpleName(),
                "Accept",
                "application/json");
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
        Document document = null;
        if (state.ordinal() < State.GAVCE.ordinal()) {
            Parser parser = state == State.GA ? Parser.xmlParser() : Parser.htmlParser();
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
                    boolean matches = context.getSha1() == null;
                    if (context.getSha1() != null) {
                        try (RemoteRepositorySearchTransport.Response sha1Response =
                                transport.get(uri + ".sha1", commonHeaders)) {
                            if (response.getCode() == 200) {
                                String remoteSha1 = readChecksum(sha1Response.getBody());
                                matches = Objects.equals(context.getSha1(), remoteSha1);
                            }
                        }
                    }
                    if (matches) {
                        page.add(create(
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

    protected boolean isChecksum(String name) {
        return name.endsWith(".sha1") || name.endsWith(".md5") || name.endsWith(".sha256") || name.endsWith(".sha512");
    }

    protected boolean isSignature(String name) {
        return name.endsWith(".asc") || name.endsWith(".sigstore");
    }

    protected boolean isMetadata(String name) {
        return name.equals("maven-metadata.xml");
    }

    /**
     * Returns {@code true} if the name is not empty, not directory special (".."), is not metadata
     * is not signature and is not checksum. Hence, it should be a name of interest.
     */
    protected boolean accept(String name) {
        return !name.isEmpty() && !name.contains("..") && !isMetadata(name) && !isSignature(name) && !isChecksum(name);
    }

    /**
     * Extracts the "name" from {@code href} attribute. In case of Maven Central, the href
     * attribute contains name in form of {@code "name/"} (followed by slash), if name denotes
     * a directory. The trailing slash is removed by this method, if any.
     * <p>
     * Override this method if needed (parsing different HTML output than Maven Central).
     */
    protected String nameInHref(Element element) {
        String name = element.attr("href");
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Method parsing document out of HTML page like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/</a>
     * <p>
     * Note: this method is "best effort" and may enlist non-existent As (think nested Gs).
     * <p>
     * Override this method if needed (parsing different HTML output than Maven Central).
     */
    protected int populateG(Context context, Document document, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/
        Element contents = document.getElementById("contents");
        if (contents != null) {
            for (Element element : contents.getElementsByTag("a")) {
                String name = nameInHref(element);
                if (accept(name)) {
                    page.add(create(context.getGroupId(), name, null, null, null));
                }
            }
        }
        return page.size();
    }

    /**
     * Method parsing document out of XML Maven Metadata like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml</a>
     */
    protected int populateGA(Context context, Document document, List<Record> page) {
        // Maven Metadata XML like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml
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
        return page.size();
    }

    /**
     * Method parsing document out of HTML page like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/</a>
     * <p>
     * Note: this method is "best effort" and may enlist fake artifacts.
     * <p>
     * Override this method if needed (parsing different HTML output than Maven Central).
     */
    protected int populateGAV(Context context, Document document, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/
        Element contents = document.getElementById("contents");
        if (contents != null) {
            for (Element element : contents.getElementsByTag("a")) {
                // skip possible subdirectories and files without extensions
                String name = element.attr("href");
                if (name.endsWith("/") || !name.contains(".")) {
                    continue;
                }
                name = nameInHref(element);
                if (accept(name)) {
                    if (name.startsWith(context.getArtifactId())) {
                        name = name.substring(context.getArtifactId().length() + 1);
                        if (name.startsWith(context.getVersion())) {
                            name = name.substring(context.getVersion().length() + 1);
                            String ext = null;
                            String classifier = null;
                            if (name.contains(".")) {
                                while (name.contains(".")) {
                                    if (ext == null) {
                                        ext = name.substring(name.lastIndexOf('.') + 1);
                                    } else {
                                        ext = name.substring(name.lastIndexOf('.') + 1) + "." + ext;
                                    }
                                    name = name.substring(0, name.lastIndexOf('.'));
                                }
                                classifier = name.isEmpty() ? null : name;
                            } else {
                                ext = name;
                            }
                            page.add(create(
                                    context.getGroupId(),
                                    context.getArtifactId(),
                                    context.getVersion(),
                                    classifier,
                                    ext));
                        }
                    }
                }
            }
        }
        return page.size();
    }

    /**
     * Creates a {@link Record} instance using passed in field values. All field values except
     * {@code groupId} are optional (nullable).
     */
    protected Record create(
            String groupId, String artifactId, String version, String classifier, String fileExtension) {
        HashMap<Field, Object> result = new HashMap<>();

        mayPut(result, MAVEN.GROUP_ID, groupId);
        mayPut(result, MAVEN.ARTIFACT_ID, artifactId);
        mayPut(result, MAVEN.VERSION, version);
        mayPut(result, MAVEN.CLASSIFIER, classifier);
        mayPut(result, MAVEN.FILE_EXTENSION, fileExtension);
        return new Record(getBackendId(), getRepositoryId(), null, null, result);
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
                if (line.length() > 0) {
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

    private static void mayPut(Map<Field, Object> result, Field fieldName, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String && ((String) value).isBlank()) {
            return;
        }
        result.put(fieldName, value);
    }
}
