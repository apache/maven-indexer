package org.apache.maven.index;

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

/**
 * Enumeration provides values indicating artifact availability
 */
public enum ArtifactAvailability
{
    /**
     * Artifact is not present locally
     */
    NOT_PRESENT( 0 ),

    /**
     * Artifact is present locally
     */
    PRESENT( 1 ),

    /**
     * Artifact is not available
     */
    NOT_AVAILABLE( 2 );

    private final int n;

    private ArtifactAvailability( int n )
    {
        this.n = n;
    }

    @Override
    public String toString()
    {
        return Integer.toString( n );
    }

    public static ArtifactAvailability fromString( String s )
    {
        try
        {
            switch ( Integer.parseInt( s ) )
            {
                case 1:
                    return PRESENT;
                case 2:
                    return NOT_AVAILABLE;
                default:
                    return NOT_PRESENT;
            }
        }
        catch ( NumberFormatException ex )
        {
            return NOT_PRESENT;
        }
    }
}
