Indexer Search Indexer Backend
==============================

Search API Indexer Core Backend implementation. As it uses Maven Indexer, needs Java 11+.

Examples:

```java
  // obtain Indexer instance
  Indexer indexer = ...
  IndexingContext indexingContext = ...

  SearchBackend backend = new IndexerCoreSearchBackendImpl( indexer, indexingContext );
```