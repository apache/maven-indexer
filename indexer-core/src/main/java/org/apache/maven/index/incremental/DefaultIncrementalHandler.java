package org.apache.maven.index.incremental;

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
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public class DefaultIncrementalHandler
    implements IncrementalHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    public List<Integer> getIncrementalUpdates( IndexPackingRequest request, Properties properties )
        throws IOException
    {
        getLogger().debug( "Handling Incremental Updates" );

        if ( !validateProperties( properties ) )
        {
            getLogger().debug( "Invalid properties found, resetting them and doing no incremental packing." );
            return null;
        }

        // Get the list of document ids that have been added since the last time
        // the index ran
        List<Integer> chunk =
            getIndexChunk( request, parse( properties.getProperty( IndexingContext.INDEX_TIMESTAMP ) ) );

        getLogger().debug( "Found " + chunk.size() + " differences to put in incremental index." );

        // if no documents, then we don't need to do anything, no changes
        if ( chunk.size() > 0 )
        {
            updateProperties( properties, request );
        }

        cleanUpIncrementalChunks( request, properties );

        return chunk;
    }

    public List<String> loadRemoteIncrementalUpdates( IndexUpdateRequest request, Properties localProperties,
                                                      Properties remoteProperties )
        throws IOException
    {
        List<String> filenames = null;
        // If we have local properties, will parse and see what we need to download
        if ( canRetrieveAllChunks( localProperties, remoteProperties ) )
        {
            filenames = new ArrayList<>();

            int maxCounter = Integer.parseInt( remoteProperties.getProperty( IndexingContext.INDEX_CHUNK_COUNTER ) );
            int currentCounter = Integer.parseInt( localProperties.getProperty( IndexingContext.INDEX_CHUNK_COUNTER ) );

            // Start with the next one
            currentCounter++;

            while ( currentCounter <= maxCounter )
            {
                filenames.add( IndexingContext.INDEX_FILE_PREFIX + "." + currentCounter++ + ".gz" );
            }
        }

        return filenames;
    }

    private boolean validateProperties( Properties properties )
    {
        if ( properties == null || properties.isEmpty() )
        {
            return false;
        }

        if ( properties.getProperty( IndexingContext.INDEX_TIMESTAMP ) == null )
        {
            return false;
        }

        if ( parse( properties.getProperty( IndexingContext.INDEX_TIMESTAMP ) ) == null )
        {
            return false;
        }

        initializeProperties( properties );

        return true;
    }

    public void initializeProperties( Properties properties )
    {
        if ( properties.getProperty( IndexingContext.INDEX_CHAIN_ID ) == null )
        {
            properties.setProperty( IndexingContext.INDEX_CHAIN_ID, Long.toString( new Date().getTime() ) );
            properties.remove( IndexingContext.INDEX_CHUNK_COUNTER );
        }

        if ( properties.getProperty( IndexingContext.INDEX_CHUNK_COUNTER ) == null )
        {
            properties.setProperty( IndexingContext.INDEX_CHUNK_COUNTER, "0" );
        }
    }

    private List<Integer> getIndexChunk( IndexPackingRequest request, Date timestamp )
        throws IOException
    {
        final List<Integer> chunk = new ArrayList<>();
        final IndexReader r = request.getIndexReader();
        Bits liveDocs = MultiFields.getLiveDocs( r );
        for ( int i = 0; i < r.maxDoc(); i++ )
        {
            if ( liveDocs == null || liveDocs.get( i ) )
            {
                Document d = r.document( i );

                String lastModified = d.get( ArtifactInfo.LAST_MODIFIED );

                if ( lastModified != null )
                {
                    Date t = new Date( Long.parseLong( lastModified ) );

                    // Only add documents that were added after the last time we indexed
                    if ( t.after( timestamp ) )
                    {
                        chunk.add( i );
                    }
                }
            }
        }

        return chunk;
    }

    private void updateProperties( Properties properties, IndexPackingRequest request )
        throws IOException
    {
        Set<Object> keys = new HashSet<>( properties.keySet() );
        Map<Integer, String> dataMap = new TreeMap<>();

        // First go through and retrieve all keys and their values
        for ( Object key : keys )
        {
            String sKey = (String) key;

            if ( sKey.startsWith( IndexingContext.INDEX_CHUNK_PREFIX ) )
            {
                Integer count = Integer.valueOf( sKey.substring( IndexingContext.INDEX_CHUNK_PREFIX.length() ) );
                String value = properties.getProperty( sKey );

                dataMap.put( count, value );
                properties.remove( key );
            }
        }

        String val = properties.getProperty( IndexingContext.INDEX_CHUNK_COUNTER );

        int i = 0;
        // Next put the items back in w/ proper keys
        for ( Entry<Integer, String> entry : dataMap.entrySet() )
        {
            // make sure to end if we reach limit, 0 based
            if ( i >= ( request.getMaxIndexChunks() - 1 ) )
            {
                break;
            }

            properties.put( IndexingContext.INDEX_CHUNK_PREFIX + ( entry.getKey() + 1 ), entry.getValue() );

            i++;
        }

        int nextValue = Integer.parseInt( val ) + 1;

        // Now put the new one in, and update the counter
        properties.put( IndexingContext.INDEX_CHUNK_PREFIX + "0", Integer.toString( nextValue ) );
        properties.put( IndexingContext.INDEX_CHUNK_COUNTER, Integer.toString( nextValue ) );
    }

    private void cleanUpIncrementalChunks( IndexPackingRequest request, Properties properties )
        throws IOException
    {
        File[] files = request.getTargetDir().listFiles( new FilenameFilter()
        {
            public boolean accept( File dir, String name )
            {
                String[] parts = name.split( "\\." );

                if ( parts.length == 3 && parts[0].equals( IndexingContext.INDEX_FILE_PREFIX ) && parts[2].equals(
                    "gz" ) )
                {
                    return true;
                }

                return false;
            }
        } );

        for ( int i = 0; i < files.length; i++ )
        {
            String[] parts = files[i].getName().split( "\\." );

            boolean found = false;
            for ( Entry<Object, Object> entry : properties.entrySet() )
            {
                if ( entry.getKey().toString().startsWith( IndexingContext.INDEX_CHUNK_PREFIX )
                    && entry.getValue().equals( parts[1] ) )
                {
                    found = true;
                    break;
                }
            }

            if ( !found )
            {
                files[i].delete();
            }
        }
    }

    private Date parse( String s )
    {
        try
        {
            SimpleDateFormat df = new SimpleDateFormat( IndexingContext.INDEX_TIME_FORMAT );
            df.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
            return df.parse( s );
        }
        catch ( ParseException e )
        {
            return null;
        }
    }

    private boolean canRetrieveAllChunks( Properties localProps, Properties remoteProps )
    {
        // no localprops, can't retrieve chunks
        if ( localProps == null )
        {
            return false;
        }

        String localChainId = localProps.getProperty( IndexingContext.INDEX_CHAIN_ID );
        String remoteChainId = remoteProps.getProperty( IndexingContext.INDEX_CHAIN_ID );

        // If no chain id, or not the same, do whole download
        if ( StringUtils.isEmpty( localChainId ) || !localChainId.equals( remoteChainId ) )
        {
            return false;
        }

        String counterProp = localProps.getProperty( IndexingContext.INDEX_CHUNK_COUNTER );

        // no counter, cant retrieve chunks
        // not a number, cant retrieve chunks
        if ( StringUtils.isEmpty( counterProp ) || !StringUtils.isNumeric( counterProp ) )
        {
            return false;
        }

        int currentLocalCounter = Integer.parseInt( counterProp );

        // check remote props for existence of next chunk after local
        // if we find it, then we are ok to retrieve the rest of the chunks
        for ( Object key : remoteProps.keySet() )
        {
            String sKey = (String) key;

            if ( sKey.startsWith( IndexingContext.INDEX_CHUNK_PREFIX ) )
            {
                String value = remoteProps.getProperty( sKey );

                // If we have the current counter, or the next counter, we are good to go
                if ( Integer.toString( currentLocalCounter ).equals( value ) || Integer.toString(
                    currentLocalCounter + 1 ).equals( value ) )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
