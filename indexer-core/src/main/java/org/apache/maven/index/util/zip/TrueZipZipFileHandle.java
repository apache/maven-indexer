package org.apache.maven.index.util.zip;

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
