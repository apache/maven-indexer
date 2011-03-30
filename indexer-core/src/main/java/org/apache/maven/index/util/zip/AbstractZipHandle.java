package org.apache.maven.index.util.zip;

import java.io.File;

public abstract class AbstractZipHandle
    implements ZipHandle
{
    private final File targetFile;

    public AbstractZipHandle( final File targetFile )
    {
        if ( targetFile == null || !targetFile.isFile() )
        {
            throw new IllegalArgumentException(
                "The targetFile may not be null, and has to point to an existing file (not a directory!)" );
        }

        this.targetFile = targetFile;
    }

    public File getTargetFile()
    {
        return targetFile;
    }
}
