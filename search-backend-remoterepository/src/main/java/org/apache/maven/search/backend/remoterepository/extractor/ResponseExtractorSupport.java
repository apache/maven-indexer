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
package org.apache.maven.search.backend.remoterepository.extractor;

import java.util.List;

import org.apache.maven.search.Record;
import org.apache.maven.search.backend.remoterepository.Context;
import org.apache.maven.search.backend.remoterepository.RecordFactory;
import org.apache.maven.search.backend.remoterepository.ResponseExtractor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * A support class for extractors.
 */
public abstract class ResponseExtractorSupport implements ResponseExtractor {
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
     * This is Maven metadata parsing, is NOT remote end specific, unlike HTML parsing.
     */
    @Override
    public int populateGA(Context context, Document document, RecordFactory recordFactory, List<Record> page) {
        // Maven Metadata XML like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml
        Element metadata = document.getElementsByTag("metadata").first();
        if (metadata != null) {
            Element versioning = metadata.getElementsByTag("versioning").first();
            if (versioning != null) {
                Element versions = versioning.getElementsByTag("versions").first();
                if (versions != null) {
                    for (Element version : versions.getElementsByTag("version")) {
                        page.add(recordFactory.create(
                                context.getGroupId(), context.getArtifactId(), version.text(), null, null));
                    }
                }
            }
        }
        return page.size();
    }
}
