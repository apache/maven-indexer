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

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.index.context.IndexingContext;

/**
 * A special reusable filter, that filters the result set to unique Repository-GroupId-ArtifactId combination, leaving
 * out Version. There is a switch to make the Indexer-wide unique by ignoring repositories too.
 * 
 * @author cstamas
 * @deprecated Use {@link UniqueArtifactFilterPostprocessor} instead.
 */
public class UniqueGAArtifactFilterPostprocessor
    implements ArtifactInfoFilter
{
    private static final String VERSION_LATEST = "LATEST";

    private final boolean repositoriesIgnored;

    private final Set<String> gas = new HashSet<String>();

    public UniqueGAArtifactFilterPostprocessor( boolean repositoriesIgnored )
    {
        this.repositoriesIgnored = repositoriesIgnored;
    }

    public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
    {
        String key = ai.getGroupId() + ai.getArtifactId() + ai.getPackaging() + ai.getClassifier();

        if ( !repositoriesIgnored )
        {
            key = ai.getRepository() + key;
        }

        if ( gas.contains( key ) )
        {
            return false;
        }
        else
        {
            gas.add( key );

            postprocess( ctx, ai );

            return true;
        }
    }

    public void postprocess( IndexingContext ctx, ArtifactInfo ai )
    {
        ai.setVersion( VERSION_LATEST );

        if ( repositoriesIgnored )
        {
            ai.setContext( null );

            ai.setRepository( null );
        }
    }
}
