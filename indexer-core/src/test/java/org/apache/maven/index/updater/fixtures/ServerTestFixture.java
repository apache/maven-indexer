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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class ServerTestFixture {

    private static final String SERVER_ROOT_RESOURCE_PATH = "index-updater/server-root";

    private static final String SIXTY_TWO_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static final String LONG_PASSWORD = SIXTY_TWO_CHARS + SIXTY_TWO_CHARS;

    private final HttpServerFixture server;

    public ServerTestFixture(final int port) throws Exception {
        server = new HttpServerFixture(port, getBase());
        server.addBasicAuthentication(
                "/protected/", "POC Server", Map.of("user", "password", "longuser", LONG_PASSWORD));
        server.addSlowResponse("/slow/");
        server.addRedirectTrap("/redirect-trap/");
        server.start();
    }

    private static File getBase() throws URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(SERVER_ROOT_RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Cannot find classpath resource: " + SERVER_ROOT_RESOURCE_PATH);
        }

        return new File(resource.toURI().normalize());
    }

    public void stop() {
        server.stop();
    }
}
