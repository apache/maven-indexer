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

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.treeview.TreeNode.Type;

/**
 * A default implementation of TreeNodeFactory, that is fairly simple to extend.
 * 
 * @author Tamas Cservenak
 */
public class DefaultTreeNodeFactory
    implements TreeNodeFactory
{
    private final String repositoryId;

    public DefaultTreeNodeFactory( String id )
    {
        this.repositoryId = id;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public TreeNode createGNode( IndexTreeView tview, TreeViewRequest req, String path, String groupName )
    {
        TreeNode result = createNode( tview, req, path, false, groupName, Type.G );

        return decorateGNode( tview, req, path, groupName, result );
    }

    protected TreeNode decorateGNode( IndexTreeView tview, TreeViewRequest req, String path, String groupName,
                                      TreeNode node )
    {
        return node;
    }

    public TreeNode createANode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path )
    {
        TreeNode result = createNode( tview, req, path, false, ai.getArtifactId(), Type.A );

        result.setGroupId( ai.getGroupId() );

        result.setArtifactId( ai.getArtifactId() );

        return decorateANode( tview, req, ai, path, result );
    }

    protected TreeNode decorateANode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                      TreeNode node )
    {
        return node;
    }

    public TreeNode createVNode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path )
    {
        TreeNode result = createNode( tview, req, path, false, ai.getVersion(), Type.V );

        result.setGroupId( ai.getGroupId() );

        result.setArtifactId( ai.getArtifactId() );

        result.setVersion( ai.getVersion() );

        return decorateVNode( tview, req, ai, path, result );
    }

    protected TreeNode decorateVNode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                      TreeNode node )
    {
        return node;
    }

    public TreeNode createArtifactNode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path )
    {
        StringBuilder sb = new StringBuilder( ai.getArtifactId() ).append( "-" ).append( ai.getVersion() );

        if ( ai.getClassifier() != null )
        {
            sb.append( "-" ).append( ai.getClassifier() );
        }

        sb.append( "." ).append( ai.getFileExtension() == null ? "jar" : ai.getFileExtension() );

        TreeNode result = createNode( tview, req, path, true, sb.toString(), Type.artifact );

        result.setGroupId( ai.getGroupId() );

        result.setArtifactId( ai.getArtifactId() );

        result.setVersion( ai.getVersion() );

        return decorateArtifactNode( tview, req, ai, path, result );
    }

    protected TreeNode decorateArtifactNode( IndexTreeView tview, TreeViewRequest req, ArtifactInfo ai, String path,
                                             TreeNode node )
    {
        return node;
    }

    protected TreeNode createNode( IndexTreeView tview, TreeViewRequest req, String path, boolean leaf,
                                   String nodeName, Type type )
    {
        TreeNode result = instantiateNode( tview, req, path, leaf, nodeName );

        result.setPath( path );

        result.setType( type );

        result.setLeaf( leaf );

        result.setNodeName( nodeName );

        result.setRepositoryId( getRepositoryId() );

        return result;
    }

    protected TreeNode instantiateNode( IndexTreeView tview, TreeViewRequest req, String path, boolean leaf,
                                        String nodeName )
    {
        return new DefaultTreeNode( tview, req );
    }
}