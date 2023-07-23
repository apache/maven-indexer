Indexer Search SMO Backend
==========================

Search API SMO (https://search.maven.org/) backend implementation, needs Java 8+.

By default uses GSON only, so is Java8, Android and GraalVM (untested) friendly.
The default transport is java.net.HttpUrlConnection, but is pluggable.

Examples:

```java
  SmoSearchBackendImpl backend = new SmoSearchBackendImpl(); // creates default SMO backend
```
