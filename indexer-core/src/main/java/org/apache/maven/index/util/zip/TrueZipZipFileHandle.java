package org.apache.maven.index.util.zip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import de.schlichtherle.truezip.zip.ZipEntry;
import de.schlichtherle.truezip.zip.ZipFile;

public class TrueZipZipFileHandle
    extends AbstractZipHandle
    implements ZipHandle
{
    private final ZipFile zipFile;

    public TrueZipZipFileHandle( final File targetFile )
        throws IOException
    {
        super( targetFile );

        this.zipFile = new ZipFile( targetFile );
    }

    protected ZipFile getZipFile()
    {
        return zipFile;
    }

    public boolean hasEntry( String path )
        throws IOException
    {
        return getZipFile().getEntry( path ) != null;
    }

    public List<String> getEntries()
    {
        return getEntries( new EntryNameFilter()
        {
            public boolean accepts( String entryName )
            {
                return true;
            }
        } );
    }

    public List<String> getEntries( EntryNameFilter filter )
    {
        ArrayList<String> entries = new ArrayList<String>();

        Enumeration<? extends ZipEntry> en = getZipFile().entries();

        while ( en.hasMoreElements() )
        {
            final ZipEntry e = en.nextElement();

            final String name = e.getName();

            if ( filter != null && !filter.accepts( name ) )
            {
                continue;
            }

            entries.add( name );
        }

        return entries;
    }

    public InputStream getEntryContent( String path )
        throws IOException
    {
        ZipEntry entry = getZipFile().getEntry( path );

        if ( entry != null )
        {
            return getZipFile().getInputStream( entry );
        }
        else
        {
            return null;
        }
    }

    public void close()
        throws IOException
    {
        getZipFile().close();
    }

}
