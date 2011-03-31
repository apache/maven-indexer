package org.apache.maven.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;

public class Mindexer14HitLimitTest
    extends AbstractNexusIndexerTest
{
    protected File repo = new File( getBasedir(), "target/repo/mindexer14" );

    @Override
    protected void prepareNexusIndexer( NexusIndexer nexusIndexer )
        throws Exception
    {
        repo.mkdirs();

        context =
            nexusIndexer.addIndexingContext( "mindexer14", "mindexer14", repo, indexDir, null, null, MIN_CREATORS );

        nexusIndexer.scan( context, false );
    }

    protected void createDummyAis( final String gid, final String aid, final int count )
        throws IOException
    {
        int version = 0;

        for ( int i = 0; i < count; i++ )
        {
            final ArtifactInfo ai = new ArtifactInfo( "mindexer14", gid, aid, String.valueOf( version++ ), null );

            final ArtifactContext ac = new ArtifactContext( null, null, null, ai, ai.calculateGav() );

            nexusIndexer.addArtifactToIndex( ac, context );
        }

    }

    public void testFlatSearchTotalHitsLie1k()
        throws Exception
    {
        createDummyAis( "org.test", "mindexer14", 1010 );

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.test" ) );

        FlatSearchRequest request = new FlatSearchRequest( query );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );

        assertEquals( 1010, response.getTotalHitsCount() );

        response.close();
    }

    public void testFlatSearchUnlimited()
        throws Exception
    {
        createDummyAis( "org.test", "mindexer14", 1010 );

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.test" ) );

        FlatSearchRequest request = new FlatSearchRequest( query );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );

        assertEquals( 1010, response.getTotalHitsCount() );
        assertEquals( 1010, response.getReturnedHitsCount() );
        assertEquals( 1010, response.getResults().size() );

        response.close();
    }

    public void testFlatSearchLimited()
        throws Exception
    {
        createDummyAis( "org.test", "mindexer14", 1010 );

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.test" ) );

        FlatSearchRequest request = new FlatSearchRequest( query );
        request.setCount( 234 );

        FlatSearchResponse response = nexusIndexer.searchFlat( request );

        assertEquals( 1010, response.getTotalHitsCount() );
        assertEquals( 234, response.getReturnedHitsCount() );
        assertEquals( 234, response.getResults().size() );

        response.close();
    }

    public void testGroupedSearchTotalHitsLie1k()
        throws Exception
    {
        createDummyAis( "org.test", "mindexer14", 1010 );

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.test" ) );

        GroupedSearchRequest request = new GroupedSearchRequest( query, new GAGrouping() );

        GroupedSearchResponse response = nexusIndexer.searchGrouped( request );

        assertEquals( 1010, response.getTotalHitsCount() );
        // in case of GroupedSearch, grouping is the one that defines count
        // we have 1010 dummies with same GA, and GA grouping, hence count is 1 just like the map has 1 entry
        assertEquals( 1, response.getReturnedHitsCount() );
        assertEquals( 1, response.getResults().size() );

        response.close();
    }

    public void testIteratorSearchTotalHitsLie1k()
        throws Exception
    {
        createDummyAis( "org.test", "mindexer14", 1010 );

        Query query = nexusIndexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.test" ) );

        IteratorSearchRequest request = new IteratorSearchRequest( query );

        IteratorSearchResponse response = nexusIndexer.searchIterator( request );

        assertEquals( 1010, response.getTotalHitsCount() );

        response.close();
    }
}
