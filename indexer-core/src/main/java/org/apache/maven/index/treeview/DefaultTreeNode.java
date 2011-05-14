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

/**
 * The simplest treenode, that does not adds any "decoration" to the nodes.
 * 
 * @author Tamas Cservenak
 */
public class DefaultTreeNode
    extends AbstractTreeNode
{
    /**
     * Constructor that takes an IndexTreeView implementation and a TreeNodeFactory implementation.
     * 
     * @param tview
     * @param factory
     */
    public DefaultTreeNode( IndexTreeView tview, TreeViewRequest request )
    {
        super( tview, request );
    }
}