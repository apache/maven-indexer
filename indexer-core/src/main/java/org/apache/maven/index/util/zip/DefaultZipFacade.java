package org.apache.maven.index.util.zip;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = ZipFacade.class )
public class DefaultZipFacade
    implements ZipFacade
{
    public static final long MEGABYTE = 1048576L;

    public static final long JAVA_ZIPFILE_SIZE_THRESHOLD = Long.getLong(
        "org.apache.maven.index.util.zip.DefaultZipFacade.javaZipFileSizeThreshold", 100L * MEGABYTE );

    private static final boolean TRUEZIP_AVAILABLE;

    static
    {
        Class<?> clazz;

        try
        {
            clazz = Class.forName( "de.schlichtherle.truezip.zip.ZipFile" );
        }
        catch ( ClassNotFoundException e )
        {
            clazz = null;
        }

        TRUEZIP_AVAILABLE = clazz != null;
    }

    public ZipHandle getZipHandle( File targetFile )
        throws IOException
    {
        if ( targetFile.isFile() )
        {
            if ( TRUEZIP_AVAILABLE && targetFile.length() > JAVA_ZIPFILE_SIZE_THRESHOLD )
            {
                return new TrueZipZipFileHandle( targetFile );
            }
            else
            {
                return new JavaZipFileHandle( targetFile );
            }
        }

        throw new IOException( "The targetFile should point to an existing ZIP file!" );
    }
}
