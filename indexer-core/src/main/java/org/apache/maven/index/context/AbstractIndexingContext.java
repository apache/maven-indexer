package org.apache.maven.index.context;

import java.io.IOException;

public abstract class AbstractIndexingContext
    implements IndexingContext
{
    public boolean isReceivingUpdates()
    {
        try
        {
            return getIndexDirectory().fileExists( INDEX_UPDATER_PROPERTIES_FILE );
        }
        catch ( IOException e )
        {
            return false;
        }
    }
}
