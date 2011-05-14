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

import java.io.File;

import org.apache.maven.index.AbstractNexusIndexerTest;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.treeview.DefaultTreeNodeFactory;
import org.apache.maven.index.treeview.IndexTreeView;
import org.apache.maven.index.treeview.TreeNode;

public class IndexTreeViewTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "src/test/repo" );

    protected IndexTreeView indexTreeView;

    protected boolean debug = false;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        indexTreeView = lookup( IndexTreeView.class );
    }

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        context = nexusIndexer.addIndexingContext( "test-minimal", "test", repo, indexDir, null, null, MIN_CREATORS );
        nexusIndexer.scan( context );
    }

    protected int prettyPrint( boolean debug, TreeNode node, int level )
        throws Exception
    {
        if ( debug )
        {
            System.out.print( node.getPath() + " := " + node.getNodeName() + ", type=" + node.getType() );
            System.out.println();
        }

        int files = node.isLeaf() ? 1 : 0;

        if ( !node.isLeaf() )
        {
            for ( TreeNode child : node.listChildren() )
            {
                files += prettyPrint( debug, child, level + 2 );
            }
        }

        if ( debug && level == 0 )
        {
            System.out.println( " ===== " );
            System.out.println( " TOTAL LEAFS:  " + files );
        }

        return files;
    }

    public void testRoot()
        throws Exception
    {
        TreeViewRequest req =
            new TreeViewRequest( new DefaultTreeNodeFactory( context.getRepositoryId() ), "/", context );
        TreeNode root = indexTreeView.listNodes( req );

        int leafsFound = prettyPrint( debug, root, 0 );

        assertEquals( "The group name should be here", "/", root.getNodeName() );
        assertEquals( 12, root.getChildren().size() );
        assertEquals( 49, leafsFound );
    }

    public void testPathIsAboveRealGroup()
        throws Exception
    {
        TreeViewRequest req =
            new TreeViewRequest( new DefaultTreeNodeFactory( context.getRepositoryId() ), "/org/", context );
        TreeNode root = indexTreeView.listNodes( req );

        int leafsFound = prettyPrint( debug, root, 0 );

        assertEquals( "The group name should be here", "org", root.getNodeName() );
        assertEquals( 4, root.getChildren().size() );
        assertEquals( 22, leafsFound );
    }

    public void testPathIsRealGroup()
        throws Exception
    {
        TreeViewRequest req =
            new TreeViewRequest( new DefaultTreeNodeFactory( context.getRepositoryId() ), "/org/slf4j/", context );
        TreeNode root = indexTreeView.listNodes( req );

        int leafsFound = prettyPrint( debug, root, 0 );

        assertEquals( "The group name should be here", "slf4j", root.getNodeName() );
        assertEquals( 3, root.getChildren().size() );
        assertEquals( 10, leafsFound );
    }

    public void testPathIsRealGroupArtifact()
        throws Exception
    {
        TreeViewRequest req =
            new TreeViewRequest( new DefaultTreeNodeFactory( context.getRepositoryId() ), "/org/slf4j/slf4j-log4j12/",
                context );
        TreeNode root = indexTreeView.listNodes( req );

        int leafsFound = prettyPrint( debug, root, 0 );

        assertEquals( "slf4j-log4j12", root.getNodeName() );
        assertEquals( 1, root.getChildren().size() );
        assertEquals( 4, leafsFound );
    }

    public void testPathIsRealGroupArtifactVersion()
        throws Exception
    {
        TreeViewRequest req =
            new TreeViewRequest( new DefaultTreeNodeFactory( context.getRepositoryId() ),
                "/org/slf4j/slf4j-log4j12/1.4.1/", context );
        TreeNode root = indexTreeView.listNodes( req );

        int leafsFound = prettyPrint( debug, root, 0 );

        assertEquals( "The group name should be here", "1.4.1", root.getNodeName() );
        assertEquals( 1, root.getChildren().size() );
        assertEquals( 4, leafsFound );
    }
}