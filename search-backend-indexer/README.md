Indexer Search Indexer Backend
==============================

Search API Indexer Core Backend implementation.

Examples:

```java
  // obtain Indexer instance
  Indexer indexer = ...
  IndexingContext indexingContext = ...

  SearchBackend backend = new IndexerCoreSearchBackendImpl( indexer, indexingContext );
```