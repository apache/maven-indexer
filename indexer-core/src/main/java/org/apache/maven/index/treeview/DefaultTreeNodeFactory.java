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
package org.apache.maven.index.treeview;

import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.treeview.TreeNode.Type;

/**
 * A default implementation of TreeNodeFactory, that is fairly simple to extend.
 * 
 * @author Tamas Cservenak
 */
public class DefaultTreeNodeFactory
    implements TreeNodeFactory
{
    private IndexingContext context;

    public DefaultTreeNodeFactory( IndexingContext ctx )
    {
        this.context = ctx;
    }

    public IndexingContext getIndexingContext()
    {
        return context;
    }

    public TreeNode createGNode( IndexTreeView tview, String path, String groupName )
    {
        TreeNode result = createNode( tview, path, false, groupName, Type.G );

        return decorateGNode( tview, path, groupName, result );
    }

    protected TreeNode decorateGNode( IndexTreeView tview, String path, String groupName, TreeNode node )
    {
        return node;
    }

    public TreeNode createANode( IndexTreeView tview, ArtifactInfo ai, String path )
    {
        TreeNode result = createNode( tview, path, false, ai.artifactId, Type.A );

        result.setGroupId( ai.groupId );

        result.setArtifactId( ai.artifactId );

        return decorateANode( tview, ai, path, result );
    }

    protected TreeNode decorateANode( IndexTreeView tview, ArtifactInfo ai, String path, TreeNode node )
    {
        return node;
    }

    public TreeNode createVNode( IndexTreeView tview, ArtifactInfo ai, String path )
    {
        TreeNode result = createNode( tview, path, false, ai.version, Type.V );

        result.setGroupId( ai.groupId );

        result.setArtifactId( ai.artifactId );

        result.setVersion( ai.version );

        return decorateVNode( tview, ai, path, result );
    }

    protected TreeNode decorateVNode( IndexTreeView tview, ArtifactInfo ai, String path, TreeNode node )
    {
        return node;
    }

    public TreeNode createArtifactNode( IndexTreeView tview, ArtifactInfo ai, String path )
    {
        StringBuffer sb = new StringBuffer( ai.artifactId ).append( "-" ).append( ai.version );

        if ( ai.classifier != null )
        {
            sb.append( "-" ).append( ai.classifier );
        }

        sb.append( "." ).append( ai.fextension == null ? "jar" : ai.fextension );

        TreeNode result = createNode( tview, path, true, sb.toString(), Type.artifact );

        result.setGroupId( ai.groupId );

        result.setArtifactId( ai.artifactId );

        result.setVersion( ai.version );

        return decorateArtifactNode( tview, ai, path, result );
    }

    protected TreeNode decorateArtifactNode( IndexTreeView tview, ArtifactInfo ai, String path, TreeNode node )
    {
        return node;
    }

    protected TreeNode createNode( IndexTreeView tview, String path, boolean leaf, String nodeName, Type type )
    {
        TreeNode result = instantiateNode( tview, path, leaf, nodeName );

        result.setPath( path );

        result.setType( type );

        result.setLeaf( leaf );

        result.setNodeName( nodeName );

        result.setRepositoryId( getIndexingContext().getRepositoryId() );

        return result;
    }

    protected TreeNode instantiateNode( IndexTreeView tview, String path, boolean leaf, String nodeName )
    {
        return new DefaultTreeNode( tview, this );
    }

}
