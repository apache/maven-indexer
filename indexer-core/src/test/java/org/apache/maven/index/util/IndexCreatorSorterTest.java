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

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.AbstractTestSupport;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IndexCreatorSorterTest
    extends AbstractTestSupport
{
    @Inject
    private List<IndexCreator> creators;

    @Inject
    private Map<String, IndexCreator> creatorMap;

    @Test
    public void testLookupList()
        throws Exception
    {
        final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( creators );

        // we are interested in IDs only
        final List<String> sortedCreatorIds = new ArrayList<>();
        for ( IndexCreator creator : sortedCreators )
        {
            sortedCreatorIds.add( creator.getId() );
        }

        // ensure we fulfil some basic conditions
        assertTrue( "min should be present", sortedCreatorIds.contains( "min" ) );
        assertTrue( "maven-plugin should be present", sortedCreatorIds.contains( "maven-plugin" ) );
        assertTrue( "maven-archetype should be present", sortedCreatorIds.contains( "maven-archetype" ) );

        // currently, both "maven-plugin" and "maven-archetype" creator depend on "min" creator
        assertTrue( "maven-archetype depends on min",
            sortedCreatorIds.indexOf( "min" ) < sortedCreatorIds.indexOf( "maven-archetype" ) );
        assertTrue( "maven-plugin depends on min",
            sortedCreatorIds.indexOf( "min" ) < sortedCreatorIds.indexOf( "maven-plugin" ) );
    }

    @Test
    public void testLookupListWithSpoofedCreator()
        throws Exception
    {
        List<IndexCreator> myIndexCreators = new ArrayList<>( creators );

        // now we add spoofs to it, this one depends on ALL creators. Note: we add it as 1st intentionally
        myIndexCreators.add( 0,
            new SpoofIndexCreator( "depend-on-all", new ArrayList<>( creatorMap.keySet() ) ) );

        // now we add spoofs to it, this one depends on only one, the "depend-on-all" creator Note: we add it as 1st
        // intentionally
        myIndexCreators.add( 0, new SpoofIndexCreator( "last", Arrays.asList( "depend-on-all" ) ) );

        final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( myIndexCreators );

        // we are interested in IDs only
        final List<String> sortedCreatorIds = new ArrayList<>();
        for ( IndexCreator creator : sortedCreators )
        {
            sortedCreatorIds.add( creator.getId() );
        }

        // ensure we fulfil some basic conditions
        assertTrue( "min should be present", sortedCreatorIds.contains( "min" ) );
        assertTrue( "maven-plugin should be present", sortedCreatorIds.contains( "maven-plugin" ) );
        assertTrue( "maven-archetype should be present", sortedCreatorIds.contains( "maven-archetype" ) );
        assertTrue( "depend-on-all should be present", sortedCreatorIds.contains( "depend-on-all" ) );
        assertTrue( "last should be present", sortedCreatorIds.contains( "last" ) );

        // "last" has to be last
        assertTrue( "last creator should be last",
            sortedCreatorIds.indexOf( "last" ) == sortedCreatorIds.size() - 1 );
        assertTrue( "depend-on-all should be next to last",
            sortedCreatorIds.indexOf( "depend-on-all" ) == sortedCreatorIds.size() - 2 );
    }

    @Test
    public void testLookupListWithNonExistentCreatorDependency()
        throws Exception
    {
        List<IndexCreator> myCreators = new ArrayList<>( creators );
        // now we add spoofs to it, this one depends on non existent creator. Note: we add it as 1st intentionally
        myCreators.add( 0,
            new SpoofIndexCreator( "non-satisfyable", Arrays.asList( "this-creator-i-depend-on-does-not-exists" ) ) );

        try
        {
            final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( myCreators );

            fail( "IndexCreator list is not satisfyable!" );
        }
        catch ( IllegalArgumentException e )
        {
            // good, check message
            final String message = e.getMessage();

            assertTrue( "Exception message should mention the problematic creator's ID",
                message.contains( "non-satisfyable" ) );
            assertTrue( "Exception message should mention the missing creator's ID",
                message.contains( "this-creator-i-depend-on-does-not-exists" ) );
        }
    }
}
