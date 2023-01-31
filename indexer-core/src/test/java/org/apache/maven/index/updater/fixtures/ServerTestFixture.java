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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;

public class ServerTestFixture {

    private static final String SERVER_ROOT_RESOURCE_PATH = "index-updater/server-root";

    private static final String SIXTY_TWO_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static final String LONG_PASSWORD = SIXTY_TWO_CHARS + SIXTY_TWO_CHARS;

    private final Server server;

    public ServerTestFixture(final int port) throws Exception {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);

        server.setConnectors(new Connector[] {connector});

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);

        constraint.setRoles(new String[] {"allowed"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/protected/*");

        UserStore userStore = new UserStore();
        userStore.addUser("user", new Password("password"), new String[] {"allowed"});
        userStore.addUser("longuser", new Password(LONG_PASSWORD), new String[] {"allowed"});
        HashLoginService hls = new HashLoginService("POC Server");
        hls.setUserStore(userStore);

        ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
        sh.setAuthenticator(new BasicAuthenticator());
        sh.setLoginService(hls);
        sh.setConstraintMappings(new ConstraintMapping[] {cm});

        WebAppContext ctx = new WebAppContext();
        ctx.setContextPath("/");

        File base = getBase();
        ctx.setWar(base.getAbsolutePath());
        ctx.setSecurityHandler(sh);

        ctx.getServletHandler().addServletWithMapping(TimingServlet.class, "/slow/*");
        ctx.getServletHandler().addServletWithMapping(InfiniteRedirectionServlet.class, "/redirect-trap/*");

        SessionHandler sessionHandler = ctx.getSessionHandler();
        sessionHandler.setUsingCookies(true);

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {ctx, new DefaultHandler()});

        server.setHandler(handlers);
        server.start();
    }

    private static File getBase() throws URISyntaxException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(SERVER_ROOT_RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException("Cannot find classpath resource: " + SERVER_ROOT_RESOURCE_PATH);
        }

        return new File(resource.toURI().normalize());
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    public static final class TimingServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
            String basePath = req.getServletPath();
            String subPath = req.getRequestURI().substring(basePath.length());

            File base;
            try {
                base = getBase();
            } catch (URISyntaxException e) {
                resp.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Cannot find server document root in classpath: " + SERVER_ROOT_RESOURCE_PATH);
                return;
            }

            File f = new File(base, "slow" + subPath);
            try (InputStream in = new FileInputStream(f)) {
                OutputStream out = resp.getOutputStream();

                int read;
                byte[] buf = new byte[64];
                while ((read = in.read(buf)) > -1) {
                    System.out.println("Sending " + read + " bytes (after pausing 1 seconds)...");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    out.write(buf, 0, read);
                }

                out.flush();
            }
        }
    }

    public static final class InfiniteRedirectionServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

        static int redirCount = 0;

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
            String path = req.getServletPath();
            String subPath = req.getRequestURI().substring(path.length());

            path += subPath + "-" + (++redirCount);
            resp.sendRedirect(path);
        }
    }
}
