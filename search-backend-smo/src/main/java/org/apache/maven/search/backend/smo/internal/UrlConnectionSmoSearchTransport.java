package org.apache.maven.search.backend.smo.internal;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.apache.maven.search.SearchRequest;

/**
 * {@link java.net.HttpURLConnection} backed transport.
 */
public class UrlConnectionSmoSearchTransport extends SmoSearchTransportSupport
{
    @Override
    public String fetch( SearchRequest searchRequest, String serviceUri ) throws IOException
    {
        HttpURLConnection httpConnection = (HttpURLConnection) new URL( serviceUri ).openConnection();
        httpConnection.setInstanceFollowRedirects( false );
        httpConnection.setRequestProperty( "User-Agent", getUserAgent() );
        httpConnection.setRequestProperty( "Accept", "application/json" );
        int httpCode = httpConnection.getResponseCode();
        if ( httpCode == HttpURLConnection.HTTP_OK )
        {
            try ( InputStream inputStream = httpConnection.getInputStream() )
            {
                try ( Scanner scanner = new Scanner( inputStream, StandardCharsets.UTF_8 ) )
                {
                    return scanner.useDelimiter( "\\A" ).next();
                }
            }
        }
        else
        {
            throw new IOException( "Unexpected response code: " + httpCode );
        }
    }
}
