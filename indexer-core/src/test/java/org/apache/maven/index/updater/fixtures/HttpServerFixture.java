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
package org.apache.maven.index.updater.fixtures;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HttpServerFixture {

    private static final int BUFFER_SIZE = 64;

    private final Path root;

    private final HttpServer server;

    private final AtomicInteger redirects = new AtomicInteger();

    public HttpServerFixture(final int port, final File root) throws IOException {
        this.root = root.toPath().toRealPath();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", this::serveFile);
    }

    public void addBasicAuthentication(final String path, final String realm, final Map<String, String> credentials) {
        server.createContext(path, this::serveFile).setAuthenticator(new BasicAuthenticator(realm) {
            @Override
            public boolean checkCredentials(final String username, final String password) {
                return password.equals(credentials.get(username));
            }
        });
    }

    public void addRedirectTrap(final String path) {
        server.createContext(path, exchange -> {
            exchange.getResponseHeaders()
                    .set("Location", exchange.getRequestURI().getPath() + "-" + redirects.incrementAndGet());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
    }

    public void addSlowResponse(final String path) {
        server.createContext(path, exchange -> serveFile(exchange, true));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void serveFile(final HttpExchange exchange) throws IOException {
        serveFile(exchange, false);
    }

    private void serveFile(final HttpExchange exchange, final boolean slow) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        Path file =
                root.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, Files.size(file));
        try (InputStream in = Files.newInputStream(file);
                OutputStream out = exchange.getResponseBody()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (slow) {
                    pause();
                }
                out.write(buffer, 0, read);
                out.flush();
            }
        }
    }

    private static void pause() throws IOException {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending a delayed response", e);
        }
    }
}
