package org.apache.maven.index.util.zip;

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

import java.io.File;
import java.io.IOException;

public class ZipFacade
{
    public static final long MEGABYTE = 1048576L;

    public static final long JAVA_ZIPFILE_SIZE_THRESHOLD = Long.getLong(
        "org.apache.maven.index.util.zip.ZipFacade.javaZipFileSizeThreshold", 100L * MEGABYTE );

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

    public static ZipHandle getZipHandle( File targetFile )
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

        throw new IOException( "The targetFile should point to an existing ZIP file:" + targetFile );
    }

    public static void close( ZipHandle handle )
        throws IOException
    {
        if ( handle != null )
        {
            handle.close();
        }
    }
}
