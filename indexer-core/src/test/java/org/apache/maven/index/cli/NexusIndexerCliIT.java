package org.apache.maven.index.cli;

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
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

public class NexusIndexerCliIT
    extends AbstractNexusIndexerCliTest
{

    private StreamConsumer sout;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        sout = new StreamConsumer()
        {
            public void consumeLine( String line )
            {
                try
                {
                    out.write( line.getBytes() );
                    out.write( "\n".getBytes() );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            }
        };
    }

    private Commandline createCommandLine()
    {
        try
        {
            Commandline cmd = new Commandline();
            cmd.setExecutable( "java" );
            cmd.setWorkingDirectory( new File( "." ).getCanonicalFile() );
            cmd.createArg().setValue( "-jar" );
            cmd.createArg().setValue( new File( System.getProperty( "indexerJar" ) ).getCanonicalPath() );
            return cmd;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected int execute( String... args )
    {
        Commandline cmd = createCommandLine();
        for ( String arg : args )
        {
            cmd.createArg().setValue( arg );
        }
        try
        {
            return CommandLineUtils.executeCommandLine( cmd, sout, sout );
        }
        catch ( CommandLineException e )
        {
            return -1;
        }
    }

}
