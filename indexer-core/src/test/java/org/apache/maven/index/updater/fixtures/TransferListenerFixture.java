package org.apache.maven.index.updater.fixtures;

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

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;

public class TransferListenerFixture
    implements TransferListener
{
    private static final int ONE_CHUNK = 64;

    private int col = 0;

    private int count = 0;

    private int kb = 0;

    public void transferStarted( final TransferEvent transferEvent )
    {
        System.out.println( "Started transfer: " + transferEvent.getResource().getName() );
    }

    public void transferProgress( final TransferEvent transferEvent, final byte[] buffer, final int length )
    {
        if ( buffer == null )
        {
            return;
        }

        count += buffer.length;

        if ( ( count / ONE_CHUNK ) > kb )
        {
            if ( col > 80 )
            {
                System.out.println();
                col = 0;
            }

            System.out.print( '.' );
            col++;
            kb++;
        }
    }

    public void transferInitiated( final TransferEvent transferEvent )
    {
    }

    public void transferError( final TransferEvent transferEvent )
    {
        System.out.println( "[ERROR]: " + transferEvent.getException().getLocalizedMessage() );
        transferEvent.getException().printStackTrace();
    }

    public void transferCompleted( final TransferEvent transferEvent )
    {
        System.out.println( "\nCompleted transfer: " + transferEvent.getResource().getName() + " ("
            + (double) ( count / ONE_CHUNK ) + " chunks of size: " + ONE_CHUNK + " bytes)" );
    }

    public void debug( final String message )
    {
        System.out.println( "[DEBUG]: " + message );
    }
}
