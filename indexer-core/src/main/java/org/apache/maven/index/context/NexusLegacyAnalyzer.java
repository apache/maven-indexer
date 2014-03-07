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

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.apache.maven.index.ArtifactInfo;

/**
 * A Nexus specific <a
 * href="http://lucene.apache.org/java/2_4_0/api/core/org/apache/lucene/analysis/Analyzer.html">Lucene Analyzer</a> used
 * to produce legacy index transfer format
 * 
 * @author Jason van Zyl
 */
public final class NexusLegacyAnalyzer
    extends Analyzer
{
    private static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer( Version.LUCENE_46 );
    
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader)
    {
        try
        {
            return new TokenStreamComponents((Tokenizer) tokenizer(fieldName, reader));
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }


    protected TokenStream tokenizer( String field, final Reader reader ) throws IOException
    {
        if ( !isTextField( field ) )
        {
            return new CharTokenizer(Version.LUCENE_46, reader )
            {
                @Override
                protected boolean isTokenChar(int c )
                {
                    return Character.isLetterOrDigit( c );
                }

                @Override
                protected int normalize(int c )
                {
                    return Character.toLowerCase( c );
                }
            };
        }
        else
        {
            return DEFAULT_ANALYZER.tokenStream(field, reader );
        }
    }

    protected boolean isTextField( String field )
    {
        return ArtifactInfo.NAME.equals( field ) || ArtifactInfo.DESCRIPTION.equals( field )
            || ArtifactInfo.NAMES.equals( field );

    }

    
}
