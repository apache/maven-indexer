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

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.maven.index.context.IndexingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractNexusIndexerTest extends AbstractIndexCreatorHelper {
    protected NexusIndexer nexusIndexer;

    protected Directory indexDir = new ByteBuffersDirectory();

    protected IndexingContext context;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        //        indexDir = new SimpleFSDirectory(new File("/tmp/nexus-test"));
        super.setUp();
        // FileUtils.deleteDirectory( indexDir );
        nexusIndexer = lookup(NexusIndexer.class);
        prepareNexusIndexer(nexusIndexer);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        unprepareNexusIndexer(nexusIndexer);
        super.tearDown();
        // TODO: Brian reported, does not work on Windows because of left open files?
        // FileUtils.deleteDirectory( indexDir );
    }

    protected abstract void prepareNexusIndexer(NexusIndexer nexusIndexer) throws Exception;

    protected void unprepareNexusIndexer(NexusIndexer nexusIndexer) throws Exception {
        nexusIndexer.removeIndexingContext(context, false);
    }

    protected void assertGroup(int expected, String group, IndexingContext context) throws IOException {
        // ArtifactInfo.UINFO - UN_TOKENIZED
        // ArtifactInfo.GROUP_ID - TOKENIZED

        Term term = new Term(ArtifactInfo.GROUP_ID, group);
        PrefixQuery pq = new PrefixQuery(term);

        // new WildcardQuery( //
        // SpanTermQuery pq = new SpanTermQuery( term );
        // PhraseQuery pq = new PhraseQuery();
        // pq.add( new Term( ArtifactInfo.UINFO, group + "*" ) );

        FlatSearchResponse response = nexusIndexer.searchFlat(new FlatSearchRequest(pq, context));
        Collection<ArtifactInfo> artifacts = response.getResults();
        assertEquals(expected, artifacts.size(), artifacts.toString());
    }
}
