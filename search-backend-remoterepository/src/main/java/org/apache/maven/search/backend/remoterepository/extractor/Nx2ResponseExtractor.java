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
import org.jsoup.select.Elements;

/**
 * Extractor for Sonatype Nexus2.
 */
public class Nx2ResponseExtractor extends ResponseExtractorSupport {
    protected boolean accept(String name) {
        return !"Parent Directory".equals(name) && super.accept(name);
    }

    private String name(Element element) {
        String name = element.text();
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    @Override
    public int populateG(Context context, Document document, RecordFactory recordFactory, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/
        Elements elements = document.getElementsByTag("a");
        for (Element element : elements) {
            String name = name(element);
            if (accept(name)) {
                page.add(recordFactory.create(context.getGroupId(), name, null, null, null));
            }
        }
        return page.size();
    }

    @Override
    public int populateGAV(Context context, Document document, RecordFactory recordFactory, List<Record> page) {
        // Index HTML page like this one:
        // https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/
        Elements elements = document.getElementsByTag("a");
        for (Element element : elements) {
            // skip possible subdirectories and files without extensions
            String name = element.attr("href");
            if (name.endsWith("/") || !name.contains(".")) {
                continue;
            }
            populateGAVName(context, name(element), recordFactory, page);
        }
        return page.size();
    }
}
