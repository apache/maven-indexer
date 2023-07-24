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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Extractor for Maven Central.
 */
public class MavenCentralResponseExtractor extends ResponseExtractorSupport {
    /**
     * Extracts the "name" from {@code href} attribute. In case of Maven Central, the href
     * attribute contains name in realative form as {@code "name/"} (followed by slash), if name denotes
     * a directory. The trailing slash is removed by this method, if any.
     */
    private String nameInHref(Element element) {
        String name = element.attr("href");
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    @Override
    public int populateG(Context context, Document document, RecordFactory recordFactory, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/
        Element contents = document.getElementById("contents");
        if (contents != null) {
            for (Element element : contents.getElementsByTag("a")) {
                String name = nameInHref(element);
                if (accept(name)) {
                    page.add(recordFactory.create(context.getGroupId(), name, null, null, null));
                }
            }
        }
        return page.size();
    }

    @Override
    public int populateGAV(Context context, Document document, RecordFactory recordFactory, List<Record> page) {
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
                            page.add(recordFactory.create(
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
}
