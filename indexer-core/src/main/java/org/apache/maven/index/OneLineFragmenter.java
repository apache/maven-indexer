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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.Fragmenter;

public class OneLineFragmenter
    implements Fragmenter
{
    private String text;

    public void start( String originalText )
    {
        setText( originalText );
    }

    protected boolean isNewline( char c )
    {
        return c == '\n';
    }

    protected char getChar( int pos )
    {
        if ( ( pos < 0 ) || ( pos > ( getText().length() - 1 ) ) )
        {
            // return no newline ;)
            return ' ';
        }
        else
        {
            return getText().charAt( pos );
        }
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public boolean isNewFragment()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void start( String arg0, TokenStream arg1 )
    {
        // TODO Auto-generated method stub
        
    }
}
