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
package org.apache.maven.index.reader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.util.Objects.requireNonNull;

/**
 * A trivial HTTP {@link ResourceHandler} that uses {@link URI} to fetch remote content. This implementation does not
 * handle any advanced cases, like redirects, authentication, etc.
 */
public class HttpResourceHandler implements ResourceHandler {
    private final HttpClient client =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final URI root;

    public HttpResourceHandler(final URI root) {
        this.root = requireNonNull(root);
    }

    @Override
    public Resource locate(final String name) {
        return new HttpResource(name);
    }

    private class HttpResource implements Resource {
        private final String name;

        private HttpResource(final String name) {
            this.name = name;
        }

        @Override
        public InputStream read() throws IOException {
            HttpRequest request =
                    HttpRequest.newBuilder().uri(root.resolve(name)).GET().build();
            try {
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                    return response.body();
                } else {
                    throw new IOException("Unexpected response: " + response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
        }
    }
}
