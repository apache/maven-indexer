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
package org.apache.maven.search.backend.remoterepository;

import java.util.List;

import org.apache.maven.search.Record;
import org.jsoup.nodes.Document;

/**
 * A component extracting data from response, that is aware of any remote specifics (like HTML structure).
 */
public interface ResponseExtractor {
    /**
     * Method parsing document out of HTML page like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/</a>
     * <p>
     * Note: this method is "best effort" and may enlist non-existent As (think nested Gs).
     */
    int populateG(Context context, Document document, RecordFactory recordFactory, List<Record> page);

    /**
     * Method parsing document out of XML Maven Metadata like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/maven-metadata.xml</a>
     */
    int populateGA(Context context, Document document, RecordFactory recordFactory, List<Record> page);

    /**
     * Method parsing document out of HTML page like this one:
     * <a href="https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/">https://repo.maven.apache.org/maven2/org/apache/maven/indexer/search-api/7.0.3/</a>
     * <p>
     * Note: this method is "best effort" and may enlist fake artifacts.
     */
    int populateGAV(Context context, Document document, RecordFactory recordFactory, List<Record> page);
}
