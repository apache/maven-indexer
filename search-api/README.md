Indexer Search API
==================

Defines a simple Search API usable to most common Maven related searches.

Example of GA search:

```java
    // obtain some backend instance
    SearchBackend backend = ...

    SearchRequest searchRequest = new SearchRequest( and( 
            fieldQuery( MAVEN.GROUP_ID, "org.apache.maven.plugins" ),
            fieldQuery( MAVEN.ARTIFACT_ID, "maven-clean-plugin" ) )
    );
    SearchResponse searchResponse = backend.search( searchRequest );
    process( searchResponse.getPage() ); // here consume the page data
    while ( searchResponse.getCurrentHits() > 0 ) // if ALL needed, page through it
    {
        searchResponse = backend.search( searchResponse.getSearchRequest().nextPage() );
        process( searchResponse.getPage() );
    }
```

