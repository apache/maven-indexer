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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Field;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.treeview.TreeNode.Type;
import org.codehaus.plexus.util.StringUtils;

@Singleton
@Named
public class DefaultIndexTreeView
    implements IndexTreeView
{

    private final Indexer indexer;


    @Inject
    public DefaultIndexTreeView( Indexer indexer )
    {
        this.indexer = indexer;
    }

    protected Indexer getIndexer()
    {
        return indexer;
    }

    public TreeNode listNodes( TreeViewRequest request )
        throws IOException
    {
        // get the last path elem
        String name = null;

        if ( !"/".equals( request.getPath() ) )
        {

            if ( request.getPath().endsWith( "/" ) )
            {
                name = request.getPath().substring( 0, request.getPath().length() - 1 );
            }
            else
            {
                name = request.getPath();
            }

            name = name.substring( name.lastIndexOf( '/' ) + 1, name.length() );

            // root is "/"
            if ( !name.equals( "/" ) && name.endsWith( "/" ) )
            {
                name = name.substring( 0, name.length() - 1 );
            }

        }
        else
        {
            name = "/";
        }

        // the root node depends on request we have, so let's see
        TreeNode result = request.getFactory().createGNode( this, request, request.getPath(), name );

        if ( request.hasFieldHints() )
        {
            listChildren( result, request, null );
        }
        else
        {
            // non hinted way, the "old" way
            if ( "/".equals( request.getPath() ) )
            {
                // get root groups and finish
                Set<String> rootGroups = request.getIndexingContext().getRootGroups();

                for ( String group : rootGroups )
                {
                    if ( group.length() > 0 )
                    {
                        result.getChildren().add(
                            request.getFactory().createGNode( this, request, request.getPath() + group + "/", group ) );
                    }
                }
            }
            else
            {
                Set<String> allGroups = request.getIndexingContext().getAllGroups();

                listChildren( result, request, allGroups );
            }
        }

        return result;
    }

    /**
     * @param root
     * @param request
     * @param allGroups
     * @throws IOException
     */
    protected void listChildren( TreeNode root, TreeViewRequest request, Set<String> allGroups )
        throws IOException
    {
        String path = root.getPath();

        Map<String, TreeNode> folders = new HashMap<String, TreeNode>();

        String rootPartialGroupId = StringUtils.strip( root.getPath().replaceAll( "/", "." ), "." );

        folders.put( Type.G + ":" + rootPartialGroupId, root );

        IteratorSearchResponse artifacts = getArtifacts( root, request );

        try
        {
            for ( ArtifactInfo ai : artifacts )
            {
                String versionKey = Type.V + ":" + ai.getArtifactId() + ":" + ai.getVersion();

                TreeNode versionResource = folders.get( versionKey );

                if ( versionResource == null )
                {
                    String artifactKey = Type.A + ":" + ai.getArtifactId();

                    TreeNode artifactResource = folders.get( artifactKey );

                    if ( artifactResource == null )
                    {
                        TreeNode groupParentResource = root;

                        TreeNode groupResource = root;

                        // here comes the twist: we have to search for parent G node
                        String partialGroupId = null;

                        String[] groupIdElems = ai.getGroupId().split( "\\." );

                        for ( String groupIdElem : groupIdElems )
                        {
                            if ( partialGroupId == null )
                            {
                                partialGroupId = groupIdElem;
                            }
                            else
                            {
                                partialGroupId = partialGroupId + "." + groupIdElem;
                            }

                            String groupKey = Type.G + ":" + partialGroupId;

                            groupResource = folders.get( groupKey );

                            // it needs to be created only if not found (is null) and is _below_ groupParentResource
                            if ( groupResource == null
                                && groupParentResource.getPath().length() < getPathForAi( ai, MAVEN.GROUP_ID ).length() )
                            {
                                String gNodeName =
                                    partialGroupId.lastIndexOf( '.' ) > -1 ? partialGroupId.substring(
                                        partialGroupId.lastIndexOf( '.' ) + 1, partialGroupId.length() )
                                        : partialGroupId;

                                groupResource =
                                    request.getFactory().createGNode( this, request,
                                        "/" + partialGroupId.replaceAll( "\\.", "/" ) + "/", gNodeName );

                                groupParentResource.getChildren().add( groupResource );

                                folders.put( groupKey, groupResource );

                                groupParentResource = groupResource;
                            }
                            else if ( groupResource != null )
                            {
                                // we found it as already existing, break if this is the node we want
                                if ( groupResource.getPath().equals( getPathForAi( ai, MAVEN.GROUP_ID ) ) )
                                {
                                    break;
                                }

                                groupParentResource = groupResource;
                            }
                        }

                        artifactResource =
                            request.getFactory().createANode( this, request, ai, getPathForAi( ai, MAVEN.ARTIFACT_ID ) );

                        groupParentResource.getChildren().add( artifactResource );

                        folders.put( artifactKey, artifactResource );
                    }

                    versionResource =
                        request.getFactory().createVNode( this, request, ai, getPathForAi( ai, MAVEN.VERSION ) );

                    artifactResource.getChildren().add( versionResource );

                    folders.put( versionKey, versionResource );
                }

                String nodePath = getPathForAi( ai, null );

                versionResource.getChildren().add(
                    request.getFactory().createArtifactNode( this, request, ai, nodePath ) );
            }
        }
        finally
        {
            artifacts.close();
        }

        if ( !request.hasFieldHints() )
        {
            Set<String> groups = getGroups( path, allGroups );

            for ( String group : groups )
            {
                TreeNode groupResource = root.findChildByPath( path + group + "/", Type.G );

                if ( groupResource == null )
                {
                    groupResource = request.getFactory().createGNode( this, request, path + group + "/", group );

                    root.getChildren().add( groupResource );
                }
                else
                {
                    // if the folder has been created as an artifact name,
                    // we need to check for possible nested groups as well
                    listChildren( groupResource, request, allGroups );
                }
            }
        }
    }

    /**
     * Builds a path out from ArtifactInfo. The field parameter controls "how deep" the path goes. Possible values are
     * MAVEN.GROUP_ID (builds a path from groupId only), MAVEN.ARTIFACT_ID (builds a path from groupId + artifactId),
     * MAVEN.VERSION (builds a path up to version) or anything else (including null) will build "full" artifact path.
     * 
     * @param ai
     * @param field
     * @return path
     */
    protected String getPathForAi( ArtifactInfo ai, Field field )
    {
        StringBuilder sb = new StringBuilder( "/" );

        sb.append( ai.getGroupId().replaceAll( "\\.", "/" ) );

        if ( MAVEN.GROUP_ID.equals( field ) )
        {
            // stop here
            return sb.append( "/" ).toString();
        }

        sb.append( "/" ).append( ai.getArtifactId() );

        if ( MAVEN.ARTIFACT_ID.equals( field ) )
        {
            // stop here
            return sb.append( "/" ).toString();
        }

        sb.append( "/" ).append( ai.getVersion() );

        if ( MAVEN.VERSION.equals( field ) )
        {
            // stop here
            return sb.append( "/" ).toString();
        }

        sb.append( "/" ).append( ai.getArtifactId() ).append( "-" ).append( ai.getVersion() );

        if ( ai.getClassifier() != null )
        {
            sb.append( "-" ).append( ai.getClassifier() );
        }

        sb.append( "." ).append( ai.getFileExtension() == null ? "jar" : ai.getFileExtension() );

        return sb.toString();
    }

    protected Set<String> getGroups( String path, Set<String> allGroups )
    {
        path = path.substring( 1 ).replace( '/', '.' );

        int n = path.length();

        Set<String> result = new HashSet<String>();

        for ( String group : allGroups )
        {
            if ( group.startsWith( path ) )
            {
                group = group.substring( n );

                int nextDot = group.indexOf( '.' );

                if ( nextDot > -1 )
                {
                    group = group.substring( 0, nextDot );
                }

                if ( group.length() > 0 && !result.contains( group ) )
                {
                    result.add( group );
                }
            }
        }

        return result;
    }

    protected IteratorSearchResponse getArtifacts( TreeNode root, TreeViewRequest request )
        throws IOException
    {
        if ( request.hasFieldHints() )
        {
            return getHintedArtifacts( root, request );
        }

        String path = root.getPath();

        IteratorSearchResponse result = null;

        String g = null;

        String a = null;

        String v = null;

        // "working copy" of path
        String wp = null;

        // remove last / from path
        if ( path.endsWith( "/" ) )
        {
            path = path.substring( 0, path.length() - 1 );
        }

        // 1st try, let's consider path is a group

        // reset wp
        wp = path;

        g = wp.substring( 1 ).replace( '/', '.' );

        result = getArtifactsByG( g, request );

        if ( result.getTotalHitsCount() > 0 )
        {
            return result;
        }
        else
        {
            result.close();
        }

        // 2nd try, lets consider path a group + artifactId, we must ensure there is at least one / but not as root

        if ( path.lastIndexOf( '/' ) > 0 )
        {
            // reset wp
            wp = path;

            a = wp.substring( wp.lastIndexOf( '/' ) + 1, wp.length() );

            g = wp.substring( 1, wp.lastIndexOf( '/' ) ).replace( '/', '.' );

            result = getArtifactsByGA( g, a, request );

            if ( result.getTotalHitsCount() > 0 )
            {
                return result;
            }
            else
            {
                result.close();
            }

            // 3rd try, let's consider path a group + artifactId + version. There is no 100% way to detect this!

            try
            {
                // reset wp
                wp = path;

                v = wp.substring( wp.lastIndexOf( '/' ) + 1, wp.length() );

                wp = wp.substring( 0, wp.lastIndexOf( '/' ) );

                a = wp.substring( wp.lastIndexOf( '/' ) + 1, wp.length() );

                g = wp.substring( 1, wp.lastIndexOf( '/' ) ).replace( '/', '.' );

                result = getArtifactsByGAV( g, a, v, request );

                if ( result.getTotalHitsCount() > 0 )
                {
                    return result;
                }
                else
                {
                    result.close();
                }
            }
            catch ( StringIndexOutOfBoundsException e )
            {
                // nothing
            }
        }

        // if we are here, no hits found
        return IteratorSearchResponse.empty( result.getQuery() );
    }

    protected IteratorSearchResponse getHintedArtifacts( TreeNode root, TreeViewRequest request )
        throws IOException
    {
        // we know that hints are there: G hint, GA hint or GAV hint
        if ( request.hasFieldHint( MAVEN.GROUP_ID, MAVEN.ARTIFACT_ID, MAVEN.VERSION ) )
        {
            return getArtifactsByGAV( request.getFieldHint( MAVEN.GROUP_ID ),
                request.getFieldHint( MAVEN.ARTIFACT_ID ), request.getFieldHint( MAVEN.VERSION ), request );
        }
        else if ( request.hasFieldHint( MAVEN.GROUP_ID, MAVEN.ARTIFACT_ID ) )
        {
            return getArtifactsByGA( request.getFieldHint( MAVEN.GROUP_ID ), request.getFieldHint( MAVEN.ARTIFACT_ID ),
                request );
        }
        else if ( request.hasFieldHint( MAVEN.GROUP_ID ) )
        {
            return getArtifactsByG( request.getFieldHint( MAVEN.GROUP_ID ), request );
        }
        else
        {
            // if we are here, no hits found or something horribly went wrong?
            return IteratorSearchResponse.empty( null );
        }
    }

    protected IteratorSearchResponse getArtifactsByG( String g, TreeViewRequest request )
        throws IOException
    {
        return getArtifactsByGAVField( g, null, null, request );
    }

    protected IteratorSearchResponse getArtifactsByGA( String g, String a, TreeViewRequest request )
        throws IOException
    {
        return getArtifactsByGAVField( g, a, null, request );
    }

    protected IteratorSearchResponse getArtifactsByGAV( String g, String a, String v, TreeViewRequest request )
        throws IOException
    {
        return getArtifactsByGAVField( g, a, v, request );
    }

    protected IteratorSearchResponse getArtifactsByGAVField( String g, String a, String v, TreeViewRequest request )
        throws IOException
    {
        assert g != null;

        Query groupIdQ = null;
        Query artifactIdQ = null;
        Query versionQ = null;

        // minimum must have
        groupIdQ = getIndexer().constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( g ) );

        if ( StringUtils.isNotBlank( a ) )
        {
            artifactIdQ = getIndexer().constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( a ) );
        }

        if ( StringUtils.isNotBlank( v ) )
        {
            versionQ = getIndexer().constructQuery( MAVEN.VERSION, new SourcedSearchExpression( v ) );
        }

        BooleanQuery q = new BooleanQuery();

        q.add( new BooleanClause( groupIdQ, BooleanClause.Occur.MUST ) );

        if ( artifactIdQ != null )
        {
            q.add( new BooleanClause( artifactIdQ, BooleanClause.Occur.MUST ) );
        }

        if ( versionQ != null )
        {
            q.add( new BooleanClause( versionQ, BooleanClause.Occur.MUST ) );
        }

        IteratorSearchRequest searchRequest = new IteratorSearchRequest( q, request.getArtifactInfoFilter() );

        searchRequest.getContexts().add( request.getIndexingContext() );

        IteratorSearchResponse result = getIndexer().searchIterator( searchRequest );

        return result;
    }
}