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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.maven.index.ArtifactInfo;

/**
 * A Nexus specific <a
 * href="http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/analysis/Analyzer.html">Lucene Analyzer</a> used
 * to produce legacy index transfer format
 * 
 * @author Jason van Zyl
 */
public final class NexusLegacyAnalyzer
    extends AnalyzerWrapper
{
    private static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();

    private static final Analyzer LETTER_OR_DIGIT_ANALYZER = new Analyzer()
    {
        @Override
        protected TokenStreamComponents createComponents( final String fieldName )
        {
            return new TokenStreamComponents( new CharTokenizer()
            {
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
            } );
        }
    };

    public NexusLegacyAnalyzer()
    {
        super( PER_FIELD_REUSE_STRATEGY );
    }

    @Override
    protected Analyzer getWrappedAnalyzer( String fieldName )
    {
        if ( !isTextField( fieldName ) )
        {
            return LETTER_OR_DIGIT_ANALYZER;
        }
        else
        {
            return DEFAULT_ANALYZER;
        }
    }

    protected boolean isTextField( String field )
    {
        return ArtifactInfo.NAME.equals( field ) || ArtifactInfo.DESCRIPTION.equals( field )
            || ArtifactInfo.NAMES.equals( field );
    }
}
