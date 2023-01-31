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
package org.apache.maven.index;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.maven.index.context.IndexingContext;

/**
 * A Search Request that will perform the new "iterator-like" type of search. It is pageable as plus, but on downside,
 * it's corresponding result does not result size in advance.
 *
 * @author cstamas
 */
public class IteratorSearchRequest extends AbstractSearchPageableRequest {
    public IteratorSearchRequest(Query query) {
        this(query, null, null);
    }

    public IteratorSearchRequest(Query query, ArtifactInfoFilter filter) {
        this(query, null, filter);
    }

    public IteratorSearchRequest(Query query, IndexingContext context) {
        this(query, context != null ? Arrays.asList(context) : null, null);
    }

    public IteratorSearchRequest(Query query, List<IndexingContext> contexts) {
        this(query, contexts, null);
    }

    public IteratorSearchRequest(Query query, List<IndexingContext> contexts, ArtifactInfoFilter filter) {
        super(query, contexts);

        setArtifactInfoFilter(filter);
    }
}
