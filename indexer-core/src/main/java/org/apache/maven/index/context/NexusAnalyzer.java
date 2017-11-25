package org.apache.maven.index.context;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;

/**
 * A Nexus specific analyzer. Only difference from Lucene's SimpleAnalyzer is that we use LetterOrDigitTokenizer instead
 * of LowerCaseTokenizer. LetterOrDigitTokenizer does pretty much the same as LowerCaseTokenizer, it normalizes to lower
 * case letter, but it takes letters and numbers too (as opposed to LowerCaseTokenizer) as token chars.
 * 
 * @author Eugene Kuleshov
 * @author cstamas
 */
public final class NexusAnalyzer
    extends AnalyzerWrapper
{
    private static final Analyzer CLASS_NAMES_ANALYZER = new Analyzer()
    {
        @Override
        protected TokenStreamComponents createComponents( String fieldName )
        {
            return new TokenStreamComponents( new DeprecatedClassnamesTokenizer() );
        }
    };

    private static final Analyzer LETTER_OR_DIGIT_ANALYZER = new Analyzer()
    {
        @Override
        protected TokenStreamComponents createComponents( String filedName )
        {
            return new TokenStreamComponents( new LetterOrDigitTokenizer() );
        }
    };

    public NexusAnalyzer()
    {
        super( PER_FIELD_REUSE_STRATEGY );
    }

    @Override
    protected Analyzer getWrappedAnalyzer( String fieldName )
    {
        if ( JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.getKey().equals( fieldName ) )
        {
            // To keep "backward" compatibility, we have to use old flawed tokenizer.
            return CLASS_NAMES_ANALYZER;
        }
        else
        {
            return LETTER_OR_DIGIT_ANALYZER;
        }
    }

    // ==

    public static class NoopTokenizer
        extends CharTokenizer
    {
        public NoopTokenizer()
        {
            super();
        }

        @Override
        protected boolean isTokenChar( int i )
        {
            return true;
        }
    }

    @Deprecated
    public static class DeprecatedClassnamesTokenizer
        extends CharTokenizer
    {
        public DeprecatedClassnamesTokenizer()
        {
            super();
        }
        
        @Override
        protected boolean isTokenChar( int i )
        {
            return i != '\n';
        }
        
        @Override
        protected int normalize( int c )
        {
            return Character.toLowerCase( c );
        }
    }

    public static class LetterOrDigitTokenizer
        extends CharTokenizer
    {
        public LetterOrDigitTokenizer()
        {
            super();
        }

        @Override
        protected boolean isTokenChar( int c )
        {
            return Character.isLetterOrDigit( c );
        }

        @Override
        protected int normalize( int c )
        {
            return Character.toLowerCase( c );
        }
    }

}
