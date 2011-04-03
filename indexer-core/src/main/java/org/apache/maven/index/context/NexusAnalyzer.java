package org.apache.maven.index.context;

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

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;

/**
 * A Nexus specific analyzer. Only difference from Lucene's SimpleAnalyzer is that we use LetterOrDigitTokenizer instead
 * of LowerCaseTokenizer. LetterOrDigitTokenizer does pretty much the same as LowerCaseTokenizer, it normalizes to lower
 * case letter, but it takes letters and numbers too (as opposed to LowerCaseTokenizer) as token chars.
 * 
 * @author Eugene Kuleshov
 * @author cstamas
 */
public class NexusAnalyzer
    extends Analyzer
{
    public TokenStream tokenStream( String fieldName, Reader reader )
    {
        return getTokenizer( fieldName, reader );
    }

    protected Tokenizer getTokenizer( String fieldName, Reader reader )
    {
        if ( JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.getKey().equals( fieldName ) )
        {
            // To keep "backward" compatibility, we have to use old flawed tokenizer.
            return new DeprecatedClassnamesTokenizer( reader );
        }
        else
        {
            return new LetterOrDigitTokenizer( reader );
        }
    }

    // ==

    public static class NoopTokenizer
        extends CharTokenizer
    {
        public NoopTokenizer( Reader in )
        {
            super( in );
        }

        @Override
        protected boolean isTokenChar( char c )
        {
            return true;
        }
    }

    @Deprecated
    public static class DeprecatedClassnamesTokenizer
        extends CharTokenizer
    {
        public DeprecatedClassnamesTokenizer( Reader in )
        {
            super( in );
        }

        @Override
        protected boolean isTokenChar( char c )
        {
            return c != '\n';
        }

        @Override
        protected char normalize( char c )
        {
            return Character.toLowerCase( c );
        }
    }

    public static class LetterOrDigitTokenizer
        extends CharTokenizer
    {
        public LetterOrDigitTokenizer( Reader in )
        {
            super( in );
        }

        @Override
        protected boolean isTokenChar( char c )
        {
            return Character.isLetterOrDigit( c );
        }

        @Override
        protected char normalize( char c )
        {
            return Character.toLowerCase( c );
        }
    }

}
