package org.apache.maven.index;

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

import java.io.Closeable;
import java.util.Iterator;

/**
 * IteratorResultSet, that returns the result hit's in "iterator-like fashion", instead lifting all the hits into memory
 * (and bashing IO and RAM consumption). This makes search making client memory usage thin, and makes possible to
 * implement things like "streaming" results etc. The result set is java.util.Iterator, but is made also
 * java.lang.Iterable to make it possible to take part in operations like "for-each", but it will usually return itself
 * as Iterator.
 * 
 * @author cstamas
 */
public interface IteratorResultSet
    extends Iterator<ArtifactInfo>, Iterable<ArtifactInfo>, Closeable
{
    /**
     * Returns the up-to-date number of the actual number of loaded Lucene Documents that were converted into
     * ArtifactInfo object until last next() invocation. Warning: this method will return ALL touched/loaded document
     * count, even those that are filtered out and NOT returned by iterator's next() method!
     * 
     * @return total number of processed ArtifactInfos so far
     */
    int getTotalProcessedArtifactInfoCount();
}
