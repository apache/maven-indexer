package org.apache.maven.indexer.examples.indexing;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.Indexer;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.context.IndexCreator;

/**
 * A simple configuration holder class.
 * This class contains the mapped indexers.
 *
 * @author mtodorov
 */
@Named
@Singleton
public class IndexerConfiguration
{

    private Indexer indexer;

    private Scanner scanner;

    private Map<String, IndexCreator> indexers;


    @Inject
    public IndexerConfiguration( Indexer indexer,
                                 Scanner scanner,
                                 Map<String, IndexCreator> indexers )
    {
        this.indexer = indexer;
        this.scanner = scanner;
        this.indexers = indexers;
    }

    public List<IndexCreator> getIndexersAsList()
    {
        List<IndexCreator> indexersAsList = new ArrayList<>();
        for ( Map.Entry entry : indexers.entrySet() )
        {
            indexersAsList.add( ( IndexCreator ) entry.getValue() );
        }

        return indexersAsList;
    }

    public Indexer getIndexer()
    {
        return indexer;
    }

    public void setIndexer( Indexer indexer )
    {
        this.indexer = indexer;
    }

    public Scanner getScanner()
    {
        return scanner;
    }

    public void setScanner( Scanner scanner )
    {
        this.scanner = scanner;
    }

    public Map<String, IndexCreator> getIndexers()
    {
        return indexers;
    }

    public void setIndexers( Map<String, IndexCreator> indexers )
    {
        this.indexers = indexers;
    }

}
