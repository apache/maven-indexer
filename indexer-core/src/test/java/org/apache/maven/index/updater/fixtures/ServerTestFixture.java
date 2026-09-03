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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

public class ServerTestFixture {

    private static final String SERVER_ROOT_RESOURCE_PATH = "index-updater/server-root";

    private static final int BUFFER_SIZE = 64;

    private static final long CHUNK_DELAY_MILLIS = 1000;

    private final Path base;

    private final Server server;

    private final AtomicInteger redirectionCount = new AtomicInteger();

    public ServerTestFixture(final int port) throws Exception {
        this(port, getBase());
    }

    public ServerTestFixture(final int port, final Path base) throws Exception {
        this.base = base.toAbsolutePath().normalize();
        server = new Server(port);
        server.setHandler(new FixtureHandler());
        server.start();
    }

    private static Path getBase() throws URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(SERVER_ROOT_RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Cannot find classpath resource: " + SERVER_ROOT_RESOURCE_PATH);
        }

        return Paths.get(resource.toURI()).normalize();
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    private boolean serve(final String requestPath, final Response response, final Callback callback)
            throws IOException {
        Path file = resolve(requestPath);
        if (file == null || !Files.isRegularFile(file)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            callback.succeeded();
            return true;
        }

        response.setStatus(HttpStatus.OK_200);
        response.getHeaders().put(HttpHeader.CONTENT_LENGTH, Files.size(file));
        OutputStream out = Content.Sink.asOutputStream(response);
        Files.copy(file, out);
        out.flush();
        callback.succeeded();
        return true;
    }

    private boolean serveSlowly(final String requestPath, final Response response, final Callback callback)
            throws IOException {
        Path file = resolve(requestPath);
        if (file == null || !Files.isRegularFile(file)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            callback.succeeded();
            return true;
        }

        response.setStatus(HttpStatus.OK_200);
        try (InputStream in = Files.newInputStream(file)) {
            OutputStream out = Content.Sink.asOutputStream(response);
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
            out.flush();
        }
        callback.succeeded();
        return true;
    }

    private Path resolve(final String requestPath) {
        Path resolved = base.resolve(requestPath.substring(1)).normalize();
        return resolved.startsWith(base) ? resolved : null;
    }

    private final class FixtureHandler extends Handler.Abstract {
        @Override
        public boolean handle(final Request request, final Response response, final Callback callback)
                throws Exception {
            String requestPath = Request.getPathInContext(request);
            if (requestPath.startsWith("/slow/")) {
                return serveSlowly(requestPath, response, callback);
            }
            if (requestPath.startsWith("/redirect-trap/")) {
                String location = requestPath + "-" + redirectionCount.incrementAndGet();
                Response.sendRedirect(request, response, callback, HttpStatus.MOVED_TEMPORARILY_302, location, false);
                return true;
            }
            return serve(requestPath, response, callback);
        }
    }
}
