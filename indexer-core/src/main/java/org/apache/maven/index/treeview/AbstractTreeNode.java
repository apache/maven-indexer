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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base set of functionality for the TreeNode that all implementations will need.
 */
public abstract class AbstractTreeNode
    implements TreeNode
{
    /**
     * The type of node.
     */
    private String type;

    /**
     * Flag that determines if the node is a leaf.
     */
    private boolean leaf;

    /**
     * The name of the node.
     */
    private String nodeName;

    /**
     * The path of the node.
     */
    private String path;

    /**
     * The children of this node.
     */
    private List<TreeNode> children;

    /**
     * The group id of this node.
     */
    private String groupId;

    /**
     * The artifact id of this node.
     */
    private String artifactId;

    /**
     * The version of this node.
     */
    private String version;

    /**
     * The repository id that this node is stored in.
     */
    private String repositoryId;

    final private transient IndexTreeView treeView;

    final private transient TreeViewRequest request;

    /**
     * Constructor that takes an IndexTreeView implementation and a TreeNodeFactory implementation;
     * 
     * @param tview
     * @param factory
     */
    public AbstractTreeNode( IndexTreeView tview, TreeViewRequest request )
    {
        this.treeView = tview;

        this.request = request;
    }

    /**
     * Get the type of node.
     * 
     * @return Type
     */
    public Type getType()
    {
        return Type.valueOf( type );
    }

    /**
     * Set the type of node.
     * 
     * @param Type
     */
    public void setType( Type type )
    {
        this.type = type.name();
    }

    /**
     * Get flag that determines if the node is a leaf.
     * 
     * @return boolean
     */
    public boolean isLeaf()
    {
        return leaf;
    }

    /**
     * Set flag that determines if the node is a leaf.
     * 
     * @param boolean
     */
    public void setLeaf( boolean leaf )
    {
        this.leaf = leaf;
    }

    /**
     * Get the name of the node.
     * 
     * @return String
     */
    public String getNodeName()
    {
        return nodeName;
    }

    /**
     * Set the name of the node.
     * 
     * @param String
     */
    public void setNodeName( String nodeName )
    {
        this.nodeName = nodeName;
    }

    /**
     * Get the path of the node.
     * 
     * @return String
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Set the path of the node.
     * 
     * @param String
     */
    public void setPath( String path )
    {
        this.path = path;
    }

    /**
     * Get the group id of this node.
     * 
     * @return String
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * Set the group id of this node.
     * 
     * @param String
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    /**
     * Get the artifact id of this node.
     * 
     * @return String
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * Set the artifact id of this node.
     * 
     * @param String
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * Get the version of this node.
     * 
     * @return String
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Set the version of this node.
     * 
     * @param String
     */
    public void setVersion( String version )
    {
        this.version = version;
    }

    /**
     * Get the repository id that this node is stored in.
     * 
     * @return String
     */
    public String getRepositoryId()
    {
        return repositoryId;
    }

    /**
     * Set the repository id that this node is stored in.
     * 
     * @param String
     */
    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    /**
     * Get the children of this node. If this is a leaf node, null will be returned. This will NOT perform any actions
     * on the index to retrieve the children, will only return children that have already been loaded via the
     * listChildren method.
     * 
     * @return List<TreeNode>
     */
    public List<TreeNode> getChildren()
    {
        if ( children == null && !isLeaf() )
        {
            children = new ArrayList<>();
        }

        return children;
    }

    /**
     * Get the children of this node. If this is a leaf node, null will be returned. This will use the index to retrieve
     * the list of child nodes.
     * 
     * @return List<TreeNode>
     */
    public List<TreeNode> listChildren()
        throws IOException
    {
        if ( !isLeaf() && getChildren().isEmpty() && !isLeaf() )
        {
            children =
                treeView.listNodes(
                    new TreeViewRequest( request.getFactory(), getPath(), request.getFieldHints(),
                        request.getArtifactInfoFilter(), request.getIndexingContext() ) ).getChildren();
        }

        return children;
    }

    /**
     * Find a TreeNode based upon a path and Type check.
     * 
     * @return TreeNode
     */
    public TreeNode findChildByPath( String path, Type type )
        throws IOException
    {
        for ( TreeNode child : getChildren() )
        {
            if ( path.equals( child.getPath() ) && type.equals( child.getType() ) )
            {
                return child;
            }
        }

        return null;
    }
}