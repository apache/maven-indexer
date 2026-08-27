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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class ServerTestFixture {

    private static final String SERVER_ROOT_RESOURCE_PATH = "index-updater/server-root";

    private static final int BUFFER_SIZE = 64;

    private static final long CHUNK_DELAY_MILLIS = 1000;

    private final Path base;

    private final HttpServer server;

    private final AtomicInteger redirectionCount = new AtomicInteger();

    public ServerTestFixture(final int port) throws Exception {
        this(port, getBase());
    }

    public ServerTestFixture(final int port, final Path base) throws IOException {
        this.base = base.toAbsolutePath().normalize();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/slow/", this::serveSlowly);
        server.createContext("/redirect-trap/", this::redirect);
        server.createContext("/", this::serve);
        server.start();
    }

    private static Path getBase() throws URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(SERVER_ROOT_RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Cannot find classpath resource: " + SERVER_ROOT_RESOURCE_PATH);
        }

        return Paths.get(resource.toURI()).normalize();
    }

    public void stop() {
        server.stop(0);
    }

    private void serve(final HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }

        Path file = resolve(exchange.getRequestURI());
        if (file == null || !Files.isRegularFile(file)) {
            sendEmptyResponse(exchange, 404);
            return;
        }

        exchange.sendResponseHeaders(200, Files.size(file));
        try (OutputStream out = exchange.getResponseBody()) {
            Files.copy(file, out);
        }
    }

    private void serveSlowly(final HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }

        Path file = resolve(exchange.getRequestURI());
        if (file == null || !Files.isRegularFile(file)) {
            sendEmptyResponse(exchange, 404);
            return;
        }

        exchange.sendResponseHeaders(200, 0);
        try (InputStream in = Files.newInputStream(file);
                OutputStream out = exchange.getResponseBody()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) > -1) {
                try {
                    Thread.sleep(CHUNK_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while serving a slow response", e);
                }
                out.write(buffer, 0, read);
            }
        }
    }

    private void redirect(final HttpExchange exchange) throws IOException {
        if (!requireGet(exchange)) {
            return;
        }

        URI requestUri = exchange.getRequestURI();
        String location = requestUri.getRawPath() + "-" + redirectionCount.incrementAndGet();
        if (requestUri.getRawQuery() != null) {
            location += "?" + requestUri.getRawQuery();
        }

        exchange.getResponseHeaders().set("Location", location);
        sendEmptyResponse(exchange, 302);
    }

    private Path resolve(final URI requestUri) {
        String requestPath = requestUri.getPath();
        if (requestPath == null || !requestPath.startsWith("/")) {
            return null;
        }

        Path resolved = base.resolve(requestPath.substring(1)).normalize();
        return resolved.startsWith(base) ? resolved : null;
    }

    private static boolean requireGet(final HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            return true;
        }

        exchange.getResponseHeaders().set("Allow", "GET");
        sendEmptyResponse(exchange, 405);
        return false;
    }

    private static void sendEmptyResponse(final HttpExchange exchange, final int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
