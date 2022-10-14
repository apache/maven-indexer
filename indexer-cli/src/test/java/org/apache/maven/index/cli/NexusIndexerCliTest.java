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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.maven.index.cli.NexusIndexerCli;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class NexusIndexerCliTest
    extends AbstractNexusIndexerCliTest
{

    protected NexusIndexerCli cli;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        cli = new NexusIndexerCli();

        System.setOut( new PrintStream( out ) );
        System.setErr( new PrintStream( out ) );
    }

    @Override
    protected int execute( String... args )
    {
        return cli.execute( args );
    }

    private final static String LS = System.getProperty( "line.separator" );

    private static class OptionComparator
        implements Comparator<Option>
    {
        public int compare( Option opt1, Option opt2 )
        {
            return opt1.getOpt().compareToIgnoreCase( opt2.getOpt() );
        }
    }

    public String getOptionsAsHtml()
    {
        List<Option> optList = new ArrayList<>( new NexusIndexerCli().buildCliOptions().getOptions() );
        optList.sort( new OptionComparator() );

        StringBuilder sb = new StringBuilder();
        boolean a = true;
        sb.append( "<table border='1' class='zebra-striped'><tr class='a'><th><b>Options</b></th><th><b>Description</b></th></tr>" );
        for ( Option option : optList )
        {
            a = !a;
            sb.append( "<tr class='" ).append( a ? 'a' : 'b' ).append( "'><td><code>-<a name='" );
            sb.append( option.getOpt() );
            sb.append( "'>" );
            sb.append( option.getOpt() );
            sb.append( "</a>,--<a name='" );
            sb.append( option.getLongOpt() );
            sb.append( "'>" );
            sb.append( option.getLongOpt() );
            sb.append( "</a>" );
            if ( option.hasArg() )
            {
                if ( option.hasArgName() )
                {
                    sb.append( " &lt;" ).append( option.getArgName() ).append( "&gt;" );
                }
                else
                {
                    sb.append( ' ' );
                }
            }
            sb.append( "</code></td><td>" );
            sb.append( option.getDescription() );
            sb.append( "</td></tr>" );
            sb.append( LS );
        }
        sb.append( "</table>" );
        return sb.toString();
    }

    @Test
    public void testOptionsAsHtml()
        throws IOException
    {
        File options = getTestFile( "target/test-classes/options.html" );
        FileUtils.fileWrite( options, "UTF-8", getOptionsAsHtml() );
    }
}
