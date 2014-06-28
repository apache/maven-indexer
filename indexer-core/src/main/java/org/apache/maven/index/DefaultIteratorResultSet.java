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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusIndexMultiSearcher;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;

/**
 * Default implementation of IteratorResultSet. TODO: there is too much of logic, refactor this!
 * 
 * @author cstamas
 */
public class DefaultIteratorResultSet
    implements IteratorResultSet
{
    private final IteratorSearchRequest searchRequest;

    private final NexusIndexMultiSearcher indexSearcher;

    private final List<IndexingContext> contexts;

    private final int[] starts;

    private final ArtifactInfoFilter filter;

    private final ArtifactInfoPostprocessor postprocessor;

    private final List<MatchHighlightRequest> matchHighlightRequests;

    private final TopDocs hits;

    private final int from;

    private final int count;

    private final int maxRecPointer;

    private int pointer;

    private int processedArtifactInfoCount;

    private ArtifactInfo ai;

    protected DefaultIteratorResultSet( final IteratorSearchRequest request,
                                        final NexusIndexMultiSearcher indexSearcher,
                                        final List<IndexingContext> contexts, final TopDocs hits )
        throws IOException
    {
        this.searchRequest = request;

        this.indexSearcher = indexSearcher;

        this.contexts = contexts;

        {
            int maxDoc = 0;
            this.starts = new int[contexts.size() + 1]; // build starts array
            // this is good to do as we have NexusIndexMultiSearcher passed in contructor, so it is already open, hence
            // #acquire() already invoked on underlying NexusIndexMultiReader
            final List<IndexSearcher> acquiredSearchers = indexSearcher.getNexusIndexMultiReader().getAcquiredSearchers();
            for ( int i = 0; i < contexts.size(); i++ )
            {
                starts[i] = maxDoc;
                maxDoc += acquiredSearchers.get( i ).getIndexReader().maxDoc(); // compute maxDocs
            }
            starts[contexts.size()] = maxDoc;
        }

        this.filter = request.getArtifactInfoFilter();

        this.postprocessor = request.getArtifactInfoPostprocessor();

        this.matchHighlightRequests = request.getMatchHighlightRequests();

        List<MatchHighlightRequest> matchHighlightRequests = new ArrayList<MatchHighlightRequest>();
        for ( MatchHighlightRequest hr : request.getMatchHighlightRequests() )
        {
            Query rewrittenQuery = hr.getQuery().rewrite( indexSearcher.getIndexReader() );
            matchHighlightRequests.add( new MatchHighlightRequest( hr.getField(), rewrittenQuery, hr.getHighlightMode() ) );
        }

        this.hits = hits;

        this.from = request.getStart();

        this.count =
            ( request.getCount() == AbstractSearchRequest.UNDEFINED ? hits.scoreDocs.length : Math.min(
                request.getCount(), hits.scoreDocs.length ) );

        this.pointer = from;

        this.processedArtifactInfoCount = 0;

        this.maxRecPointer = from + count;

        ai = createNextAi();

        if ( ai == null )
        {
            cleanUp();
        }
    }

    public boolean hasNext()
    {
        return ai != null;
    }

    public ArtifactInfo next()
    {
        ArtifactInfo result = ai;

        try
        {
            ai = createNextAi();
        }
        catch ( IOException e )
        {
            ai = null;

            throw new IllegalStateException( "Cannot fetch next ArtifactInfo!", e );
        }
        finally
        {
            if ( ai == null )
            {
                cleanUp();
            }
        }

        return result;
    }

    public void remove()
    {
        throw new UnsupportedOperationException( "Method not supported on " + getClass().getName() );
    }

    public Iterator<ArtifactInfo> iterator()
    {
        return this;
    }

    public void close()
    {
        cleanUp();
    }

    public int getTotalProcessedArtifactInfoCount()
    {
        return processedArtifactInfoCount;
    }

    @Override
    public void finalize()
        throws Throwable
    {
        super.finalize();

        if ( !cleanedUp )
        {
            System.err.println( "#WARNING: Lock leaking from " + getClass().getName() + " for query "
                + searchRequest.getQuery().toString() );

            cleanUp();
        }
    }

    // ==

    protected ArtifactInfo createNextAi()
        throws IOException
    {
        ArtifactInfo result = null;

        // we should stop if:
        // a) we found what we want
        // b) pointer advanced over more documents that user requested
        // c) pointer advanced over more documents that hits has
        // or we found what we need
        while ( ( result == null ) && ( pointer < maxRecPointer ) && ( pointer < hits.scoreDocs.length ) )
        {
            Document doc = indexSearcher.doc( hits.scoreDocs[pointer].doc );

            IndexingContext context = getIndexingContextForPointer( doc, hits.scoreDocs[pointer].doc );

            result = IndexUtils.constructArtifactInfo( doc, context );

            if ( result != null )
            {
                // WARNING: NOT FOR PRODUCTION SYSTEMS, THIS IS VERY COSTLY OPERATION
                // For debugging only!!!
                if ( searchRequest.isLuceneExplain() )
                {
                    result.getAttributes().put( Explanation.class.getName(),
                        indexSearcher.explain( searchRequest.getQuery(), hits.scoreDocs[pointer].doc ).toString() );
                }

                result.setLuceneScore( hits.scoreDocs[pointer].score );

                result.setRepository( context.getRepositoryId() );

                result.setContext( context.getId() );

                if ( filter != null )
                {
                    if ( !filter.accepts( context, result ) )
                    {
                        result = null;
                    }
                }

                if ( result != null && postprocessor != null )
                {
                    postprocessor.postprocess( context, result );
                }

                if ( result != null && matchHighlightRequests.size() > 0 )
                {
                    calculateHighlights( context, doc, result );
                }
            }

            pointer++;
            processedArtifactInfoCount++;
        }

        return result;
    }

    private volatile boolean cleanedUp = false;

    protected synchronized void cleanUp()
    {
        if ( cleanedUp )
        {
            return;
        }

        try
        {
            indexSearcher.release();
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( e );
        }

        this.cleanedUp = true;
    }

    /**
     * Creates the MatchHighlights and adds them to ArtifactInfo if found/can.
     * 
     * @param context
     * @param d
     * @param ai
     */
    protected void calculateHighlights( IndexingContext context, Document d, ArtifactInfo ai )
        throws IOException
    {
        IndexerField field = null;

        String text = null;

        List<String> highlightFragment = null;

        for ( MatchHighlightRequest hr : matchHighlightRequests )
        {
            field = selectStoredIndexerField( hr.getField() );

            if ( field != null )
            {
                text = ai.getFieldValue( field.getOntology() );

                if ( text != null )
                {
                    highlightFragment = highlightField( context, hr, field, text );

                    if ( highlightFragment != null && highlightFragment.size() > 0 )
                    {
                        MatchHighlight matchHighlight = new MatchHighlight( hr.getField(), highlightFragment );

                        ai.getMatchHighlights().add( matchHighlight );
                    }
                }
            }
        }
    }

    /**
     * Select a STORED IndexerField assigned to passed in Field.
     * 
     * @param field
     * @return
     */
    protected IndexerField selectStoredIndexerField( Field field )
    {
        // hack here
        if ( MAVEN.CLASSNAMES.equals( field ) )
        {
            return JarFileContentsIndexCreator.FLD_CLASSNAMES;
        }
        else
        {
            return field.getIndexerFields().isEmpty() ? null : field.getIndexerFields().iterator().next();
        }
    }

    /**
     * Returns a string that contains match fragment highlighted in style as user requested.
     * 
     * @param context
     * @param hr
     * @param field
     * @param text
     * @return
     * @throws IOException
     */
    protected List<String> highlightField( IndexingContext context, MatchHighlightRequest hr, IndexerField field,
                                           String text )
        throws IOException
    {
        // exception with classnames
        if ( MAVEN.CLASSNAMES.equals( field.getOntology() ) )
        {
            text = text.replace( '/', '.' ).replaceAll( "^\\.", "" ).replaceAll( "\n\\.", "\n" );
        }
        
        Analyzer analyzer = context.getAnalyzer();
        TokenStream baseTokenStream = analyzer.tokenStream( field.getKey(), new StringReader( text ) );
        baseTokenStream.reset();
        
        CachingTokenFilter tokenStream = new CachingTokenFilter(baseTokenStream);

        Formatter formatter = null;

        if ( MatchHighlightMode.HTML.equals( hr.getHighlightMode() ) )
        {
            formatter = new SimpleHTMLFormatter();
        }
        else
        {
            tokenStream.reset();
            tokenStream.end();
            tokenStream.close();
            throw new UnsupportedOperationException( "Hightlight more \"" + hr.getHighlightMode().toString()
                + "\" is not supported!" );
        }

        List<String> bestFragments = getBestFragments( hr.getQuery(), formatter, tokenStream, text, 3 );
        
        tokenStream.end();
        tokenStream.close();
        
        return bestFragments;
    }

    protected final List<String> getBestFragments( Query query, Formatter formatter, TokenStream tokenStream,
                                                   String text, int maxNumFragments )
        throws IOException
    {
        Highlighter highlighter = new Highlighter( formatter, new CleaningEncoder(), new QueryScorer( query ) );

        highlighter.setTextFragmenter( new OneLineFragmenter() );

        maxNumFragments = Math.max( 1, maxNumFragments ); // sanity check

        TextFragment[] frag;
        // Get text
        ArrayList<String> fragTexts = new ArrayList<String>( maxNumFragments );

        try
        {
            frag = highlighter.getBestTextFragments( tokenStream, text, false, maxNumFragments );

            for ( int i = 0; i < frag.length; i++ )
            {
                if ( ( frag[i] != null ) && ( frag[i].getScore() > 0 ) )
                {
                    fragTexts.add( frag[i].toString() );
                }
            }
        }
        catch ( InvalidTokenOffsetsException e )
        {
            // empty?
        }

        return fragTexts;
    }

    protected IndexingContext getIndexingContextForPointer( Document doc, int docPtr )
    {
        return contexts.get( readerIndex( docPtr, this.starts, this.contexts.size() ) );
    }

    private static int readerIndex( int n, int[] starts, int numSubReaders )
    { // find reader for doc n:
        int lo = 0; // search starts array
        int hi = numSubReaders - 1; // for first element less

        while ( hi >= lo )
        {
            int mid = ( lo + hi ) >>> 1;
            int midValue = starts[mid];
            if ( n < midValue )
            {
                hi = mid - 1;
            }
            else if ( n > midValue )
            {
                lo = mid + 1;
            }
            else
            { // found a match
                while ( mid + 1 < numSubReaders && starts[mid + 1] == midValue )
                {
                    mid++; // scan to last match
                }
                return mid;
            }
        }
        return hi;
    }
}
