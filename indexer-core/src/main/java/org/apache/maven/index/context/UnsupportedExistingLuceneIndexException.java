package org.apache.maven.index.context;

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

/**
 * Thrown when a user tries to create a NexusInder IndexingContext over and existing Lucene index. The reason for
 * throwing this exception may be multiple: non-NexusIndexer Lucene index, index version is wrong, repositoryId does not
 * matches the context repositoryId, etc.
 * 
 * @author Tamas Cservenak
 * @deprecated The deprecated {@link org.apache.maven.index.NexusIndexer} uses this exception. Use
 * {@link org.apache.maven.index.Indexer} instead.
 */
public class UnsupportedExistingLuceneIndexException
    extends Exception
{
    private static final long serialVersionUID = -3206758653346308322L;

    public UnsupportedExistingLuceneIndexException( String message )
    {
        super( message );
    }

    public UnsupportedExistingLuceneIndexException( String message, ExistingLuceneIndexMismatchException e )
    {
        super( message, e );
    }
}
