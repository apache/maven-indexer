package org.apache.maven.search.backend.smo;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.search.SearchRequest;

/**
 * A trivial "transport abstraction" to make possible pluggable implementations.
 */
public abstract class SmoSearchTransportSupport
{
    private final String clientVersion;

    public SmoSearchTransportSupport()
    {
        this.clientVersion = discoverVersion();
    }

    private String discoverVersion()
    {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream( "smo-version.properties" );
        if ( inputStream != null )
        {
            try ( InputStream is = inputStream )
            {
                properties.load( is );
            }
            catch ( IOException e )
            {
                // fall through
            }
        }
        return properties.getProperty( "version", "unknown" );
    }

    /**
     * Exposes this backend version, for example to be used in HTTP {@code User-Agent} string, never {@code null}.
     */
    protected String getClientVersion()
    {
        return clientVersion;
    }

    /**
     * Exposes full HTTP {@code User-Agent} string ready to be used by HTTP clients, never {@code null}.
     */
    protected String getUserAgent()
    {
        return "Apache Search SMO/" + getClientVersion();
    }

    /**
     * This method should issue a HTTP GET requests using {@code serviceUri} and return body payload as {@link String}
     * ONLY if the response was HTTP 200 Ok and there was a payload returned by service. In any other case, it should
     * throw, never return {@code null}. The payload is expected to be {@code application/json}, so client may add
     * headers to request. Also, the payload is expected to be "relatively small".
     */
    public abstract String fetch( SearchRequest searchRequest, String serviceUri ) throws IOException;
}
