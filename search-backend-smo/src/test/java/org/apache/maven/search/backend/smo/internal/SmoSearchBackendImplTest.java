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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.request.BooleanQuery;
import org.apache.maven.search.api.request.FieldQuery;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.backend.smo.SmoSearchBackend;
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory;
import org.apache.maven.search.backend.smo.SmoSearchResponse;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("This is not a test, is more a showcase")
public class SmoSearchBackendImplTest {
    private final SmoSearchBackend backend = new SmoSearchBackendFactory().createDefault();

    private void dumpSingle(AtomicInteger counter, List<Record> page) {
        for (Record record : page) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getValue(MAVEN.GROUP_ID))
                    .append(":")
                    .append(record.getValue(MAVEN.ARTIFACT_ID))
                    .append(":")
                    .append(record.getValue(MAVEN.VERSION));
            if (record.hasField(MAVEN.PACKAGING)) {
                if (record.hasField(MAVEN.CLASSIFIER)) {
                    sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
                }
                sb.append(":").append(record.getValue(MAVEN.PACKAGING));
            }

            List<String> remarks = new ArrayList<>();
            if (record.getLastUpdated() != null) {
                remarks.add("lastUpdate=" + Instant.ofEpochMilli(record.getLastUpdated()));
            }
            if (record.hasField(MAVEN.VERSION_COUNT)) {
                remarks.add("versionCount=" + record.getValue(MAVEN.VERSION_COUNT));
            }
            if (record.hasField(MAVEN.HAS_SOURCE)) {
                remarks.add("hasSource=" + record.getValue(MAVEN.HAS_SOURCE));
            }
            if (record.hasField(MAVEN.HAS_JAVADOC)) {
                remarks.add("hasJavadoc=" + record.getValue(MAVEN.HAS_JAVADOC));
            }

            System.out.print(counter.incrementAndGet() + ". " + sb);
            if (!remarks.isEmpty()) {
                System.out.print(" " + remarks);
            }
            System.out.println();
        }
    }

    private void dumpPage(SmoSearchResponse searchResponse) throws IOException {
        AtomicInteger counter = new AtomicInteger(0);
        System.out.println(
                "QUERY: " + searchResponse.getSearchRequest().getQuery().toString());
        System.out.println("URL: " + searchResponse.getSearchUri());
        dumpSingle(counter, searchResponse.getPage());
        while (searchResponse.getTotalHits() > searchResponse.getCurrentHits()) {
            System.out.println("NEXT PAGE (size "
                    + searchResponse.getSearchRequest().getPaging().getPageSize() + ")");
            searchResponse = backend.search(searchResponse.getSearchRequest().nextPage());
            dumpSingle(counter, searchResponse.getPage());
            if (counter.get() > 50) {
                System.out.println("ABORTED TO NOT SPAM");
                break; // do not spam the SMO service
            }
        }
        System.out.println();
    }

    @Test
    public void smoke() throws IOException {
        SearchRequest searchRequest = new SearchRequest(Query.query("smoke"));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void g() throws IOException {
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void ga() throws IOException {
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin")));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void gav() throws IOException {
        SearchRequest searchRequest = new SearchRequest(BooleanQuery.and(
                FieldQuery.fieldQuery(MAVEN.GROUP_ID, "org.apache.maven.plugins"),
                FieldQuery.fieldQuery(MAVEN.ARTIFACT_ID, "maven-clean-plugin"),
                FieldQuery.fieldQuery(MAVEN.VERSION, "3.1.0")));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void sha1() throws IOException {
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.SHA1, "8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12"));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void cn() throws IOException {
        SearchRequest searchRequest =
                new SearchRequest(FieldQuery.fieldQuery(MAVEN.CLASS_NAME, "MavenRepositorySystem"));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }

    @Test
    public void fqcn() throws IOException {
        SearchRequest searchRequest = new SearchRequest(
                FieldQuery.fieldQuery(MAVEN.FQ_CLASS_NAME, "org.apache.maven.bridge.MavenRepositorySystem"));
        SmoSearchResponse searchResponse = backend.search(searchRequest);
        System.out.println("TOTAL HITS: " + searchResponse.getTotalHits());
        dumpPage(searchResponse);
    }
}
