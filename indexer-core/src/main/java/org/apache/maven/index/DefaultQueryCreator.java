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

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.StringReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Version;
import org.apache.maven.index.context.NexusAnalyzer;
import org.apache.maven.index.creator.JarFileContentsIndexCreator;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;
import org.apache.maven.index.expr.SearchExpression;
import org.apache.maven.index.expr.SearchTyped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link QueryCreator} constructs Lucene query for provided query text.
 * <p>
 * By default wildcards are created such as query text matches beginning of the field value or beginning of the
 * class/package name segment for {@link ArtifactInfo#NAMES NAMES} field. But it can be controlled by using special
 * markers:
 * <ul>
 * <li>* - any character</li>
 * <li>'^' - beginning of the text</li>
 * <li>'$' or '&lt;' or ' ' end of the text</li>
 * </ul>
 * For example:
 * <ul>
 * <li>junit - matches junit and junit-foo, but not foo-junit</li>
 * <li>*junit - matches junit, junit-foo and foo-junit</li>
 * <li>^junit$ - matches junit, but not junit-foo, nor foo-junit</li>
 * </ul>
 * 
 * @author Eugene Kuleshov
 */
@Singleton
@Named
public class DefaultQueryCreator
    implements QueryCreator
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Logger getLogger()
    {
        return logger;
    }

    // ==

    public IndexerField selectIndexerField( final Field field, final SearchType type )
    {
        IndexerField lastField = null;

        for ( IndexerField indexerField : field.getIndexerFields() )
        {
            lastField = indexerField;

            if ( type.matchesIndexerField( indexerField ) )
            {
                return indexerField;
            }
        }

        return lastField;
    }

    public Query constructQuery( final Field field, final SearchExpression expression )
        throws ParseException
    {
        SearchType searchType = SearchType.SCORED;

        if ( expression instanceof SearchTyped )
        {
            searchType = ( (SearchTyped) expression ).getSearchType();
        }

        return constructQuery( field, expression.getStringValue(), searchType );
    }

    public Query constructQuery( final Field field, final String query, final SearchType type )
        throws ParseException
    {
        if ( type == null )
        {
            throw new NullPointerException( "Cannot construct query with type of \"null\"!" );
        }

        if ( field == null )
        {
            throw new NullPointerException( "Cannot construct query for field \"null\"!" );
        }
        else
        {
            return constructQuery( field, selectIndexerField( field, type ), query, type );
        }
    }

    @Deprecated
    public Query constructQuery( String field, String query )
    {
        Query result = null;

        if ( MinimalArtifactInfoIndexCreator.FLD_GROUP_ID_KW.getKey().equals( field )
            || MinimalArtifactInfoIndexCreator.FLD_ARTIFACT_ID_KW.getKey().equals( field )
            || MinimalArtifactInfoIndexCreator.FLD_VERSION_KW.getKey().equals( field )
            || JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.getKey().equals( field ) )
        {
            // these are special untokenized fields, kept for use cases like TreeView is (exact matching).
            result = legacyConstructQuery( field, query );
        }
        else
        {
            QueryParser qp = new QueryParser( Version.LUCENE_46, field, new NexusAnalyzer() );

            // small cheap trick
            // if a query is not "expert" (does not contain field:val kind of expression)
            // but it contains star and/or punctuation chars, example: "common-log*"
            if ( !query.contains( ":" ) )
            {
                if ( query.contains( "*" ) && query.matches( ".*(\\.|-|_).*" ) )
                {
                    query =
                        query.toLowerCase().replaceAll( "\\*", "X" ).replaceAll( "\\.|-|_", " " ).replaceAll( "X", "*" );
                }
            }

            try
            {
                result = qp.parse( query );
            }
            catch ( ParseException e )
            {
                getLogger().debug(
                    "Query parsing with \"legacy\" method, we got ParseException from QueryParser: " + e.getMessage() );

                result = legacyConstructQuery( field, query );
            }
        }

        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Query parsed as: " + result.toString() );
        }

        return result;
    }

    // ==

    public Query constructQuery( final Field field, final IndexerField indexerField, final String query,
                                 final SearchType type )
        throws ParseException
    {
        if ( indexerField == null )
        {
            getLogger().warn(
                "Querying for field \""
                    + field.toString()
                    + "\" without any indexer field was tried. Please review your code, and consider adding this field to index!" );

            return null;
        }
        if ( !indexerField.isIndexed() )
        {
            getLogger().warn(
                "Querying for non-indexed field " + field.toString()
                    + " was tried. Please review your code or consider adding this field to index!" );

            return null;
        }

        if ( query.startsWith( "*" ) || query.startsWith( "?" ) )
        {
            throw new ParseException( "Query cannot start with '*' or '?'!" );
        }

        if ( Field.NOT_PRESENT.equals( query ) )
        {
            return new WildcardQuery( new Term( indexerField.getKey(), "*" ) );
        }

        if ( SearchType.EXACT.equals( type ) )
        {
            if ( indexerField.isKeyword() )
            {
                // no tokenization should happen against the field!
                if ( query.contains( "*" ) || query.contains( "?" ) )
                {
                    return new WildcardQuery( new Term( indexerField.getKey(), query ) );
                }
                else
                {
                    // exactly what callee wants
                    return new TermQuery( new Term( indexerField.getKey(), query ) );
                }
            }
            else if ( !indexerField.isKeyword() && indexerField.isStored() )
            {
                // TODO: resolve this better! Decouple QueryCreator and IndexCreators!
                // This is a hack/workaround here
                if ( JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.equals( indexerField ) )
                {
                    if ( query.startsWith( "/" ) )
                    {
                        return new TermQuery( new Term( indexerField.getKey(), query.toLowerCase().replaceAll( "\\.",
                            "/" ) ) );
                    }
                    else
                    {
                        return new TermQuery( new Term( indexerField.getKey(), "/"
                            + query.toLowerCase().replaceAll( "\\.", "/" ) ) );
                    }
                }
                else
                {
                    getLogger().warn(
                        type.toString()
                            + " type of querying for non-keyword (but stored) field "
                            + indexerField.getOntology().toString()
                            + " was tried. Please review your code, or indexCreator involved, since this type of querying of this field is currently unsupported." );

                    // will never succeed (unless we supply him "filter" too, but that would kill performance)
                    // and is possible with stored fields only
                    return null;
                }
            }
            else
            {
                getLogger().warn(
                    type.toString()
                        + " type of querying for non-keyword (and not stored) field "
                        + indexerField.getOntology().toString()
                        + " was tried. Please review your code, or indexCreator involved, since this type of querying of this field is impossible." );

                // not a keyword indexerField, nor stored. No hope at all. Impossible even with "filtering"
                return null;
            }
        }
        else if ( SearchType.SCORED.equals( type ) )
        {
            if ( JarFileContentsIndexCreator.FLD_CLASSNAMES.equals( indexerField ) )
            {
                String qpQuery = query.toLowerCase().replaceAll( "\\.", " " ).replaceAll( "/", " " );
                // tokenization should happen against the field!
                QueryParser qp = new QueryParser( Version.LUCENE_46, indexerField.getKey(), new NexusAnalyzer() );
                qp.setDefaultOperator( Operator.AND );
                return qp.parse( qpQuery );
            }
            else if ( indexerField.isKeyword() )
            {
                // no tokenization should happen against the field!
                if ( query.contains( "*" ) || query.contains( "?" ) )
                {
                    return new WildcardQuery( new Term( indexerField.getKey(), query ) );
                }
                else
                {
                    BooleanQuery bq = new BooleanQuery();

                    Term t = new Term( indexerField.getKey(), query );

                    bq.add( new TermQuery( t ), Occur.SHOULD );

                    PrefixQuery pq = new PrefixQuery( t );
                    pq.setBoost( 0.8f );

                    bq.add( pq, Occur.SHOULD );

                    return bq;
                }
            }
            else
            {
                // to save "original" query
                String qpQuery = query;

                // tokenization should happen against the field!
                QueryParser qp = new QueryParser( Version.LUCENE_46, indexerField.getKey(), new NexusAnalyzer() );
                qp.setDefaultOperator( Operator.AND );

                // small cheap trick
                // if a query is not "expert" (does not contain field:val kind of expression)
                // but it contains star and/or punctuation chars, example: "common-log*"
                // since Lucene does not support multi-terms WITH wildcards.
                // So, here, we "mimic" NexusAnalyzer (this should be fixed!)
                // but do this with PRESERVING original query!
                if ( qpQuery.matches( ".*(\\.|-|_|/).*" ) )
                {
                    qpQuery =
                        qpQuery.toLowerCase().replaceAll( "\\*", "X" ).replaceAll( "\\.|-|_|/", " " ).replaceAll( "X",
                            "*" ).replaceAll( " \\* ", "" ).replaceAll( "^\\* ", "" ).replaceAll( " \\*$", "" );
                }

                // "fix" it with trailing "*" if not there, but only if it not ends with a space
                if ( !qpQuery.endsWith( "*" ) && !qpQuery.endsWith( " " ) )
                {
                    qpQuery += "*";
                }

                try
                {
                    // qpQuery = "\"" + qpQuery + "\"";

                    BooleanQuery q1 = new BooleanQuery();

                    q1.add( qp.parse( qpQuery ), Occur.SHOULD );

                    if ( qpQuery.contains( " " ) )
                    {
                        q1.add( qp.parse( "\"" + qpQuery + "\"" ), Occur.SHOULD );
                    }

                    Query q2 = null;

                    int termCount = countTerms( indexerField, query );

                    // try with KW only if the processed query in qpQuery does not have spaces!
                    if ( !query.contains( " " ) && termCount > 1 )
                    {
                        // get the KW field
                        IndexerField keywordField = selectIndexerField( indexerField.getOntology(), SearchType.EXACT );

                        if ( keywordField.isKeyword() )
                        {
                            q2 = constructQuery( indexerField.getOntology(), keywordField, query, type );
                        }
                    }

                    if ( q2 == null )
                    {
                        return q1;
                    }
                    else
                    {
                        BooleanQuery bq = new BooleanQuery();

                        // trick with order
                        bq.add( q2, Occur.SHOULD );
                        bq.add( q1, Occur.SHOULD );

                        return bq;
                    }
                }
                catch ( ParseException e )
                {
                    // TODO: we are not falling back anymore to legacy!
                    throw e;

                    // getLogger().debug(
                    // "Query parsing with \"legacy\" method, we got ParseException from QueryParser: "
                    // + e.getMessage() );
                    //
                    // return legacyConstructQuery( indexerField.getKey(), query );
                }
            }
        }
        else
        {
            // what search type is this?
            return null;
        }
    }

    public Query legacyConstructQuery( String field, String query )
    {
        if ( query == null || query.length() == 0 )
        {
            getLogger().info( "Empty or null query for field:" + field );

            return null;
        }

        String q = query.toLowerCase();

        char h = query.charAt( 0 );

        if ( JarFileContentsIndexCreator.FLD_CLASSNAMES_KW.getKey().equals( field )
            || JarFileContentsIndexCreator.FLD_CLASSNAMES.getKey().equals( field ) )
        {
            q = q.replaceAll( "\\.", "/" );

            if ( h == '^' )
            {
                q = q.substring( 1 );

                if ( q.charAt( 0 ) != '/' )
                {
                    q = '/' + q;
                }
            }
            else if ( h != '*' )
            {
                q = "*/" + q;
            }
        }
        else
        {
            if ( h == '^' )
            {
                q = q.substring( 1 );
            }
            else if ( h != '*' )
            {
                q = "*" + q;
            }
        }

        int l = q.length() - 1;
        char c = q.charAt( l );
        if ( c == ' ' || c == '<' || c == '$' )
        {
            q = q.substring( 0, q.length() - 1 );
        }
        else if ( c != '*' )
        {
            q += "*";
        }

        int n = q.indexOf( '*' );
        if ( n == -1 )
        {
            return new TermQuery( new Term( field, q ) );
        }
        else if ( n > 0 && n == q.length() - 1 )
        {
            return new PrefixQuery( new Term( field, q.substring( 0, q.length() - 1 ) ) );
        }

        return new WildcardQuery( new Term( field, q ) );
    }

    // ==

    private NexusAnalyzer nexusAnalyzer = new NexusAnalyzer();

    protected int countTerms( final IndexerField indexerField, final String query )
    {
        try
        {
            TokenStream ts = nexusAnalyzer.tokenStream(indexerField.getKey(), new StringReader(query));
            ts.reset();

            int result = 0;

            while ( ts.incrementToken() )
            {
                result++;
            }
            
            ts.end();
            ts.close();

            return result;
        }
        catch ( IOException e )
        {
            // will not happen
            return 1;
        }
    }
}
