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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

/**
 * Holds basic information about Indexer field, how it is stored. To keep this centralized, and not spread across code.
 * Since Lucene 2.x, the field names are encoded, so please use real chatty names instead of cryptic chars!
 * 
 * @author cstamas
 */
public class IndexerField
{
    private final org.apache.maven.index.Field ontology;

    private final IndexerFieldVersion version;

    private final String key;

    private final FieldType fieldType;

    /** Indexed, not tokenized, not stored. */
    public static final FieldType KEYWORD_NOT_STORED = new FieldType();

    /** Indexed, not tokenized, stored. */
    public static final FieldType KEYWORD_STORED = new FieldType();

    /** Indexed, tokenized, not stored. */
    public static final FieldType ANALYZED_NOT_STORED = new FieldType();

    /** Indexed, tokenized, stored. */
    public static final FieldType ANALYZED_STORED = new FieldType();

    static
    {
        KEYWORD_NOT_STORED.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
        KEYWORD_NOT_STORED.setStored( false );
        KEYWORD_NOT_STORED.setTokenized( false );
        KEYWORD_NOT_STORED.freeze();

        KEYWORD_STORED.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
        KEYWORD_STORED.setStored( true );
        KEYWORD_STORED.setTokenized( false );
        KEYWORD_STORED.freeze();

        ANALYZED_NOT_STORED.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
        ANALYZED_NOT_STORED.setStored( false );
        ANALYZED_NOT_STORED.setTokenized( true );
        ANALYZED_NOT_STORED.freeze();

        ANALYZED_STORED.setIndexOptions( IndexOptions.DOCS_AND_FREQS_AND_POSITIONS );
        ANALYZED_STORED.setStored( true );
        ANALYZED_STORED.setTokenized( true );
        ANALYZED_STORED.freeze();
    }

    public IndexerField( final org.apache.maven.index.Field ontology, final IndexerFieldVersion version,
            final String key, final String description, final FieldType fieldType )
    {
        this.ontology = ontology;

        this.version = version;

        this.key = key;

        this.fieldType = fieldType;

        ontology.addIndexerField( this );
    }

    public org.apache.maven.index.Field getOntology()
    {
        return ontology;
    }

    public IndexerFieldVersion getVersion()
    {
        return version;
    }

    public String getKey()
    {
        return key;
    }

    public FieldType getFieldType()
    {
        return fieldType;
    }

    public boolean isIndexed()
    {
        return fieldType.indexOptions() != IndexOptions.NONE;
    }

    public boolean isKeyword()
    {
        return isIndexed() && !fieldType.tokenized();
    }

    public boolean isStored()
    {
        return fieldType.stored();
    }

    public Field toField( String value )
    {
        return new Field( getKey(), value, getFieldType() );
    }
}
