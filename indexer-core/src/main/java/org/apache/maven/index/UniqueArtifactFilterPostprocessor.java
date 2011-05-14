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
 */
public class UniqueArtifactFilterPostprocessor
    implements ArtifactInfoFilter
{
    public static final String COLLAPSED = "COLLAPSED";

    private final Set<Field> uniqueFields = new HashSet<Field>();

    private final Set<String> gas = new HashSet<String>();

    public UniqueArtifactFilterPostprocessor()
    {
    }

    public UniqueArtifactFilterPostprocessor( Set<Field> uniqueFields )
    {
        this.uniqueFields.addAll( uniqueFields );
    }

    public boolean accepts( IndexingContext ctx, ArtifactInfo ai )
    {
        StringBuilder sb = new StringBuilder();

        for ( Field field : uniqueFields )
        {
            sb.append( ai.getFieldValue( field ) ).append( ":" );
        }

        String key = sb.toString().substring( 0, sb.length() - 1 );

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
        for ( Field field : ai.getFields() )
        {
            if ( !uniqueFields.contains( field ) )
            {
                ai.setFieldValue( field, COLLAPSED );
            }
        }
    }

    public void addField( Field field )
    {
        uniqueFields.add( field );
    }
}
