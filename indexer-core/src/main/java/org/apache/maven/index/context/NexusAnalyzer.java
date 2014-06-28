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

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;
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
    extends Analyzer
{

    public NexusAnalyzer()
    {
        super(PER_FIELD_REUSE_STRATEGY);
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

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader)
    {
        return new TokenStreamComponents(getTokenizer(fieldName, reader));
    }

    // ==

    public static class NoopTokenizer
        extends CharTokenizer
    {
        public NoopTokenizer( Reader in )
        {
            super( Version.LUCENE_46, in );
        }

        @Override
        protected boolean isTokenChar(int i)
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
            super( Version.LUCENE_46, in );
        }
        
        @Override
        protected boolean isTokenChar(int i)
        {
            return i != '\n';
        }
        
        @Override
        protected int normalize(int c)
        {
            return Character.toLowerCase(c);
        }
    }

    public static class LetterOrDigitTokenizer
        extends CharTokenizer
    {
        public LetterOrDigitTokenizer( Reader in )
        {
            super( Version.LUCENE_46, in );
        }

        @Override
        protected boolean isTokenChar(int c)
        {
            return Character.isLetterOrDigit( c );
        }

        @Override
        protected int normalize(int c)
        {
            return Character.toLowerCase(c);
        }
    }

}
