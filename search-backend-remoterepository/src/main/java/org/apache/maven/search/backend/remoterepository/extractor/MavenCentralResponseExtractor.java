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

import org.apache.maven.search.api.Record;
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
     * attribute contains name in relative form as {@code "name/"} (followed by slash), if name denotes
     * a directory. The trailing slash is removed by this method, if any.
     */
    protected String nameInHref(Element element) {
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
                    page.add(recordFactory.create(context.getGroupId(), name, null, null, null, null));
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
                populateGAVName(context, nameInHref(element), recordFactory, page);
            }
        }
        return page.size();
    }
}
