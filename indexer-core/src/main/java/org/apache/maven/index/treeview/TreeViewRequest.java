package org.apache.maven.index.treeview;

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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.index.ArtifactInfoFilter;
import org.apache.maven.index.Field;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexingContext;

public class TreeViewRequest
{
    private final TreeNodeFactory factory;

    private final String path;

    private final ArtifactInfoFilter artifactInfoFilter;

    private final Map<Field, String> fieldHints;

    private final IndexingContext indexingContext;

    public TreeViewRequest( final TreeNodeFactory factory, final String path, final IndexingContext ctx )
    {
        this( factory, path, null, null, ctx );
    }

    public TreeViewRequest( final TreeNodeFactory factory, final String path, final Map<Field, String> hints,
                            final ArtifactInfoFilter artifactInfoFilter, final IndexingContext ctx )
    {
        this.factory = factory;

        this.path = path;

        this.fieldHints = new HashMap<Field, String>();

        if ( hints != null && hints.size() != 0 )
        {
            this.fieldHints.putAll( hints );
        }

        this.artifactInfoFilter = artifactInfoFilter;

        this.indexingContext = ctx;
    }

    public TreeNodeFactory getFactory()
    {
        return factory;
    }

    public String getPath()
    {
        return path;
    }

    public ArtifactInfoFilter getArtifactInfoFilter()
    {
        return artifactInfoFilter;
    }

    public void addFieldHint( Field field, String hint )
    {
        fieldHints.put( field, hint );
    }

    public void removeFieldHint( Field field )
    {
        fieldHints.remove( field );
    }

    public boolean hasFieldHints()
    {
        return fieldHints.size() > 0 && ( hasFieldHint( MAVEN.GROUP_ID ) );
    }

    public boolean hasFieldHint( Field... fields )
    {
        for ( Field f : fields )
        {
            if ( !fieldHints.containsKey( f ) )
            {
                return false;
            }
        }

        return true;
    }

    public String getFieldHint( Field field )
    {
        return fieldHints.get( field );
    }

    public Map<Field, String> getFieldHints()
    {
        return fieldHints;
    }

    public IndexingContext getIndexingContext()
    {
        return indexingContext;
    }
}