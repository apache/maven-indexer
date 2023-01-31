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
package org.apache.maven.search.backend.smo.internal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.apache.maven.search.backend.smo.SmoSearchTransport;

/**
 * Java 11 {@link HttpClient} backed transport.
 */
public class Java11HttpClientSmoSearchTransport implements SmoSearchTransport
{
    private final HttpClient client = HttpClient.newBuilder().followRedirects( HttpClient.Redirect.NEVER ).build();

    @Override
    public String fetch( String serviceUri, Map<String, String> headers ) throws IOException
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri( URI.create( serviceUri ) ).GET();
        for ( Map.Entry<String, String> header : headers.entrySet() )
        {
            builder.header( header.getKey(), header.getValue() );
        }
        HttpRequest request = builder.build();
        try
        {
            HttpResponse<String> response = client.send( request, HttpResponse.BodyHandlers.ofString() );
            if ( response.statusCode() == HttpURLConnection.HTTP_OK )
            {
                return response.body();
            }
            else
            {
                throw new IOException( "Unexpected response: " + response );
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new IOException( e );
        }
    }
}
