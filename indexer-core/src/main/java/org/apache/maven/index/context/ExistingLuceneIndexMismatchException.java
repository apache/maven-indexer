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

import java.io.IOException;

/**
 * Thrown when a user tries to create a NexusInder IndexingContext over and existing Lucene index, and there is a
 * mismatch. The reason for mismatch may be multiple: non-NexusIndexer Lucene index (basically missing the descriptor
 * document), index version is wrong or unknown, repositoryId from context and descriptor document does not matches,
 * etc. This exception is not thrown in cases when "reclaim" is done, as in those cases, even if an unknown index is
 * found, descriptor will be forcefully added with current context information, potentially replacing the existing
 * descriptor, if any.
 * 
 * @author Tamas Cservenak
 * @since 5.1.0
 */
public class ExistingLuceneIndexMismatchException
    extends IOException
{
    private static final long serialVersionUID = -6587046761831878804L;

    public ExistingLuceneIndexMismatchException( String message )
    {
        super( message );
    }
}
