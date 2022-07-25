package org.apache.maven.index.reader.resource;

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

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

import org.apache.maven.index.reader.ResourceHandler.Resource;

/**
 * A {@link Resource} that represents a {@link URL}.
 */
public class UrlResource implements Resource
{
    private final URL url;

    public UrlResource( URL url )
    {
        Objects.requireNonNull( url, "url cannot be null" );
        this.url = url;
    }

    @Override
    public InputStream read() throws IOException
    {
        try
        {
            return new RetryInputStream(url);
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
    }

    private class RetryInputStream extends FilterInputStream {
        private long count;

        @SuppressWarnings("UnstableApiUsage")
        protected RetryInputStream(URL url) throws IOException {
            super(new CountingInputStream(url.openStream()));
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (Exception e) {
                tryReconnect(e);
                return super.read();
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            try {
                return super.read(b);
            } catch (Exception e) {
                tryReconnect(e);
                return super.read(b);
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return super.read(b, off, len);
            } catch (Exception e) {
                tryReconnect(e);
                return super.read(b, off, len);
            }
        }

        @SuppressWarnings("UnstableApiUsage")
        private void tryReconnect(Exception se) throws IOException {
            in.close();
            count += ((CountingInputStream) in).getCount();
            try {
                var conn = url.openConnection();
                conn.addRequestProperty("Range", "bytes=" + count + "-");
                in = new CountingInputStream(conn.getInputStream());
                log.info("Handled disconnect at {} bytes; Content-Range: {}", count, conn.getHeaderField("Content-Range"), se);
            } catch (Exception e) {
                e.addSuppressed(se);
                throw e;
            }
        }
    }
}
