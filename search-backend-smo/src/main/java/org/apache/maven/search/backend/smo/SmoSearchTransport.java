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
package org.apache.maven.search.backend.smo;

import java.io.IOException;
import java.util.Map;

/**
 * A trivial "transport abstraction" to make possible pluggable implementations.
 */
public interface SmoSearchTransport {
    /**
     * This method should issue a HTTP GET requests using {@code serviceUri} and return body payload as {@link String}
     * ONLY if the response was HTTP 200 Ok and there was a payload returned by service. In any other case, it should
     * throw, never return {@code null}. The payload is expected to be {@code application/json}, so client may add
     * headers to request. Also, the payload is expected to be "relatively small" that may be enforced.
     */
    String fetch(String serviceUri, Map<String, String> headers) throws IOException;
}
