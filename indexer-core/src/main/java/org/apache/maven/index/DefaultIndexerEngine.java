package org.apache.maven.index;

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

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * A default {@link IndexerEngine} implementation.
 * 
 * @author Tamas Cservenak
 */
@Component( role = IndexerEngine.class )
public class DefaultIndexerEngine
    extends AbstractLogEnabled
    implements IndexerEngine
{

    public void index( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        // skip artifacts not obeying repository layout (whether m1 or m2)
        if ( ac != null && ac.getGav() != null )
        {
            Document d = ac.createDocument( context );

            if ( d != null )
            {
                context.getIndexWriter().addDocument( d );

                context.updateTimestamp();
            }
        }
    }

    public void update( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        if ( ac != null && ac.getGav() != null )
        {
            Document d = ac.createDocument( context );

            if ( d != null )
            {
                IndexWriter w = context.getIndexWriter();

                w.updateDocument( new Term( ArtifactInfo.UINFO, ac.getArtifactInfo().getUinfo() ), d );

                updateGroups( context, ac );

                context.updateTimestamp();
            }
        }
    }

    public void remove( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        if ( ac != null )
        {
            String uinfo = ac.getArtifactInfo().getUinfo();
            // add artifact deletion marker
            Document doc = new Document();
            doc.add( new Field( ArtifactInfo.DELETED, uinfo, Field.Store.YES, Field.Index.NO ) );
            doc.add( new Field( ArtifactInfo.LAST_MODIFIED, //
                Long.toString( System.currentTimeMillis() ), Field.Store.YES, Field.Index.NO ) );
            IndexWriter w = context.getIndexWriter();
            w.addDocument( doc );
            w.deleteDocuments( new Term( ArtifactInfo.UINFO, uinfo ) );
            context.updateTimestamp();
        }
    }

    // ==

    private void updateGroups( IndexingContext context, ArtifactContext ac )
        throws IOException
    {
        String rootGroup = ac.getArtifactInfo().getRootGroup();
        Set<String> rootGroups = context.getRootGroups();
        if ( !rootGroups.contains( rootGroup ) )
        {
            rootGroups.add( rootGroup );
            context.setRootGroups( rootGroups );
        }

        Set<String> allGroups = context.getAllGroups();
        if ( !allGroups.contains( ac.getArtifactInfo().groupId ) )
        {
            allGroups.add( ac.getArtifactInfo().groupId );
            context.setAllGroups( allGroups );
        }
    }
}
