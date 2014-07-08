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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.AbstractTestSupport;
import org.junit.Assert;

public class IndexCreatorSorterTest
    extends AbstractTestSupport
{
    public void testLookupList()
        throws Exception
    {
        final List<IndexCreator> creators = getContainer().lookupList( IndexCreator.class );

        final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( creators );

        // we are interested in IDs only
        final List<String> sortedCreatorIds = new ArrayList<String>();
        for ( IndexCreator creator : sortedCreators )
        {
            sortedCreatorIds.add( creator.getId() );
        }

        // ensure we fulfil some basic conditions
        Assert.assertTrue( "min should be present", sortedCreatorIds.contains( "min" ) );
        Assert.assertTrue( "maven-plugin should be present", sortedCreatorIds.contains( "maven-plugin" ) );
        Assert.assertTrue( "maven-archetype should be present", sortedCreatorIds.contains( "maven-archetype" ) );

        // currently, both "maven-plugin" and "maven-archetype" creator depend on "min" creator
        Assert.assertTrue( "maven-archetype depends on min",
            sortedCreatorIds.indexOf( "min" ) < sortedCreatorIds.indexOf( "maven-archetype" ) );
        Assert.assertTrue( "maven-plugin depends on min",
            sortedCreatorIds.indexOf( "min" ) < sortedCreatorIds.indexOf( "maven-plugin" ) );
    }

    public void testLookupListWithSpoofedCreator()
        throws Exception
    {
        final List<IndexCreator> creators =
            new ArrayList<IndexCreator>( getContainer().lookupList( IndexCreator.class ) );

        // now we add spoofs to it, this one depends on ALL creators. Note: we add it as 1st intentionally
        creators.add( 0,
            new SpoofIndexCreator( "depend-on-all", new ArrayList<String>(
                getContainer().lookupMap( IndexCreator.class ).keySet() ) ) );

        // now we add spoofs to it, this one depends on only one, the "depend-on-all" creator Note: we add it as 1st
        // intentionally
        creators.add( 0, new SpoofIndexCreator( "last", Arrays.asList( "depend-on-all" ) ) );

        final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( creators );

        // we are interested in IDs only
        final List<String> sortedCreatorIds = new ArrayList<String>();
        for ( IndexCreator creator : sortedCreators )
        {
            sortedCreatorIds.add( creator.getId() );
        }

        // ensure we fulfil some basic conditions
        Assert.assertTrue( "min should be present", sortedCreatorIds.contains( "min" ) );
        Assert.assertTrue( "maven-plugin should be present", sortedCreatorIds.contains( "maven-plugin" ) );
        Assert.assertTrue( "maven-archetype should be present", sortedCreatorIds.contains( "maven-archetype" ) );
        Assert.assertTrue( "depend-on-all should be present", sortedCreatorIds.contains( "depend-on-all" ) );
        Assert.assertTrue( "last should be present", sortedCreatorIds.contains( "last" ) );

        // "last" has to be last
        Assert.assertTrue( "last creator should be last",
            sortedCreatorIds.indexOf( "last" ) == sortedCreatorIds.size() - 1 );
        Assert.assertTrue( "depend-on-all should be next to last",
            sortedCreatorIds.indexOf( "depend-on-all" ) == sortedCreatorIds.size() - 2 );
    }

    public void testLookupListWithNonExistentCreatorDependency()
        throws Exception
    {
        final List<IndexCreator> creators =
            new ArrayList<IndexCreator>( getContainer().lookupList( IndexCreator.class ) );

        // now we add spoofs to it, this one depends on non existent creator. Note: we add it as 1st intentionally
        creators.add( 0,
            new SpoofIndexCreator( "non-satisfyable", Arrays.asList( "this-creator-i-depend-on-does-not-exists" ) ) );

        try
        {
            final List<IndexCreator> sortedCreators = IndexCreatorSorter.sort( creators );

            Assert.fail( "IndexCreator list is not satisfyable!" );
        }
        catch ( IllegalArgumentException e )
        {
            // good, check message
            final String message = e.getMessage();

            Assert.assertTrue( "Exception message should mention the problematic creator's ID",
                message.contains( "non-satisfyable" ) );
            Assert.assertTrue( "Exception message should mention the missing creator's ID",
                message.contains( "this-creator-i-depend-on-does-not-exists" ) );
        }
    }
}
