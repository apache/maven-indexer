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
package org.apache.maven.index.reader.resource;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.maven.index.reader.ResourceHandler;
import org.apache.maven.index.reader.ResourceHandler.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BufferedResourceHandlerTest {

    @Test
    void locate() throws Exception {
        Resource resource = () -> new ByteArrayInputStream(new byte[] {'a'});
        InputStream in = new BufferedResourceHandler(new SingleResourceHandler("test.txt", resource))
                .locate("test.txt")
                .read();
        assertInstanceOf(BufferedInputStream.class, in);
        assertEquals('a', in.read());
    }

    @Test
    void locateNull() throws Exception {
        Resource resource = () -> null;
        assertNull(new BufferedResourceHandler(new SingleResourceHandler("test.txt", resource))
                .locate("test.txt")
                .read());
    }

    @Test
    void close() throws Exception {
        SingleResourceHandler resourceHandler = new SingleResourceHandler("test.txt", null);
        new BufferedResourceHandler(resourceHandler).close();
        assertTrue(resourceHandler.closed);
    }

    /** Serves one named resource and records whether it was closed. */
    private static class SingleResourceHandler implements ResourceHandler {
        private final String name;
        private final Resource resource;
        boolean closed;

        SingleResourceHandler(String name, Resource resource) {
            this.name = name;
            this.resource = resource;
        }

        @Override
        public Resource locate(String name) {
            assertEquals(this.name, name);
            return resource;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
