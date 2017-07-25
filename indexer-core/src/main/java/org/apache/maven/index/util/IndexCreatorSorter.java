package org.apache.maven.index.util;

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
import java.util.HashMap;
import java.util.List;

import org.apache.maven.index.context.IndexCreator;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

public class IndexCreatorSorter
{
    public static List<IndexCreator> sort( List<? extends IndexCreator> creators )
        throws IllegalArgumentException
    {
        try
        {
            final HashMap<String, IndexCreator> creatorsById = new HashMap<String, IndexCreator>( creators.size() );

            DAG dag = new DAG();

            for ( IndexCreator creator : creators )
            {
                creatorsById.put( creator.getId(), creator );

                dag.addVertex( creator.getId() );

                for ( String depId : creator.getCreatorDependencies() )
                {
                    dag.addEdge( creator.getId(), depId );
                }
            }

            List<String> sortedIds = TopologicalSorter.sort( dag );

            final ArrayList<IndexCreator> sortedCreators = new ArrayList<>( creators.size() );

            for ( String id : sortedIds )
            {
                final IndexCreator creator = creatorsById.get( id );

                if ( creator != null )
                {
                    sortedCreators.add( creator );
                }
                else
                {
                    throw new IllegalArgumentException( String.format(
                        "IndexCreator with ID=\"%s\" does not exists, the present creator ID=\"%s\" depends on it!",
                        id, dag.getParentLabels( id ) ) );
                }
            }

            return sortedCreators;
        }
        catch ( CycleDetectedException e )
        {
            throw new IllegalArgumentException( "Supplied IndexCreator inter-dependencies", e );
        }

    }
}
