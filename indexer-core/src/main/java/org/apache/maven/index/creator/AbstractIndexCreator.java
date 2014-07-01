package org.apache.maven.index.creator;

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
import java.util.Collections;
import java.util.List;

import org.apache.maven.index.context.IndexCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract base class for {@link IndexCreator} implementations.
 * 
 * @author Jason van Zyl
 */
public abstract class AbstractIndexCreator
    implements IndexCreator
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    private final String id;

    private final List<String> creatorDependencies;

    protected AbstractIndexCreator( final String id )
    {
        this( id, null );
    }

    protected AbstractIndexCreator( final String id, final List<String> creatorDependencies )
    {
        this.id = id;

        final ArrayList<String> deps = new ArrayList<String>();

        if ( creatorDependencies != null && !creatorDependencies.isEmpty() )
        {
            deps.addAll( creatorDependencies );
        }

        this.creatorDependencies = Collections.unmodifiableList( deps );
    }

    public String getId()
    {
        return id;
    }

    public List<String> getCreatorDependencies()
    {
        return creatorDependencies;
    }

    public static String bos( boolean b )
    {
        return b ? "1" : "0";
    }

    public static boolean sob( String b )
    {
        return b.equals( "1" );
    }
}
