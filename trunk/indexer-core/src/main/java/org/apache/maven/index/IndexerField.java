/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;

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

    private final Store storeMethod;

    private final Index indexMethod;

    private final TermVector termVector;

    public IndexerField( final org.apache.maven.index.Field ontology, final IndexerFieldVersion version,
                         final String key, final String description, final Store storeMethod, final Index indexMethod )
    {
        this( ontology, version, key, description, storeMethod, indexMethod, null );
    }

    public IndexerField( final org.apache.maven.index.Field ontology, final IndexerFieldVersion version,
                         final String key, final String description, final Store storeMethod, final Index indexMethod,
                         final TermVector termVector )
    {
        this.ontology = ontology;

        this.version = version;

        this.key = key;

        this.storeMethod = storeMethod;

        this.indexMethod = indexMethod;

        this.termVector = termVector;

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

    public Field.Store getStoreMethod()
    {
        return storeMethod;
    }

    public Field.Index getIndexMethod()
    {
        return indexMethod;
    }

    public Field.TermVector getTermVector()
    {
        return termVector;
    }

    public boolean isIndexed()
    {
        return !Index.NO.equals( indexMethod );
    }

    public boolean isKeyword()
    {
        return isIndexed() && !Index.TOKENIZED.equals( indexMethod );
    }

    public boolean isStored()
    {
        return !( Store.NO.equals( storeMethod ) );
    }

    public Field toField( String value )
    {
        if ( getTermVector() != null )
        {
            return new Field( getKey(), value, getStoreMethod(), getIndexMethod(), getTermVector() );
        }
        else
        {
            return new Field( getKey(), value, getStoreMethod(), getIndexMethod() );
        }
    }
}
