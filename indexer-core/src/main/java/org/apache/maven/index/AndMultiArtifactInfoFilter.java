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

import java.util.List;

import org.apache.maven.index.context.IndexingContext;

/**
 * This is a aggregated artifact info filter that performs AND operation (all filter has to accept the artifact info, if
 * one rejects, results is reject). It is implemented in "fail fast" way, as soon as some member ArtifactFilter rejects,
 * it will be rejected.
 * 
 * @author cstamas
 */
public class AndMultiArtifactInfoFilter
    extends AbstractMultiArtifactInfoFilter
{
    public AndMultiArtifactInfoFilter( List<ArtifactInfoFilter> filters )
    {
        super( filters );
    }

    @Override
    protected boolean accepts( List<ArtifactInfoFilter> filters, IndexingContext ctx, ArtifactInfo ai )
    {
        for ( ArtifactInfoFilter filter : filters )
        {
            if ( !filter.accepts( ctx, ai ) )
            {
                return false;
            }

        }

        return true;
    }
}
