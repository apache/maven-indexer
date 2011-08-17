package org.apache.maven.index.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.creator.AbstractIndexCreator;

public class SpoofIndexCreator
    extends AbstractIndexCreator
{
    protected SpoofIndexCreator( final String id, final List<String> creatorDependencies )
    {
        super( id, creatorDependencies );
    }

    public Collection<IndexerField> getIndexerFields()
    {
        return Collections.emptyList();
    }

    public void populateArtifactInfo( ArtifactContext artifactContext )
        throws IOException
    {
        // TODO Auto-generated method stub
    }

    public void updateDocument( ArtifactInfo artifactInfo, Document document )
    {
        // TODO Auto-generated method stub
    }

    public boolean updateArtifactInfo( Document document, ArtifactInfo artifactInfo )
    {
        // TODO Auto-generated method stub
        return false;
    }
}
