package org.apache.maven.index.packer;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.codehaus.plexus.util.IOUtil;

/**
 * A util class to calculate various digests on Strings. Useful for some simple password management.
 * 
 * @author cstamas
 */
public class DigesterUtils
{
    /**
     * Calculates a digest for a String user the requested algorithm.
     * 
     * @param alg
     * @param is
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static String getDigest( String alg, InputStream is )
        throws NoSuchAlgorithmException
    {
        String result = null;

        try
        {
            try
            {
                byte[] buffer = new byte[1024];

                MessageDigest md = MessageDigest.getInstance( alg );

                int numRead;

                do
                {
                    numRead = is.read( buffer );

                    if ( numRead > 0 )
                    {
                        md.update( buffer, 0, numRead );
                    }
                }
                while ( numRead != -1 );

                result = new String( encodeHex( md.digest() ) );
            }
            finally
            {
                is.close();
            }
        }
        catch ( IOException e )
        {
            // hrm
            result = null;
        }

        return result;
    }

    // SHA1

    /**
     * Calculates a SHA1 digest for a string.
     * 
     * @param content
     * @return
     */
    public static String getSha1Digest( String content )
    {
        try
        {
            InputStream fis = new ByteArrayInputStream( content.getBytes( "UTF-8" ) );

            return getDigest( "SHA1", fis );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
        catch ( UnsupportedEncodingException e )
        {
            // will not happen
            return null;
        }
    }

    /**
     * Calculates a SHA1 digest for a stream.
     * 
     * @param is
     * @return
     */
    public static String getSha1Digest( InputStream is )
    {
        try
        {
            return getDigest( "SHA1", is );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
    }

    /**
     * Calculates a SHA1 digest for a file.
     * 
     * @param file
     * @return
     */
    public static String getSha1Digest( File file )
        throws IOException
    {
        try (FileInputStream fis = new FileInputStream( file ))
        {
            return getDigest( "SHA1", fis );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
        catch ( FileNotFoundException e )
        {
            // will not happen
            return null;
        }
    }

    // MD5

    /**
     * Calculates a SHA1 digest for a string.
     * 
     * @param content
     * @return
     */
    public static String getMd5Digest( String content )
    {
        try
        {
            InputStream fis = new ByteArrayInputStream( content.getBytes( "UTF-8" ) );

            return getDigest( "MD5", fis );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
        catch ( UnsupportedEncodingException e )
        {
            // will not happen
            return null;
        }
    }

    /**
     * Calculates a SHA1 digest for a stream.
     * 
     * @param is
     * @return
     */
    public static String getMd5Digest( InputStream is )
    {
        try
        {
            return getDigest( "MD5", is );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
    }

    /**
     * Calculates a SHA1 digest for a file.
     * 
     * @param file
     * @return
     */
    public static String getMd5Digest( File file )
        throws IOException
    {

        try (InputStream fis = new FileInputStream( file ))
        {
            return getDigest( "MD5", fis );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // will not happen
            return null;
        }
        catch ( FileNotFoundException e )
        {
            // will not happen
            return null;
        }
    }

    // --

    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
        'f' };

    /**
     * Blatantly copied from commons-codec version 1.3
     * 
     * @param data
     * @return
     */
    public static char[] encodeHex( byte[] data )
    {
        int l = data.length;

        char[] out = new char[l << 1];

        // two characters form the hex value.
        for ( int i = 0, j = 0; i < l; i++ )
        {
            out[j++] = DIGITS[( 0xF0 & data[i] ) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }

        return out;
    }

}
