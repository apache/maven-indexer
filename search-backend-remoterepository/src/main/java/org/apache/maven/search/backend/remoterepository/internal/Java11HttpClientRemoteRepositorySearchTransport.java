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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchTransport;

import static java.util.Objects.requireNonNull;

/**
 * Java 11 {@link HttpClient} backed transport.
 */
public class Java11HttpClientRemoteRepositorySearchTransport implements RemoteRepositorySearchTransport {
    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private static class ResponseImpl implements Response {

        private final HttpResponse<?> response;

        private final InputStream inputStream;

        private ResponseImpl(HttpResponse<?> response, InputStream inputStream) {
            this.response = requireNonNull(response);
            this.inputStream = inputStream;
        }

        @Override
        public int getCode() {
            return response.statusCode();
        }

        @Override
        public Map<String, String> getHeaders() {
            return response.headers().map().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(
                            e.getKey(), e.getValue().get(0)))
                    .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        }

        @Override
        public InputStream getBody() {
            return inputStream;
        }

        @Override
        public void close() throws IOException {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    @Override
    public Response get(String serviceUri, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder().uri(URI.create(serviceUri)).GET();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            return new ResponseImpl(response, response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public Response head(String serviceUri, Map<String, String> headers) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serviceUri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        HttpRequest request = builder.build();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return new ResponseImpl(response, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
