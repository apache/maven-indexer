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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchTransport;

/**
 * {@link HttpURLConnection} backed transport.
 */
public class RemoteRepositorySearchTransportSupport implements RemoteRepositorySearchTransport {
    protected static class ResponseImpl implements Response {
        private final int code;
        private final Map<String, String> headers;
        private final InputStream inputStream;

        protected ResponseImpl(int code, Map<String, String> headers, InputStream inputStream) {
            this.code = code;
            this.headers = headers;
            this.inputStream = inputStream;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public Map<String, String> getHeaders() {
            return headers;
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
        HttpURLConnection httpConnection = (HttpURLConnection) new URL(serviceUri).openConnection();
        httpConnection.setInstanceFollowRedirects(false);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        int code = httpConnection.getResponseCode();
        Map<String, String> hdr = httpConnection.getHeaderFields().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().get(0)))
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        return new ResponseImpl(code, hdr, code == 200 ? httpConnection.getInputStream() : null);
    }
}
