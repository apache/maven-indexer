Indexer Core Notes
==================

MINDEXER-2 related Index core changes
-------------------------------------

New locking semantics introduced to be able to cope with multithreaded processing. This mostly affects server-like apps integrating the indexer, not as much IDEs.

IndexContext new methods:

* commit/rollback -- for commiting changes, but also reopening readers/searchers if appropriate.
* lock/unlock -- to perform shared locking, guaranteeing no reader/searcher reopen will occur.

IteratorSearchResult/IteratorResultSet new methods:

* both become Closeable. If you do NOT consume all of iterator (when automatic cleanup happens), you have to explicitly call result.close() to release locks!

Others:

* warmUp of readers/searchers added, currently a naive implementation, later we can refine it
* smaller fixes


Notes: if an app integrating Indexer Core did NOT tamper with indexingContexts directly (by using one of it's ctx.getIndexWriter(),
ctx.getIndexReader() and ctx.getIndexSearcher()), it should almost not notice anything. The only change needed is to explicitly close
non-consumed IteratorSearchResults. Otherwise, you have to adapt and manually fiddle with locking of contexts, or use proper API calls. In future, these
methods and direct IndexingContext use within integrating applications will be highly discouraged!