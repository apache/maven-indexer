package org.apache.maven.index.util.zip;

import java.io.File;
import java.io.IOException;

public interface ZipFacade
{
    ZipHandle getZipHandle( File targetFile )
        throws IOException;
}
