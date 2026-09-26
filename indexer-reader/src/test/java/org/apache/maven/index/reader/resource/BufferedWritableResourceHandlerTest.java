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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.index.reader.WritableResourceHandler;
import org.apache.maven.index.reader.WritableResourceHandler.WritableResource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BufferedWritableResourceHandlerTest {

    @Test
    void locate() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableResource writableResource = new WritableResource() {
            @Override
            public OutputStream write() {
                return baos;
            }

            @Override
            public InputStream read() {
                throw new UnsupportedOperationException();
            }
        };
        OutputStream out = new BufferedWritableResourceHandler(
                        new SingleWritableResourceHandler("test.txt", writableResource))
                .locate("test.txt")
                .write();
        assertInstanceOf(BufferedOutputStream.class, out);
        assertArrayEquals(new byte[] {}, baos.toByteArray());
        out.write('a');
        assertArrayEquals(new byte[] {}, baos.toByteArray());
        out.flush();
        assertArrayEquals(new byte[] {'a'}, baos.toByteArray());
    }

    @Test
    void close() throws Exception {
        SingleWritableResourceHandler handler = new SingleWritableResourceHandler("test.txt", null);
        new BufferedWritableResourceHandler(handler).close();
        assertTrue(handler.closed);
    }

    /** Serves one named writable resource and records whether it was closed. */
    private static class SingleWritableResourceHandler implements WritableResourceHandler {
        private final String name;
        private final WritableResource resource;
        boolean closed;

        SingleWritableResourceHandler(String name, WritableResource resource) {
            this.name = name;
            this.resource = resource;
        }

        @Override
        public WritableResource locate(String name) {
            assertEquals(this.name, name);
            return resource;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
