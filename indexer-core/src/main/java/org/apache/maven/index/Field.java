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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Field
{
    public static final String NOT_PRESENT = "N/P";

    private final Field parent;

    private final String namespace;

    private final String fieldName;

    private final String description;

    private final List<IndexerField> indexerFields;

    public Field( final Field parent, final String namespace, final String name, final String description )
    {
        this.parent = parent;

        this.namespace = namespace;

        this.fieldName = name;

        this.description = description;

        this.indexerFields = new ArrayList<>();
    }

    public Field getParent()
    {
        return parent;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public String getDescription()
    {
        return description;
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Collections.unmodifiableList( indexerFields );
    }

    public boolean addIndexerField( IndexerField field )
    {
        return indexerFields.add( field );
    }

    public boolean removeIndexerField( IndexerField field )
    {
        return indexerFields.remove( field );
    }

    public String getFQN()
    {
        return getNamespace() + getFieldName();
    }

    public String toString()
    {
        return getFQN() + " (with " + getIndexerFields().size() + " registered index fields)";
    }
}
